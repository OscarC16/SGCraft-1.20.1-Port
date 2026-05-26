package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BlockRenderDispatcher implements ResourceManagerReloadListener {
    private final BlockModelShaper blockModelShaper;
    private final MaterialSet materials;
    private final ModelBlockRenderer modelRenderer;
    private @Nullable LiquidBlockRenderer liquidBlockRenderer;
    private final RandomSource singleThreadRandom = RandomSource.create();
    private final List<BlockModelPart> singleThreadPartList = new ArrayList<>();
    private final BlockColors blockColors;

    public BlockRenderDispatcher(BlockModelShaper p_173399_, MaterialSet p_433091_, BlockColors p_173401_) {
        this.blockModelShaper = p_173399_;
        this.materials = p_433091_;
        this.blockColors = p_173401_;
        this.modelRenderer = new ModelBlockRenderer(this.blockColors);
    }

    public BlockModelShaper getBlockModelShaper() {
        return this.blockModelShaper;
    }

    public void renderBreakingTexture(BlockState p_110919_, BlockPos p_110920_, BlockAndTintGetter p_110921_, PoseStack p_110922_, VertexConsumer p_110923_) {
        if (p_110919_.getRenderShape() == RenderShape.MODEL) {
            BlockStateModel blockstatemodel = this.blockModelShaper.getBlockModel(p_110919_);
            this.singleThreadRandom.setSeed(p_110919_.getSeed(p_110920_));
            this.singleThreadPartList.clear();
            blockstatemodel.collectParts(p_110921_, p_110920_, p_110919_, this.singleThreadRandom, this.singleThreadPartList);
            this.modelRenderer
                .tesselateBlock(p_110921_, this.singleThreadPartList, p_110919_, p_110920_, p_110922_, type -> p_110923_, true, OverlayTexture.NO_OVERLAY);
        }
    }

    @Deprecated // Neo: Buffer lookup parameter
    public void renderBatched(
        BlockState p_234356_,
        BlockPos p_234357_,
        BlockAndTintGetter p_234358_,
        PoseStack p_234359_,
        VertexConsumer p_234360_,
        boolean p_234361_,
        List<BlockModelPart> p_410643_
    ) {
        this.renderBatched(p_234356_, p_234357_, p_234358_, p_234359_, type -> p_234360_, p_234361_, p_410643_);
    }

    public void renderBatched(
        BlockState p_234356_,
        BlockPos p_234357_,
        BlockAndTintGetter p_234358_,
        PoseStack p_234359_,
        java.util.function.Function<net.minecraft.client.renderer.chunk.ChunkSectionLayer, VertexConsumer> bufferLookup,
        boolean p_234361_,
        List<BlockModelPart> p_410643_
    ) {
        try {
            this.modelRenderer.tesselateBlock(p_234358_, p_410643_, p_234356_, p_234357_, p_234359_, bufferLookup, p_234361_, OverlayTexture.NO_OVERLAY);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, p_234358_, p_234357_, p_234356_);
            throw new ReportedException(crashreport);
        }
    }

    public void renderLiquid(BlockPos p_234364_, BlockAndTintGetter p_234365_, VertexConsumer p_234366_, BlockState p_234367_, FluidState p_234368_) {
        try {
            if (net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions.of(p_234368_).renderFluid(p_234368_, p_234365_, p_234364_, p_234366_, p_234367_)) return;
            Objects.requireNonNull(this.liquidBlockRenderer).tesselate(p_234365_, p_234364_, p_234366_, p_234367_, p_234368_);
        } catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating liquid in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
            CrashReportCategory.populateBlockDetails(crashreportcategory, p_234365_, p_234364_, p_234367_);
            throw new ReportedException(crashreport);
        }
    }

    public ModelBlockRenderer getModelRenderer() {
        return this.modelRenderer;
    }

    public BlockStateModel getBlockModel(BlockState p_110911_) {
        return this.blockModelShaper.getBlockModel(p_110911_);
    }

    @Deprecated // Neo: Level and pos parameters
    public void renderSingleBlock(BlockState p_110913_, PoseStack p_110914_, MultiBufferSource p_110915_, int p_110916_, int p_110917_) {
        renderSingleBlock(p_110913_, p_110914_, p_110915_, p_110916_, p_110917_, net.minecraft.world.level.EmptyBlockAndTintGetter.INSTANCE, BlockPos.ZERO);
    }

    public void renderSingleBlock(BlockState p_110913_, PoseStack p_110914_, MultiBufferSource p_110915_, int p_110916_, int p_110917_, BlockAndTintGetter level, BlockPos pos) {
        RenderShape rendershape = p_110913_.getRenderShape();
        if (rendershape != RenderShape.INVISIBLE) {
            BlockStateModel blockstatemodel = this.getBlockModel(p_110913_);
            int i = this.blockColors.getColor(p_110913_, null, null, 0);
            float f = (i >> 16 & 0xFF) / 255.0F;
            float f1 = (i >> 8 & 0xFF) / 255.0F;
            float f2 = (i & 0xFF) / 255.0F;
            ModelBlockRenderer.renderModel(
                    p_110914_.last(), p_110915_, blockstatemodel, f, f1, f2, p_110916_, p_110917_, level, pos, p_110913_
            );
        }
    }

    @Override
    public void onResourceManagerReload(ResourceManager p_110909_) {
        this.liquidBlockRenderer = new LiquidBlockRenderer(this.materials);
    }

    public LiquidBlockRenderer getLiquidBlockRenderer() {
        return Objects.requireNonNull(this.liquidBlockRenderer);
    }
}
