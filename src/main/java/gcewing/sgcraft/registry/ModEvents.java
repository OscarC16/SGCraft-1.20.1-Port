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
    }
}
