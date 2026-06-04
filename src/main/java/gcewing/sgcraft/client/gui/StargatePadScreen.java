package gcewing.sgcraft.client.gui;

import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.client.ClientAddressBook;
import gcewing.sgcraft.network.DialPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.List;

public class StargatePadScreen extends Screen {

    private final BlockPos stargatePos;
    private final List<String> addresses;
    private int currentPage = 0;
    private String selectedAddress = "";
    private static final int ITEMS_PER_PAGE = 5;

    private StargatePadButton prevPageBtn;
    private StargatePadButton nextPageBtn;
    private StargatePadButton actionBtn;
    private EditBox nameField;
    private final List<StargatePadButton> addressButtons = new ArrayList<>();

    public StargatePadScreen(BlockPos stargatePos, List<String> addresses) {
        super(Component.translatable("gui.sgcraft.pad.title"));
        this.stargatePos = stargatePos;
        this.addresses = addresses;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        createButtons();
    }

    private void createButtons() {
        // Capture any currently typed text to preserve it through recreation
        String currentTyped = "";
        if (nameField != null) {
            currentTyped = nameField.getValue();
        }

        this.clearWidgets();
        this.addressButtons.clear();

        int left = (width - 280) / 2;
        int top = (height - 250) / 2;

        int startIdx = currentPage * ITEMS_PER_PAGE;

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIdx + i;
            int btnY = top + 90 + i * 20;
            if (idx < addresses.size()) {
                String addr = addresses.get(idx);
                String alias = ClientAddressBook.getName(addr);
                String btnText = alias.isEmpty() ? SGAddressing.formatAddress(addr, " ", " ") : alias;

                StargatePadButton btn = new StargatePadButton(left + 20, btnY, 170, 18, Component.literal(btnText), b -> {
                    selectedAddress = addr;
                    if (nameField != null) {
                        nameField.setValue(ClientAddressBook.getName(addr));
                    }
                });
                btn.setSelectedSupplier(() -> selectedAddress.equals(addr));
                
                this.addRenderableWidget(btn);
                addressButtons.add(btn);
            }
        }

        prevPageBtn = new StargatePadButton(left + 20, top + 192, 25, 18, Component.literal("<-"), b -> {
            if (currentPage > 0) {
                currentPage--;
                createButtons();
            }
        });
        prevPageBtn.active = (currentPage > 0);
        this.addRenderableWidget(prevPageBtn);

        nextPageBtn = new StargatePadButton(left + 165, top + 192, 25, 18, Component.literal("->"), b -> {
            if ((currentPage + 1) * ITEMS_PER_PAGE < addresses.size()) {
                currentPage++;
                createButtons();
            }
        });
        nextPageBtn.active = ((currentPage + 1) * ITEMS_PER_PAGE < addresses.size());
        this.addRenderableWidget(nextPageBtn);

        actionBtn = new StargatePadButton(left + 205, top + 90, 55, 98, Component.literal("Action"), b -> {
            triggerAction();
        }, true, false);
        this.addRenderableWidget(actionBtn);

        // Edit box for portal nicknames
        nameField = new EditBox(this.font, left + 70, top + 226, 125, 12, Component.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setBordered(false); // Borderless for custom clean styling
        nameField.setTextColor(0xFF00FFFF);
        nameField.setTextColorUneditable(0xFF888888);
        if (!selectedAddress.isEmpty() && currentTyped.isEmpty()) {
            nameField.setValue(ClientAddressBook.getName(selectedAddress));
        } else {
            nameField.setValue(currentTyped);
        }
        this.addRenderableWidget(nameField);

        // Save alias button
        StargatePadButton saveBtn = new StargatePadButton(left + 205, top + 220, 55, 18, Component.translatable("gui.sgcraft.pad.save"), b -> {
            if (!selectedAddress.isEmpty()) {
                String newName = nameField.getValue().trim();
                ClientAddressBook.setName(selectedAddress, newName);
                createButtons();
            }
        });
        this.addRenderableWidget(saveBtn);
    }

