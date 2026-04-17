package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.item.ItemStack;
import gcewing.sgcraft.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DHDBlockEntity extends BlockEntity {
    private static final Logger LOGGER = LoggerFactory.getLogger("SGCraft");
    
    public enum DHDState {
        IDLE, LINKED, ACTIVE
    }

    // Configuration options (Updated to 13x13x13 total volume)
    public static int linkRangeX = 6; // Total width 13
    public static int linkRangeY = 6; // Total height 13
    public static int linkRangeZ = 6; // Total depth 13

    public DHDState getDHDState() {
        if (!isLinkedToStargate) return DHDState.IDLE;
        SGBaseBlockEntity stargate = getLinkedStargateTE();
        if (stargate != null && stargate.isActive()) return DHDState.ACTIVE;
        return DHDState.LINKED;
    }

    public boolean isLinkedToStargate = false;
    public BlockPos linkedStargatePos = BlockPos.ZERO;
    public String enteredAddress = ""; // Used by DHDScreen

    public double energyInBuffer = 0;
    public double maxEnergyBuffer = 100000;

    public final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.is(ModItems.NAQUADAH.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final LazyOptional<IItemHandler> inventoryHolder = LazyOptional.of(() -> inventory);

    public DHDBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DHD_BLOCK_ENTITY.get(), pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;

        // Auto-link if not linked, every 2 seconds
        if (!isLinkedToStargate && level.getGameTime() % 40 == 0) {
            checkForLink();
        }
        
        // Verify link if linked, every 5 seconds
        if (isLinkedToStargate && level.getGameTime() % 100 == 0) {
            getLinkedStargateTE(); // This now triggers verification/cleanup
        }
    }

    public void checkForLink() {
        if (level == null || level.isClientSide) return;
        if (isLinkedToStargate) return;

        for (int i = -linkRangeX; i <= linkRangeX; i++) {
            for (int j = -linkRangeY; j <= linkRangeY; j++) {
                for (int k = -linkRangeZ; k <= linkRangeZ; k++) {
                    // Skip checking own position
                    if (i == 0 && j == 0 && k == 0) continue;
                    
                    BlockPos p = worldPosition.offset(i, j, k);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be instanceof SGBaseBlockEntity targetStargate) {
                        if (linkToStargate(targetStargate)) {
                            LOGGER.info("DHD at {} auto-linked to Stargate at {}", worldPosition, p);
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

            // Notify clients
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.sendBlockUpdated(targetStargate.getBlockPos(), targetStargate.getBlockState(), targetStargate.getBlockState(), 3);
            return true;
        }
        return false;
    }

    public void clearLinkToStargate() {
        LOGGER.info("DHD at {} unlinking from Stargate at {}", worldPosition, linkedStargatePos);
        this.isLinkedToStargate = false;
        this.linkedStargatePos = BlockPos.ZERO;
        this.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public SGBaseBlockEntity getLinkedStargateTE() {
        if (isLinkedToStargate && level != null) {
            BlockEntity be = level.getBlockEntity(linkedStargatePos);
            if (be instanceof SGBaseBlockEntity targetStargate) {
                // Heartbeat/Verification: Ensure Stargate still thinks it is linked TO US
                if (targetStargate.isLinkedToController && worldPosition.equals(targetStargate.linkedControllerPos)) {
                    return targetStargate;
                } else {
                    // Stargate is linked to someone else or lost our link, clear locally
                    clearLinkToStargate();
                }
            } else if (!level.isClientSide) {
                // Stargate block is gone, clear locally
                clearLinkToStargate();
            }
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("isLinked", isLinkedToStargate);
        tag.putInt("stargateX", linkedStargatePos.getX());
        tag.putInt("stargateY", linkedStargatePos.getY());
        tag.putInt("stargateZ", linkedStargatePos.getZ());
        tag.putString("enteredAddress", enteredAddress);
        tag.putDouble("energy", energyInBuffer);
        tag.put("inventory", inventory.serializeNBT());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isLinkedToStargate = tag.getBoolean("isLinked");
        linkedStargatePos = new BlockPos(tag.getInt("stargateX"), tag.getInt("stargateY"), tag.getInt("stargateZ"));
        enteredAddress = tag.getString("enteredAddress");
        energyInBuffer = tag.getDouble("energy");
        inventory.deserializeNBT(tag.getCompound("inventory"));
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryHolder.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryHolder.invalidate();
    }
}
