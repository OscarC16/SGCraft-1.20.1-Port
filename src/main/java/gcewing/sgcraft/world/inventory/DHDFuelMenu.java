package gcewing.sgcraft.world.inventory;

import gcewing.sgcraft.block.entity.DHDBlockEntity;
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

public class DHDFuelMenu extends AbstractContainerMenu {

    private final DHDBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    public DHDFuelMenu(int containerId, Inventory inv, FriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), ContainerLevelAccess.NULL);
    }

    public DHDFuelMenu(int containerId, Inventory inv, BlockEntity entity, ContainerLevelAccess access) {
        super(ModMenuTypes.DHD_FUEL_MENU.get(), containerId);
        this.blockEntity = (DHDBlockEntity) entity;
        this.levelAccess = access;

        IItemHandler inventory = blockEntity.inventory;

        // Fuel slots (4 slots in 2x2 grid) at positions from original DHDFuelContainer
        this.addSlot(new SlotItemHandler(inventory, 0, 174, 84));
        this.addSlot(new SlotItemHandler(inventory, 1, 192, 84));
        this.addSlot(new SlotItemHandler(inventory, 2, 174, 102));
        this.addSlot(new SlotItemHandler(inventory, 3, 192, 102));

        // Energy levels sync
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return (int)blockEntity.energyInBuffer; }
            @Override public void set(int value) { blockEntity.energyInBuffer = value; }
        });
        addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override public int get() { return (int)blockEntity.maxEnergyBuffer; }
            @Override public void set(int value) { blockEntity.maxEnergyBuffer = value; }
        });

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
            if (index < 4) {
                if (!this.moveItemStackTo(itemstack1, 4, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
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
}