    private void triggerAction() {
        if (minecraft == null || minecraft.level == null) return;
        BlockEntity be = minecraft.level.getBlockEntity(stargatePos);
        if (!(be instanceof SGBaseBlockEntity gate)) return;

        if (gate.state == SGBaseBlockEntity.State.Idle) {
            if (!selectedAddress.isEmpty()) {
                minecraft.getConnection().send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(
                    new DialPacket(stargatePos, selectedAddress)
                ));
            }
        } else {
            minecraft.getConnection().send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(
                new DialPacket(stargatePos, "")
            ));
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (minecraft == null || minecraft.player == null || minecraft.level == null) {
            onClose();
            return;
        }
        
        // Chebyshev/Cubic distance check (dx, dy, dz) to align with server search cube
        double dx = Math.abs(minecraft.player.getX() - (stargatePos.getX() + 0.5));
        double dy = Math.abs(minecraft.player.getY() - (stargatePos.getY() + 0.5));
        double dz = Math.abs(minecraft.player.getZ() - (stargatePos.getZ() + 0.5));
        if (dx > 9.5 || dy > 9.5 || dz > 9.5) {
            onClose();
            return;
        }
        
        BlockEntity be = minecraft.level.getBlockEntity(stargatePos);
        if (!(be instanceof SGBaseBlockEntity gate) || !gate.isMerged) {
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);

        int left = (width - 280) / 2;
        int top = (height - 250) / 2;

        // Main Background Panel (Semi-transparent dark slate-blue glassmorphic theme)
        guiGraphics.fill(left, top, left + 280, top + 250, 0xEE0B1218);
        drawOutline(guiGraphics, left, top, 280, 250, 0x8800BFFF);
        drawOutline(guiGraphics, left + 1, top + 1, 278, 248, 0x3300BFFF);

        // Cyberpunk tech corner brackets
        drawTechCorner(guiGraphics, left, top, 1, 1, 15, 0xFF00FFFF);
        drawTechCorner(guiGraphics, left + 280, top, -1, 1, 15, 0xFF00FFFF);
        drawTechCorner(guiGraphics, left, top + 250, 1, -1, 15, 0xFF00FFFF);
        drawTechCorner(guiGraphics, left + 280, top + 250, -1, -1, 15, 0xFF00FFFF);

        // Header Title
        guiGraphics.drawCenteredString(this.font, this.title, left + 140, top + 8, 0xFF00FFFF);
        guiGraphics.fill(left + 10, top + 19, left + 270, top + 20, 0x6600BFFF);

        BlockEntity be = null;
        if (minecraft != null && minecraft.level != null) {
            be = minecraft.level.getBlockEntity(stargatePos);
        }

        if (be instanceof SGBaseBlockEntity gate) {
            // Local Stargate Name / Address
            String localAddr = SGAddressing.formatAddress(gate.homeAddress, "-", "-");
            Component localComp = Component.translatable("gui.sgcraft.pad.local", localAddr);
            guiGraphics.drawString(this.font, localComp, left + 15, top + 24, 0xFFE0E0E0, false);

            // State Display
            Component stateName = getStateComponent(gate.state);
            Component stateComp = Component.translatable("gui.sgcraft.pad.state", stateName);
            int stateColor = getStateColor(gate.state);
            guiGraphics.drawString(this.font, stateComp, left + 15, top + 34, stateColor, false);

            // Dialed Address if not idle
            if (gate.state != SGBaseBlockEntity.State.Idle) {
                String targetAddr = SGAddressing.formatAddress(gate.dialledAddress, "-", "-");
                Component targetComp = Component.translatable("gui.sgcraft.pad.target", targetAddr);
                guiGraphics.drawString(this.font, targetComp, left + 15, top + 44, 0xFFFFAA00, false);
            }

            // Stargate Address Symbols Display Panel
            String symbolAddr = (gate.state == SGBaseBlockEntity.State.Idle) ? gate.homeAddress : gate.dialledAddress;
            if (symbolAddr != null && !symbolAddr.isEmpty()) {
                int n = symbolAddr.length();
                int cellSize = 12;
                int panelW = n * cellSize + 8;
                int panelH = 18;
                int panelX = left + 215 - panelW / 2;
                int panelY = top + 21;
                guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0x66000000);
                drawOutline(guiGraphics, panelX, panelY, panelW, panelH, 0x6600BFFF);
                drawMiniAddressSymbols(guiGraphics, left + 215, top + 24, symbolAddr);
            }

