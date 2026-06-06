package gcewing.sgcraft.world.inventory;

import gcewing.sgcraft.block.entity.NaquadahGeneratorBlockEntity;
import gcewing.sgcraft.registry.ModMenuTypes;
import gcewing.sgcraft.registry.ModItems;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("removal")
public class NaquadahGeneratorMenu extends AbstractContainerMenu {

    private final NaquadahGeneratorBlockEntity blockEntity;
    private final ContainerLevelAccess levelAccess;

    // Client-side constructor
    public NaquadahGeneratorMenu(int containerId, Inventory inv, RegistryFriendlyByteBuf extraData) {
        this(containerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), ContainerLevelAccess.NULL);
    }

    // Server-side constructor
    public NaquadahGeneratorMenu(int containerId, Inventory inv, BlockEntity entity, ContainerLevelAccess access) {
        super(ModMenuTypes.NAQUADAH_GENERATOR_MENU.get(), containerId);
        this.blockEntity = (NaquadahGeneratorBlockEntity) entity;
        this.levelAccess = access;

        // Data Slots for synchronization

        // 1. Energy lower 16 bits
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.energy & 0xFFFF;
            }

            @Override
            public void set(int value) {
                blockEntity.energy = (blockEntity.energy & 0xFFFF0000) | (value & 0xFFFF);
            }
        });

        // 2. Energy upper 16 bits
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (blockEntity.energy >> 16) & 0xFFFF;
            }

            @Override
            public void set(int value) {
                blockEntity.energy = (blockEntity.energy & 0x0000FFFF) | ((value & 0xFFFF) << 16);
            }
        });

        // 3. fuelTicks
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.fuelTicks;
            }

            @Override
            public void set(int value) {
                blockEntity.fuelTicks = value;
            }
        });

        // 4. maxFuelTicks
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.maxFuelTicks;
            }

            @Override
            public void set(int value) {
                blockEntity.maxFuelTicks = value;
            }
        });

        // 5. enabled
        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return blockEntity.enabled ? 1 : 0;
            }

            @Override
            public void set(int value) {
                blockEntity.enabled = value == 1;
            }
        });

        IItemHandler inventory = blockEntity.inventory;

        // Fuel Slot 0 (index 0) at x=71, y=36
        this.addSlot(new SlotItemHandler(inventory, 0, 71, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.NAQUADAH.get());
            }
        });

        // Fuel Slot 1 (index 1) at x=89, y=36
        this.addSlot(new SlotItemHandler(inventory, 1, 89, 36) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.is(ModItems.NAQUADAH.get());
            }
        });

        // Player inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Player hotbar
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
        }
    }

    public NaquadahGeneratorBlockEntity getBlockEntity() {
        return blockEntity;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index < 2) { // From fuel slots to player inventory
                if (!this.moveItemStackTo(itemstack1, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else { // From player inventory to fuel slots
                if (itemstack1.is(ModItems.NAQUADAH.get())) {
                    if (!this.moveItemStackTo(itemstack1, 0, 2, false)) {
                        return ItemStack.EMPTY;
                    }
                } else {
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
}
