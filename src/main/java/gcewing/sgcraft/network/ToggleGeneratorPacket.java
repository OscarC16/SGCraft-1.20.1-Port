package gcewing.sgcraft.network;

import gcewing.sgcraft.SGCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ToggleGeneratorPacket(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ToggleGeneratorPacket> TYPE = new Type<>(Identifier.fromNamespaceAndPath(SGCraft.MODID, "toggle_generator"));

    public static final StreamCodec<FriendlyByteBuf, ToggleGeneratorPacket> STREAM_CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, ToggleGeneratorPacket::pos,
        ToggleGeneratorPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
