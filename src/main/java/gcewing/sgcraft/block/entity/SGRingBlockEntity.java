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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("isMerged", isMerged);
        tag.putInt("baseX", basePos.getX());
        tag.putInt("baseY", basePos.getY());
        tag.putInt("baseZ", basePos.getZ());
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        isMerged = tag.getBoolean("isMerged");
        basePos = new BlockPos(tag.getInt("baseX"), tag.getInt("baseY"), tag.getInt("baseZ"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
