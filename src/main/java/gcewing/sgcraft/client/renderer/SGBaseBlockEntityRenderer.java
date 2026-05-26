package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.SGBlockStates;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class SGBaseBlockEntityRenderer implements BlockEntityRenderer<SGBaseBlockEntity, SGBaseBlockEntityRenderer.SGRenderState> {
    public static class SGRenderState extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public SGBaseBlockEntity te;
        public float partialTicks;
    }
    private static final Identifier STARGATE_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/tileentity/stargate.png");
    private static final Identifier PUDDLE_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/tileentity/eventhorizon.png");
    private static final Identifier IRIS_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/tileentity/iris.png");
    private static final int NUM_IRIS_BLADES = 12;


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

    // Texture atlas layout
    static final int TEXTURE_TILES_WIDE = 32;
    static final int TEXTURE_TILES_HIGH = 2;
    static final double TEXTURE_SCALE_U = 1.0 / (TEXTURE_TILES_WIDE * 16);
    static final double TEXTURE_SCALE_V = 1.0 / (TEXTURE_TILES_HIGH * 16);

    // Tile indices
    static final int RING_FACE_TEXTURE_INDEX = 0x01;
    static final int RING_TEXTURE_INDEX = 0x00;
    static final int RING_SYMBOL_TEXTURE_INDEX = 0x20;
    static final int CHEVRON_TEXTURE_INDEX = 0x03;
    static final int CHEVRON_LIT_TEXTURE_INDEX = 0x02;

    static final double[] SIN = new double[NUM_RING_SEGMENTS + 1];
    static final double[] COS = new double[NUM_RING_SEGMENTS + 1];

    static {
        for (int i = 0; i <= NUM_RING_SEGMENTS; i++) {
            double a = 2 * Math.PI * i / NUM_RING_SEGMENTS;
            SIN[i] = Math.sin(a);
            COS[i] = Math.cos(a);
        }
    }

    private double u0, v0;

    public SGBaseBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public SGRenderState createRenderState() {
        return new SGRenderState();
    }

    @Override
    public void extractRenderState(SGBaseBlockEntity te, SGRenderState state, float partialTicks, net.minecraft.world.phys.Vec3 pos, net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay overlay) {
        state.te = te;
        state.partialTicks = partialTicks;
        net.minecraft.client.renderer.blockentity.BlockEntityRenderer.super.extractRenderState(te, state, partialTicks, pos, overlay);
    }

    @Override
    public void submit(SGRenderState state, PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, net.minecraft.client.renderer.state.CameraRenderState cameraState) {
        net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered = collector.order(state.lightCoords);
        renderInternal(state.te, state.partialTicks, poseStack, ordered, state.lightCoords, OverlayTexture.NO_OVERLAY);
    }

    public void renderInternal(SGBaseBlockEntity te, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered, int combinedLight, int combinedOverlay) {
        if (!te.isMerged) return;

        BlockState state = te.getBlockState();
        Direction facing = state.getValue(SGBlockStates.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5, 2.5, 0.5);
        
        renderCamouflage(te, poseStack, ordered, combinedLight, combinedOverlay, facing);

        float rotation = switch (facing) {
            case NORTH -> 180f;
            case SOUTH -> 0f;
            case WEST -> 270f;
            case EAST -> 90f;
            default -> 0f;
        };
        poseStack.mulPose(Axis.YP.rotationDegrees(rotation));

        // Outer ring
        ordered.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(STARGATE_TEXTURE), (pose, vc) -> {
            renderRing(vc, pose.pose(), pose.normal(), combinedLight, RING_MID_RADIUS - RING_OVERLAP, RING_OUTER_RADIUS, true, RING_Z_OFFSET);
        });

        // Inner ring
        float ringAngle = (float) te.ringAngle;
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(ringAngle));
        ordered.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(STARGATE_TEXTURE), (pose, vc) -> {
            renderRing(vc, pose.pose(), pose.normal(), combinedLight, RING_INNER_RADIUS, RING_MID_RADIUS, false, 0);
        });
        poseStack.popPose();

        // Chevrons
        renderChevrons(te, partialTicks, poseStack, ordered, combinedLight);

        // Event Horizon
        if (te.state == SGBaseBlockEntity.State.Connected || te.state == SGBaseBlockEntity.State.Transient || te.state == SGBaseBlockEntity.State.Disconnecting) {
            renderEventHorizon(te, poseStack, ordered, combinedLight, false); // Front
            renderEventHorizon(te, poseStack, ordered, combinedLight, true);  // Back
        }

        if (te.hasIrisUpgrade) {
            renderIris(te, partialTicks, poseStack, ordered, combinedLight);
        }

        poseStack.popPose();
    }

    private void renderEventHorizon(SGBaseBlockEntity te, PoseStack poseStack, net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered, int combinedLight, boolean back) {
        poseStack.pushPose();
        if (back) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }

        final boolean finalBack = back;
        ordered.submitCustomGeometry(poseStack, RenderTypes.entitySolid(PUDDLE_TEXTURE), (pose, vc) -> {
            Matrix4f matrixPose = pose.pose();
            Matrix3f normal = pose.normal();

            final double rclip = 2.5;
            final double ehBandWidth = RING_INNER_RADIUS / SGBaseBlockEntity.ehGridRadialSize;
            double[][] grid = te.getEventHorizonGrid()[0];
            boolean flat = te.irisPhase < 1.0f;

            // Quads for the rings > 0
            for (int i = 1; i < SGBaseBlockEntity.ehGridRadialSize; i++) {
                for (int j = 0; j < SGBaseBlockEntity.ehGridPolarSize; j++) {
                    ehVertexQuad(vc, matrixPose, normal, grid, i, j, rclip, ehBandWidth, combinedLight, flat, finalBack);
                    ehVertexQuad(vc, matrixPose, normal, grid, i + 1, j, rclip, ehBandWidth, combinedLight, flat, finalBack);
                    ehVertexQuad(vc, matrixPose, normal, grid, i + 1, j + 1, rclip, ehBandWidth, combinedLight, flat, finalBack);
                    ehVertexQuad(vc, matrixPose, normal, grid, i, j + 1, rclip, ehBandWidth, combinedLight, flat, finalBack);
                }
            }

            // Center Fan
            double zCenter = flat ? 0 : ehClip(grid[1][0], 0, rclip);
            if (finalBack && !flat) zCenter = Math.min(zCenter, 0.1);
            for (int j = 0; j < SGBaseBlockEntity.ehGridPolarSize; j++) {
                ehVertexQuad(vc, matrixPose, normal, grid, 1, j, rclip, ehBandWidth, combinedLight, flat, finalBack);
                ehVertexQuad(vc, matrixPose, normal, grid, 1, j + 1, rclip, ehBandWidth, combinedLight, flat, finalBack);
                for (int k = 0; k < 2; k++) {
                    vc.addVertex(matrixPose, 0, 0, (float) zCenter)
                            .setColor(255, 255, 255, 255)
                            .setUv(0, 0)
                            .setOverlay(OverlayTexture.NO_OVERLAY)
                            .setLight(combinedLight)
                            .setNormal(0, 0, 1);
                }
            }
        });
        poseStack.popPose();
    }

    private void ehVertexQuad(VertexConsumer vc, Matrix4f pose, Matrix3f normal, double[][] grid, int i, int j,
                             double rclip, double ehBandWidth, int combinedLight, boolean flat, boolean back) {
        double r = i * ehBandWidth;
        int jj = j % SGBaseBlockEntity.ehGridPolarSize;
        if (jj < 0) jj += SGBaseBlockEntity.ehGridPolarSize;

        double x = r * COS[jj];
        double y = r * SIN[jj];
        double z = flat ? 0 : ehClip(grid[j + 1][i], r, rclip);
        
        if (back && !flat) {
            z = Math.min(z, 0.1);
        }

        vc.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(255, 255, 255, 255)
                .setUv((float) x, (float) y)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(combinedLight)
                .setNormal(0, 0, 1);
    }

    private double ehClip(double z, double r, double rclip) {
        if (r >= rclip) {
            z = Math.min(z, 0);
        }
        return z;
    }



    private void renderIris(SGBaseBlockEntity te, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered, int light) {
        float t = net.minecraft.util.Mth.lerp(partialTicks, te.prevIrisPhase, te.irisPhase);
        double aperture = t * t;
        
        // Save atlas UVs and reset for standalone texture
        double oldU0 = u0, oldV0 = v0;
        u0 = 0; v0 = 0;
        
        final double finalAperture = aperture;
        ordered.submitCustomGeometry(poseStack, RenderTypes.entityCutout(IRIS_TEXTURE), (pose, vc) -> {
            for (int i = 0; i < NUM_IRIS_BLADES; i++) {
                renderIrisBlade(vc, pose, finalAperture, light, i, NUM_IRIS_BLADES);
            }
        });
        
        // Restore atlas UVs
        u0 = oldU0; v0 = oldV0;
    }

    private void renderIrisBlade(VertexConsumer vc, PoseStack.Pose pose, double aperture, int light, int i, int n) {
        double angleStep = 360.0 / n;
        double rad = Math.PI / 180.0;
        float p2x = (float) (2.3 * Math.cos(rad * (angleStep * i)));
        float p2y = (float) (2.3 * Math.sin(rad * (angleStep * i)));
        float p3x = (float) (2.3 * Math.cos(rad * (angleStep * (i + 0.5))));
        float p3y = (float) (2.3 * Math.sin(rad * (angleStep * (i + 0.5))));
        float p4x = (float) (2.3 * Math.cos(rad * (angleStep * (i + 1))));
        float p4y = (float) (2.3 * Math.sin(rad * (angleStep * (i + 1))));

        double k = aperture * (2.1 / 2.3);
        double cosA = Math.cos(rad * angleStep);
        double sinA = Math.sin(rad * angleStep);
        double mC = (1 - (1 - k) * cosA);
        double mS = (1 - k) * sinA;
        double mDet = mC * mC + mS * mS;
        float p1x = (float) ((k / mDet) * (mC * p4x - mS * p4y));
        float p1y = (float) ((k / mDet) * (mS * p4x + mC * p4y));

        double z0 = 0.01;
        double z1 = 0.1;
        Matrix4f matrixPose = pose.pose();
        Matrix3f normal = pose.normal();

        // Front face
        vertex(vc, matrixPose, normal, light, 0, 0, 1, 255, 255, 255, p1x, p1y, z1, 0, 0);
        vertex(vc, matrixPose, normal, light, 0, 0, 1, 255, 255, 255, p2x, p2y, z0, 0, 25);
        vertex(vc, matrixPose, normal, light, 0, 0, 1, 255, 255, 255, p3x, p3y, z0, 0, 0);
        vertex(vc, matrixPose, normal, light, 0, 0, 1, 255, 255, 255, p4x, p4y, z0, 0, 25);

        // Back face
        vertex(vc, matrixPose, normal, light, 0, 0, -1, 255, 255, 255, p1x, p1y, -z1, 0, 0);
        vertex(vc, matrixPose, normal, light, 0, 0, -1, 255, 255, 255, p4x, p4y, -z0, 0, 25);
        vertex(vc, matrixPose, normal, light, 0, 0, -1, 255, 255, 255, p3x, p3y, -z0, 0, 0);
        vertex(vc, matrixPose, normal, light, 0, 0, -1, 255, 255, 255, p2x, p2y, -z0, 0, 25);
    }

    private void renderRing(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light, double r1, double r2, boolean isOuter, double dz) {
        double z = RING_DEPTH / 2 + dz;

        // Sides and Back use the same tile
        selectTile(RING_TEXTURE_INDEX);
        double sideU0 = u0, sideV0 = v0;

        // Pre-cache front face tile
        int frontTile = isOuter ? RING_FACE_TEXTURE_INDEX : RING_SYMBOL_TEXTURE_INDEX;
        selectTile(frontTile);
        double frontU0 = u0, frontV0 = v0;

        double symWidth = 512.0 / NUM_RING_SEGMENTS;
        double symLen = 512.0;

        for (int i = 0; i < NUM_RING_SEGMENTS; i++) {
            double s1 = SIN[i], c1 = COS[i];
            double s2 = SIN[i+1], c2 = COS[i+1];

            // Sides and Back
            u0 = sideU0; v0 = sideV0;

            // Outer surface (if outer ring) or Inner surface (if inner ring)
            if (isOuter) {
                quad(vc, pose, normalMat, light, (float)c1, (float)s1, 0,
                    r2 * c1, r2 * s1, z, 0, 0,
                    r2 * c1, r2 * s1, -z, 0, 16,
                    r2 * c2, r2 * s2, -z, 16, 16,
                    r2 * c2, r2 * s2, z, 16, 0);
            } else {
                quad(vc, pose, normalMat, light, (float)-c1, (float)-s1, 0,
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
                double u = symLen - (i + 1) * symWidth;
                quad(vc, pose, normalMat, light, 0, 0, 1,
                    r1 * c1, r1 * s1, z, u + symWidth, 16,
                    r2 * c1, r2 * s1, z, u + symWidth, 0,
                    r2 * c2, r2 * s2, z, u, 0,
                    r1 * c2, r1 * s2, z, u, 16);
            }
        }
    }

    private void renderChevrons(SGBaseBlockEntity te, float partialTicks, PoseStack poseStack, net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered, int light) {
        for (int i = 0; i < 9; i++) {
            if ((i == 4 || i == 5) && !te.hasChevronUpgrade) continue;
            float engageAmount = net.minecraft.util.Mth.lerp(partialTicks, te.prevChevronEngageAmount[i], te.chevronEngageAmount[i]);
            renderChevron(te, i, engageAmount, poseStack, ordered, light);
        }
    }

    private void renderChevron(SGBaseBlockEntity te, int i, float engageAmount, PoseStack poseStack, net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered, int light) {
        float a = 40f;
        
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotationDegrees(90 - i * a));
        double displacement = engageAmount * 0.125;
        poseStack.translate(-displacement, 0, 0);

        final float finalEngageAmount = engageAmount;
        ordered.submitCustomGeometry(poseStack, RenderTypes.entityCutoutNoCull(STARGATE_TEXTURE), (pose, vc) -> {
            renderChevron(vc, pose.pose(), pose.normal(), light, finalEngageAmount);
        });
        poseStack.popPose();
    }

    private void renderChevron(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light, float engageAmount) {
        double r1 = CHEVRON_INNER_RADIUS;
        double r2 = CHEVRON_OUTER_RADIUS;
        double z2 = RING_DEPTH / 2;
        double z1 = z2 + CHEVRON_DEPTH;
        double w1 = CHEVRON_BORDER_WIDTH;
        double w2 = w1 * 1.25;
        double x1 = r1, y1 = CHEVRON_WIDTH / 4;
        double x2 = r2, y2 = CHEVRON_WIDTH / 2;

        selectTile(CHEVRON_TEXTURE_INDEX);
        selectTile(CHEVRON_TEXTURE_INDEX);
        // Face 1 (right arm)
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
        // Side 2 (inner edge)
        quad(vc, pose, normalMat, light, -1, 0, 0,
            x1, y1, z1, 0, 0,
            x1, y1, z2, 0, 4,
            x1, -y1, z2, 16, 4,
            x1, -y1, z1, 16, 0);
        // Face 3 (left arm)
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

        selectTile(CHEVRON_LIT_TEXTURE_INDEX);
        int r = (int) (100 + engageAmount * 155);
        int g = (int) (70 + engageAmount * 130);
        int b = (int) (50 + engageAmount * 100);
        int litLevel = engageAmount > 0.5f ? 0xF000F0 : light;
        
        // Front face lit center (Face 4)
        quadColor(vc, pose, normalMat, litLevel, 0, 0, 1, r, g, b,
            x2, y2 - w2, z1 + 0.001, 0, 4,
            x1 + w1, y1 - w1, z1 + 0.001, 4, 16,
            x1 + w1, -y1 + w1, z1 + 0.001, 12, 16,
            x2, -y2 + w2, z1 + 0.001, 16, 4);

        // Side face lit center (End 4)
        quadColor(vc, pose, normalMat, litLevel, 1, 0, 0, r, g, b,
            x2, y2 - w2, z2, 0, 0,
            x2, y2 - w2, z1, 0, 4,
            x2, -y2 + w2, z1, 16, 4,
            x2, -y2 + w2, z2, 16, 0);
    }

    private void selectTile(int index) {
        u0 = (index % TEXTURE_TILES_WIDE) * (TEXTURE_SCALE_U * 16);
        v0 = (index / TEXTURE_TILES_WIDE) * (TEXTURE_SCALE_V * 16);
    }

    private void quad(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light, float nx, float ny, float nz,
                      double x0, double y0, double z0, double u0v, double v0v,
                      double x1, double y1, double z1, double u1v, double v1v,
                      double x2, double y2, double z2, double u2v, double v2v,
                      double x3, double y3, double z3, double u3v, double v3v) {
        quadColor(vc, pose, normalMat, light, nx, ny, nz, 255, 255, 255,
            x0, y0, z0, u0v, v0v, x1, y1, z1, u1v, v1v, x2, y2, z2, u2v, v2v, x3, y3, z3, u3v, v3v);
    }

    private void quadColor(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light, float nx, float ny, float nz, int r, int g, int b,
                           double x0, double y0, double z0, double u0v, double v0v,
                           double x1, double y1, double z1, double u1v, double v1v,
                           double x2, double y2, double z2, double u2v, double v2v,
                           double x3, double y3, double z3, double u3v, double v3v) {
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x0, y0, z0, u0v, v0v);
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x1, y1, z1, u1v, v1v);
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x2, y2, z2, u2v, v2v);
        vertex(vc, pose, normalMat, light, nx, ny, nz, r, g, b, x3, y3, z3, u3v, v3v);
    }

    private void vertex(VertexConsumer vc, Matrix4f pose, Matrix3f normalMat, int light, float nx, float ny, float nz, int r, int g, int b, double x, double y, double z, double u, double v) {
        float fu = (float) (u0 + u * TEXTURE_SCALE_U);
        float fv = (float) (v0 + v * TEXTURE_SCALE_V);
        
        Vector3f normal = new Vector3f(nx, ny, nz);
        normal.mul(normalMat);
        
        vc.addVertex(pose, (float) x, (float) y, (float) z)
          .setColor(r, g, b, 255)
          .setUv(fu, fv)
          .setOverlay(OverlayTexture.NO_OVERLAY)
          .setLight(light)
          .setNormal(normal.x(), normal.y(), normal.z());
    }

    @Override
    public net.minecraft.world.phys.AABB getRenderBoundingBox(SGBaseBlockEntity blockEntity) {
        return new net.minecraft.world.phys.AABB(blockEntity.getBlockPos()).inflate(5.0);
    }

    private void renderCamouflage(SGBaseBlockEntity te, PoseStack poseStack, net.minecraft.client.renderer.OrderedSubmitNodeCollector ordered, int light, int overlay, Direction facing) {
        for (int i = 0; i < 5; i++) {
            net.minecraft.world.item.ItemStack stack = te.inventory.getStackInSlot(i);
            if (stack.isEmpty() || !(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem)) continue;

            poseStack.pushPose();
            int lateralOffset = i - 2; // CORRECTED INVERSION (Slot 0 is on player's left, Slot 4 is on player's right)
            int idx = 0, idz = 0;
            switch (facing) {
                case NORTH -> idx = -lateralOffset;
                case SOUTH -> idx = lateralOffset;
                case WEST -> idz = lateralOffset;    // RESTORED CORRECT MAPPING (Confirmed by user)
                case EAST -> idz = -lateralOffset;   // RESTORED CORRECT MAPPING (Confirmed by user)
                default -> {}
            }
            
            poseStack.translate(idx - 0.5, -2.5, idz - 0.5);

            BlockState state = blockItem.getBlock().defaultBlockState();
            net.minecraft.core.BlockPos camoPos = te.getBlockPos().offset(idx, 0, idz);

            if (te.getLevel() != null) {
                net.minecraft.client.renderer.block.BlockRenderDispatcher blockRenderer = net.minecraft.client.Minecraft.getInstance().getBlockRenderer();
                
                // Get the block's render type
                net.minecraft.client.renderer.rendertype.RenderType rt = net.minecraft.client.renderer.ItemBlockRenderTypes.getRenderType(state);
                
                net.minecraft.client.renderer.block.model.BlockStateModel model = blockRenderer.getBlockModel(state);
                
                // Render the block natively using submitCustomGeometry and ModelBlockRenderer.tesselateBlock with checkSides = false to ensure perfect lighting and shadows!
                ordered.submitCustomGeometry(poseStack, rt, (pose, vc) -> {
                    com.mojang.blaze3d.vertex.PoseStack callbackStack = new com.mojang.blaze3d.vertex.PoseStack();
                    callbackStack.last().set(pose);
                    
                    java.util.List<net.minecraft.client.renderer.block.model.BlockModelPart> parts = model.collectParts(
                        te.getLevel(), camoPos, state, net.minecraft.util.RandomSource.create(state.getSeed(camoPos))
                    );
                    
                    blockRenderer.getModelRenderer().tesselateBlock(
                        te.getLevel(),
                        parts,
                        state,
                        camoPos,
                        callbackStack,
                        type -> vc,
                        false, // checkSides = false to bypass ambient occlusion culling inside solid Stargate blocks
                        overlay
                    );
                });
            } else {
                ordered.submitBlock(poseStack, state, light, overlay, 0);
            }

            poseStack.popPose();
        }
    }
}

