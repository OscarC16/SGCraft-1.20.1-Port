package gcewing.sgcraft.client;

import gcewing.sgcraft.network.OpenPadPacket;
import gcewing.sgcraft.client.gui.StargatePadScreen;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;

@OnlyIn(Dist.CLIENT)
public class ClientPacketHandler {

    public static void handleOpenPadPacket(final OpenPadPacket packet, final IPayloadContext context) {
        Minecraft.getInstance().setScreen(new StargatePadScreen(packet.stargatePos(), packet.addresses()));
    }
}
