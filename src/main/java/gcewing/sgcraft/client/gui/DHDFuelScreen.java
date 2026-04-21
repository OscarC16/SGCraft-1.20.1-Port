package gcewing.sgcraft.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.world.inventory.DHDFuelMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class DHDFuelScreen extends SGScreen<DHDFuelMenu> {

    public static final ResourceLocation GUI_TEXTURE = new ResourceLocation(SGCraft.MODID, "textures/gui/dhd_fuel_gui.png");

    public DHDFuelScreen(DHDFuelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 208;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        RenderSystem.setShaderTexture(0, GUI_TEXTURE);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        // Main background
        guiGraphics.blit(GUI_TEXTURE, x, y, 0, 0, imageWidth, imageHeight, 256, 256);

        // Fuel Gauge
        drawFuelGauge(guiGraphics, x, y);
    }

    private void drawFuelGauge(GuiGraphics guiGraphics, int x, int y) {
        double energy = menu.getBlockEntity().energyInBuffer;
        double maxEnergy = menu.getBlockEntity().maxEnergyBuffer;
        int fuelGaugeHeight = 34;
        int fuelGaugeWidth = 16;
        int fuelGaugeX = 214;
        int fuelGaugeY = 84;
        int fuelGaugeU = 0;
        int fuelGaugeV = 208;

        int level = (maxEnergy > 0) ? (int)(fuelGaugeHeight * energy / maxEnergy) : 0;
        if (level > fuelGaugeHeight) level = fuelGaugeHeight;

        if (level > 0) {
            // Draw energy bar from bottom up
            guiGraphics.blit(GUI_TEXTURE, x + fuelGaugeX, y + fuelGaugeY + fuelGaugeHeight - level, 
                             fuelGaugeU, fuelGaugeV + fuelGaugeHeight - level, 
                             fuelGaugeWidth, level, 256, 256);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Colors from original SGCraft (0x004c66)
        int textColor = 0x004c66;
        int cx = imageWidth / 2;
        
        String title = Component.translatable("gui.sgcraft.fuel.title").getString();
        String fuelLabel = Component.translatable("gui.sgcraft.fuel.label").getString();
        
        guiGraphics.drawString(this.font, title, cx - this.font.width(title) / 2, 8, textColor, false);
        guiGraphics.drawString(this.font, fuelLabel, 150, 96, textColor, false);
        
        // Inventory label (standard position)
        guiGraphics.drawString(this.font, this.playerInventoryTitle, 48, 114, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, delta);
        
        // Tooltip for energy gauge
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (mouseX >= x + 214 && mouseX <= x + 214 + 16 && mouseY >= y + 84 && mouseY <= y + 84 + 34) {
            int energy = (int)menu.getBlockEntity().energyInBuffer;
            int maxEnergy = (int)menu.getBlockEntity().maxEnergyBuffer;
            guiGraphics.renderTooltip(this.font, Component.literal(String.format("%d / %d FE", energy, maxEnergy)), mouseX, mouseY);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
