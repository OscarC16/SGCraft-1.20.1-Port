package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockFeatureRenderer {
    private final PoseStack poseStack = new PoseStack();

    public void render(SubmitNodeCollection p_440410_, MultiBufferSource.BufferSource p_434682_, BlockRenderDispatcher p_432881_, OutlineBufferSource p_440734_) {
        for (SubmitNodeStorage.MovingBlockSubmit submitnodestorage$movingblocksubmit : p_440410_.getMovingBlockSubmits()) {
            MovingBlockRenderState movingblockrenderstate = submitnodestorage$movingblocksubmit.movingBlockRenderState();
            BlockState blockstate = movingblockrenderstate.blockState;
            List<BlockModelPart> list = p_432881_.getBlockModel(blockstate)
                .collectParts(movingblockrenderstate.level, movingblockrenderstate.blockPos, movingblockrenderstate.blockState, RandomSource.create(blockstate.getSeed(movingblockrenderstate.randomSeedPos)));
            PoseStack posestack = new PoseStack();
            posestack.mulPose(submitnodestorage$movingblocksubmit.pose());
            p_432881_.getModelRenderer()
                .tesselateBlock(
                    movingblockrenderstate,
                    list,
                    blockstate,
                    movingblockrenderstate.blockPos,
                    posestack,
                    // TODO: this needs further thought as it violates the "one submit == one rendertype" contract
                    renderType -> p_434682_.getBuffer(net.neoforged.neoforge.client.RenderTypeHelper.getMovingBlockRenderType(renderType)),
                    false,
                    OverlayTexture.NO_OVERLAY
                );
        }

        for (SubmitNodeStorage.BlockSubmit submitnodestorage$blocksubmit : p_440410_.getBlockSubmits()) {
            this.poseStack.pushPose();
            this.poseStack.last().set(submitnodestorage$blocksubmit.pose());
            p_432881_.renderSingleBlock(
                submitnodestorage$blocksubmit.state(),
                this.poseStack,
                p_434682_,
                submitnodestorage$blocksubmit.lightCoords(),
                submitnodestorage$blocksubmit.overlayCoords()
            );
            if (submitnodestorage$blocksubmit.outlineColor() != 0) {
                p_440734_.setColor(submitnodestorage$blocksubmit.outlineColor());
                p_432881_.renderSingleBlock(
                    submitnodestorage$blocksubmit.state(),
                    this.poseStack,
                    p_440734_,
                    submitnodestorage$blocksubmit.lightCoords(),
                    submitnodestorage$blocksubmit.overlayCoords()
                );
            }

            this.poseStack.popPose();
        }

        for (SubmitNodeStorage.BlockModelSubmit submitnodestorage$blockmodelsubmit : p_440410_.getBlockModelSubmits()) {
            ModelBlockRenderer.renderModel(
                submitnodestorage$blockmodelsubmit.pose(),
                p_434682_.getBuffer(submitnodestorage$blockmodelsubmit.renderType()),
                submitnodestorage$blockmodelsubmit.model(),
                submitnodestorage$blockmodelsubmit.r(),
                submitnodestorage$blockmodelsubmit.g(),
                submitnodestorage$blockmodelsubmit.b(),
                submitnodestorage$blockmodelsubmit.lightCoords(),
                submitnodestorage$blockmodelsubmit.overlayCoords()
            );
            if (submitnodestorage$blockmodelsubmit.outlineColor() != 0) {
                p_440734_.setColor(submitnodestorage$blockmodelsubmit.outlineColor());
                ModelBlockRenderer.renderModel(
                    submitnodestorage$blockmodelsubmit.pose(),
                    p_440734_.getBuffer(submitnodestorage$blockmodelsubmit.renderType()),
                    submitnodestorage$blockmodelsubmit.model(),
                    submitnodestorage$blockmodelsubmit.r(),
                    submitnodestorage$blockmodelsubmit.g(),
                    submitnodestorage$blockmodelsubmit.b(),
                    submitnodestorage$blockmodelsubmit.lightCoords(),
                    submitnodestorage$blockmodelsubmit.overlayCoords()
                );
            }
        }
    }
}
