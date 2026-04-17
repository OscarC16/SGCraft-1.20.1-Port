package gcewing.sgcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.network.DialPacket;
import gcewing.sgcraft.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;


public class DHDScreen extends Screen {

    public static final ResourceLocation DHD_GUI_TEXTURE = new ResourceLocation(SGCraft.MODID, "textures/gui/dhd_gui.png");
    public static final ResourceLocation DHD_CENTRE_TEXTURE = new ResourceLocation(SGCraft.MODID, "textures/gui/dhd_centre.png");
    public static final ResourceLocation SYMBOL_TEXTURE = new ResourceLocation(SGCraft.MODID, "textures/gui/symbols48.png");

    private static final int TEXTURE_W = 512;
    private static final int TEXTURE_H = 256;

    final static int dhdWidth = 320;
    final static int dhdHeight = 120;
    final static double dhdRadius1 = dhdWidth * 0.1;
    final static double dhdRadius2 = dhdWidth * 0.275;
    final static double dhdRadius3 = dhdWidth * 0.45;

    int dhdTop, dhdCentreX, dhdCentreY;
    double buttonRX, buttonRY;
    String enteredAddress = "";
    private final DHDBlockEntity dhd;

    public DHDScreen(DHDBlockEntity dhd) {
        super(Component.translatable("gui.sgcraft.dhd.title"));
        this.dhd = dhd;
        this.enteredAddress = dhd.enteredAddress;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        super.init();
        dhdTop = height - dhdHeight;
        dhdCentreX = width / 2;
        dhdCentreY = dhdTop + dhdHeight / 2;
        
        buttonRX = dhdWidth * 48 / 512.0;
        buttonRY = dhdHeight * 48 / 256.0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        renderBackground(guiGraphics);
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        drawBackgroundImage(guiGraphics);
        drawOrangeButton(guiGraphics);
        
        drawEnteredSymbols(guiGraphics);
        drawEnteredString(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    void drawBackgroundImage(GuiGraphics guiGraphics) {
        // Original SGCraft texture is 512x256, we scale it to dhdWidth x dhdHeight (320x120)
        guiGraphics.blit(DHD_GUI_TEXTURE, (width - dhdWidth) / 2, height - dhdHeight, dhdWidth, dhdHeight, 0, 0, TEXTURE_W, TEXTURE_H, TEXTURE_W, TEXTURE_H);
    }

    void drawOrangeButton(GuiGraphics guiGraphics) {
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        boolean connected = stargate != null && stargate.isMerged;
        boolean active = connected && stargate.isActive();

        if (stargate == null || !stargate.isMerged) {
            RenderSystem.setShaderColor(0.2f, 0.2f, 0.2f, 1.0f);
        } else if (active) {
            RenderSystem.setShaderColor(1.0f, 0.5f, 0.0f, 1.0f);
        } else {
            RenderSystem.setShaderColor(0.5f, 0.25f, 0.0f, 1.0f);
        }

        // Scaled blit for the orange button dome
        // Source coordinates: 64, 0 (W=64, H=48) in dhd_centre.png (128x64)
        guiGraphics.blit(DHD_CENTRE_TEXTURE, (int)(dhdCentreX - buttonRX), (int)(dhdCentreY - buttonRY - 6), 
                         (int)(2 * buttonRX), (int)(1.5 * buttonRY), 
                         64, 0, 64, 48, 128, 64);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    void drawEnteredSymbols(GuiGraphics guiGraphics) {
        int n = enteredAddress.length();
        if (n == 0) return;
        
        int cellSize = 24;
        int x0 = width / 2 - (n * cellSize) / 2;
        // Moved down from dhdTop - 80 to dhdTop - 74 to fix upward offset
        int y0 = dhdTop - 74;

        for (int i = 0; i < n; i++) {
            char c = enteredAddress.charAt(i);
            int s = SGAddressing.charToSymbol(c);
            if (s < 0) continue;

            int row = s / 10;
            int col = s % 10;
            // Scale 48x48 glyph to 24x24 cell
            guiGraphics.blit(SYMBOL_TEXTURE, x0 + i * cellSize, y0, cellSize, cellSize, (float)(col * 48), (float)(row * 48), 48, 48, 512, 256);
        }
    }

    void drawEnteredString(GuiGraphics guiGraphics) {
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        // Only draw the address string (dashes) if linked and IDLE
        if (stargate == null || !stargate.isMerged || stargate.isActive()) {
            return;
        }

        int addressLength = stargate.getNumChevrons();
        String padded = SGAddressing.padAddress(enteredAddress, "|", addressLength);
        guiGraphics.drawCenteredString(this.font, padded, width / 2, dhdTop - 20, 0xffffff);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int i = findDHDButton((int)mouseX, (int)mouseY);
            if (i >= 0) {
                dhdButtonPressed(i);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            backspace();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            orangeButtonPressed();
            return true;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        String C = String.valueOf(codePoint).toUpperCase();
        if (SGAddressing.isValidSymbolChar(C)) {
            enterCharacter(C.charAt(0));
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    int findDHDButton(int mx, int my) {
        int x = -(mx - dhdCentreX);
        int y = -(my - dhdCentreY);
        // Check orange dome
        if (y > 0 && Math.hypot(x, y) <= dhdRadius1)
            return 0;
        
        y = y * dhdWidth / dhdHeight;
        double r = Math.hypot(x, y);
        if (r > dhdRadius3 || r <= dhdRadius1)
            return -1;
        
        double a = Math.toDegrees(Math.atan2(y, x));
        if (a < 0) a += 360;
        
        int i0, nb;
        if (r > dhdRadius2) {
            i0 = 1; nb = 26;
        } else {
            i0 = 27; nb = 11;
        }
        return i0 + (int)Math.floor(a * nb / 360);
    }

    void dhdButtonPressed(int i) {
        if (i == 0) {
            orangeButtonPressed();
        } else if (i < 37) {
            enterCharacter(SGAddressing.symbolToChar(i - 1));
        } else {
            backspace();
        }
    }

    private void enterCharacter(char c) {
        playClickSound();
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        // State checks: Symbol/Keyboard entry ONLY while Idle
        if (stargate == null || !stargate.isMerged || stargate.isActive()) {
            return;
        }

        int addressLength = stargate.getNumChevrons();
        if (enteredAddress.length() < addressLength) {
            enteredAddress += c;
            updateEnteredAddress();
        }
    }

    private void backspace() {
        playClickSound();
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        // State checks: Backspace ONLY while Idle
        if (stargate == null || !stargate.isMerged || stargate.isActive()) {
            return;
        }

        if (enteredAddress.length() > 0) {
            enteredAddress = enteredAddress.substring(0, enteredAddress.length() - 1);
            updateEnteredAddress();
        }
    }

    private void orangeButtonPressed() {
        playClickSound();
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        if (stargate != null && stargate.isMerged) {
            // Send Dial/Disconnect packet
            ModNetwork.CHANNEL.sendToServer(new DialPacket(stargate.getBlockPos(), enteredAddress));
            
            // Reset dialing address (Original mod parity)
            enteredAddress = "";
            updateEnteredAddress();
            
            this.onClose();
        }
    }

    private void updateEnteredAddress() {
        dhd.enteredAddress = this.enteredAddress;
        // Logic to send address update to server could be added here
    }
}
