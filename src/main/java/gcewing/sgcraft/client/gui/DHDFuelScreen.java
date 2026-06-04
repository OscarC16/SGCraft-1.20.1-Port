package gcewing.sgcraft.client.gui;

import gcewing.sgcraft.SGCraft;
import gcewing.sgcraft.world.inventory.DHDFuelMenu;
import gcewing.sgcraft.block.entity.SGBaseBlockEntity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.player.Inventory;

public class DHDFuelScreen extends SGScreen<DHDFuelMenu> {

    public static final Identifier GUI_TEXTURE = Identifier.fromNamespaceAndPath(SGCraft.MODID, "textures/gui/dhd_fuel_gui.png");

    public DHDFuelScreen(DHDFuelMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 208;
        this.titleLabelY = 1000;
        this.inventoryLabelY = 1000;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTicks, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        
        // Main background
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, x, y, 0f, 0f, imageWidth, imageHeight, 256, 256);

        // Fuel Gauge and Title labels
        drawFuelGauge(guiGraphics, x, y);
    }

    private void drawFuelGauge(GuiGraphics guiGraphics, int x, int y) {
        double energy = menu.getBlockEntity().energyInBuffer;
        double maxEnergy = SGBaseBlockEntity.MAX_ENERGY;
        int fuelGaugeHeight = 34;
        int fuelGaugeWidth = 16;
        int fuelGaugeX = 214;
        int fuelGaugeY = 84;
        int fuelGaugeU = 0;
        int fuelGaugeV = 208;
        int textColor = 0xFF004C66;
        int cx = imageWidth / 2;
        
        String titleStr = Component.translatable("gui.sgcraft.fuel.title").getString();
        String fuelLabel = Component.translatable("gui.sgcraft.fuel.label").getString();
        
        guiGraphics.drawString(this.font, titleStr, x + cx - this.font.width(titleStr) / 2, y + 8, textColor, false);
        guiGraphics.drawString(this.font, fuelLabel, x + 150, y + 96, textColor, false);
        
        // Draw green progress/energy bar
        if (maxEnergy <= 0) maxEnergy = 2000000;
        double fraction = Math.min(1.0, Math.max(0.0, energy / maxEnergy));
        int height = (int) (fraction * fuelGaugeHeight);
        if (height > 0) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GUI_TEXTURE, 
                x + fuelGaugeX, y + fuelGaugeY + (fuelGaugeHeight - height), 
                (float) fuelGaugeU, (float) (fuelGaugeV + (fuelGaugeHeight - height)), 
                fuelGaugeWidth, height, 256, 256);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        
        // Tooltip for energy gauge
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int energyBarX = x + 214;
        int energyBarY = y + 84;
        int energyBarWidth = 16;
        int energyBarHeight = 34;
        var dhd = menu.getBlockEntity();
        if (mouseX >= energyBarX && mouseX < energyBarX + energyBarWidth && mouseY >= energyBarY && mouseY < energyBarY + energyBarHeight) {
            int energy = (int)dhd.energyInBuffer;
            int maxEnergy = SGBaseBlockEntity.MAX_ENERGY;
            guiGraphics.setComponentTooltipForNextFrame(this.font, java.util.List.of(Component.literal(String.format("%d / %d FE", energy, maxEnergy))), mouseX, mouseY);
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
