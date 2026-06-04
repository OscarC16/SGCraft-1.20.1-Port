package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface OrderedSubmitNodeCollector {
    void submitShadow(PoseStack p_439112_, float p_439891_, List<EntityRenderState.ShadowPiece> p_439759_);

    void submitNameTag(
        PoseStack p_439537_,
        @Nullable Vec3 p_439628_,
        int p_439109_,
        Component p_439200_,
        boolean p_439687_,
        int p_451645_,
        double p_440364_,
        CameraRenderState p_451474_
    );

    void submitText(
        PoseStack p_440112_,
        float p_439364_,
        float p_439156_,
        FormattedCharSequence p_438924_,
        boolean p_440612_,
        Font.DisplayMode p_439113_,
        int p_440164_,
        int p_439316_,
        int p_440620_,
        int p_440227_
    );

    void submitFlame(PoseStack p_440539_, EntityRenderState p_440393_, Quaternionf p_439403_);

    void submitLeash(PoseStack p_440528_, EntityRenderState.LeashState p_440576_);

    <S> void submitModel(
        Model<? super S> p_440647_,
        S p_440718_,
        PoseStack p_440558_,
        RenderType p_459039_,
        int p_440365_,
        int p_438951_,
        int p_439205_,
        @Nullable TextureAtlasSprite p_439682_,
        int p_439754_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_439871_
    );

    default <S> void submitModel(
        Model<? super S> p_439327_,
        S p_439516_,
        PoseStack p_438927_,
        RenderType p_459216_,
        int p_439354_,
        int p_439771_,
        int p_440616_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_439732_
    ) {
        this.submitModel(p_439327_, p_439516_, p_438927_, p_459216_, p_439354_, p_439771_, -1, null, p_440616_, p_439732_);
    }

    default void submitModelPart(
        ModelPart p_439389_, PoseStack p_438887_, RenderType p_458907_, int p_439289_, int p_440106_, @Nullable TextureAtlasSprite p_439647_
    ) {
        this.submitModelPart(p_439389_, p_438887_, p_458907_, p_439289_, p_440106_, p_439647_, false, false, -1, null, 0);
    }

    default void submitModelPart(
        ModelPart p_438869_,
        PoseStack p_440328_,
        RenderType p_459082_,
        int p_438949_,
        int p_439805_,
        @Nullable TextureAtlasSprite p_440472_,
        int p_458930_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_459100_
    ) {
        this.submitModelPart(p_438869_, p_440328_, p_459082_, p_438949_, p_439805_, p_440472_, false, false, p_458930_, p_459100_, 0);
    }

    default void submitModelPart(
        ModelPart p_440506_,
        PoseStack p_439549_,
        RenderType p_459119_,
        int p_439997_,
        int p_440251_,
        @Nullable TextureAtlasSprite p_440420_,
        boolean p_438944_,
        boolean p_440215_
    ) {
        this.submitModelPart(p_440506_, p_439549_, p_459119_, p_439997_, p_440251_, p_440420_, p_438944_, p_440215_, -1, null, 0);
    }

    void submitModelPart(
        ModelPart p_439129_,
        PoseStack p_439842_,
        RenderType p_459115_,
        int p_440195_,
        int p_440355_,
        @Nullable TextureAtlasSprite p_439737_,
        boolean p_459052_,
        boolean p_459086_,
        int p_439760_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_442691_,
        int p_459097_
    );

    void submitBlock(PoseStack p_438936_, BlockState p_439547_, int p_440601_, int p_440032_, int p_440736_);

    void submitMovingBlock(PoseStack p_439854_, MovingBlockRenderState p_440284_);

    void submitBlockModel(
        PoseStack p_439157_,
        RenderType p_459202_,
        BlockStateModel p_439853_,
        float p_440368_,
        float p_440148_,
        float p_440307_,
        int p_440602_,
        int p_440529_,
        int p_440743_
    );

    void submitItem(
        PoseStack p_439086_,
        ItemDisplayContext p_439900_,
        int p_439678_,
        int p_440575_,
        int p_440740_,
        int[] p_440087_,
        List<BakedQuad> p_440405_,
        RenderType p_459126_,
        ItemStackRenderState.FoilType p_438984_
    );

    void submitCustomGeometry(PoseStack p_440145_, RenderType p_458925_, SubmitNodeCollector.CustomGeometryRenderer p_439967_);

    void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer p_445517_);
}
