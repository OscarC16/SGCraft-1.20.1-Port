package gcewing.sgcraft.client.gui;

import gcewing.sgcraft.block.entity.NaquadahGeneratorBlockEntity;
import gcewing.sgcraft.world.inventory.NaquadahGeneratorMenu;
import gcewing.sgcraft.network.ToggleGeneratorPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class NaquadahGeneratorScreen extends AbstractContainerScreen<NaquadahGeneratorMenu> {

    private GeneratorToggleButton toggleBtn;

    public NaquadahGeneratorScreen(NaquadahGeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;

        toggleBtn = new GeneratorToggleButton(left + 20, top + 35, 45, 18, b -> {
            if (minecraft != null && minecraft.getConnection() != null) {
                minecraft.getConnection().send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(
                    new ToggleGeneratorPacket(menu.getBlockEntity().getBlockPos())
                ));
            }
        });
        this.addRenderableWidget(toggleBtn);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;

        // 1. Semi-transparent dark slate-blue glassmorphic background
        guiGraphics.fill(left, top, left + imageWidth, top + imageHeight, 0xEE0B1218);
        
        // 2. High-tech cian outline border
        StargatePadScreen.drawOutline(guiGraphics, left, top, imageWidth, imageHeight, 0x8800BFFF);
        StargatePadScreen.drawOutline(guiGraphics, left + 1, top + 1, imageWidth - 2, imageHeight - 2, 0x3300BFFF);

        // 3. Fuel Slots Highlight (Neon cyan boxes around slot 0 at 71, 36 and slot 1 at 89, 36)
        drawSlotHighlight(guiGraphics, left + 71, top + 36);
        drawSlotHighlight(guiGraphics, left + 89, top + 36);

        // 4. Energy Bar (Vertical) at 140, 20, width 16, height 50
        int barX = left + 140;
        int barY = top + 20;
        int barW = 16;
        int barH = 50;
        guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF08121E);
        StargatePadScreen.drawOutline(guiGraphics, barX, barY, barW, barH, 0x8800BFFF);

        NaquadahGeneratorBlockEntity gen = menu.getBlockEntity();
        float energyRatio = (float) gen.energy / NaquadahGeneratorBlockEntity.MAX_ENERGY;
        int fillH = (int) (energyRatio * (barH - 2));
        if (fillH > 0) {
            guiGraphics.fill(barX + 1, barY + barH - 1 - fillH, barX + barW - 1, barY + barH - 1, 0xFF00FFCC);
        }

        // Draw horizontal grid marks in energy bar
        for (int step = 10; step < barH; step += 10) {
            guiGraphics.fill(barX, barY + step, barX + barW, barY + step + 1, 0x3300BFFF);
        }

        // 5. Fuel Progress Bar (Horizontal) below slot
        int fuelBarX = left + 65;
        int fuelBarY = top + 58;
        int fuelBarW = 46;
        int fuelBarH = 6;
        guiGraphics.fill(fuelBarX, fuelBarY, fuelBarX + fuelBarW, fuelBarY + fuelBarH, 0xFF08121E);
        StargatePadScreen.drawOutline(guiGraphics, fuelBarX, fuelBarY, fuelBarW, fuelBarH, 0x8800BFFF);

        if (gen.maxFuelTicks > 0 && gen.fuelTicks > 0) {
            float fuelRatio = (float) gen.fuelTicks / gen.maxFuelTicks;
            int fuelFillW = (int) (fuelRatio * (fuelBarW - 2));
            if (fuelFillW > 0) {
                guiGraphics.fill(fuelBarX + 1, fuelBarY + 1, fuelBarX + 1 + fuelFillW, fuelBarY + fuelBarH - 1, 0xFFFF8C00);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Custom color for title labels
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF00FFFF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xFFAAAAAA, false);

        // Render generator state text
        NaquadahGeneratorBlockEntity gen = menu.getBlockEntity();
        Component stateComp;
        int stateColor;
        if (!gen.enabled) {
            stateComp = Component.translatable("gui.sgcraft.generator.state.disabled");
            stateColor = 0xFFFF3333; // Red
        } else if (gen.fuelTicks > 0) {
            stateComp = Component.translatable("gui.sgcraft.generator.state.generating");
            stateColor = 0xFF00FF00; // Green
        } else {
            stateComp = Component.translatable("gui.sgcraft.generator.state.no_fuel");
            stateColor = 0xFFFFCC00; // Yellow
        }
        guiGraphics.drawString(this.font, stateComp, 20, 24, stateColor, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTicks);
        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(guiGraphics, mouseX, mouseY);

        // Tooltip for Energy Bar
        int left = (width - imageWidth) / 2;
        int top = (height - imageHeight) / 2;
        int barX = left + 140;
        int barY = top + 20;
        int barW = 16;
        int barH = 50;

        if (mouseX >= barX && mouseX < barX + barW && mouseY >= barY && mouseY < barY + barH) {
            NaquadahGeneratorBlockEntity gen = menu.getBlockEntity();
            String tooltipStr = String.format("%d / %d FE", gen.energy, NaquadahGeneratorBlockEntity.MAX_ENERGY);
            guiGraphics.setComponentTooltipForNextFrame(this.font, java.util.List.of(Component.literal(tooltipStr)), mouseX, mouseY);
        }
    }

    private void drawSlotHighlight(GuiGraphics guiGraphics, int slotX, int slotY) {
        guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY, 0xFF00FFCC);
        guiGraphics.fill(slotX - 1, slotY + 16, slotX + 17, slotY + 17, 0xFF00FFCC);
        guiGraphics.fill(slotX - 1, slotY, slotX, slotY + 16, 0xFF00FFCC);
        guiGraphics.fill(slotX + 16, slotY, slotX + 17, slotY + 16, 0xFF00FFCC);
        guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0x3300FFCC);
    }

    // --- Cyberpunk ON/OFF Toggle Button Class ---
    private class GeneratorToggleButton extends AbstractWidget {
        private final OnPress onPress;

        public interface OnPress {
            void onPress(GeneratorToggleButton button);
        }

        public GeneratorToggleButton(int x, int y, int width, int height, OnPress onPress) {
            super(x, y, width, height, Component.empty());
            this.onPress = onPress;
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDouble) {
            if (this.active && this.visible && event.button() == 0) {
                double mouseX = event.x();
                double mouseY = event.y();
                if (mouseX >= this.getX() && mouseX < this.getX() + this.width &&
                    mouseY >= this.getY() && mouseY < this.getY() + this.height) {
                    this.playDownSound(Minecraft.getInstance().getSoundManager());
                    this.onPress.onPress(this);
                    return true;
                }
            }
            return false;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
            if (!this.visible) return;

            boolean enabled = menu.getBlockEntity().enabled;
            boolean hovered = this.isHoveredOrFocused();

            int bgColor = enabled 
                ? (hovered ? 0x6600FF66 : 0x3300FF66) 
                : (hovered ? 0x66FF3333 : 0x33FF3333);
            int borderColor = enabled ? 0xFF00FF66 : 0xFFFF3333;
            int textColor = 0xFFFFFFFF;
            Component btnMsg = enabled 
                ? Component.translatable("gui.sgcraft.generator.on")
                : Component.translatable("gui.sgcraft.generator.off");

            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);
            StargatePadScreen.drawOutline(guiGraphics, this.getX(), this.getY(), this.width, this.height, borderColor);

            int textY = this.getY() + (this.height - 8) / 2;
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, btnMsg, this.getX() + this.width / 2, textY, textColor);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
            // Empty
        }
    }
}
