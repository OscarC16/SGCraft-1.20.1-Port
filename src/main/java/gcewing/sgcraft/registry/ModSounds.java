package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registers custom sound events for the Stargate mod.
 */
public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SGCraft.MODID);

    public static final RegistryObject<SoundEvent> STARGATE_RING_START = registerSound("stargate.ring_start");
    public static final RegistryObject<SoundEvent> STARGATE_RING_STOP = registerSound("stargate.ring_stop");
    public static final RegistryObject<SoundEvent> STARGATE_RING_LOOP = registerSound("stargate.ring_loop");
    public static final RegistryObject<SoundEvent> STARGATE_CHEVRON_ENGAGE = registerSound("stargate.chevron_engage");
    public static final RegistryObject<SoundEvent> STARGATE_WORMHOLE_OPEN = registerSound("stargate.wormhole_open");
    public static final RegistryObject<SoundEvent> STARGATE_WORMHOLE_CLOSE = registerSound("stargate.wormhole_close");
    public static final RegistryObject<SoundEvent> STARGATE_WORMHOLE_IDLE = registerSound("stargate.wormhole_idle");
    public static final RegistryObject<SoundEvent> STARGATE_ABORT = registerSound("stargate.abort");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(SGCraft.MODID, name)));
    }
}
