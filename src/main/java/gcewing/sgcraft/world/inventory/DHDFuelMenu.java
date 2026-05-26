package gcewing.sgcraft.world.inventory;

import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.registry.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

@SuppressWarnings("removal")
public class DHDFuelMenu extends AbstractContainerMenu {

    private final DHDBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // Client-side constructor
    public DHDFuelMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), ContainerLevelAccess.NULL);
    }

    // Server-side constructor
    public DHDFuelMenu(int containerId, Inventory inv, BlockEntity entity, ContainerLevelAccess access) {
        super(ModMenuTypes.DHD_FUEL_MENU.get(), containerId);
        this.blockEntity = (DHDBlockEntity) entity;
        this.levelAccess = access;

        // Sync linked stargate energy to client
        this.addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                DHDBlockEntity dhd = getBlockEntity();
                gcewing.sgcraft.block.entity.SGBaseBlockEntity sg = dhd.getLinkedStargateTE();
                return sg != null ? sg.energy : 0;
            }

            @Override
            public void set(int value) {
                getBlockEntity().energyInBuffer = value;
            }
        });

        IItemHandler inventory = blockEntity.inventory;

        // Fuel slots (2x2 grid)
        for (int i = 0; i < 4; i++) {
            int cx = i % 2;
            int cy = i / 2;
            this.addSlot(new DHDFuelSlot(inventory, i, 174 + cx * 18, 84 + cy * 18));
        }

        // Player inventory
        addPlayerInventory(inv);
        addPlayerHotbar(inv);
    }

    public DHDBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 4) { // From fuel slots to player inventory
                if (!this.moveItemStackTo(itemstack1, 4, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory to fuel slots
                if (!this.moveItemStackTo(itemstack1, 0, 4, false)) {
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
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 48 + j * 18, 124 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 48 + i * 18, 182));
        }
    }

    private static class DHDFuelSlot extends SlotItemHandler {
        public DHDFuelSlot(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.getItem() == gcewing.sgcraft.registry.ModItems.NAQUADAH.get();
        }
    }
}
