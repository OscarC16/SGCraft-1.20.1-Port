package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.DHDBlock;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.block.entity.DHDBlockEntity.DHDState;
import gcewing.sgcraft.client.model.SmegModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class DHDBlockEntityRenderer implements BlockEntityRenderer<DHDBlockEntity> {

    // Texture files for indices 0 and 1 (top dome and side panels - used as full textures)
    private static final ResourceLocation TEX_DHD_TOP = new ResourceLocation(SGCraft.MODID, "textures/block/dhd_top.png");
    private static final ResourceLocation TEX_DHD_SIDE = new ResourceLocation(SGCraft.MODID, "textures/block/dhd_side.png");
    // Detail texture (2x2 tiled) for indices 2 (side detail) and 3 (button)
    private static final ResourceLocation TEX_DHD_DETAIL = new ResourceLocation(SGCraft.MODID, "textures/block/dhd/dhd_detail.png");

    // Full texture files array (indices 0 and 1 use their own textures, 2 and 3 share the detail atlas)
    private static final ResourceLocation[] TEXTURE_FILES = {
        TEX_DHD_TOP,    // 0 = dome surface
        TEX_DHD_SIDE,   // 1 = pedestal sides
        TEX_DHD_DETAIL, // 2 = side panel detail (uses tile 1,1 = bottom-right = blue)
        TEX_DHD_DETAIL  // 3 = center button (tile depends on DHD state)
    };

    // Quadrant UV offsets for the 2x2 tiled detail texture
    // Each tile covers 0.5 of UV space. Format: {uOffset, vOffset}
    //
    // Layout of dhd_detail.png:
    // ┌────────┬────────┐
    // │ (0,0)  │ (1,0)  │
    // │ Orange │ Gray   │
    // │ dark   │        │
    // ├────────┼────────┤
    // │ (0,1)  │ (1,1)  │
    // │ Orange │ Blue   │
    // │ bright │        │
    // └────────┴────────┘

    // Side detail panels always use tile (1,1) = bottom-right = blue quadrant
    private static final float DETAIL_U_OFFSET = 0.5f;
    private static final float DETAIL_V_OFFSET = 0.5f;

    // Button quadrant offsets per DHD state:
    // IDLE (unlinked)  → tile(1,0) = top-right (gray)
    // LINKED           → tile(0,0) = top-left (dark orange)
    // ACTIVE           → tile(0,1) = bottom-left (bright orange)
    private static final float[][] BUTTON_TILE_OFFSETS = {
        {0.0f, 0.0f}, // IDLE   → top-left (gray)
        {0.5f, 0.0f}, // LINKED → top-right (dark orange)
        {0.0f, 0.5f}, // ACTIVE → bottom-left (bright orange)
    };

    private SmegModel model;
    private boolean modelLoaded = false;

    public DHDBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    private void loadModel() {
        if (!modelLoaded) {
            try {
                ResourceLocation modelLoc = new ResourceLocation(SGCraft.MODID, "models/dhd.smeg");
                model = SmegModel.fromResource(modelLoc);
                modelLoaded = true;
            } catch (Exception e) {
                System.err.println("[SGCraft] Failed to load DHD model: " + e.getMessage());
                e.printStackTrace();
                modelLoaded = true;
            }
        }
    }

    @Override
    public void render(DHDBlockEntity dhd, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        loadModel();
        if (model == null || model.faces == null) return;

        BlockState state = dhd.getBlockState();
        if (!(state.getBlock() instanceof DHDBlock)) return;

        Direction facing = state.getValue(DHDBlock.FACING);
        DHDState dhdState = dhd.getDHDState();

        poseStack.pushPose();

        // Move to center of block on XZ, Y at block bottom
        poseStack.translate(0.5, 0.0, 0.5);

        // Rotation based on block facing
        // Parity with original SGCraft mapping + getOpposite()
        float rotation = switch (facing) {
            case SOUTH -> 0;
            case EAST -> 90;
            case NORTH -> 180;
            case WEST -> 270;
            default -> 0;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Render each texture group
        for (int texIdx = 0; texIdx < TEXTURE_FILES.length; texIdx++) {
            renderFacesForTexture(poseStack, buffer, combinedLight, texIdx, dhdState);
        }

        poseStack.popPose();
    }

    private void renderFacesForTexture(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int texIdx, DHDState dhdState) {
        ResourceLocation texFile = TEXTURE_FILES[texIdx];
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texFile));

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();

        // Determine UV tiling offsets for this texture index
        float tileUOffset = 0;
        float tileVOffset = 0;
        float tileScale = 1.0f;
        boolean isEmissive = false;

        if (texIdx == 2) {
            // Side panel detail → always bottom-right quadrant (blue)
            tileUOffset = DETAIL_U_OFFSET;
            tileVOffset = DETAIL_V_OFFSET;
            tileScale = 0.5f;
        } else if (texIdx == 3) {
            // Button → quadrant depends on DHD state
            int stateIdx = dhdState.ordinal();
            tileUOffset = BUTTON_TILE_OFFSETS[stateIdx][0];
            tileVOffset = BUTTON_TILE_OFFSETS[stateIdx][1];
            tileScale = 0.5f;
            // Active state makes the button emissive (full brightness)
            isEmissive = (dhdState == DHDState.ACTIVE);
        }

        int lightValue = isEmissive ? LightTexture.FULL_BRIGHT : combinedLight;

        for (SmegModel.Face face : model.faces) {
            if (face.texture != texIdx) continue;

            // Calculate shade from the original mod's formula
            double[] firstVert = face.vertices[face.triangles[0][0]];
            float fnx = (float) firstVert[3];
            float fny = (float) firstVert[4];
            float fnz = (float) firstVert[5];
            float shade = (float)(0.6 * fnx * fnx + 0.8 * fnz * fnz + (fny > 0 ? 1.0 : 0.5) * fny * fny);
            shade = Math.max(shade, 0.4f);
            int r = (int)(255 * shade);
            int g = (int)(255 * shade);
            int b = (int)(255 * shade);

            for (int[] tri : face.triangles) {
                // Emit 4 vertices (v0, v1, v2, v2) to form a degenerate quad (Minecraft uses QUADS mode)
                for (int i = 0; i < 3; i++) {
                    emitVertex(vc, pose, normalMat, face.vertices[tri[i]], lightValue, r, g, b, tileUOffset, tileVOffset, tileScale);
                }
                // Duplicate last vertex to complete the quad
                emitVertex(vc, pose, normalMat, face.vertices[tri[2]], lightValue, r, g, b, tileUOffset, tileVOffset, tileScale);
            }
        }
    }

    private void emitVertex(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, double[] v,
                            int lightValue, int r, int g, int b,
                            float tileUOffset, float tileVOffset, float tileScale) {
        float x = (float) v[0];
        float y = (float) v[1];
        float z = (float) v[2];
        float nx = (float) v[3];
        float ny = (float) v[4];
        float nz = (float) v[5];

        // Apply tiling: scale UVs to the quadrant size, then offset to the correct tile
        float u = (float) v[6] * tileScale + tileUOffset;
        float vt = (float) v[7] * tileScale + tileVOffset;

        vc.vertex(pose, x, y, z)
            .color(r, g, b, 255)
            .uv(u, vt)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(lightValue)
            .normal(normalMat, nx, ny, nz)
            .endVertex();
    }

    @Override
    public boolean shouldRenderOffScreen(DHDBlockEntity blockEntity) {
        return true;
    }
}
