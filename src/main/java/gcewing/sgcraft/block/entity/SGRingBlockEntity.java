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

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;

public class SGRingBlockEntity extends BlockEntity {

    public boolean isMerged = false;
    public BlockPos basePos = BlockPos.ZERO;

    public SGRingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SG_RING_BLOCK_ENTITY.get(), pos, state);
    }

    public SGBaseBlockEntity getBaseTE() {
        if (isMerged && level != null) {
            BlockEntity be = level.getBlockEntity(basePos);
            if (be instanceof SGBaseBlockEntity)
                return (SGBaseBlockEntity) be;
        }
        return null;
    }

    public void setMerged(boolean merged, BlockPos base) {
        this.isMerged = merged;
        this.basePos = base != null ? base : BlockPos.ZERO;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("isMerged", isMerged);
        output.putLong("basePos", basePos.asLong());
    }

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        isMerged = input.getBooleanOr("isMerged", false);
        basePos = BlockPos.of(input.getLongOr("basePos", 0L));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putBoolean("isMerged", isMerged);
        tag.putLong("basePos", basePos.asLong());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.level.isClientSide()) {
            if (this.isMerged) {
                BlockState baseState = this.level.getBlockState(this.basePos);
                if (baseState.getBlock() instanceof gcewing.sgcraft.block.SGBaseBlock baseBlock) {
                    baseBlock.unmerge(this.level, this.basePos);
                }
            }
        }
        super.preRemoveSideEffects(pos, state);
    }
}