            // Energy Bar with tech increments
            int barX = left + 15;
            int barY = top + 56;
            int barW = 250;
            int barH = 10;
            guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF08121E);
            float energyRatio = (float) gate.energy / SGBaseBlockEntity.MAX_ENERGY;
            int fillW = (int) (energyRatio * (barW - 2));
            if (fillW > 0) {
                guiGraphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, 0xFFFF8C00);
            }
            // Draw grid tick marks for sci-fi look
            for (int tick = 25; tick < barW; tick += 25) {
                guiGraphics.fill(barX + tick, barY, barX + tick + 1, barY + barH, 0x3300BFFF);
            }
            drawOutline(guiGraphics, barX, barY, barW, barH, 0x8800BFFF);

            // Energy text
            String energyStr = String.format("%d/%d", gate.energy, SGBaseBlockEntity.MAX_ENERGY);
            Component energyComp = Component.translatable("gui.sgcraft.pad.energy", energyStr);
            guiGraphics.drawCenteredString(this.font, energyComp, left + 140, top + 68, 0xFFAAAAAA);

            // Update Action Button
            if (actionBtn instanceof StargatePadButton padBtn) {
                padBtn.isAction = true;
                padBtn.isRed = (gate.state != SGBaseBlockEntity.State.Idle);
                if (gate.state == SGBaseBlockEntity.State.Idle) {
                    padBtn.setMessage(Component.translatable("gui.sgcraft.pad.dial"));
                    padBtn.active = !selectedAddress.isEmpty();
                } else {
                    padBtn.setMessage(Component.translatable("gui.sgcraft.pad.disconnect"));
                    padBtn.active = true;
                }
            }
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("message.sgcraft.no_stargate_found"), left + 140, top + 40, 0xFFCC0000);
            actionBtn.active = false;
        }

        // Address book title
        Component bookTitle = Component.translatable("gui.sgcraft.pad.address_book");
        guiGraphics.drawString(this.font, bookTitle, left + 15, top + 78, 0xFF00FFFF, false);

        // Page number
        int totalPages = Math.max(1, (addresses.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        Component pageComp = Component.translatable("gui.sgcraft.pad.page", currentPage + 1, totalPages);
        guiGraphics.drawCenteredString(this.font, pageComp, left + 105, top + 197, 0xFFAAAAAA);

        // Name input label
        Component nameLabel = Component.translatable("gui.sgcraft.pad.name_label");
        guiGraphics.drawString(this.font, nameLabel, left + 15, top + 224, 0xFFAAAAAA, false);

        // Custom borders for borderless EditBox
        int editX = left + 65;
        int editY = top + 220;
        int editW = 135;
        int editH = 18;
        guiGraphics.fill(editX, editY, editX + editW, editY + editH, 0xFF08121E);
        int borderCol = (nameField != null && nameField.isFocused()) ? 0xFF00FFCC : 0x8800BFFF;
        drawOutline(guiGraphics, editX - 1, editY - 1, editW + 2, editH + 2, borderCol);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    public static void drawOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    private void drawTechCorner(GuiGraphics guiGraphics, int x, int y, int dirX, int dirY, int length, int color) {
        int xStart, xEnd;
        if (dirX > 0) {
            xStart = x;
            xEnd = x + length;
        } else {
            xStart = x - length;
            xEnd = x;
        }
        
        int yStart, yEnd;
        if (dirY > 0) {
            yStart = y;
            yEnd = y + length;
        } else {
            yStart = y - length;
            yEnd = y;
        }
        
        int hY1 = dirY > 0 ? y : y - 2;
        int hY2 = dirY > 0 ? y + 2 : y;
        guiGraphics.fill(xStart, hY1, xEnd, hY2, color);
        
        int vX1 = dirX > 0 ? x : x - 2;
        int vX2 = dirX > 0 ? x + 2 : x;
        guiGraphics.fill(vX1, yStart, vX2, yEnd, color);
    }

    private Component getStateComponent(SGBaseBlockEntity.State state) {
        String key = "gui.sgcraft.state." + state.name().toLowerCase();
        return Component.translatable(key);
    }

    private int getStateColor(SGBaseBlockEntity.State state) {
        return switch (state) {
            case Idle -> 0xFF00FF00;       // Green
            case Dialing -> 0xFFFF9900;    // Orange
            case Connected -> 0xFF00FFFF;  // Cyan
            case Disconnecting -> 0xFFFF3333; // Red
            case Transient -> 0xFFCC00FF;  // Purple
            case InterDialling -> 0xFFFFCC00; // Yellow
        };
    }

    protected void drawMiniAddressSymbols(GuiGraphics guiGraphics, int x, int y, String address) {
        int n = address.length();
        int cellSize = 12;
        int symbolWidth = 48;
        int symbolHeight = 48;
        int symbolsPerRow = 10;
        int x0 = x - (n * cellSize) / 2;
        int y0 = y;

        for (int i = 0; i < n; i++) {
            char c = address.charAt(i);
            int s = SGAddressing.charToSymbol(c);
            if (s < 0) continue;

            int row = s / symbolsPerRow;
            int col = s % symbolsPerRow;

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SGScreen.SYMBOL_TEXTURE, 
                x0 + i * cellSize, y0, 
                (float)(col * symbolWidth), (float)(row * symbolHeight), 
                cellSize, cellSize, 
                symbolWidth, symbolHeight, 
                512, 256);
        }
    }

    // --- Custom Rendered Cyberpunk Button Class ---
    public static class StargatePadButton extends AbstractWidget {
        public interface OnPress {
            void onPress(StargatePadButton button);
        }

        private final OnPress onPress;
        public boolean isAction;
        public boolean isRed;
        private java.util.function.BooleanSupplier selectedSupplier = () -> false;

        public StargatePadButton(int x, int y, int width, int height, Component message, OnPress onPress, boolean isAction, boolean isRed) {
            super(x, y, width, height, message);
            this.onPress = onPress;
            this.isAction = isAction;
            this.isRed = isRed;
        }

        public StargatePadButton(int x, int y, int width, int height, Component message, OnPress onPress) {
            this(x, y, width, height, message, onPress, false, false);
        }

        public void setSelectedSupplier(java.util.function.BooleanSupplier selectedSupplier) {
            this.selectedSupplier = selectedSupplier;
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

            boolean hovered = this.isHoveredOrFocused();
            boolean selected = selectedSupplier.getAsBoolean();
            
            int bgColor;
            int borderColor;
            int textColor;

            if (!this.active) {
                bgColor = 0x221A2630;
                borderColor = 0x44334E68;
                textColor = 0x669EADBA;
            } else if (isAction) {
                if (isRed) {
                    bgColor = hovered ? 0x66FF3333 : 0x33FF3333;
                    borderColor = hovered ? 0xFFFF6666 : 0xFFFF3333;
                    textColor = 0xFFFFFFFF;
                } else {
                    bgColor = hovered ? 0x6600FF66 : 0x3300FF66;
                    borderColor = hovered ? 0xFF66FF66 : 0xFF00FF66;
                    textColor = 0xFFFFFFFF;
                }
            } else {
                bgColor = hovered ? 0x4400BFFF : 0x1100BFFF;
                borderColor = hovered ? 0xFF00FFCC : 0x8800BFFF;
                textColor = hovered ? 0xFFFFFFFF : 0xFF00FFFF;
            }

            if (selected) {
                borderColor = 0xFF00FFCC;
                bgColor = hovered ? 0x5500FFCC : 0x2200FFCC;
            }

            // Draw clean flat background
            guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bgColor);

            // Draw clean high-tech border outline
            StargatePadScreen.drawOutline(guiGraphics, this.getX(), this.getY(), this.width, this.height, borderColor);

            // Draw centered text
            int textY = this.getY() + (this.height - 8) / 2;
            guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, textY, textColor);
        }

        @Override
        protected void updateWidgetNarration(net.minecraft.client.gui.narration.NarrationElementOutput narrationElementOutput) {
            // Empty
        }
    }
}
