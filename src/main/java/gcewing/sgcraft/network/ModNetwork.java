package gcewing.sgcraft.network;

import com.mojang.logging.LogUtils;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.block.entity.NaquadahGeneratorBlockEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

@EventBusSubscriber(modid = SGCraft.MODID)
public class ModNetwork {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(SGCraft.MODID);

        registrar.playToServer(
            DialPacket.TYPE,
            DialPacket.STREAM_CODEC,
            ModNetwork::handleDialPacket
        );

        registrar.playToClient(
            OpenPadPacket.TYPE,
            OpenPadPacket.STREAM_CODEC,
            ModNetwork::handleOpenPadPacket
        );

        registrar.playToServer(
            ToggleGeneratorPacket.TYPE,
            ToggleGeneratorPacket.STREAM_CODEC,
            ModNetwork::handleToggleGeneratorPacket
        );
    }

    private static void handleOpenPadPacket(final OpenPadPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            gcewing.sgcraft.client.ClientPacketHandler.handleOpenPadPacket(packet, context);
        });
    }

    private static void handleDialPacket(final DialPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                LOGGER.info("Received DialPacket from player {} for Stargate at {} with address: '{}'", player.getName().getString(), packet.pos(), packet.address());
                
                BlockEntity be = player.level().getBlockEntity(packet.pos());
                if (be instanceof SGBaseBlockEntity st) {
                    // Verificación de seguridad: El jugador debe estar razonablemente cerca del DHD/Stargate (32 bloques máx)
                    if (player.distanceToSqr(packet.pos().getX() + 0.5, packet.pos().getY() + 0.5, packet.pos().getZ() + 0.5) < 1024) {
                        st.connectOrDisconnect(packet.address(), player);
                    }
                }
            }
        });
    }

    private static void handleToggleGeneratorPacket(final ToggleGeneratorPacket packet, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                BlockEntity be = player.level().getBlockEntity(packet.pos());
                if (be instanceof NaquadahGeneratorBlockEntity gen) {
                    if (player.distanceToSqr(packet.pos().getX() + 0.5, packet.pos().getY() + 0.5, packet.pos().getZ() + 0.5) < 64) {
                        gen.enabled = !gen.enabled;
                        gen.setChanged();
                    }
                }
            }
        });
    }
}
