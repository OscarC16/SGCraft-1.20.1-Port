package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.block.NaquadahGeneratorBlock;
import gcewing.sgcraft.registry.ModBlockEntities;
import gcewing.sgcraft.registry.ModItems;
import gcewing.sgcraft.world.inventory.NaquadahGeneratorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("removal")
public class NaquadahGeneratorBlockEntity extends BlockEntity implements EnergyHandler, MenuProvider {

    public static final int MAX_ENERGY = 1000000;
    public int energy = 0;

    public int fuelTicks = 0;
    public int maxFuelTicks = 0;
    public boolean enabled = true;

    public final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(ModItems.NAQUADAH.get());
        }
    };

    private final SnapshotJournal<Integer> energyJournal = new SnapshotJournal<Integer>() {
        @Override
        protected Integer createSnapshot() {
            return energy;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            energy = snapshot;
            setChanged();
        }

        @Override
        protected void onRootCommit(Integer snapshot) {
            setChanged();
        }
    };

    public NaquadahGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.NAQUADAH_GENERATOR_BLOCK_ENTITY.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide()) {
            return;
        }

        boolean wasActive = state.getValue(NaquadahGeneratorBlock.ACTIVE);
        boolean isActive = false;

        // 1. Generation Logic
        if (enabled) {
            if (fuelTicks > 0) {
                fuelTicks--;
                energy = Math.min(MAX_ENERGY, energy + 1000);
                isActive = true;
                setChanged();
            } else {
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack fuelStack = inventory.getStackInSlot(i);
                    if (!fuelStack.isEmpty() && energy < MAX_ENERGY) {
                        if (fuelStack.is(ModItems.NAQUADAH.get())) {
                            inventory.extractItem(i, 1, false);
                            fuelTicks = 200;
                            maxFuelTicks = 200;
                            energy = Math.min(MAX_ENERGY, energy + 1000);
                            isActive = true;
                            setChanged();
                            break;
                        }
                    }
                }
            }
        }

        // Update block state if activity changed
        if (isActive != wasActive) {
            level.setBlock(pos, state.setValue(NaquadahGeneratorBlock.ACTIVE, isActive), 3);
        }

        // 2. Active Energy Pushing Logic to adjacent sides
        if (energy > 0) {
            int maxPushLimit = 10000;
            Direction facing = state.getValue(NaquadahGeneratorBlock.FACING);
            Direction leftSide = facing.getCounterClockWise();
            Direction rightSide = facing.getClockWise();
            Direction[] sides = { leftSide, rightSide };
            for (Direction side : sides) {
                if (energy <= 0) break;
                BlockPos targetPos = pos.relative(side);
                EnergyHandler targetHandler = level.getCapability(Capabilities.Energy.BLOCK, targetPos, side.getOpposite());
                if (targetHandler != null) {
                    int toPush = Math.min(energy, maxPushLimit);
                    try (Transaction transaction = Transaction.openRoot()) {
                        int inserted = targetHandler.insert(toPush, transaction);
                        if (inserted > 0) {
                            this.energy -= inserted;
                            transaction.commit();
                            setChanged();
                        }
                    }
                }
            }
        }
    }

    @Override
    public long getAmountAsLong() {
        return energy;
    }

    @Override
    public long getCapacityAsLong() {
        return MAX_ENERGY;
    }

    @Override
    public int insert(int amount, TransactionContext transactionContext) {
        // Generators only produce energy, they do not accept input.
        return 0;
    }

    @Override
    public int extract(int amount, TransactionContext transactionContext) {
        if (amount <= 0) {
            return 0;
        }
        int toExtract = Math.min(amount, energy);
        if (toExtract > 0) {
            energyJournal.updateSnapshots(transactionContext);
            this.energy -= toExtract;
            setChanged();
            return toExtract;
        }
        return 0;
    }

    @Override
    protected void saveAdditional(@NotNull ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("energy", energy);
        output.putInt("fuelTicks", fuelTicks);
        output.putInt("maxFuelTicks", maxFuelTicks);
        output.putBoolean("enabled", enabled);
        inventory.serialize(output.child("inventory"));
    }

    @Override
    public void loadAdditional(@NotNull ValueInput input) {
        super.loadAdditional(input);
        energy = input.getIntOr("energy", 0);
        fuelTicks = input.getIntOr("fuelTicks", 0);
        maxFuelTicks = input.getIntOr("maxFuelTicks", 0);
        enabled = input.getBooleanOr("enabled", true);
        inventory.deserialize(input.childOrEmpty("inventory"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.putInt("energy", energy);
        tag.putInt("fuelTicks", fuelTicks);
        tag.putInt("maxFuelTicks", maxFuelTicks);
        tag.putBoolean("enabled", enabled);
        
        net.minecraft.world.level.storage.TagValueOutput output = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING, registries);
        inventory.serialize(output);
        tag.put("inventory", output.buildResult());
        
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.sgcraft.naquadah_generator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new NaquadahGeneratorMenu(containerId, playerInventory, this, ContainerLevelAccess.create(level, worldPosition));
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level != null && !this.level.isClientSide()) {
            for (int i = 0; i < this.inventory.getSlots(); i++) {
                net.minecraft.world.item.ItemStack stack = this.inventory.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(this.level, pos.getX(), pos.getY(), pos.getZ(), stack);
                }
            }
        }
        super.preRemoveSideEffects(pos, state);
    }
}
