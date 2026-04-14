package gcewing.sgcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.SGCraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class SGScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {

    public static final ResourceLocation SYMBOL_TEXTURE = new ResourceLocation(SGCraft.MODID, "textures/gui/symbols48.png");
    
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

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, SYMBOL_TEXTURE);
        
        // Activamos el filtrado lineal para suavizar los bordes al escalar
        RenderSystem.texParameter(3553, 10240, 9729); // GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR
        RenderSystem.texParameter(3553, 10241, 9729); // GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR

        for (int i = 0; i < n; i++) {
            char c = address.charAt(i);
            int s = SGAddressing.charToSymbol(c);
            if (s < 0) continue;

            int row = s / SYMBOLS_PER_ROW;
            int col = s % SYMBOLS_PER_ROW;

            guiGraphics.blit(SYMBOL_TEXTURE, x0 + i * CELL_SIZE, y0, 
                CELL_SIZE, CELL_SIZE, 
                col * SYMBOL_WIDTH, row * SYMBOL_HEIGHT, 
                SYMBOL_WIDTH, SYMBOL_HEIGHT, 
                512, 256);
        }
        
        // Restauramos el filtrado a NEAREST por defecto para no afectar otras GUIs
        RenderSystem.texParameter(3553, 10240, 9728); // GL_NEAREST
        RenderSystem.texParameter(3553, 10241, 9728); // GL_NEAREST
        
        RenderSystem.disableBlend();
    }
}
