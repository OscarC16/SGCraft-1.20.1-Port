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
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import gcewing.sgcraft.SGState;
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
    private static final ResourceLocation EVENT_HORIZON_TEXTURE = new ResourceLocation(SGCraft.MODID,
            "textures/tileentity/eventhorizon.png");
    private static final ResourceLocation IRIS_TEXTURE = new ResourceLocation(SGCraft.MODID,
            "textures/tileentity/iris.png");

    private static final int NUM_IRIS_BLADES = 12;

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

        // Render outer ring
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(STARGATE_TEXTURE));
        renderRing(vc, poseStack.last().pose(), poseStack.last().normal(), combinedLight,
                RING_MID_RADIUS - RING_OVERLAP, RING_OUTER_RADIUS, true,
                RING_Z_OFFSET);

        // Render inner ring (with rotation animation)
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees((float) te.ringAngle));
        renderRing(vc, poseStack.last().pose(), poseStack.last().normal(), combinedLight, RING_INNER_RADIUS,
                RING_MID_RADIUS, false, 0);
        poseStack.popPose();

        // Render chevrons
        renderChevrons(te, poseStack, buffer, combinedLight);

        // Render Event Horizon
        if (te.state == SGState.Transient || te.state == SGState.Connected
                || te.state == SGState.Disconnecting) {
            renderEventHorizon(te, poseStack, buffer, combinedLight, false); // Front
            renderEventHorizon(te, poseStack, buffer, combinedLight, true);  // Back
        }

        if (te.hasIrisUpgrade) {
            renderIris(te, poseStack, buffer, combinedLight);
        }

        poseStack.popPose();
    }

    /**
     * Renders block models for camouflage slots if they contain block items.
     * These are rendered world-aligned (no horizontal rotation) for texture
     * consistency.
     */
    private void renderCamouflage(SGBaseBlockEntity te, PoseStack poseStack, MultiBufferSource buffer, int light,
            int overlay, Direction facing) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();

        for (int i = 0; i < 5; i++) {
            ItemStack stack = te.inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem blockItem))
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
                        true, random, seed, overlay, ModelData.EMPTY, rt);
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

        // Cache constants for inner symbols
        double symWidth = RING_SYMBOL_SEGMENT_WIDTH;
        double symLen = RING_SYMBOL_TEXTURE_LENGTH;
        double symHeight = RING_SYMBOL_TEXTURE_HEIGHT;

        for (int i = 0; i < NUM_RING_SEGMENTS; i++) {
            double c1 = COS[i], s1 = SIN[i];
            double c2 = COS[i + 1], s2 = SIN[i + 1];

            // Sides and Back
            u0 = sideU0;
            v0 = sideV0;

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
            u0 = frontU0;
            v0 = frontV0;
            if (isOuter) {
                quad(vc, pose, normalMat, light, 0, 0, 1,
                        r1 * c1, r1 * s1, z, 16, 16,
                        r2 * c1, r2 * s1, z, 16, 0,
                        r2 * c2, r2 * s2, z, 0, 0,
                        r1 * c2, r1 * s2, z, 0, 16);
            } else {
                // Inner ring front — glyph symbols
                double u = symLen - (i + 1) * symWidth;
                quad(vc, pose, normalMat, light, 0, 0, 1,
                        r1 * c1, r1 * s1, z, u + symWidth, symHeight,
                        r2 * c1, r2 * s1, z, u + symWidth, 0,
                        r2 * c2, r2 * s2, z, u, 0,
                        r1 * c2, r1 * s2, z, u, symHeight);
            }
        }
    }

    /**
     * Renders 7 chevrons around the ring at their canonical positions.
     */
    private void renderChevrons(SGBaseBlockEntity te, PoseStack poseStack, MultiBufferSource buffer, int light) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(STARGATE_TEXTURE));

        float a = 40f; // 40 degrees spacing allows 9 chevrons to cover 360 degrees of the ring
        boolean hasUpgrade = te.getNumChevrons() == 9;

        for (int i = 0; i < 9; i++) {
            // Hide bottom chevrons if not upgraded (Indices 0 and 8)
            if ((i == 0 || i == 8) && !hasUpgrade)
                continue;

            float engageAmount = te.chevronEngageAmount[i];

            poseStack.pushPose();
            // Rotate to chevron position: 90 degrees (top) minus offset from top
            // Indices: 4=90(top), 3=130, 2=170, 1=210, 0=250(bottom-left)
            // 5=50, 6=10, 7=-30, 8=-70(bottom-right)
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 - (i - 4) * a));

            // Move chevron inward based on engage amount (approx 0.125 blocks)
            double displacement = engageAmount * 0.125;
            poseStack.translate(-displacement, 0, 0);

            Matrix4f pose = poseStack.last().pose();
            Matrix3f normalMat = poseStack.last().normal();

            renderChevron(vc, pose, normalMat, light, engageAmount);

            poseStack.popPose();
        }
    }

    /**
     * Renders a single chevron at the current transformation — a V-shaped 3D
     * bracket.
     */
    private void renderChevron(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light, float engageAmount) {
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

        // Chevron lit center (emissive based on engage amount)
        selectTile(CHEVRON_LIT_TEXTURE_INDEX);
        int r = (int) (100 + engageAmount * 155);
        int g = (int) (70 + engageAmount * 130);
        int b = (int) (50 + engageAmount * 100);

        int litLevel = engageAmount > 0.5f ? 0xF000F0 : light;

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
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float r, float g, float b, float a, int light,
                int overlay, boolean applyLighting) {
            parent.putBulkData(pose, quad, r * factor, g * factor, b * factor, a, light, overlay, applyLighting);
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b,
                float a, int[] lightmap, int overlay, boolean applyLighting) {
            float rf = r * factor;
            float gf = g * factor;
            float bf = b * factor;
            parent.putBulkData(pose, quad, brightness, rf, gf, bf, a, lightmap, overlay, applyLighting);
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            return parent.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            return parent.color((int) (r * factor), (int) (g * factor), (int) (b * factor), a);
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            return parent.uv(u, v);
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            return parent.overlayCoords(u, v);
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            return parent.uv2(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return parent.normal(x, y, z);
        }

        @Override
        public void endVertex() {
            parent.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            parent.defaultColor((int) (r * factor), (int) (g * factor), (int) (b * factor), a);
        }

        @Override
        public void unsetDefaultColor() {
            parent.unsetDefaultColor();
        }
    }

    private void renderEventHorizon(SGBaseBlockEntity te, PoseStack poseStack, MultiBufferSource buffer,
            int combinedLight, boolean back) {
        poseStack.pushPose();
        if (back) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }
        VertexConsumer vc = buffer.getBuffer(RenderType.entitySolid(EVENT_HORIZON_TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        final double rclip = 2.5;
        final double ehBandWidth = RING_INNER_RADIUS / SGBaseBlockEntity.ehGridRadialSize;
        double[][] grid = te.getEventHorizonGrid()[0];
        boolean flat = te.irisPhase < 1.0f;

        // Quads for the rings > 0
        for (int i = 1; i < SGBaseBlockEntity.ehGridRadialSize; i++) {
            for (int j = 0; j < SGBaseBlockEntity.ehGridPolarSize; j++) {
                ehVertexQuad(vc, pose, normal, grid, i, j, rclip, ehBandWidth, combinedLight, flat, back);
                ehVertexQuad(vc, pose, normal, grid, i + 1, j, rclip, ehBandWidth, combinedLight, flat, back);
                ehVertexQuad(vc, pose, normal, grid, i + 1, j + 1, rclip, ehBandWidth, combinedLight, flat, back);
                ehVertexQuad(vc, pose, normal, grid, i, j + 1, rclip, ehBandWidth, combinedLight, flat, back);
            }
        }

        // Center Fan
        double zCenter = flat ? 0 : ehClip(grid[1][0], 0, rclip);
        if (back && !flat) zCenter = Math.min(zCenter, 0.1);
        for (int j = 0; j < SGBaseBlockEntity.ehGridPolarSize; j++) {
            ehVertexQuad(vc, pose, normal, grid, 1, j, rclip, ehBandWidth, combinedLight, flat, back);
            ehVertexQuad(vc, pose, normal, grid, 1, j + 1, rclip, ehBandWidth, combinedLight, flat, back);
            for (int k = 0; k < 2; k++) {
                vc.vertex(pose, 0, 0, (float) zCenter)
                        .color(255, 255, 255, 255)
                        .uv(0, 0)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(combinedLight)
                        .normal(normal, 0, 0, 1)
                        .endVertex();
            }
        }
        poseStack.popPose();
    }

    private void ehVertexQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal, double[][] grid, int i, int j,
            double rclip, double ehBandWidth, int combinedLight, boolean flat, boolean back) {
        double r = i * ehBandWidth;
        // Normalize j index to wrap around SIN/COS arrays
        int jj = j % SGBaseBlockEntity.ehGridPolarSize;
        if (jj < 0)
            jj += SGBaseBlockEntity.ehGridPolarSize;

        double x = r * COS[jj];
        double y = r * SIN[jj];
        double z = flat ? 0 : ehClip(grid[j + 1][i], r, rclip);
        
        if (back && !flat) {
            z = Math.min(z, 0.1); // Allow ripples but suppress Kawoosh expansion
        }

        vc.vertex(pose, (float) x, (float) y, (float) z)
                .color(255, 255, 255, 255)
                // Reverting to raw x, y for UVs to restore original texture tiling/scale
                .uv((float) x, (float) y)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(combinedLight)
                .normal(normal, 0, 0, 1)
                .endVertex();
    }

    private double ehClip(double z, double r, double rclip) {
        if (r >= rclip) {
            z = Math.min(z, 0);
        }
        return z;
    }

    private void renderIris(SGBaseBlockEntity te, PoseStack poseStack, MultiBufferSource buffer, int light) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutout(IRIS_TEXTURE));

        // 0.0 is closed, 1.0 is open. original a was 0.8 * aperture
        // Animation Easing:
        // Opening (0->1): Slow start, fast end
        // Closing (1->0): Fast start, slow end
        float t = te.irisPhase;
        double aperture = t * t;
        for (int i = 0; i < NUM_IRIS_BLADES; i++) {
            renderIrisBlade(vc, poseStack, aperture, light, i, NUM_IRIS_BLADES);
        }
    }

    private void renderIrisBlade(VertexConsumer vc, PoseStack poseStack, double aperture, int light, int i, int n) {
        double angleStep = 360.0 / n;
        double rad = Math.PI / 180.0;

        // Points on the ring (Fixed Radius 2.3)
        float p2x = (float) (2.3 * Math.cos(rad * (angleStep * i)));
        float p2y = (float) (2.3 * Math.sin(rad * (angleStep * i)));
        float p3x = (float) (2.3 * Math.cos(rad * (angleStep * (i + 0.5))));
        float p3y = (float) (2.3 * Math.sin(rad * (angleStep * (i + 0.5))));
        float p4x = (float) (2.3 * Math.cos(rad * (angleStep * (i + 1))));
        float p4y = (float) (2.3 * Math.sin(rad * (angleStep * (i + 1))));

        // Matrix Sliding Math: Solve for P1 such that it lies on the segment [P1_next,
        // P2_next]
        // k is the "openness" factor. We scale aperture to reach the desired radius
        // (2.1)
        double k = aperture * (2.1 / 2.3);
        double cosA = Math.cos(rad * angleStep);
        double sinA = Math.sin(rad * angleStep);

        // M = I - (1-k)R
        double mC = (1 - (1 - k) * cosA);
        double mS = (1 - k) * sinA;
        double mDet = mC * mC + mS * mS;

        // Inverse matrix to find P1 globally
        float p1x = (float) ((k / mDet) * (mC * p4x - mS * p4y));
        float p1y = (float) ((k / mDet) * (mS * p4x + mC * p4y));

        // Render parameters
        double z0 = 0.01;
        double z1 = 0.1;
        Matrix4f pose = poseStack.last().pose();
        Matrix3f normal = poseStack.last().normal();

        // Front Face (CCW) - Pure white for texture
        vertex(vc, pose, normal, light, 0, 0, 1, 255, 255, 255, p1x, p1y, z1, 0, 0);
        vertex(vc, pose, normal, light, 0, 0, 1, 255, 255, 255, p2x, p2y, z0, 0, 25);
        vertex(vc, pose, normal, light, 0, 0, 1, 255, 255, 255, p3x, p3y, z0, 0, 0);
        vertex(vc, pose, normal, light, 0, 0, 1, 255, 255, 255, p4x, p4y, z0, 0, 25);

        // Back Face (CW from front, CCW from back)
        vertex(vc, pose, normal, light, 0, 0, -1, 255, 255, 255, p1x, p1y, z1, 0, 0);
        vertex(vc, pose, normal, light, 0, 0, -1, 255, 255, 255, p4x, p4y, z0, 0, 25);
        vertex(vc, pose, normal, light, 0, 0, -1, 255, 255, 255, p3x, p3y, z0, 0, 0);
        vertex(vc, pose, normal, light, 0, 0, -1, 255, 255, 255, p2x, p2y, z0, 0, 25);
    }
}
