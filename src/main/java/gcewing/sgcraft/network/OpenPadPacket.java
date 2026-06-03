package gcewing.sgcraft.network;

import gcewing.sgcraft.SGCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import java.util.List;

public record OpenPadPacket(BlockPos stargatePos, List<String> addresses) implements CustomPacketPayload {

    public static final Type<OpenPadPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SGCraft.MODID, "open_pad"));

    public static final StreamCodec<FriendlyByteBuf, OpenPadPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, OpenPadPacket::stargatePos,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenPadPacket::addresses,
        OpenPadPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
