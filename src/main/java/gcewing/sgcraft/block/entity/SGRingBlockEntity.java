package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tile entity for Stargate Ring and Chevron blocks.
 * Tracks whether this block has been merged into a Stargate structure
 * and stores the position of the base block that controls it.
 */
public class SGRingBlockEntity extends BlockEntity {

    public boolean isMerged = false;
    public BlockPos basePos = BlockPos.ZERO;

    public SGRingBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SG_RING_BLOCK_ENTITY.get(), pos, state);
    }

    /**
     * Get the base tile entity of the Stargate this ring belongs to.
     * @return The SGBaseBlockEntity, or null if not merged or base is missing.
     */
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

    // --- NBT Serialization ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("isMerged", isMerged);
        tag.putInt("baseX", basePos.getX());
        tag.putInt("baseY", basePos.getY());
        tag.putInt("baseZ", basePos.getZ());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isMerged = tag.getBoolean("isMerged");
        basePos = new BlockPos(tag.getInt("baseX"), tag.getInt("baseY"), tag.getInt("baseZ"));
    }

    // --- Client Sync ---

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
