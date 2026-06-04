package net.minecraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.CustomFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.feature.ModelPartFeatureRenderer;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
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
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SubmitNodeCollection implements OrderedSubmitNodeCollector {
    private final List<SubmitNodeStorage.ShadowSubmit> shadowSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.FlameSubmit> flameSubmits = new ArrayList<>();
    private final NameTagFeatureRenderer.Storage nameTagSubmits = new NameTagFeatureRenderer.Storage();
    private final List<SubmitNodeStorage.TextSubmit> textSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.LeashSubmit> leashSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.BlockSubmit> blockSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.MovingBlockSubmit> movingBlockSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.BlockModelSubmit> blockModelSubmits = new ArrayList<>();
    private final List<SubmitNodeStorage.ItemSubmit> itemSubmits = new ArrayList<>();
    private final List<SubmitNodeCollector.ParticleGroupRenderer> particleGroupRenderers = new ArrayList<>();
    private final ModelFeatureRenderer.Storage modelSubmits = new ModelFeatureRenderer.Storage();
    private final ModelPartFeatureRenderer.Storage modelPartSubmits = new ModelPartFeatureRenderer.Storage();
    private final CustomFeatureRenderer.Storage customGeometrySubmits = new CustomFeatureRenderer.Storage();
    private final SubmitNodeStorage submitNodeStorage;
    private boolean wasUsed = false;

    public SubmitNodeCollection(SubmitNodeStorage p_439136_) {
        this.submitNodeStorage = p_439136_;
    }

    @Override
    public void submitShadow(PoseStack p_439535_, float p_440101_, List<EntityRenderState.ShadowPiece> p_439163_) {
        this.wasUsed = true;
        PoseStack.Pose posestack$pose = p_439535_.last();
        this.shadowSubmits.add(new SubmitNodeStorage.ShadowSubmit(new Matrix4f(posestack$pose.pose()), p_440101_, p_439163_));
    }

    @Override
    public void submitNameTag(
        PoseStack p_439367_,
        @Nullable Vec3 p_438925_,
        int p_439620_,
        Component p_439305_,
        boolean p_439557_,
        int p_451646_,
        double p_439599_,
        CameraRenderState p_451017_
    ) {
        this.wasUsed = true;
        this.nameTagSubmits.add(p_439367_, p_438925_, p_439620_, p_439305_, p_439557_, p_451646_, p_439599_, p_451017_);
    }

    @Override
    public void submitText(
        PoseStack p_439023_,
        float p_439368_,
        float p_439904_,
        FormattedCharSequence p_440321_,
        boolean p_440709_,
        Font.DisplayMode p_440071_,
        int p_439526_,
        int p_438892_,
        int p_438928_,
        int p_439544_
    ) {
        this.wasUsed = true;
        this.textSubmits
            .add(
                new SubmitNodeStorage.TextSubmit(
                    new Matrix4f(p_439023_.last().pose()), p_439368_, p_439904_, p_440321_, p_440709_, p_440071_, p_439526_, p_438892_, p_438928_, p_439544_
                )
            );
    }

    @Override
    public void submitFlame(PoseStack p_439894_, EntityRenderState p_440565_, Quaternionf p_440356_) {
        this.wasUsed = true;
        this.flameSubmits.add(new SubmitNodeStorage.FlameSubmit(p_439894_.last().copy(), p_440565_, p_440356_));
    }

    @Override
    public void submitLeash(PoseStack p_439556_, EntityRenderState.LeashState p_439612_) {
        this.wasUsed = true;
        this.leashSubmits.add(new SubmitNodeStorage.LeashSubmit(new Matrix4f(p_439556_.last().pose()), p_439612_));
    }

    @Override
    public <S> void submitModel(
        Model<? super S> p_439195_,
        S p_439802_,
        PoseStack p_440268_,
        RenderType p_459064_,
        int p_439166_,
        int p_440711_,
        int p_440107_,
        @Nullable TextureAtlasSprite p_439534_,
        int p_440065_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_439734_
    ) {
        this.wasUsed = true;
        SubmitNodeStorage.ModelSubmit<S> modelsubmit = new SubmitNodeStorage.ModelSubmit<>(
            p_440268_.last().copy(), p_439195_, p_439802_, p_439166_, p_440711_, p_440107_, p_439534_, p_440065_, p_439734_
        );
        this.modelSubmits.add(p_459064_, modelsubmit);
    }

    @Override
    public void submitModelPart(
        ModelPart p_439739_,
        PoseStack p_440200_,
        RenderType p_459081_,
        int p_440445_,
        int p_440228_,
        @Nullable TextureAtlasSprite p_439571_,
        boolean p_439311_,
        boolean p_439220_,
        int p_440219_,
        ModelFeatureRenderer.@Nullable CrumblingOverlay p_443014_,
        int p_451693_
    ) {
        this.wasUsed = true;
        this.modelPartSubmits
            .add(
                p_459081_,
                new SubmitNodeStorage.ModelPartSubmit(
                    p_440200_.last().copy(), p_439739_, p_440445_, p_440228_, p_439571_, p_439311_, p_439220_, p_440219_, p_443014_, p_451693_
                )
            );
    }

    @Override
    public void submitBlock(PoseStack p_439713_, BlockState p_440079_, int p_439604_, int p_440651_, int p_440726_) {
        this.wasUsed = true;
        this.blockSubmits.add(new SubmitNodeStorage.BlockSubmit(p_439713_.last().copy(), p_440079_, p_439604_, p_440651_, p_440726_));
        Minecraft.getInstance()
            .getModelManager()
            .specialBlockModelRenderer()
            .renderByBlock(p_440079_.getBlock(), ItemDisplayContext.NONE, p_439713_, this.submitNodeStorage, p_439604_, p_440651_, p_440726_);
    }

    @Override
    public void submitMovingBlock(PoseStack p_440463_, MovingBlockRenderState p_439643_) {
        this.wasUsed = true;
        this.movingBlockSubmits.add(new SubmitNodeStorage.MovingBlockSubmit(new Matrix4f(p_440463_.last().pose()), p_439643_));
    }

    @Override
    public void submitBlockModel(
        PoseStack p_440540_,
        RenderType p_459111_,
        BlockStateModel p_439527_,
        float p_439126_,
        float p_440382_,
        float p_439125_,
        int p_439360_,
        int p_439774_,
        int p_440732_
    ) {
        this.wasUsed = true;
        this.blockModelSubmits
            .add(
                new SubmitNodeStorage.BlockModelSubmit(
                    p_440540_.last().copy(), p_459111_, p_439527_, p_439126_, p_440382_, p_439125_, p_439360_, p_439774_, p_440732_
                )
            );
    }

    @Override
    public void submitItem(
        PoseStack p_439257_,
        ItemDisplayContext p_438997_,
        int p_440097_,
        int p_440099_,
        int p_440739_,
        int[] p_439128_,
        List<BakedQuad> p_439773_,
        RenderType p_459153_,
        ItemStackRenderState.FoilType p_439433_
    ) {
        this.wasUsed = true;
        this.itemSubmits
            .add(
                new SubmitNodeStorage.ItemSubmit(
                    p_439257_.last().copy(), p_438997_, p_440097_, p_440099_, p_440739_, p_439128_, p_439773_, p_459153_, p_439433_
                )
            );
    }

    @Override
    public void submitCustomGeometry(PoseStack p_439977_, RenderType p_458960_, SubmitNodeCollector.CustomGeometryRenderer p_439808_) {
        this.wasUsed = true;
        this.customGeometrySubmits.add(p_439977_, p_458960_, p_439808_);
    }

    @Override
    public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer p_447324_) {
        this.wasUsed = true;
        this.particleGroupRenderers.add(p_447324_);
    }

    public List<SubmitNodeStorage.ShadowSubmit> getShadowSubmits() {
        return this.shadowSubmits;
    }

    public List<SubmitNodeStorage.FlameSubmit> getFlameSubmits() {
        return this.flameSubmits;
    }

    public NameTagFeatureRenderer.Storage getNameTagSubmits() {
        return this.nameTagSubmits;
    }

    public List<SubmitNodeStorage.TextSubmit> getTextSubmits() {
        return this.textSubmits;
    }

    public List<SubmitNodeStorage.LeashSubmit> getLeashSubmits() {
        return this.leashSubmits;
    }

    public List<SubmitNodeStorage.BlockSubmit> getBlockSubmits() {
        return this.blockSubmits;
    }

    public List<SubmitNodeStorage.MovingBlockSubmit> getMovingBlockSubmits() {
        return this.movingBlockSubmits;
    }

    public List<SubmitNodeStorage.BlockModelSubmit> getBlockModelSubmits() {
        return this.blockModelSubmits;
    }

    public ModelPartFeatureRenderer.Storage getModelPartSubmits() {
        return this.modelPartSubmits;
    }

    public List<SubmitNodeStorage.ItemSubmit> getItemSubmits() {
        return this.itemSubmits;
    }

    public List<SubmitNodeCollector.ParticleGroupRenderer> getParticleGroupRenderers() {
        return this.particleGroupRenderers;
    }

    public ModelFeatureRenderer.Storage getModelSubmits() {
        return this.modelSubmits;
    }

    public CustomFeatureRenderer.Storage getCustomGeometrySubmits() {
        return this.customGeometrySubmits;
    }

    public boolean wasUsed() {
        return this.wasUsed;
    }

    public void clear() {
        this.shadowSubmits.clear();
        this.flameSubmits.clear();
        this.nameTagSubmits.clear();
        this.textSubmits.clear();
        this.leashSubmits.clear();
        this.blockSubmits.clear();
        this.movingBlockSubmits.clear();
        this.blockModelSubmits.clear();
        this.itemSubmits.clear();
        this.particleGroupRenderers.clear();
        this.modelSubmits.clear();
        this.customGeometrySubmits.clear();
        this.modelPartSubmits.clear();
    }

    public void endFrame() {
        this.modelSubmits.endFrame();
        this.modelPartSubmits.endFrame();
        this.customGeometrySubmits.endFrame();
        this.wasUsed = false;
    }
}
