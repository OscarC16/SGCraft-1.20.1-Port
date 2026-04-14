package gcewing.sgcraft.network;

import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DialPacket {
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
                BlockEntity be = player.level().getBlockEntity(message.pos);
                if (be instanceof SGBaseBlockEntity st) {
                    // Security check: Player must be within 10 blocks of the stargate base
                    if (player.distanceToSqr(message.pos.getX() + 0.5, message.pos.getY() + 0.5, message.pos.getZ() + 0.5) < 100) {
                        st.connectOrDisconnect(message.address, player);
                    }
                }
            }
        });
        context.setPacketHandled(true);
    }
}
