package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.client.model.SmegModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/**
 * Renders the DHD item using the same SMEG 3D model as the placed block.
 * This is used for inventory, hand, ground, and item frame rendering.
 */
public class DHDItemRenderer extends BlockEntityWithoutLevelRenderer {

    private static final ResourceLocation[] TEXTURE_FILES = {
        new ResourceLocation(SGCraft.MODID, "textures/block/dhd_top.png"),
        new ResourceLocation(SGCraft.MODID, "textures/block/dhd_side.png"),
        new ResourceLocation(SGCraft.MODID, "textures/block/dhd/dhd_detail.png"),
        new ResourceLocation(SGCraft.MODID, "textures/block/dhd/dhd_detail.png")
    };

    // Detail tile: side panels = bottom-right (blue)
    private static final float DETAIL_U_OFFSET = 0.5f;
    private static final float DETAIL_V_OFFSET = 0.5f;

    // Button tile: IDLE = top-left (gray) for item rendering
    private static final float BUTTON_U_OFFSET = 0.0f;
    private static final float BUTTON_V_OFFSET = 0.0f;

    private SmegModel model;
    private boolean modelLoaded = false;

    public DHDItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet) {
        super(dispatcher, modelSet);
    }

    private void loadModel() {
        if (!modelLoaded) {
            try {
                ResourceLocation modelLoc = new ResourceLocation(SGCraft.MODID, "models/dhd.smeg");
                model = SmegModel.fromResource(modelLoc);
                modelLoaded = true;
            } catch (Exception e) {
                System.err.println("[SGCraft] Failed to load DHD item model: " + e.getMessage());
                modelLoaded = true;
            }
        }
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack,
                             MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        loadModel();
        if (model == null || model.faces == null) return;

        poseStack.pushPose();

        // Center the model in the item space
        poseStack.translate(0.5, 0.0, 0.5);

        // Slight rotation for a nice 3D look in GUI
        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.YP.rotationDegrees(30));
        }

        // Render each texture group
        for (int texIdx = 0; texIdx < TEXTURE_FILES.length; texIdx++) {
            renderFacesForTexture(poseStack, buffer, combinedLight, texIdx);
        }

        poseStack.popPose();
    }

    private void renderFacesForTexture(PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int texIdx) {
        if (texIdx >= model.groupedFaces.length) return;
        SmegModel.Face[] currentFaces = model.groupedFaces[texIdx];
        if (currentFaces == null || currentFaces.length == 0) return;

        ResourceLocation texFile = TEXTURE_FILES[texIdx];
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(texFile));

        Matrix4f pose = poseStack.last().pose();
        Matrix3f normalMat = poseStack.last().normal();

        // Tiling offsets
        float tileUOffset = 0;
        float tileVOffset = 0;
        float tileScale = 1.0f;

        if (texIdx == 2) {
            // Side detail → blue quadrant
            tileUOffset = DETAIL_U_OFFSET;
            tileVOffset = DETAIL_V_OFFSET;
            tileScale = 0.5f;
        } else if (texIdx == 3) {
            // Button → gray (IDLE state for items)
            tileUOffset = BUTTON_U_OFFSET;
            tileVOffset = BUTTON_V_OFFSET;
            tileScale = 0.5f;
        }

        for (SmegModel.Face face : currentFaces) {
            int r = face.r;
            int g = face.g;
            int b = face.b;

            for (int[] tri : face.triangles) {
                // Emit degenerate quad: v0, v1, v2, v2
                emitVertex(vc, pose, normalMat, face.vertices[tri[0]], combinedLight, r, g, b, tileUOffset, tileVOffset, tileScale);
                emitVertex(vc, pose, normalMat, face.vertices[tri[1]], combinedLight, r, g, b, tileUOffset, tileVOffset, tileScale);
                emitVertex(vc, pose, normalMat, face.vertices[tri[2]], combinedLight, r, g, b, tileUOffset, tileVOffset, tileScale);
                emitVertex(vc, pose, normalMat, face.vertices[tri[2]], combinedLight, r, g, b, tileUOffset, tileVOffset, tileScale);
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
}
