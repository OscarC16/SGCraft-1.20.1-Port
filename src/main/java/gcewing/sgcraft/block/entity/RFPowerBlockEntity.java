package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;

public class RFPowerBlockEntity extends BlockEntity implements EnergyHandler {

    private final SnapshotJournal<Integer> energyJournal = new SnapshotJournal<Integer>() {
        @Override
        protected Integer createSnapshot() {
            SGBaseBlockEntity stargate = getStargate();
            return stargate != null ? stargate.energy : 0;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            SGBaseBlockEntity stargate = getStargate();
            if (stargate != null) {
                stargate.energy = snapshot;
                stargate.sync();
            }
        }

        @Override
        protected void onRootCommit(Integer snapshot) {
            SGBaseBlockEntity stargate = getStargate();
            if (stargate != null) {
                stargate.sync();
            }
        }
    };

    public RFPowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RF_POWER_UNIT_BLOCK_ENTITY.get(), pos, state);
    }

    private SGBaseBlockEntity getStargate() {
        if (level == null || level.isClientSide()) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.above());
        if (be instanceof SGBaseBlockEntity stargate) {
            return stargate;
        }
        return null;
    }

    @Override
    public long getAmountAsLong() {
        SGBaseBlockEntity stargate = getStargate();
        return stargate != null ? stargate.energy : 0L;
    }

    @Override
    public long getCapacityAsLong() {
        SGBaseBlockEntity stargate = getStargate();
        return stargate != null ? SGBaseBlockEntity.MAX_ENERGY : 0L;
    }

    @Override
    public int insert(int amount, TransactionContext transactionContext) {
        if (amount <= 0) {
            return 0;
        }
        SGBaseBlockEntity stargate = getStargate();
        if (stargate == null) {
            return 0;
        }
        int space = SGBaseBlockEntity.MAX_ENERGY - stargate.energy;
        int inserted = Math.min(amount, space);
        if (inserted > 0) {
            energyJournal.updateSnapshots(transactionContext);
            stargate.energy += inserted;
            stargate.setChanged();
            return inserted;
        }
        return 0;
    }

    @Override
    public int extract(int amount, TransactionContext transactionContext) {
        // Unidirectional flow: do not allow extracting energy from the stargate.
        return 0;
    }

    public Object getEnergyStorage() {
        return this;
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
    }
}
