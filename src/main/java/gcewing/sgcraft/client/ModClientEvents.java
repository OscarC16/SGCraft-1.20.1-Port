package gcewing.sgcraft.client;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.client.renderer.SGBaseBlockEntityRenderer;
import gcewing.sgcraft.client.renderer.DHDBlockEntityRenderer;
import gcewing.sgcraft.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SGCraft.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), SGBaseBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DHD_BLOCK_ENTITY.get(), DHDBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerClientExtensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event) {
        event.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
            private gcewing.sgcraft.client.renderer.DHDItemRenderer renderer;

            @Override
            public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new gcewing.sgcraft.client.renderer.DHDItemRenderer();
                }
                return renderer;
            }
        }, gcewing.sgcraft.registry.ModItems.STARGATE_CONTROLLER_ITEM.get());
    }

    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(gcewing.sgcraft.registry.ModMenuTypes.SG_BASE_MENU.get(), gcewing.sgcraft.client.gui.SGBaseScreen::new);
        event.register(gcewing.sgcraft.registry.ModMenuTypes.DHD_FUEL_MENU.get(), gcewing.sgcraft.client.gui.DHDFuelScreen::new);
    }
}
