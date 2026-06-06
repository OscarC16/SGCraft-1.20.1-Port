package gcewing.sgcraft.client;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.DHDBlock;
import gcewing.sgcraft.block.SGBaseBlock;
import gcewing.sgcraft.block.SGRingBlock;
import gcewing.sgcraft.block.SGBlockStates;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ExtractBlockOutlineRenderStateEvent;

@EventBusSubscriber(modid = SGCraft.MODID, value = Dist.CLIENT)
public class ModClientGameEvents {

    @SubscribeEvent
    public static void onExtractBlockOutline(ExtractBlockOutlineRenderStateEvent event) {
        BlockState state = event.getBlockState();
        if (state == null) return;
        
        // Hide selection outline for Stargate Controller (DHD)
        if (state.getBlock() instanceof DHDBlock) {
            event.setCanceled(true);
            return;
        }

        // Hide selection outline for Naquadah Generator
        if (state.getBlock() instanceof gcewing.sgcraft.block.NaquadahGeneratorBlock) {
            event.setCanceled(true);
            return;
        }

        // Hide selection outline for Stargate components (Base, Ring, Chevron) ONLY when merged
        if (state.getBlock() instanceof SGBaseBlock || state.getBlock() instanceof SGRingBlock) {
            if (state.hasProperty(SGBlockStates.MERGED) && state.getValue(SGBlockStates.MERGED)) {
                event.setCanceled(true);
            }
        }
    }
}
