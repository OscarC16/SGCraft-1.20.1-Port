package gcewing.sgcraft.block.entity;

import gcewing.sgcraft.SGState;
import gcewing.sgcraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.Direction;

/**
 * Tile entity for the Stargate Base block — the center-bottom of the multiblock structure.
 * Manages the merge state, Stargate operational state, and chevron data.
 */
public class SGBaseBlockEntity extends BlockEntity {

    // Multiblock merge state
    public boolean isMerged = false;

    // Operational state (for future use — dialling, connected, etc.)
    public SGState state = SGState.Idle;

    // Chevron/dialling data
    public int numEngagedChevrons = 0;
    public boolean hasChevronUpgrade = false;
    public boolean hasIrisUpgrade = false;

    // Ring angle (for future animation)
    public double ringAngle = 0;
    public double lastRingAngle = 0;

    // DHD controller link (for future use)
    public boolean isLinkedToController = false;
    public BlockPos linkedControllerPos = BlockPos.ZERO;

    // Stargate Address data
    public String homeAddress = "";
    public String addressError = null;

    // Inventory for camouflage (5 slots)
    public final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };
    private final LazyOptional<IItemHandler> inventoryHolder = LazyOptional.of(() -> inventory);

    public SGBaseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SG_BASE_BLOCK_ENTITY.get(), pos, state);
    }

    public int getNumChevrons() {
        return hasChevronUpgrade ? 9 : 7;
    }

    public boolean isActive() {
        return state != SGState.Idle;
    }

    public void setMerged(boolean merged) {
        if (isMerged != merged) {
            isMerged = merged;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public void connectOrDisconnect(String address, Player player) {
        if (level == null || level.isClientSide) return;
        
        if (state == SGState.Idle) {
            if (!address.isEmpty()) {
                // Future: implementation of dialing sequence
                System.out.println("Stargate at " + worldPosition + " starting dial to " + address);
            }
        } else {
            // Future: implementation of disconnection
            System.out.println("Stargate at " + worldPosition + " disconnecting");
        }
    }

    // --- NBT Serialization ---

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("isMerged", isMerged);
        tag.putInt("state", state.ordinal());
        tag.putInt("numEngagedChevrons", numEngagedChevrons);
        tag.putBoolean("hasChevronUpgrade", hasChevronUpgrade);
        tag.putBoolean("hasIrisUpgrade", hasIrisUpgrade);
        tag.putDouble("ringAngle", ringAngle);
        tag.putBoolean("isLinkedToController", isLinkedToController);
        tag.putInt("linkedX", linkedControllerPos.getX());
        tag.putInt("linkedY", linkedControllerPos.getY());
        tag.putInt("linkedZ", linkedControllerPos.getZ());
        tag.putString("homeAddress", homeAddress != null ? homeAddress : "");
        if (addressError != null) tag.putString("addressError", addressError);
        tag.put("inventory", inventory.serializeNBT());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        isMerged = tag.getBoolean("isMerged");
        state = SGState.valueOf(tag.getInt("state"));
        numEngagedChevrons = tag.getInt("numEngagedChevrons");
        hasChevronUpgrade = tag.getBoolean("hasChevronUpgrade");
        hasIrisUpgrade = tag.getBoolean("hasIrisUpgrade");
        ringAngle = tag.getDouble("ringAngle");
        isLinkedToController = tag.getBoolean("isLinkedToController");
        linkedControllerPos = new BlockPos(tag.getInt("linkedX"), tag.getInt("linkedY"), tag.getInt("linkedZ"));
        homeAddress = tag.getString("homeAddress");
        addressError = tag.contains("addressError") ? tag.getString("addressError") : null;
        inventory.deserializeNBT(tag.getCompound("inventory"));
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

    // --- Render Bounding Box ---

    @Override
    public AABB getRenderBoundingBox() {
        // The Stargate ring extends ~2.5 blocks in each direction from the base and 5 blocks up
        BlockPos pos = getBlockPos();
        return new AABB(
            pos.getX() - 2, pos.getY(), pos.getZ() - 2,
            pos.getX() + 3, pos.getY() + 5, pos.getZ() + 3
        );
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
