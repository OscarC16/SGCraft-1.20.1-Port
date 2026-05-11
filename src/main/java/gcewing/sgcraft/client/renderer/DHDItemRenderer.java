package gcewing.sgcraft.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import gcewing.sgcraft.registry.ModBlocks;

public class DHDItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final DHDBlockEntity dummy = new DHDBlockEntity(BlockPos.ZERO, ModBlocks.STARGATE_CONTROLLER.get().defaultBlockState());

    public DHDItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffer, int combinedLight, int combinedOverlay) {
        Minecraft.getInstance().getBlockEntityRenderDispatcher().renderItem(dummy, poseStack, buffer, combinedLight, combinedOverlay);
    }
}
