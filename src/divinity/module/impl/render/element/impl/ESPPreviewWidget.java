package divinity.module.impl.render.element.impl;

import divinity.ClientManager;
import divinity.module.impl.render.ESP;
import divinity.module.impl.render.element.GuiEditElement;
import divinity.module.impl.render.element.core.DraggableElement;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class ESPPreviewWidget extends DraggableElement {

    private final ESP espModule;
    private EntityOtherPlayerMP dummyPlayer;

    // Draggable component offsets





    private int draggingElement = -1; // -1: none, 0: healthBar, 1: armorBar, 2: nametag, 3: healthTag, 4: heldItem, 5: armorItems
    private int dragOffsetX, dragOffsetY;

    private static final int RESET_BUTTON_HEIGHT = 15;
    private static final int RESET_BUTTON_WIDTH = 80;

    public ESPPreviewWidget(ESP espModule, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.espModule = espModule;
        this.name = "ESP Preview";
        this.enabled = espModule.showEspPreview.getValue(); // Initial state from property

        // Add properties to the widget for persistence
        this.getProperties().addAll(Arrays.asList(


        ));

        // Initialize dummy player
        dummyPlayer = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
        dummyPlayer.copyLocationAndAnglesFrom(mc.thePlayer);
        dummyPlayer.rotationYawHead = 0;
        dummyPlayer.prevRotationYawHead = 0;
        dummyPlayer.renderYawOffset = 0;
        dummyPlayer.prevRenderYawOffset = 0;
        dummyPlayer.rotationPitch = 0;
        dummyPlayer.prevRotationPitch = 0;

        // Give dummy player some items and armor for preview
        dummyPlayer.inventory.armorInventory[3] = new ItemStack(Items.diamond_helmet); // Helmet
        dummyPlayer.inventory.armorInventory[2] = new ItemStack(Items.diamond_chestplate); // Chestplate
        dummyPlayer.inventory.armorInventory[1] = new ItemStack(Items.diamond_leggings); // Leggings
        dummyPlayer.inventory.armorInventory[0] = new ItemStack(Items.diamond_boots); // Boots
        dummyPlayer.inventory.mainInventory[dummyPlayer.inventory.currentItem] = new ItemStack(Items.diamond_sword); // Main hand
        // dummyPlayer.inventory.offHandInventory[0] = new ItemStack(Items.shield); // Off hand (if applicable)
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!espModule.showEspPreview.getValue()) return;

        // Update enabled state from property
        this.enabled = espModule.showEspPreview.getValue();

        // Draw background
        ShaderUtils.drawRoundRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 5, new Color(9, 9, 14, 200).getRGB());

        // Draw title
        Fonts.INTER_MEDIUM.get(16).drawCenteredStringWithShadow(name, getX() + getWidth() / 2f, getY() + 5, -1);

        // Render dummy player
        drawPlayerModel(getX() + getWidth() / 2f, getY() + getHeight() / 2f + 20, 30, partialTicks, dummyPlayer);

        // Apply ESP elements to dummy player
        renderESPElements(mouseX, mouseY, partialTicks, dummyPlayer);

        // Draw reset button
        float resetButtonX = getX() + getWidth() / 2f - RESET_BUTTON_WIDTH / 2f;
        float resetButtonY = getY() + getHeight() - RESET_BUTTON_HEIGHT - 5;
        boolean hoveredReset = MouseUtils.isHovered(mouseX, mouseY, resetButtonX, resetButtonY, resetButtonX + RESET_BUTTON_WIDTH, resetButtonY + RESET_BUTTON_HEIGHT);
        ShaderUtils.drawRoundRect(resetButtonX, resetButtonY, resetButtonX + RESET_BUTTON_WIDTH, resetButtonY + RESET_BUTTON_HEIGHT, 3, hoveredReset ? ClientManager.getInstance().getSecondaryColor().getRGB() : new Color(50, 50, 50).getRGB());
        Fonts.INTER_MEDIUM.get(12).drawCenteredString("Reset Positions", resetButtonX + RESET_BUTTON_WIDTH / 2f, resetButtonY + RESET_BUTTON_HEIGHT / 2f - Fonts.INTER_MEDIUM.get(12).getHeight() / 2f, -1);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPlayerModel(float x, float y, float scale, float partialTicks, EntityLivingBase entity) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 50.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(135.0F, 0.0F, 1.0F, 0.0F);
        RenderHelper.enableStandardItemLighting();
        GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-((float) Math.atan(entity.rotationPitch / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
        GlStateManager.translate(0.0F, 0.0F, 0.0F);
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);
        rendermanager.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        rendermanager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private void renderESPElements(int mouseX, int mouseY, float partialTicks, EntityPlayerSP player) {
        float playerScreenX = getX() + getWidth() / 2f;
        float playerScreenY = getY() + getHeight() / 2f + 20 - 30; // Adjust for model scale and position

        float currentScale = espModule.espPreviewScale.getValue();

        // Health Bar
        if (espModule.armorBar.getValue()) { // Using armorBar property as a general toggle for health/armor bars
            // Adapted from ESP.java lines 229-230
            float boxWidth = .5f; // From ESP.java
            float healthBarRenderWidth = 2.5f * currentScale; // Adjusted from ESP.java
            float healthBarRenderHeight = 20f * currentScale; // Adjusted from ESP.java

            float x = playerScreenX + espModule.healthBarXOffset.getValue();
            float y = playerScreenY + espModule.healthBarYOffset.getValue();

            // Simulate health percentage for dummy player
            float healthPercentage = dummyPlayer.getHealth() / dummyPlayer.getMaxHealth();
            float filledHeight = healthBarRenderHeight * healthPercentage;

            Color healthColor = ColorUtils.getHealthColor(dummyPlayer); // Use actual health color utility

            // Draw filled health bar
            RenderUtils.drawRect(x, y + (healthBarRenderHeight - filledHeight), x + healthBarRenderWidth, y + healthBarRenderHeight, healthColor.getRGB());
            // Draw outline
            RenderUtils.drawRectOutline(x, y, x + healthBarRenderWidth, y + healthBarRenderHeight, 0.5f, Color.BLACK.getRGB());

            boolean hovered = espModule.enableEspDragging.getValue() && MouseUtils.isHovered(mouseX, mouseY, x, y, x + healthBarRenderWidth, y + healthBarRenderHeight);
            if (hovered) RenderUtils.drawRect(x, y, x + healthBarRenderWidth, y + healthBarRenderHeight, new Color(255, 255, 0, 50).getRGB()); // Highlight

            if (draggingElement == 0) {
                espModule.healthBarXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
                espModule.healthBarYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }

        // Armor Bar
        if (espModule.armorBar.getValue()) {
            // Adapted from ESP.java lines 233-234
            float armorBarRenderWidth = 20f * currentScale; // Adjusted from ESP.java
            float armorBarRenderHeight = 2.5f * currentScale; // Adjusted from ESP.java

            float x = playerScreenX + espModule.armorBarXOffset.getValue();
            float y = playerScreenY + espModule.armorBarYOffset.getValue();

            // Simulate armor percentage for dummy player
            float armorPercentage = (float) dummyPlayer.getTotalArmorValue() / 20.0f; // Max armor value is 20
            float filledWidth = armorBarRenderWidth * armorPercentage;

            Color armorColor = Color.CYAN; // From ESP.java

            // Draw filled armor bar
            RenderUtils.drawRect(x, y, x + filledWidth, y + armorBarRenderHeight, armorColor.getRGB());
            // Draw outline
            RenderUtils.drawRectOutline(x, y, x + armorBarRenderWidth, y + armorBarRenderHeight, 0.5f, Color.BLACK.getRGB());

            boolean hovered = espModule.enableEspDragging.getValue() && MouseUtils.isHovered(mouseX, mouseY, x, y, x + armorBarRenderWidth, y + armorBarRenderHeight);
            if (hovered) RenderUtils.drawRect(x, y, x + armorBarRenderWidth, y + armorBarRenderHeight, new Color(255, 255, 0, 50).getRGB()); // Highlight

            if (draggingElement == 1) {
                espModule.armorBarXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
                espModule.armorBarYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }

        // Nametag
        if (espModule.renderBox.getValue()) { // Using renderBox property as a general toggle for nametag
            // Adapted from ESP.java lines 273-322 (Nametag and Health Tag are grouped)
            String name = EnumChatFormatting.WHITE + "DummyPlayer";
            String healthText = String.format("%.1f" + EnumChatFormatting.RED + " \u2764", dummyPlayer.getHealth());

            float fontScale = 0.7f * currentScale;
            FontRenderer font = Fonts.INTER_MEDIUM.get((int)(10 * fontScale));

            double healthWidth = font.getStringWidth(healthText) + 3;
            double nameWidth = font.getStringWidth(name) + 3;

            double gap = 2 * currentScale;
            double combinedWidth = healthWidth + gap + nameWidth;

            double centerX = playerScreenX;
            double groupLeft = centerX - (combinedWidth / 2.0);

            double rectTop = playerScreenY - 10 * currentScale + espModule.nametagYOffset.getValue();
            double rectBottom = rectTop + 8 * currentScale;

            // Health rect
            double healthRightX = groupLeft + healthWidth;
            RenderUtils.drawRect((float) groupLeft + espModule.nametagXOffset.getValue(), (float) rectTop, (float) healthRightX + espModule.nametagXOffset.getValue(), (float) rectBottom, new Color(0, 0, 0, 120).getRGB());
            font.drawStringWithShadow(healthText, (float) (groupLeft + 1.5f + espModule.nametagXOffset.getValue()), (float) (rectTop + 3.5f * currentScale), 0xFFFFFFFF);

            // Name rect
            double nameLeftX = groupLeft + healthWidth + gap;
            double nameRightX = nameLeftX + nameWidth;
            RenderUtils.drawRect((float) nameLeftX + espModule.nametagXOffset.getValue(), (float) rectTop, (float) nameRightX + espModule.nametagXOffset.getValue(), (float) rectBottom, new Color(0, 0, 0, 120).getRGB());
            font.drawStringWithShadow(name, (float) (nameLeftX + 1.5f + espModule.nametagXOffset.getValue()), (float) (rectTop + 3.5f * currentScale), Color.WHITE.getRGB());

            // Dragging logic for nametag group
            if (draggingElement == 2) {
                espModule.nametagXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
                espModule.nametagYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }



        // Held Item (main hand + offhand)
        if (espModule.armorBar.getValue()) { // Using armorBar property as a general toggle for held items
            float itemBoxSize = 16f * currentScale; // Standard item render size
            float x = playerScreenX + espModule.heldItemXOffset.getValue();
            float y = playerScreenY + espModule.heldItemYOffset.getValue();

            ItemStack mainHandItem = dummyPlayer.getHeldItem();
            ItemStack offHandItem = dummyPlayer.getOffHandItem();

            // Render main hand item
            if (mainHandItem != null) {
                RenderUtils.renderItemStack(mainHandItem, (int) x, (int) y, currentScale);
                boolean hovered = espModule.enableEspDragging.getValue() && MouseUtils.isHovered(mouseX, mouseY, x, y, x + itemBoxSize, y + itemBoxSize);
                if (hovered) RenderUtils.drawRect(x, y, x + itemBoxSize, y + itemBoxSize, new Color(255, 255, 0, 50).getRGB()); // Highlight
            }

            // Render off hand item (example, adjust position as needed)
            if (offHandItem != null) {
                float offHandX = x + itemBoxSize + 2 * currentScale; // Offset from main hand
                RenderUtils.renderItemStack(offHandItem, (int) offHandX, (int) y, currentScale);
                boolean hovered = espModule.enableEspDragging.getValue() && MouseUtils.isHovered(mouseX, mouseY, offHandX, y, offHandX + itemBoxSize, y + itemBoxSize);
                if (hovered) RenderUtils.drawRect(offHandX, y, offHandX + itemBoxSize, y + itemBoxSize, new Color(255, 255, 0, 50).getRGB()); // Highlight
            }

            if (draggingElement == 4) {
                espModule.heldItemXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
                espModule.heldItemYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }

        // Equipped Armor Items
        if (espModule.armorBar.getValue()) { // Using armorBar property as a general toggle for armor items
            float armorBoxSize = 16f * currentScale;
            float x = playerScreenX + espModule.armorItemsXOffset.getValue();
            float y = playerScreenY + espModule.armorItemsYOffset.getValue();

            for (int i = 0; i < 4; i++) { // Iterate through armor slots (boots, leggings, chestplate, helmet)
                ItemStack armor = dummyPlayer.inventory.armorInventory[i];
                if (armor != null) {
                    float armorY = y + (3 - i) * (armorBoxSize + 2 * currentScale); // Render helmet at top, boots at bottom
                    RenderUtils.renderItemStack(armor, (int) x, (int) armorY, currentScale);
                    boolean hovered = espModule.enableEspDragging.getValue() && MouseUtils.isHovered(mouseX, mouseY, x, armorY, x + armorBoxSize, armorY + armorBoxSize);
                    if (hovered) RenderUtils.drawRect(x, armorY, x + armorBoxSize, armorY + armorBoxSize, new Color(255, 255, 0, 50).getRGB()); // Highlight
                }
            }

            if (draggingElement == 5) {
                espModule.armorItemsXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
                espModule.armorItemsYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (!espModule.showEspPreview.getValue()) return;

        // Handle reset button click
        float resetButtonX = getX() + getWidth() / 2f - RESET_BUTTON_WIDTH / 2f;
        float resetButtonY = getY() + getHeight() - RESET_BUTTON_HEIGHT - 5;
        if (MouseUtils.isHovered(mouseX, mouseY, resetButtonX, resetButtonY, resetButtonX + RESET_BUTTON_WIDTH, resetButtonY + RESET_BUTTON_HEIGHT)) {
            if (mouseButton == 0) {
                resetOffsets();
                return;
            }
        }

        if (espModule.enableEspDragging.getValue() && mouseButton == 0) {
            float playerScreenX = getX() + getWidth() / 2f;
            float playerScreenY = getY() + getHeight() / 2f + 20 - 30;
            float currentScale = espModule.espPreviewScale.getValue();

            // Check for dragging each element
            // Health Bar
            float healthBarX = playerScreenX + espModule.healthBarXOffset.getValue();
            float healthBarY = playerScreenY + espModule.healthBarYOffset.getValue();
            float healthBarWidth = 2f * currentScale;
            float healthBarHeight = 20f * currentScale;
            if (MouseUtils.isHovered(mouseX, mouseY, healthBarX, healthBarY, healthBarX + healthBarWidth, healthBarY + healthBarHeight)) {
                draggingElement = 0;
                dragOffsetX = (int) (mouseX - healthBarX);
                dragOffsetY = (int) (mouseY - healthBarY);
                return;
            }

            // Armor Bar
            float armorBarX = playerScreenX + espModule.armorBarXOffset.getValue();
            float armorBarY = playerScreenY + espModule.armorBarYOffset.getValue();
            float armorBarWidth = 20f * currentScale;
            float armorBarHeight = 2f * currentScale;
            if (MouseUtils.isHovered(mouseX, mouseY, armorBarX, armorBarY, armorBarX + armorBarWidth, armorBarY + armorBarHeight)) {
                draggingElement = 1;
                dragOffsetX = (int) (mouseX - armorBarX);
                dragOffsetY = (int) (mouseY - armorBarY);
                return;
            }

            // Nametag
            // Nametag and Health Tag dragging area
            float nametagFontScale = 0.8f * currentScale;
            FontRenderer font = Fonts.INTER_MEDIUM.get((int)(10 * nametagFontScale));
            String name = "DummyPlayer";
            String healthText = String.format("%.1f" + EnumChatFormatting.RED + " \u2764", dummyPlayer.getHealth());
            double healthWidth = font.getStringWidth(healthText) + 3;
            double nameWidth = font.getStringWidth(name) + 3;
            double gap = 2 * currentScale;
            double combinedWidth = healthWidth + gap + nameWidth;
            double centerX = playerScreenX;
            double groupLeft = centerX - (combinedWidth / 2.0);
            double rectTop = playerScreenY - 10 * currentScale + espModule.nametagYOffset.getValue();
            double rectBottom = rectTop + 8 * currentScale;

            float nametagDragX = (float) groupLeft + espModule.nametagXOffset.getValue();
            float nametagDragY = (float) rectTop;
            float nametagDragWidth = (float) combinedWidth;
            float nametagDragHeight = (float) (rectBottom - rectTop);

            if (MouseUtils.isHovered(mouseX, mouseY, nametagDragX, nametagDragY, nametagDragX + nametagDragWidth, nametagDragY + nametagDragHeight)) {
                draggingElement = 2;
                dragOffsetX = (int) (mouseX - nametagDragX);
                dragOffsetY = (int) (mouseY - nametagDragY);
                return;
            }



            // Held Item
            float itemBoxSize = 10f * currentScale;
            float heldItemX = playerScreenX + espModule.heldItemXOffset.getValue();
            float heldItemY = playerScreenY + espModule.heldItemYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, heldItemX, heldItemY, heldItemX + itemBoxSize, heldItemY + itemBoxSize)) {
                draggingElement = 4;
                dragOffsetX = (int) (mouseX - heldItemX);
                dragOffsetY = (int) (mouseY - heldItemY);
                return;
            }

            // Armor Items
            float armorBoxSize = 10f * currentScale;
            float armorItemsX = playerScreenX + espModule.armorItemsXOffset.getValue();
            float armorItemsY = playerScreenY + espModule.armorItemsYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, armorItemsX, armorItemsY, armorItemsX + armorBoxSize, armorItemsY + armorBoxSize * 4)) {
                draggingElement = 5;
                dragOffsetX = (int) (mouseX - armorItemsX);
                dragOffsetY = (int) (mouseY - armorItemsY);
                return;
            }
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        draggingElement = -1;
    }

    private void resetOffsets() {
        espModule.healthBarXOffset.setValue(0f);
        espModule.healthBarYOffset.setValue(0f);
        espModule.armorBarXOffset.setValue(0f);
        espModule.armorBarYOffset.setValue(0f);
        espModule.nametagXOffset.setValue(0f);
        espModule.nametagYOffset.setValue(0f);
        espModule.healthTagXOffset.setValue(0f);
        espModule.healthTagYOffset.setValue(0f);
        espModule.heldItemXOffset.setValue(0f);
        espModule.heldItemYOffset.setValue(0f);
        espModule.armorItemsXOffset.setValue(0f);
        espModule.armorItemsYOffset.setValue(0f);
    }
}
