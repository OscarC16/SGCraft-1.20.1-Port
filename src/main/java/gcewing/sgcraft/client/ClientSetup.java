package gcewing.sgcraft.client;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.client.gui.DHDFuelScreen;
import gcewing.sgcraft.client.gui.SGBaseScreen;
import gcewing.sgcraft.registry.ModMenuTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SGCraft.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.SG_BASE_MENU.get(), SGBaseScreen::new);
            MenuScreens.register(ModMenuTypes.DHD_FUEL_MENU.get(), DHDFuelScreen::new);
        });
    }
}
