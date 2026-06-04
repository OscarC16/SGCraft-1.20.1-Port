package com.mojang.blaze3d.vertex;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public interface VertexConsumer extends net.neoforged.neoforge.client.extensions.IVertexConsumerExtension {
    VertexConsumer addVertex(float p_350761_, float p_350704_, float p_350711_);

    VertexConsumer setColor(int p_350535_, int p_350875_, int p_350886_, int p_350775_);

    VertexConsumer setColor(int p_350809_);

    VertexConsumer setUv(float p_350572_, float p_350917_);

    VertexConsumer setUv1(int p_350815_, int p_350629_);

    VertexConsumer setUv2(int p_350859_, int p_351004_);

    VertexConsumer setNormal(float p_350429_, float p_350286_, float p_350836_);

    VertexConsumer setLineWidth(float p_456188_);

    default void addVertex(
        float p_351049_,
        float p_350528_,
        float p_351018_,
        int p_350427_,
        float p_350508_,
        float p_350864_,
        int p_350846_,
        int p_350731_,
        float p_350784_,
        float p_351051_,
        float p_350759_
    ) {
        this.addVertex(p_351049_, p_350528_, p_351018_);
        this.setColor(p_350427_);
        this.setUv(p_350508_, p_350864_);
        this.setOverlay(p_350846_);
        this.setLight(p_350731_);
        this.setNormal(p_350784_, p_351051_, p_350759_);
    }

    default VertexConsumer setColor(float p_350350_, float p_350356_, float p_350623_, float p_350312_) {
        return this.setColor((int)(p_350350_ * 255.0F), (int)(p_350356_ * 255.0F), (int)(p_350623_ * 255.0F), (int)(p_350312_ * 255.0F));
    }

    default VertexConsumer setLight(int p_350855_) {
        return this.setUv2(p_350855_ & 65535, p_350855_ >> 16 & 65535);
    }

    default VertexConsumer setOverlay(int p_350697_) {
        return this.setUv1(p_350697_ & 65535, p_350697_ >> 16 & 65535);
    }

    default void putBulkData(
        PoseStack.Pose p_85996_, BakedQuad p_85997_, float p_85999_, float p_86000_, float p_86001_, float p_331520_, int p_86003_, int p_331548_
    ) {
        this.putBulkData(
            p_85996_,
            p_85997_,
            new float[]{1.0F, 1.0F, 1.0F, 1.0F},
            p_85999_,
            p_86000_,
            p_86001_,
            p_331520_,
            new int[]{p_86003_, p_86003_, p_86003_, p_86003_},
            p_331548_
        );
    }

    default void putBulkData(
        PoseStack.Pose p_85988_,
        BakedQuad p_85989_,
        float[] p_331397_,
        float p_85990_,
        float p_85991_,
        float p_85992_,
        float p_331416_,
        int[] p_331378_,
        int p_85993_
    ) {
        Vector3fc vector3fc = p_85989_.direction().getUnitVec3f();
        Matrix4f matrix4f = p_85988_.pose();
        Vector3f vector3f = p_85988_.transformNormal(vector3fc, new Vector3f());
        int i = p_85989_.lightEmission();

        for (int j = 0; j < 4; j++) {
            Vector3fc vector3fc1 = p_85989_.position(j);
            long k = p_85989_.packedUV(j);
            float f = p_331397_[j];
            int l = ARGB.colorFromFloat(p_331416_, f * p_85990_, f * p_85991_, f * p_85992_);
            l = ARGB.multiply(l, p_85989_.bakedColors().color(j)); // Neo: apply baked color from the quad
            int i1 = LightTexture.lightCoordsWithEmission(p_331378_[j], i);
            Vector3f vector3f1 = matrix4f.transformPosition(vector3fc1, new Vector3f());
            float f1 = UVPair.unpackU(k);
            float f2 = UVPair.unpackV(k);
            applyBakedNormals(vector3f, p_85989_.bakedNormals(), j, p_85988_.normal()); // Neo: apply baked normals from the quad
            this.addVertex(vector3f1.x(), vector3f1.y(), vector3f1.z(), l, f1, f2, p_85993_, i1, vector3f.x(), vector3f.y(), vector3f.z());
        }
    }

    default VertexConsumer addVertex(Vector3fc p_458106_) {
        return this.addVertex(p_458106_.x(), p_458106_.y(), p_458106_.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose p_352288_, Vector3f p_352298_) {
        return this.addVertex(p_352288_, p_352298_.x(), p_352298_.y(), p_352298_.z());
    }

    default VertexConsumer addVertex(PoseStack.Pose p_350506_, float p_350934_, float p_350873_, float p_350981_) {
        return this.addVertex(p_350506_.pose(), p_350934_, p_350873_, p_350981_);
    }

    default VertexConsumer addVertex(Matrix4fc p_458205_, float p_457830_, float p_457564_, float p_457823_) {
        Vector3f vector3f = p_458205_.transformPosition(p_457830_, p_457564_, p_457823_, new Vector3f());
        return this.addVertex(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer addVertexWith2DPose(Matrix3x2fc p_457647_, float p_415815_, float p_416074_) {
        Vector2f vector2f = p_457647_.transformPosition(p_415815_, p_416074_, new Vector2f());
        return this.addVertex(vector2f.x(), vector2f.y(), 0.0F);
    }

    default VertexConsumer setNormal(PoseStack.Pose p_350592_, float p_350534_, float p_350411_, float p_350441_) {
        Vector3f vector3f = p_350592_.transformNormal(p_350534_, p_350411_, p_350441_, new Vector3f());
        return this.setNormal(vector3f.x(), vector3f.y(), vector3f.z());
    }

    default VertexConsumer setNormal(PoseStack.Pose p_362749_, Vector3f p_365318_) {
        return this.setNormal(p_362749_, p_365318_.x(), p_365318_.y(), p_365318_.z());
    }
}
