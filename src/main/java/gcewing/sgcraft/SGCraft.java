package gcewing.sgcraft;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import gcewing.sgcraft.registry.ModBlocks;
import gcewing.sgcraft.registry.ModItems;
import gcewing.sgcraft.registry.ModCreativeTabs;
import gcewing.sgcraft.registry.ModBlockEntities;
import gcewing.sgcraft.registry.ModMenuTypes;
import gcewing.sgcraft.registry.ModSounds;
import gcewing.sgcraft.network.ModNetwork;
import org.slf4j.Logger;

@Mod(SGCraft.MODID)
public class SGCraft {
    public static final String MODID = "sgcraft";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SGCraft() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenuTypes.MENUS.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        modEventBus.addListener(this::setup);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setup(final FMLCommonSetupEvent event) {
        LOGGER.info("SGCraft starting...");
        event.enqueueWork(ModNetwork::register);
    }
}
