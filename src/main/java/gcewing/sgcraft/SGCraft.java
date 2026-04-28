package gcewing.sgcraft;

import gcewing.sgcraft.registry.ModBlocks;
import gcewing.sgcraft.registry.ModItems;
import gcewing.sgcraft.registry.ModCreativeTabs;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

@Mod(SGCraft.MODID)
public class SGCraft {
    public static final String MODID = "sgcraft";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SGCraft(IEventBus modEventBus) {
        LOGGER.info("Initializing SGCraft v2.2.1 for Minecraft 1.21.11");

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
    }
}
