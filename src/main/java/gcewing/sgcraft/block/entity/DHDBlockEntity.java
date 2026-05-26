package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class DHDBlockEntity extends BlockEntity {
    public enum DHDState {
        IDLE, LINKED, ACTIVE
    }

    public boolean isLinkedToStargate = false;
    public BlockPos linkedStargatePos = BlockPos.ZERO;
    public int energy = 0;
    
    public double energyInBuffer = 0;
    public double maxEnergyBuffer = 2000000;
    public String enteredAddress = "";
    
    @SuppressWarnings("removal")
    public final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public DHDBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DHD_BLOCK_ENTITY.get(), pos, state);
    }
    
    public SGBaseBlockEntity getLinkedStargateTE() {
        if (isLinkedToStargate && level != null) {
            BlockEntity be = level.getBlockEntity(linkedStargatePos);
            if (be instanceof SGBaseBlockEntity targetStargate) {
                if (targetStargate.isLinkedToController && worldPosition.equals(targetStargate.linkedControllerPos)) {
                    return targetStargate;
                } else if (!level.isClientSide()) {
                    clearLinkToStargate();
                }
            } else if (!level.isClientSide()) {
                clearLinkToStargate();
            }
        }
        return null;
    }

    public DHDState getDHDState() {
        if (!isLinkedToStargate) return DHDState.IDLE;
        SGBaseBlockEntity stargate = getLinkedStargateTE();
        if (stargate != null && stargate.isActive()) return DHDState.ACTIVE;
        return DHDState.LINKED;
    }

    public static int linkRangeX = 6;
    public static int linkRangeY = 6;
    public static int linkRangeZ = 6;
    public static final int ENERGY_PER_NAQUADAH = 400000;

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) return;

        SGBaseBlockEntity stargate = getLinkedStargateTE();
        if (stargate != null) {
            if (stargate.energy <= SGBaseBlockEntity.MAX_ENERGY - ENERGY_PER_NAQUADAH) {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    net.minecraft.world.item.ItemStack stack = inventory.getStackInSlot(i);
                    if (!stack.isEmpty() && stack.is(gcewing.sgcraft.registry.ModItems.NAQUADAH.get())) {
                        inventory.extractItem(i, 1, false);
                        stargate.energy += ENERGY_PER_NAQUADAH;
                        stargate.setChanged();
                        break;
                    }
                }
            }
            energyInBuffer = stargate.energy;
        } else {
            energyInBuffer = 0;
        }

        if (!isLinkedToStargate && level.getGameTime() % 40 == 0) {
            checkForLink();
        }
        if (isLinkedToStargate && level.getGameTime() % 100 == 0) {
            getLinkedStargateTE();
        }
    }

    public void checkForLink() {
        if (level == null || level.isClientSide()) return;
        if (isLinkedToStargate) return;

        BlockPos myPos = this.getBlockPos();
        for (int i = -linkRangeX; i <= linkRangeX; i++) {
            for (int j = -linkRangeY; j <= linkRangeY; j++) {
                for (int k = -linkRangeZ; k <= linkRangeZ; k++) {
                    if (i == 0 && j == 0 && k == 0) continue;
                    BlockPos p = myPos.offset(i, j, k);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof SGBaseBlockEntity targetStargate) {
                        if (linkToStargate(targetStargate)) {
                            return;
                        }
                    }
                }
            }
        }
    }

    public boolean linkToStargate(SGBaseBlockEntity targetStargate) {
        if (!isLinkedToStargate && !targetStargate.isLinkedToController && targetStargate.isMerged) {
            this.linkedStargatePos = targetStargate.getBlockPos();
            this.isLinkedToStargate = true;
            this.setChanged();
            
            targetStargate.linkedControllerPos = this.getBlockPos();
            targetStargate.isLinkedToController = true;
            targetStargate.setChanged();

            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.sendBlockUpdated(targetStargate.getBlockPos(), targetStargate.getBlockState(), targetStargate.getBlockState(), 3);
            return true;
        }
        return false;
    }

    public void clearLinkToStargate() {
        if (isLinkedToStargate && level != null && !level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(linkedStargatePos);
            if (be instanceof SGBaseBlockEntity stargate) {
                stargate.isLinkedToController = false;
                stargate.linkedControllerPos = BlockPos.ZERO;
                stargate.setChanged();
                level.sendBlockUpdated(stargate.getBlockPos(), stargate.getBlockState(), stargate.getBlockState(), 3);
            }
        }
        this.isLinkedToStargate = false;
        this.linkedStargatePos = BlockPos.ZERO;
        this.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(this.getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    
    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isLinked", isLinkedToStargate);
        output.putLong("linkedPos", linkedStargatePos.asLong());
        output.putInt("energy", energy);
        output.putDouble("energyBuffer", energyInBuffer);
        inventory.serialize(output.child("inventory"));
    }

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        isLinkedToStargate = input.getBooleanOr("isLinked", false);
        linkedStargatePos = BlockPos.of(input.getLongOr("linkedPos", 0L));
        energy = input.getIntOr("energy", 0);
        energyInBuffer = input.getDoubleOr("energyBuffer", 0.0);
        if (input.child("inventory").isPresent()) {
            inventory.deserialize(input.childOrEmpty("inventory"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("isLinked", isLinkedToStargate);
        tag.putLong("linkedPos", linkedStargatePos.asLong());
        tag.putInt("energy", energy);
        tag.putDouble("energyBuffer", energyInBuffer);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.level.isClientSide()) {
            // Drop items
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = this.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
            // Clear Stargate link
            this.clearLinkToStargate();
        }
        super.preRemoveSideEffects(pos, state);
    }
}
