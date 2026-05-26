package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.SGBlockStates;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.client.model.SmegModel;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class DHDBlockEntityRenderer implements BlockEntityRenderer<DHDBlockEntity, DHDBlockEntityRenderer.DHDRenderState> {
    public static class DHDRenderState extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public DHDBlockEntity te;
        public float partialTicks;
    }

    private static final Identifier MODEL_LOC = Identifier.fromNamespaceAndPath(SGCraft.MODID, "models/dhd.smeg");
    private static final Identifier TEX_DHD_TOP = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/block/dhd/dhd_top.png");
    private static final Identifier TEX_DHD_SIDE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/block/dhd/dhd_side.png");
    private static final Identifier TEX_DHD_DETAIL = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/block/dhd/dhd_detail.png");

    private static final Identifier[] TEXTURES = {
        TEX_DHD_TOP, TEX_DHD_SIDE, TEX_DHD_DETAIL, TEX_DHD_DETAIL
    };

    private SmegModel model;

    public DHDBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public DHDRenderState createRenderState() {
        return new DHDRenderState();
    }

    @Override
    public void extractRenderState(DHDBlockEntity te, DHDRenderState state, float partialTicks, net.minecraft.world.phys.Vec3 pos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        state.te = te;
        state.partialTicks = partialTicks;
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer.super.extractRenderState(te, state, partialTicks, pos, overlay);
    }

    @Override
    public void submit(DHDRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState cameraState) {
        net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered = collector.order(state.lightCoords);
        renderInternal(state.te, state.partialTicks, poseStack, ordered, state.lightCoords, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
    }

    public void renderInternal(DHDBlockEntity te, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered, int combinedLight, int combinedOverlay) {
        if (model == null) {
            model = SmegModel.fromResource(MODEL_LOC);
        }
        if (model == null) return;

        BlockState state = te.getBlockState();
        Direction facing = state.getValue(SGBlockStates.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 0, 0.5);
        
        float rotation = switch (facing) {
            case SOUTH -> 0f;
            case EAST -> 90f;
            case NORTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        for (int i = 0; i < model.groupedFaces.length; i++) {
            if (i >= TEXTURES.length) break;
            
            int faceLight = combinedLight;
            float uOffset = 0, vOffset = 0, scale = 1.0f;
            if (i == 2) { // Side detail
                uOffset = 0.5f; vOffset = 0.5f; scale = 0.5f;
            } else if (i == 3) { // Button
                DHDBlockEntity.DHDState dhdState = te.getDHDState();
                switch (dhdState) {
                    case IDLE -> { uOffset = 0.0f; vOffset = 0.0f; }   // Gray
                    case LINKED -> { uOffset = 0.5f; vOffset = 0.0f; } // Dark Orange
                    case ACTIVE -> { 
                        uOffset = 0.0f; vOffset = 0.5f; // Bright Orange
                        faceLight = net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
                    } 
                }
                scale = 0.5f;
            }
            
            final int finalFaceLight = faceLight;
            final float finalUOffset = uOffset;
            final float finalVOffset = vOffset;
            final float finalScale = scale;
            final int faceIndex = i;
            ordered.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURES[i]), (pose, consumer) -> {
                renderFaces(pose, consumer, model.groupedFaces[faceIndex], finalFaceLight, finalUOffset, finalVOffset, finalScale);
            });
        }

        poseStack.popPose();
    }

    private void renderFaces(PoseStack.Pose pose, VertexConsumer builder, SmegModel.Face[] faces, int light, float uOff, float vOff, float scale) {
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        for (SmegModel.Face face : faces) {
            for (int[] tri : face.triangles) {
                // Emit 4 vertices for each triangle to form a degenerate quad (tri[0], tri[1], tri[2], tri[2])
                // This is required for RenderType.entityCutout which expects QUADS mode.
                emitVertex(builder, matrix, normal, face.vertices[tri[0]], light, face.r, face.g, face.b, uOff, vOff, scale);
                emitVertex(builder, matrix, normal, face.vertices[tri[1]], light, face.r, face.g, face.b, uOff, vOff, scale);
                emitVertex(builder, matrix, normal, face.vertices[tri[2]], light, face.r, face.g, face.b, uOff, vOff, scale);
                emitVertex(builder, matrix, normal, face.vertices[tri[2]], light, face.r, face.g, face.b, uOff, vOff, scale);
            }
        }
    }

    private void emitVertex(VertexConsumer builder, Matrix4f matrix, Matrix3f normal, double[] v, int light, int r, int g, int b, float uOff, float vOff, float scale) {
        Vector3f n = new Vector3f((float)v[3], (float)v[4], (float)v[5]);
        n.mul(normal);
        
        float u = (float)v[6] * scale + uOff;
        float vt = (float)v[7] * scale + vOff;
        
        builder.addVertex(matrix, (float)v[0], (float)v[1], (float)v[2])
               .setColor(r, g, b, 255)
               .setUv(u, vt)
               .setOverlay(OverlayTexture.NO_OVERLAY)
               .setLight(light)
               .setNormal(n.x(), n.y(), n.z());
    }
}
