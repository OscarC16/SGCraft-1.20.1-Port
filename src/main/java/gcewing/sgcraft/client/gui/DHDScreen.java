package gcewing.sgcraft.client.gui;

import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.block.entity.DHDBlockEntity;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import gcewing.sgcraft.network.DialPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import org.lwjgl.glfw.GLFW;

public class DHDScreen extends Screen {

    public static final Identifier DHD_GUI_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/gui/dhd_gui.png");
    public static final Identifier DHD_CENTRE_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/gui/dhd_centre.png");
    public static final Identifier SYMBOL_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/gui/symbols48.png");

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
    double lastMouseX, lastMouseY;
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
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        // Overridden to do nothing and avoid the 1.21 blur shader
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        // Manual semi-transparent background without blur
        guiGraphics.fillGradient(0, 0, this.width, this.height, 0x80000000, 0x80000000);

        drawBackgroundImage(guiGraphics);
        drawOrangeButton(guiGraphics); // This now draws panel then dome
        
        drawEnteredSymbols(guiGraphics);
        drawEnteredString(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, partialTicks);
    }

    void drawBackgroundImage(GuiGraphics guiGraphics) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DHD_GUI_TEXTURE, 
            (width - dhdWidth) / 2, height - dhdHeight, 
            0f, 0f, 
            dhdWidth, dhdHeight, 
            512, 256, 
            TEXTURE_W, TEXTURE_H);
    }

    void drawOrangeButton(GuiGraphics guiGraphics) {
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        boolean connected = stargate != null && stargate.isMerged;
        boolean active = connected && stargate.isActive();

        int color;
        if (stargate == null || !stargate.isMerged) {
            color = 0xFF333333;
        } else if (active) {
            color = 0xFFFF7F00;
        } else {
            color = 0xFF7F3F00;
        }

        int argb = color | 0xFF000000;
        int bw = (int)(2 * buttonRX);
        int bh = (int)(2 * buttonRY);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, DHD_CENTRE_TEXTURE, 
            (int)(dhdCentreX - buttonRX), (int)(dhdCentreY - buttonRY - 6), 
            64f, 0f, 
            bw, bh, 
            64, 64, 
            128, 64,
            argb);
    }

    void drawEnteredSymbols(GuiGraphics guiGraphics) {
        int n = enteredAddress.length();
        if (n == 0) return;
        
        int cellSize = 24;
        int x0 = width / 2 - (n * cellSize) / 2;
        int y0 = dhdTop - 74;

        for (int i = 0; i < n; i++) {
            char c = enteredAddress.charAt(i);
            int s = SGAddressing.charToSymbol(c);
            if (s < 0) continue;

            int row = s / 10;
            int col = s % 10;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SYMBOL_TEXTURE, 
                x0 + i * cellSize, y0, 
                (float)(col * 48), (float)(row * 48), 
                cellSize, cellSize, 
                48, 48, 
                512, 256);
        }
    }

    void drawEnteredString(GuiGraphics guiGraphics) {
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        if (stargate == null || !stargate.isMerged || stargate.isActive()) {
            return;
        }

        int addressLength = stargate.getNumChevrons();
        String padded = SGAddressing.padAddress(enteredAddress, "-", addressLength);
        guiGraphics.drawCenteredString(this.font, padded, width / 2, dhdTop - 20, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean isDouble) {
        if (event.button() == 0) {
            int i = findDHDButton((int)event.x(), (int)event.y());
            if (i >= 0) {
                dhdButtonPressed(i);
                return true;
            }
        }
        return super.mouseClicked(event, isDouble);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            this.onClose();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_BACKSPACE) {
            backspace();
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            orangeButtonPressed();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        boolean active = stargate != null && stargate.isActive();
        if (!active) {
            String C = String.valueOf(event.codepointAsString()).toUpperCase();
            if (SGAddressing.isValidSymbolChar(C)) {
                enterCharacter(C.charAt(0));
                return true;
            }
        }
        return super.charTyped(event);
    }

    private void playClickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    int findDHDButton(int mx, int my) {
        int x = -(mx - dhdCentreX);
        int y = -(my - dhdCentreY);
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
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
        boolean active = stargate != null && stargate.isActive();
        
        if (i == 0) {
            orangeButtonPressed();
        } else if (!active) {
            if (i < 37) {
                enterCharacter(SGAddressing.symbolToChar(i - 1));
            } else {
                backspace();
            }
        }
    }

    private void enterCharacter(char c) {
        playClickSound();
        SGBaseBlockEntity stargate = dhd.getLinkedStargateTE();
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
            if (minecraft != null && minecraft.getConnection() != null) {
                minecraft.getConnection().send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(
                    new DialPacket(stargate.getBlockPos(), enteredAddress)
                ));
            }
            
            enteredAddress = "";
            updateEnteredAddress();
            
            this.onClose();
        }
    }

    private void updateEnteredAddress() {
        dhd.enteredAddress = this.enteredAddress;
    }
}
