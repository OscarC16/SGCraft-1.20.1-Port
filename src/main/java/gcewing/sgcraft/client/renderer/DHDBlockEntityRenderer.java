package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.SGBlockStates;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.client.model.SmegModel;
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
import org.joml.Vector3f;

public class DHDBlockEntityRenderer implements BlockEntityRenderer<DHDBlockEntity> {

    private static final ResourceLocation MODEL_LOC = ResourceLocation.fromNamespaceAndPath(SGCraft.MODID, "models/dhd.smeg");
    private static final ResourceLocation TEX_DHD_TOP = ResourceLocation.fromNamespaceAndPath(SGCraft.MODID, "textures/block/dhd/dhd_top.png");
    private static final ResourceLocation TEX_DHD_SIDE = ResourceLocation.fromNamespaceAndPath(SGCraft.MODID, "textures/block/dhd/dhd_side.png");
    private static final ResourceLocation TEX_DHD_DETAIL = ResourceLocation.fromNamespaceAndPath(SGCraft.MODID, "textures/block/dhd/dhd_detail.png");

    private static final ResourceLocation[] TEXTURES = {
        TEX_DHD_TOP, TEX_DHD_SIDE, TEX_DHD_DETAIL, TEX_DHD_DETAIL
    };

    private SmegModel model;

    public DHDBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DHDBlockEntity te, float partialTicks, PoseStack stack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        if (model == null) {
            model = SmegModel.fromResource(MODEL_LOC);
        }
        if (model == null) return;

        BlockState state = te.getBlockState();
        Direction facing = state.getValue(SGBlockStates.FACING);

        stack.pushPose();
        stack.translate(0.5, 0, 0.5);
        
        float rotation = switch (facing) {
            case SOUTH -> 0f;
            case EAST -> 90f;
            case NORTH -> 180f;
            case WEST -> 270f;
            default -> 0f;
        };
        stack.mulPose(Axis.YP.rotationDegrees(rotation));

        for (int i = 0; i < model.groupedFaces.length; i++) {
            if (i >= TEXTURES.length) break;
            VertexConsumer builder = buffer.getBuffer(RenderType.entityCutout(TEXTURES[i]));
            
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
            
            renderFaces(stack, builder, model.groupedFaces[i], faceLight, uOffset, vOffset, scale);
        }

        stack.popPose();
    }

    private void renderFaces(PoseStack stack, VertexConsumer builder, SmegModel.Face[] faces, int light, float uOff, float vOff, float scale) {
        Matrix4f matrix = stack.last().pose();
        Matrix3f normal = stack.last().normal();

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
