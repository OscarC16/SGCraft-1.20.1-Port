package gcewing.sgcraft.client;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.client.renderer.SGBaseBlockEntityRenderer;
import gcewing.sgcraft.client.renderer.DHDBlockEntityRenderer;
import gcewing.sgcraft.registry.ModBlockEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = SGCraft.MODID, value = Dist.CLIENT)
public class ModClientEvents {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), SGBaseBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.DHD_BLOCK_ENTITY.get(), DHDBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerSpecialModelRenderers(net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) {
        event.register(
            net.minecraft.resources.Identifier.fromNamespaceAndPath(SGCraft.MODID, "dhd_item_renderer"),
            gcewing.sgcraft.client.renderer.DHDItemRenderer.CODEC
        );
    }



    @SubscribeEvent
    public static void registerScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(gcewing.sgcraft.registry.ModMenuTypes.SG_BASE_MENU.get(), gcewing.sgcraft.client.gui.SGBaseScreen::new);
        event.register(gcewing.sgcraft.registry.ModMenuTypes.DHD_FUEL_MENU.get(), gcewing.sgcraft.client.gui.DHDFuelScreen::new);
        event.register(gcewing.sgcraft.registry.ModMenuTypes.NAQUADAH_GENERATOR_MENU.get(), gcewing.sgcraft.client.gui.NaquadahGeneratorScreen::new);
    }
}
