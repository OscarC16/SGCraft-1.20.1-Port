package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.SGBaseBlock;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders the full 3D Stargate ring when the multiblock structure is merged.
 * Ported from SGBaseTERenderer.java in the original mod, converting direct
 * OpenGL calls
 * to the modern PoseStack/VertexConsumer system.
 *
 * The Stargate is rendered as procedural geometry:
 * - Outer ring: 32 segments, radius 2.25 to 2.5
 * - Inner ring: 32 segments with glyph symbols, radius 2.0 to 2.25
 * - Chevrons: 7 or 9 3D pieces positioned around the ring
 */
public class SGBaseBlockEntityRenderer implements BlockEntityRenderer<SGBaseBlockEntity> {

    private static final ResourceLocation STARGATE_TEXTURE = new ResourceLocation(SGCraft.MODID,
            "textures/tileentity/stargate.png");

    // Ring geometry constants (from original)
    static final int NUM_RING_SEGMENTS = 32;
    static final double RING_INNER_RADIUS = 2.0;
    static final double RING_MID_RADIUS = 2.25;
    static final double RING_OUTER_RADIUS = 2.5;
    static final double RING_DEPTH = 0.5;
    static final double RING_OVERLAP = 1.0 / 64.0;
    static final double RING_Z_OFFSET = 0.0001;

    // Chevron geometry constants
    static final double CHEVRON_INNER_RADIUS = 2.25;
    static final double CHEVRON_OUTER_RADIUS = RING_OUTER_RADIUS + 1.0 / 16.0;
    static final double CHEVRON_WIDTH = (CHEVRON_OUTER_RADIUS - CHEVRON_INNER_RADIUS) * 1.5;
    static final double CHEVRON_DEPTH = 0.125;
    static final double CHEVRON_BORDER_WIDTH = CHEVRON_WIDTH / 6;

    // Texture atlas layout: 32 tiles wide × 2 tiles high, each tile 16×16 pixels
    static final int TEXTURE_TILES_WIDE = 32;
    static final int TEXTURE_TILES_HIGH = 2;
    static final double TEXTURE_SCALE_U = 1.0 / (TEXTURE_TILES_WIDE * 16);
    static final double TEXTURE_SCALE_V = 1.0 / (TEXTURE_TILES_HIGH * 16);

    // Tile indices in the atlas
    static final int RING_FACE_TEXTURE_INDEX = 0x01; // Front of outer ring
    static final int RING_TEXTURE_INDEX = 0x00; // Sides/edges of ring
    static final int RING_SYMBOL_TEXTURE_INDEX = 0x20; // Inner ring glyphs (row 2, col 0)
    static final int CHEVRON_TEXTURE_INDEX = 0x03; // Chevron body
    static final int CHEVRON_LIT_TEXTURE_INDEX = 0x02; // Chevron lit center

    // Symbol texture dimensions
    static final double RING_SYMBOL_TEXTURE_LENGTH = 512.0;
    static final double RING_SYMBOL_TEXTURE_HEIGHT = 16.0;
    static final double RING_SYMBOL_SEGMENT_WIDTH = RING_SYMBOL_TEXTURE_LENGTH / NUM_RING_SEGMENTS;

    // Precomputed sin/cos tables for ring segments
    static final double[] SIN = new double[NUM_RING_SEGMENTS + 1];
    static final double[] COS = new double[NUM_RING_SEGMENTS + 1];


    static {
        for (int i = 0; i <= NUM_RING_SEGMENTS; i++) {
            double a = 2 * Math.PI * i / NUM_RING_SEGMENTS;
            SIN[i] = Math.sin(a);
            COS[i] = Math.cos(a);
        }
    }

    // Current tile origin for UV mapping
    private double u0, v0;

