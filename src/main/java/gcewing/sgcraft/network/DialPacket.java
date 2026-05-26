package gcewing.sgcraft.network;

import gcewing.sgcraft.SGCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record DialPacket(BlockPos pos, String address) implements CustomPacketPayload {

    public static final Type<DialPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SGCraft.MODID, "dial"));

    public static final StreamCodec<FriendlyByteBuf, DialPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, DialPacket::pos,
        ByteBufCodecs.STRING_UTF8, DialPacket::address,
        DialPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
