package gcewing.sgcraft.network;

import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

public class DialPacket {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final BlockPos pos;
    private final String address;

    public DialPacket(BlockPos pos, String address) {
        this.pos = pos;
        this.address = address;
    }

    public static void encode(DialPacket message, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeUtf(message.address);
    }

    public static DialPacket decode(FriendlyByteBuf buffer) {
        return new DialPacket(buffer.readBlockPos(), buffer.readUtf());
    }

    public static void handle(DialPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                LOGGER.info("Received DialPacket from player {} for Stargate at {} with address: '{}'", player.getName().getString(), message.pos, message.address);
                BlockEntity be = player.level().getBlockEntity(message.pos);
                if (be instanceof SGBaseBlockEntity st) {
                    // Security check: Player must be within 32 blocks of the stargate base (standard DHD range)
                    if (player.distanceToSqr(message.pos.getX() + 0.5, message.pos.getY() + 0.5, message.pos.getZ() + 0.5) < 1024) {
                        st.connectOrDisconnect(message.address, player);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
