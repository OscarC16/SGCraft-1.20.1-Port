package gcewing.sgcraft.client.gui;

import gcewing.sgcraft.SGAddressing;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.world.inventory.SGBaseMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

public class SGBaseScreen extends SGScreen<SGBaseMenu> {

    public static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/gui/sg_gui.png");

    public SGBaseScreen(SGBaseMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Stargate Address"));
        this.imageWidth = 256;
        this.imageHeight = 208;
        this.inventoryLabelY = 1000; // Hide default label by moving it offscreen
        this.titleLabelX = (imageWidth / 2); // Centered
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int labelColor = 0xFF004C66;
        guiGraphics.drawString(this.font, this.title, (this.imageWidth - this.font.width(this.title)) / 2, 8, labelColor, false);
        guiGraphics.drawString(this.font, Component.literal("Base Camouflage"), 48, 92, labelColor, false);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0f, 0f, imageWidth, imageHeight, 256, 256);

        // Draw Address Symbols
        String address = menu.getBlockEntity().homeAddress;
        if (address != null && !address.isEmpty()) {
            drawAddressSymbols(guiGraphics, x + imageWidth / 2, y + 34, address);
            
            // Draw address text
            String formatted = SGAddressing.formatAddress(address, "-", "-");
            guiGraphics.drawCenteredString(this.font, formatted, x + imageWidth / 2, y + 72, 0xFFFFFFFF);
        } else {
            String error = menu.getBlockEntity().addressError;
            if (error == null) error = "No Address";
            guiGraphics.drawCenteredString(this.font, error, x + imageWidth / 2, y + 44, 0xFFFFFFFF);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
