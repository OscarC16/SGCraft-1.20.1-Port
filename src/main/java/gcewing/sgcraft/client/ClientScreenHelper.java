package gcewing.sgcraft.client;

import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.client.gui.DHDScreen;
import net.minecraft.client.Minecraft;

public class ClientScreenHelper {
    public static void openDHDScreen(DHDBlockEntity dhd) {
        Minecraft.getInstance().setScreen(new DHDScreen(dhd));
    }
}
