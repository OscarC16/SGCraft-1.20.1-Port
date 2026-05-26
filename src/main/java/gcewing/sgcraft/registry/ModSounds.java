package gcewing.sgcraft.registry;

import gcewing.sgcraft.SGCraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, SGCraft.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_RING_START = registerSound("stargate.ring_start");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_RING_STOP = registerSound("stargate.ring_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_RING_LOOP = registerSound("stargate.ring_loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_CHEVRON_ENGAGE = registerSound("stargate.chevron_engage");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_WORMHOLE_OPEN = registerSound("stargate.wormhole_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_WORMHOLE_CLOSE = registerSound("stargate.wormhole_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_WORMHOLE_IDLE = registerSound("stargate.wormhole_idle");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_ABORT = registerSound("stargate.abort");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_IRIS_OPEN = registerSound("stargate.iris_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_IRIS_CLOSE = registerSound("stargate.iris_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> STARGATE_IRIS_HIT = registerSound("stargate.iris_hit");

    private static DeferredHolder<SoundEvent, SoundEvent> registerSound(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(SGCraft.MODID, name)));
    }
}
