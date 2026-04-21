package gcewing.sgcraft.client;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.DHDBlock;
import gcewing.sgcraft.block.SGBaseBlock;
import gcewing.sgcraft.block.SGRingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderHighlightEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SGCraft.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientForgeHandler {

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        BlockState state = event.getCamera().getEntity().level().getBlockState(event.getTarget().getBlockPos());
        
        // Hide outline for DHD Always
        if (state.getBlock() instanceof DHDBlock) {
            event.setCanceled(true);
            return;
        }

        // Hide outline for Stargate Base when merged
        if (state.getBlock() instanceof SGBaseBlock) {
            if (state.getValue(SGBaseBlock.MERGED)) {
                event.setCanceled(true);
                return;
            }
        }

        // Hide outline for Stargate Rings/Chevrons when merged
        if (state.getBlock() instanceof SGRingBlock) {
            if (state.getValue(SGRingBlock.MERGED)) {
                event.setCanceled(true);
                return;
            }
        }

    }
}
