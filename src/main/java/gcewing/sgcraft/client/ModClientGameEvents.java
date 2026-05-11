package gcewing.sgcraft.client;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.DHDBlock;
import gcewing.sgcraft.block.SGBaseBlock;
import gcewing.sgcraft.block.SGRingBlock;
import gcewing.sgcraft.block.SGChevronBlock;
import gcewing.sgcraft.block.SGBlockStates;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.minecraft.world.level.block.state.BlockState;

@EventBusSubscriber(modid = SGCraft.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ModClientGameEvents {

    @SubscribeEvent
    public static void onRenderHighlight(RenderHighlightEvent.Block event) {
        BlockState state = event.getCamera().getEntity().level().getBlockState(event.getTarget().getBlockPos());
        
        // Hide selection outline for DHD
        if (state.getBlock() instanceof DHDBlock) {
            event.setCanceled(true);
            return;
        }

        // Hide selection outline for Stargate components ONLY when merged
        if (state.getBlock() instanceof SGBaseBlock || 
            state.getBlock() instanceof SGRingBlock || 
            state.getBlock() instanceof SGChevronBlock) {
            
            if (state.hasProperty(SGBlockStates.MERGED) && state.getValue(SGBlockStates.MERGED)) {
                event.setCanceled(true);
            }
        }
    }
}
