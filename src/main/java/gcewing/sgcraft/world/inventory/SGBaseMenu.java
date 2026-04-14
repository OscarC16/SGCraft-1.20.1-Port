package gcewing.sgcraft.world.inventory;

import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class SGBaseMenu extends AbstractContainerMenu {

    private final SGBaseBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // Client-side constructor
    public SGBaseMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), ContainerLevelAccess.NULL);
    }

    // Server-side constructor
    public SGBaseMenu(int containerId, Inventory inv, BlockEntity entity, ContainerLevelAccess access) {
        super(ModMenuTypes.SG_BASE_MENU.get(), containerId);
        this.blockEntity = (SGBaseBlockEntity) entity;
        this.levelAccess = access;

        IItemHandler inventory = blockEntity.inventory;

        // Camouflage slots (5 slots)
        for (int i = 0; i < 5; i++) {
            this.addSlot(new SGCamouflageSlot(inventory, i, 48 + i * 18, 104));
        }

        // Player inventory
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public SGBaseBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 5) { // From camouflage slots to player inventory
                if (!this.moveItemStackTo(itemstack1, 5, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory to camouflage slots
                if (!this.moveItemStackTo(itemstack1, 0, 5, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, blockEntity.getBlockState().getBlock());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 48 + j * 18, 126 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 48 + i * 18, 184));
        }
    }

    /**
     * Custom slot for camouflage blocks with restrictions.
     */
    private static class SGCamouflageSlot extends SlotItemHandler {
        public SGCamouflageSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem))
                return false;

            net.minecraft.world.level.block.Block block = blockItem.getBlock();
            net.minecraft.world.level.block.state.BlockState state = block.defaultBlockState();
            
            // Check for opaque and full cube
            return state.canOcclude() && net.minecraft.world.level.block.Block.isShapeFullBlock(state.getCollisionShape(net.minecraft.world.level.EmptyBlockGetter.INSTANCE, net.minecraft.core.BlockPos.ZERO));
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return 1;
        }
    }
}
