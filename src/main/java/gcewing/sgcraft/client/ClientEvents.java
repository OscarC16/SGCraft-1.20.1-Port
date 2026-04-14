package gcewing.sgcraft.client;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.client.renderer.DHDBlockEntityRenderer;
import gcewing.sgcraft.client.renderer.SGBaseBlockEntityRenderer;
import gcewing.sgcraft.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SGCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.DHD_BLOCK_ENTITY.get(), DHDBlockEntityRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), SGBaseBlockEntityRenderer::new);
    }
}
