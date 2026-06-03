package gcewing.sgcraft.item;

import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.network.OpenPadPacket;
import gcewing.sgcraft.world.SGNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class StargatePadItem extends Item {

    public StargatePadItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                BlockPos playerPos = player.blockPosition();
                SGBaseBlockEntity closestGate = null;
                double closestDistSq = Double.MAX_VALUE;

                int radius = 16;
                for (int x = -radius; x <= radius; x++) {
                    for (int y = -radius; y <= radius; y++) {
                        for (int z = -radius; z <= radius; z++) {
                            BlockPos targetPos = playerPos.offset(x, y, z);
                            BlockEntity be = level.getBlockEntity(targetPos);
                            if (be instanceof SGBaseBlockEntity gate && gate.isMerged) {
                                double distSq = playerPos.distSqr(targetPos);
                                if (distSq < closestDistSq) {
                                    closestDistSq = distSq;
                                    closestGate = gate;
                                }
                            }
                        }
                    }
                }

                if (closestGate != null) {
                    SGNetwork network = SGNetwork.get(level);
                    List<String> addresses = new ArrayList<>();
                    for (String addr : network.getStargates().keySet()) {
                        if (!addr.equals(closestGate.homeAddress)) {
                            addresses.add(addr);
                        }
                    }
                    java.util.Collections.sort(addresses);

                    // Send packet to open GUI on client
                    serverPlayer.connection.send(new OpenPadPacket(closestGate.getBlockPos(), addresses));
                } else {
                    player.displayClientMessage(Component.translatable("message.sgcraft.no_stargate_found"), true);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
