package gcewing.sgcraft.registry;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModEvents {
    public static void init(net.neoforged.bus.api.IEventBus modEventBus) {
        modEventBus.addListener(RegisterCapabilitiesEvent.class, ModEvents::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntities.RF_POWER_UNIT_BLOCK_ENTITY.get(),
            (be, side) -> be
        );
        event.registerBlockEntity(
            Capabilities.Energy.BLOCK,
            ModBlockEntities.NAQUADAH_GENERATOR_BLOCK_ENTITY.get(),
            (be, side) -> {
                if (side == null) {
                    return be;
                }
                net.minecraft.world.level.block.state.BlockState state = be.getBlockState();
                if (state.hasProperty(gcewing.sgcraft.block.NaquadahGeneratorBlock.FACING)) {
                    net.minecraft.core.Direction facing = state.getValue(gcewing.sgcraft.block.NaquadahGeneratorBlock.FACING);
                    if (side == facing.getClockWise() || side == facing.getCounterClockWise()) {
                        return be;
                    }
                }
                return null;
            }
        );
    }
}