    public SGBaseBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SGBaseBlockEntity te, float partialTicks, PoseStack poseStack, MultiBufferSource buffer,
            int combinedLight, int combinedOverlay) {
        if (!te.isMerged)
            return;

        BlockState state = te.getBlockState();
        if (!(state.getBlock() instanceof SGBaseBlock))
            return;

        Direction facing = state.getValue(SGBaseBlock.FACING);

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();

        poseStack.pushPose();

        // Move to center of stargate (base block + 2.5 blocks up, centered on X/Z)
        poseStack.translate(0.5, 2.5, 0.5);

        // Render camouflage blocks in the base row (World Aligned)
        // We call this BEFORE any Stargate rotation to keep block textures
        // world-aligned
        renderCamouflage(te, poseStack, buffer, combinedLight, combinedOverlay, facing);

        // Rotate to match the facing direction of the base block
        // The ring plane is perpendicular to the facing direction
        // Fixed: Adjusted rotation angles to align ring front with base block front
        float rotation = switch (facing) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Get updated matrices after transformation
        pose = poseStack.last().pose();
        normalMat = poseStack.last().normal();

        // Render outer ring
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(STARGATE_TEXTURE));
        renderRing(vc, pose, normalMat, combinedLight, RING_MID_RADIUS - RING_OVERLAP, RING_OUTER_RADIUS, true,
                RING_Z_OFFSET);

        // Render inner ring (static for now — no rotation animation yet)
        renderRing(vc, pose, normalMat, combinedLight, RING_INNER_RADIUS, RING_MID_RADIUS, false, 0);

        // Render chevrons
        renderChevrons(te, poseStack, buffer, combinedLight);

        poseStack.popPose();
    }

    /**
     * Renders block models for camouflage slots if they contain block items.
     * These are rendered world-aligned (no horizontal rotation) for texture
     * consistency.
     */
    private void renderCamouflage(SGBaseBlockEntity te, PoseStack poseStack, MultiBufferSource buffer, int light,
            int overlay, Direction facing) {
        net.minecraft.client.renderer.block.BlockRenderDispatcher dispatcher = net.minecraft.client.Minecraft
                .getInstance().getBlockRenderer();

        for (int i = 0; i < 5; i++) {
            net.minecraft.world.item.ItemStack stack = te.inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem))
                continue;

            poseStack.pushPose();

            // Calculate world-relative offset manually based on facing
            // Base block center is at (0.5, 2.5, 0.5) from the current PoseStack origin
            // We want to move to the corner of each of the 5 base blocks
            int lateralOffset = i - 2;
            // Calculate world position for lighting and model offset
            int idx = 0, idz = 0;
            switch (facing) {
                case NORTH -> idx = -lateralOffset;
                case SOUTH -> idx = lateralOffset;
                case WEST -> idz = -lateralOffset;
                case EAST -> idz = lateralOffset;
                default -> {
                }
            }
            double dx = idx;
            double dz = idz;
            BlockPos worldPos = te.getBlockPos().offset(idx, 0, idz);

            poseStack.translate(dx - 0.5, -2.5, dz - 0.5);

            BlockState state = blockItem.getBlock().defaultBlockState();
            BakedModel model = dispatcher.getBlockModel(state);
            long seed = state.getSeed(worldPos);
            RandomSource random = RandomSource.create(seed);
            
            for (net.minecraft.client.renderer.RenderType rt : model.getRenderTypes(state, random, ModelData.EMPTY)) {
                VertexConsumer vc = buffer.getBuffer(rt);
                TintedVertexConsumer tintedVC = new TintedVertexConsumer(vc, 1.0f);
                
                dispatcher.getModelRenderer().tesselateBlock(
                    te.getLevel(), model, state, worldPos, poseStack, tintedVC,
                    true, random, seed, overlay, ModelData.EMPTY, rt
                );
            }

            poseStack.popPose();
        }
    }

    /**
     * Renders a ring section (either inner or outer) as 32 segments of quads.
     */
    private void renderRing(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light,
            double r1, double r2, boolean isOuter, double dz) {
        double z = RING_DEPTH / 2 + dz;

        // Sides and Back use the same tile, select once
        selectTile(RING_TEXTURE_INDEX);
        double sideU0 = u0, sideV0 = v0;

        // Pre-cache front face tile
        int frontTile = isOuter ? RING_FACE_TEXTURE_INDEX : RING_SYMBOL_TEXTURE_INDEX;
        selectTile(frontTile);
        double frontU0 = u0, frontV0 = v0;

        for (int i = 0; i < NUM_RING_SEGMENTS; i++) {
            double c1 = COS[i], s1 = SIN[i];
            double c2 = COS[i + 1], s2 = SIN[i + 1];

            // Sides and Back
            u0 = sideU0; v0 = sideV0;
            
            // Outer surface
            if (isOuter) {
                quad(vc, pose, normalMat, light, (float) c1, (float) s1, 0,
                        r2 * c1, r2 * s1, z, 0, 0,
                        r2 * c1, r2 * s1, -z, 0, 16,
                        r2 * c2, r2 * s2, -z, 16, 16,
                        r2 * c2, r2 * s2, z, 16, 0);
            }

            // Inner surface
            if (!isOuter) {
                quad(vc, pose, normalMat, light, (float) -c1, (float) -s1, 0,
                        r1 * c1, r1 * s1, -z, 0, 0,
                        r1 * c1, r1 * s1, z, 0, 16,
                        r1 * c2, r1 * s2, z, 16, 16,
                        r1 * c2, r1 * s2, -z, 16, 0);
            }

            // Back face
            quad(vc, pose, normalMat, light, 0, 0, -1,
                    r1 * c1, r1 * s1, -z, 0, 16,
                    r1 * c2, r1 * s2, -z, 16, 16,
                    r2 * c2, r2 * s2, -z, 16, 0,
                    r2 * c1, r2 * s1, -z, 0, 0);

            // Front face
            u0 = frontU0; v0 = frontV0;
            if (isOuter) {
                quad(vc, pose, normalMat, light, 0, 0, 1,
                        r1 * c1, r1 * s1, z, 16, 16,
                        r2 * c1, r2 * s1, z, 16, 0,
                        r2 * c2, r2 * s2, z, 0, 0,
                        r1 * c2, r1 * s2, z, 0, 16);
            } else {
                // Inner ring front — glyph symbols
                double u = RING_SYMBOL_TEXTURE_LENGTH - (i + 1) * RING_SYMBOL_SEGMENT_WIDTH;
                double du = RING_SYMBOL_SEGMENT_WIDTH;
                double dv = RING_SYMBOL_TEXTURE_HEIGHT;
                quad(vc, pose, normalMat, light, 0, 0, 1,
                        r1 * c1, r1 * s1, z, u + du, dv,
                        r2 * c1, r2 * s1, z, u + du, 0,
                        r2 * c2, r2 * s2, z, u, 0,
                        r1 * c2, r1 * s2, z, u, dv);
            }
        }
    }

    /**
     * Renders 7 chevrons around the ring at their canonical positions.
     */
    private void renderChevrons(SGBaseBlockEntity te, PoseStack poseStack, MultiBufferSource buffer, int light) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(STARGATE_TEXTURE));

        int numChevrons = te.getNumChevrons();
        int i0 = numChevrons > 7 ? 0 : 1;
        float a = 40f; // 40 degrees spacing allows 9 chevrons to cover 320 degrees of the ring

        for (int i = i0; i < i0 + numChevrons; i++) {
            int chevronIndex = i - i0;
            // Basic sequential engagement logic
            boolean engaged = chevronIndex < te.numEngagedChevrons;

            poseStack.pushPose();
            // Rotate to chevron position: 90 degrees minus offset from top-dead-center
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 - (i - 4) * a));

            Matrix4f pose = poseStack.last().pose();
            Matrix3f normalMat = poseStack.last().normal();

            renderChevron(vc, pose, normalMat, light, engaged);

            poseStack.popPose();
        }
    }

    /**
     * Renders a single chevron at the current transformation — a V-shaped 3D
     * bracket.
     */
    private void renderChevron(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light, boolean engaged) {
        double r1 = CHEVRON_INNER_RADIUS;
        double r2 = CHEVRON_OUTER_RADIUS;
        double z2 = RING_DEPTH / 2;
        double z1 = z2 + CHEVRON_DEPTH;
        double w1 = CHEVRON_BORDER_WIDTH;
        double w2 = w1 * 1.25;
        double x1 = r1, y1 = CHEVRON_WIDTH / 4;
        double x2 = r2, y2 = CHEVRON_WIDTH / 2;

        // Chevron body (the frame)
        selectTile(CHEVRON_TEXTURE_INDEX);

        // Face 1 (right arm of chevron)
        quad(vc, pose, normalMat, light, 0, 0, 1,
                x2, y2, z1, 0, 2,
                x1, y1, z1, 0, 16,
                x1 + w1, y1 - w1, z1, 4, 12,
                x2, y2 - w2, z1, 4, 2);

        // Side 1
        quad(vc, pose, normalMat, light, 0, 1, 0,
                x2, y2, z1, 0, 0,
                x2, y2, z2, 0, 4,
                x1, y1, z2, 16, 4,
                x1, y1, z1, 16, 0);

        // End 1
        quad(vc, pose, normalMat, light, 1, 0, 0,
                x2, y2, z1, 16, 0,
                x2, y2 - w2, z1, 12, 0,
                x2, y2 - w2, z2, 12, 4,
                x2, y2, z2, 16, 4);

        // Face 2 (inner arm)
        quad(vc, pose, normalMat, light, 0, 0, 1,
                x1 + w1, y1 - w1, z1, 4, 12,
                x1, y1, z1, 0, 16,
                x1, -y1, z1, 16, 16,
                x1 + w1, -y1 + w1, z1, 12, 12);

        // Side 2
        quad(vc, pose, normalMat, light, -1, 0, 0,
                x1, y1, z1, 0, 0,
                x1, y1, z2, 0, 4,
                x1, -y1, z2, 16, 4,
                x1, -y1, z1, 16, 0);

        // Face 3 (left arm of chevron)
        quad(vc, pose, normalMat, light, 0, 0, 1,
                x2, -y2 + w2, z1, 12, 0,
                x1 + w1, -y1 + w1, z1, 12, 12,
                x1, -y1, z1, 16, 16,
                x2, -y2, z1, 16, 0);

        // Side 3
        quad(vc, pose, normalMat, light, 0, -1, 0,
                x1, -y1, z1, 0, 0,
                x1, -y1, z2, 0, 4,
                x2, -y2, z2, 16, 4,
                x2, -y2, z1, 16, 0);

        // End 3
        quad(vc, pose, normalMat, light, 1, 0, 0,
                x2, -y2, z1, 0, 0,
                x2, -y2, z2, 0, 4,
                x2, -y2 + w2, z2, 4, 4,
                x2, -y2 + w2, z1, 4, 0);

        // Back face
        quad(vc, pose, normalMat, light, 0, 0, -1,
                x2, -y2, z2, 0, 0,
                x1, -y1, z2, 0, 16,
                x1, y1, z2, 16, 16,
                x2, y2, z2, 16, 0);

        // Chevron lit center (emissive when engaged)
        selectTile(CHEVRON_LIT_TEXTURE_INDEX);
        int r = engaged ? 255 : 100;
        int g = engaged ? 200 : 70; // Slightly orange tint like original
        int b = engaged ? 150 : 50;

        int litLevel = engaged ? 0xF000F0 : light;

        // Face 4 top half
        quadColor(vc, pose, normalMat, litLevel, 0, 0, 1, r, g, b,
                x2, y2 - w2, z1, 0, 4,
                x1 + w1, y1 - w1, z1, 4, 16,
                x1 + w1, 0, z1, 8, 16,
                x2, 0, z1, 8, 4);

        // Face 4 bottom half
        quadColor(vc, pose, normalMat, litLevel, 0, 0, 1, r, g, b,
                x2, 0, z1, 8, 4,
                x1 + w1, 0, z1, 8, 16,
                x1 + w1, -y1 + w1, z1, 12, 16,
                x2, -y2 + w2, z1, 16, 4);

        // End 4
        quadColor(vc, pose, normalMat, litLevel, 1, 0, 0, r, g, b,
                x2, y2 - w2, z2, 0, 0,
                x2, y2 - w2, z1, 0, 4,
                x2, -y2 + w2, z1, 16, 4,
                x2, -y2 + w2, z2, 16, 0);
    }

    // --- Texture tile selection ---

    private void selectTile(int index) {
        u0 = (index % TEXTURE_TILES_WIDE) * (TEXTURE_SCALE_U * 16);
        v0 = (index / TEXTURE_TILES_WIDE) * (TEXTURE_SCALE_V * 16);
    }

    // --- Quad emission helpers ---

    /**
     * Emit a quad (4 vertices) with the given normal and per-vertex positions/UVs.
     * UV coordinates are in tile-local pixels, transformed by the current tile
     * origin.
     */
    private void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light,
            float nx, float ny, float nz,
            double x0, double y0, double z0, double u0v, double v0v,
            double x1, double y1, double z1, double u1v, double v1v,
            double x2, double y2, double z2, double u2v, double v2v,
            double x3, double y3, double z3, double u3v, double v3v) {
        quadColor(vc, pose, normalMat, light, nx, ny, nz, 255, 255, 255,
                x0, y0, z0, u0v, v0v,
                x1, y1, z1, u1v, v1v,
                x2, y2, z2, u2v, v2v,
                x3, y3, z3, u3v, v3v);
    }

    private void quadColor(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light,
            float nx, float ny, float nz, int r, int g, int b,
            double x0, double y0, double z0, double u0v, double v0v,
            double x1, double y1, double z1, double u1v, double v1v,
            double x2, double y2, double z2, double u2v, double v2v,
            double x3, double y3, double z3, double u3v, double v3v) {
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x0, y0, z0, u0v, v0v);
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x1, y1, z1, u1v, v1v);
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x2, y2, z2, u2v, v2v);
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x3, y3, z3, u3v, v3v);
    }

    private void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light,
            float nx, float ny, float nz, int r, int g, int b,
            double x, double y, double z, double u, double v) {
        float fu = (float) (u0 + u * TEXTURE_SCALE_U);
        float fv = (float) (v0 + v * TEXTURE_SCALE_V);

        vc.vertex(pose, (float) x, (float) y, (float) z)
                .color(r, g, b, 255)
                .uv(fu, fv)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(light)
                .normal(normalMat, nx, ny, nz)
                .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(SGBaseBlockEntity blockEntity) {
        return true; // The Stargate extends well beyond one block
    }

    @Override
    public int getViewDistance() {
        return 256; // Visible from far away
    }

    /**
     * A VertexConsumer wrapper that applies a brightness factor to all colors.
     * This allows us to use the standard Minecraft renderer (with AO) while 
     * fine-tuning the tone to match the environment.
     */
    private static class TintedVertexConsumer implements VertexConsumer {
        private final VertexConsumer parent;
        private final float factor;

        public TintedVertexConsumer(VertexConsumer parent, float factor) {
            this.parent = parent;
            this.factor = factor;
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light, int overlay, boolean applyLighting) {
            parent.putBulkData(pose, quad, r * factor, g * factor, b * factor, a, light, overlay, applyLighting);
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, float a, int[] lightmap, int overlay, boolean applyLighting) {
            float rf = r * factor;
            float gf = g * factor;
            float bf = b * factor;
            parent.putBulkData(pose, quad, brightness, rf, gf, bf, a, lightmap, overlay, applyLighting);
        }

        @Override public VertexConsumer vertex(double x, double y, double z) { return parent.vertex(x, y, z); }
        @Override public VertexConsumer color(int r, int g, int b, int a) { return parent.color((int)(r * factor), (int)(g * factor), (int)(b * factor), a); }
        @Override public VertexConsumer uv(float u, float v) { return parent.uv(u, v); }
        @Override public VertexConsumer overlayCoords(int u, int v) { return parent.overlayCoords(u, v); }
        @Override public VertexConsumer uv2(int u, int v) { return parent.uv2(u, v); }
        @Override public VertexConsumer normal(float x, float y, float z) { return parent.normal(x, y, z); }
        @Override public void endVertex() { parent.endVertex(); }
        @Override public void defaultColor(int r, int g, int b, int a) { parent.defaultColor((int)(r * factor), (int)(g * factor), (int)(b * factor), a); }
        @Override public void unsetDefaultColor() { parent.unsetDefaultColor(); }
    }
}
