package gcewing.sgcraft.item;

import gcewing.sgcraft.client.renderer.DHDItemRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

/**
 * Custom BlockItem for the DHD that uses a BEWLR to render
 * the 3D SMEG model in inventory, hand, and item frames.
 */
public class DHDBlockItem extends BlockItem {

    public DHDBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private DHDItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    Minecraft mc = Minecraft.getInstance();
                    renderer = new DHDItemRenderer(
                        mc.getBlockEntityRenderDispatcher(),
                        mc.getEntityModels()
                    );
                }
                return renderer;
            }
        });
    }
}
