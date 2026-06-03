package gcewing.sgcraft.client.gui;

import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.network.DialPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
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

    private Button prevPageBtn;
    private Button nextPageBtn;
    private Button actionBtn;
    private final List<Button> addressButtons = new ArrayList<>();

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
        this.clearWidgets();
        this.addressButtons.clear();

        int left = (width - 250) / 2;
        int top = (height - 220) / 2;

        int startIdx = currentPage * ITEMS_PER_PAGE;
        int endIdx = Math.min(startIdx + ITEMS_PER_PAGE, addresses.size());

        for (int i = 0; i < ITEMS_PER_PAGE; i++) {
            int idx = startIdx + i;
            int btnY = top + 90 + i * 20;
            if (idx < addresses.size()) {
                String addr = addresses.get(idx);
                Button btn = Button.builder(Component.literal(SGAddressing.formatAddress(addr, " ", " ")), b -> {
                    selectedAddress = addr;
                }).bounds(left + 20, btnY, 140, 18).build();
                
                this.addRenderableWidget(btn);
                addressButtons.add(btn);
            }
        }

        prevPageBtn = Button.builder(Component.literal("<-"), b -> {
            if (currentPage > 0) {
                currentPage--;
                createButtons();
            }
        }).bounds(left + 20, top + 192, 30, 18).build();
        prevPageBtn.active = (currentPage > 0);
        this.addRenderableWidget(prevPageBtn);

        nextPageBtn = Button.builder(Component.literal("->"), b -> {
            if ((currentPage + 1) * ITEMS_PER_PAGE < addresses.size()) {
                currentPage++;
                createButtons();
            }
        }).bounds(left + 130, top + 192, 30, 18).build();
        nextPageBtn.active = ((currentPage + 1) * ITEMS_PER_PAGE < addresses.size());
        this.addRenderableWidget(nextPageBtn);

        actionBtn = Button.builder(Component.literal("Action"), b -> {
            triggerAction();
        }).bounds(left + 175, top + 90, 55, 98).build();
        this.addRenderableWidget(actionBtn);
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
        double distSq = minecraft.player.distanceToSqr(stargatePos.getX() + 0.5, stargatePos.getY() + 0.5, stargatePos.getZ() + 0.5);
        if (distSq > 16.0 * 16.0) {
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

        int left = (width - 250) / 2;
        int top = (height - 220) / 2;

        // Main Background Panel (Semi-transparent dark slate-blue)
        guiGraphics.fill(left, top, left + 250, top + 220, 0xEE0B1218);
        drawOutline(guiGraphics, left, top, 250, 220, 0xFF00BFFF);
        drawOutline(guiGraphics, left + 1, top + 1, 248, 218, 0x4400BFFF);

        // Header Title
        guiGraphics.drawCenteredString(this.font, this.title, left + 125, top + 8, 0xFF00FFFF);
        guiGraphics.fill(left + 10, top + 19, left + 240, top + 20, 0x8800BFFF);

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

            // Stargate Address Symbols Display
            String symbolAddr = (gate.state == SGBaseBlockEntity.State.Idle) ? gate.homeAddress : gate.dialledAddress;
            if (symbolAddr != null && !symbolAddr.isEmpty()) {
                drawMiniAddressSymbols(guiGraphics, left + 185, top + 24, symbolAddr);
            }

            // Energy Bar
            int barX = left + 15;
            int barY = top + 56;
            int barW = 220;
            int barH = 10;
            guiGraphics.fill(barX, barY, barX + barW, barY + barH, 0xFF222222);
            float energyRatio = (float) gate.energy / gate.MAX_ENERGY;
            int fillW = (int) (energyRatio * (barW - 2));
            if (fillW > 0) {
                guiGraphics.fill(barX + 1, barY + 1, barX + 1 + fillW, barY + barH - 1, 0xFFFF8C00);
            }
            drawOutline(guiGraphics, barX, barY, barW, barH, 0x8800BFFF);

            // Energy text tooltip or overlay
            String energyStr = String.format("%d/%d", gate.energy, gate.MAX_ENERGY);
            Component energyComp = Component.translatable("gui.sgcraft.pad.energy", energyStr);
            guiGraphics.drawCenteredString(this.font, energyComp, left + 125, top + 68, 0xFFAAAAAA);

            // Update Action Button
            if (gate.state == SGBaseBlockEntity.State.Idle) {
                actionBtn.setMessage(Component.translatable("gui.sgcraft.pad.dial"));
                actionBtn.active = !selectedAddress.isEmpty();
            } else {
                actionBtn.setMessage(Component.translatable("gui.sgcraft.pad.disconnect"));
                actionBtn.active = true;
            }
        } else {
            guiGraphics.drawCenteredString(this.font, Component.translatable("message.sgcraft.no_stargate_found"), left + 125, top + 40, 0xFFCC0000);
            actionBtn.active = false;
        }

        // Address book title
        Component bookTitle = Component.translatable("gui.sgcraft.pad.address_book");
        guiGraphics.drawString(this.font, bookTitle, left + 15, top + 78, 0xFF00FFFF, false);

        // Page number
        int totalPages = Math.max(1, (addresses.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE);
        Component pageComp = Component.translatable("gui.sgcraft.pad.page", currentPage + 1, totalPages);
        guiGraphics.drawCenteredString(this.font, pageComp, left + 90, top + 197, 0xFFAAAAAA);

        // Render selected button highlight
        int startIdx = currentPage * ITEMS_PER_PAGE;
        for (int i = 0; i < addressButtons.size(); i++) {
            int idx = startIdx + i;
            if (idx < addresses.size() && addresses.get(idx).equals(selectedAddress)) {
                Button btn = addressButtons.get(i);
                drawOutline(guiGraphics, btn.getX() - 1, btn.getY() - 1, btn.getWidth() + 2, btn.getHeight() + 2, 0xFF00FFCC);
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    private void drawOutline(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        guiGraphics.fill(x, y, x + width, y + 1, color);
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color);
        guiGraphics.fill(x, y, x + 1, y + height, color);
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color);
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
}
