package gcewing.sgcraft.client.gui;

import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.SGCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class SGScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    public static final Identifier SYMBOL_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/gui/symbols48.png");
    
    protected static final int SYMBOLS_PER_ROW = 10;
    protected static final int SYMBOL_WIDTH = 48;
    protected static final int SYMBOL_HEIGHT = 48;
    protected static final int CELL_SIZE = 24;

    public SGScreen(T menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    protected void drawAddressSymbols(GuiGraphics guiGraphics, int x, int y, String address) {
        int n = address.length();
        int x0 = x - (n * CELL_SIZE) / 2;
        int y0 = y;

        for (int i = 0; i < n; i++) {
            char c = address.charAt(i);
            int s = SGAddressing.charToSymbol(c);
            if (s < 0) continue;

            int row = s / SYMBOLS_PER_ROW;
            int col = s % SYMBOLS_PER_ROW;

            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SYMBOL_TEXTURE, 
                x0 + i * CELL_SIZE, y0, 
                (float)(col * SYMBOL_WIDTH), (float)(row * SYMBOL_HEIGHT), 
                CELL_SIZE, CELL_SIZE, 
                SYMBOL_WIDTH, SYMBOL_HEIGHT, 
                512, 256);
        }
    }
}
