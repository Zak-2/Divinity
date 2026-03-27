package divinity.module.impl.render.element.impl;

import divinity.ClientManager;
import divinity.module.impl.render.ESP;
import divinity.module.impl.render.element.core.DraggableElement;
import divinity.utils.ColorUtils;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.FontRenderer;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.Arrays;

/**
 * ESP Preview Widget for Minecraft 1.8.9.
 * This widget renders a dummy player and overlays draggable ESP elements.
 * It is designed to be a standalone ClickGUI component.
 */
public class ESPPreviewWidget extends DraggableElement {

    private final ESP espModule;
    private EntityOtherPlayerMP dummyPlayer;

    private int draggingElement = -1; // -1: none, 0: healthBar, 1: armorBar, 2: nametag, 4: heldItem, 5: armorItems
    private int dragOffsetX, dragOffsetY;

    private static final int RESET_BUTTON_HEIGHT = 15;
    private static final int RESET_BUTTON_WIDTH = 80;

    public ESPPreviewWidget(ESP espModule, int x, int y, int width, int height) {
        super(x, y, width, height);
        this.espModule = espModule;
        this.setName("ESP Preview");
        this.setEnabled(espModule.showEspPreview.getValue());

        // Add properties to the widget for persistence
        this.getProperties().addAll(Arrays.asList(
                espModule.espPreviewScale
        ));

        initDummyPlayer();
    }

    private void initDummyPlayer() {
        if (mc.theWorld != null && mc.thePlayer != null) {
            dummyPlayer = new EntityOtherPlayerMP(mc.theWorld, mc.thePlayer.getGameProfile());
            dummyPlayer.copyLocationAndAnglesFrom(mc.thePlayer);
            dummyPlayer.rotationYawHead = 0;
            dummyPlayer.prevRotationYawHead = 0;
            dummyPlayer.renderYawOffset = 0;
            dummyPlayer.prevRenderYawOffset = 0;
            dummyPlayer.rotationPitch = 0;
            dummyPlayer.prevRotationPitch = 0;

            // Give dummy player some items and armor for preview
            dummyPlayer.inventory.armorInventory[3] = new ItemStack(Items.diamond_helmet);
            dummyPlayer.inventory.armorInventory[2] = new ItemStack(Items.diamond_chestplate);
            dummyPlayer.inventory.armorInventory[1] = new ItemStack(Items.diamond_leggings);
            dummyPlayer.inventory.armorInventory[0] = new ItemStack(Items.diamond_boots);
            dummyPlayer.inventory.mainInventory[dummyPlayer.inventory.currentItem] = new ItemStack(Items.diamond_sword);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Only draw if enabled in ESP module
        if (!espModule.showEspPreview.getValue()) return;

        // Ensure dummy player is initialized if world is loaded
        if (dummyPlayer == null && mc.theWorld != null) {
            initDummyPlayer();
        }

        // Draw background using client's standard methods
        ShaderUtils.drawRoundRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 5, new Color(9, 9, 14, 200).getRGB());

        // Draw title
        Fonts.INTER_MEDIUM.get(16).drawCenteredStringWithShadow(getName(), getX() + getWidth() / 2f, getY() + 5, -1);

        if (dummyPlayer != null) {
            // Fix for 1.8.9 Rendering: Reset GL states before rendering entity
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            // Render dummy player
            drawPlayerModel(getX() + getWidth() / 2f, getY() + getHeight() / 2f + 40, 40, partialTicks, dummyPlayer);

            // Reset states for 2D overlay
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            
            // Apply ESP elements to dummy player
            renderESPElements(mouseX, mouseY, partialTicks);
        }

        // Draw reset button
        float resetButtonX = getX() + getWidth() / 2f - RESET_BUTTON_WIDTH / 2f;
        float resetButtonY = getY() + getHeight() - RESET_BUTTON_HEIGHT - 5;
        boolean hoveredReset = MouseUtils.isHovered(mouseX, mouseY, resetButtonX, resetButtonY, resetButtonX + RESET_BUTTON_WIDTH, resetButtonY + RESET_BUTTON_HEIGHT);
        ShaderUtils.drawRoundRect(resetButtonX, resetButtonY, resetButtonX + RESET_BUTTON_WIDTH, resetButtonY + RESET_BUTTON_HEIGHT, 3, hoveredReset ? ClientManager.getInstance().getSecondaryColor().getRGB() : new Color(50, 50, 50).getRGB());
        Fonts.INTER_MEDIUM.get(12).drawCenteredString("Reset Positions", resetButtonX + RESET_BUTTON_WIDTH / 2f, resetButtonY + RESET_BUTTON_HEIGHT / 2f - Fonts.INTER_MEDIUM.get(12).getHeight() / 2f, -1);

        // Super call handles dragging of the widget itself
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPlayerModel(float x, float y, float scale, float partialTicks, EntityLivingBase entity) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 50.0F);
        GlStateManager.scale(-scale, -scale, scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        
        // 1.8.9 Entity Rendering States
        RenderHelper.enableStandardItemLighting();
        
        RenderManager rendermanager = Minecraft.getMinecraft().getRenderManager();
        rendermanager.setPlayerViewY(180.0F);
        rendermanager.setRenderShadow(false);
        
        // Use 1.8.9 specific render call
        rendermanager.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
        
        rendermanager.setRenderShadow(true);
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
    }

    private void renderESPElements(int mouseX, int mouseY, float partialTicks) {
        float playerScreenX = getX() + getWidth() / 2f;
        float playerScreenY = getY() + getHeight() / 2f + 20 - 30;
        float currentScale = espModule.espPreviewScale.getValue();

        // 1. Health Bar
        if (espModule.armorBar.getValue()) {
            float healthBarRenderWidth = 2.5f * currentScale;
            float healthBarRenderHeight = 30f * currentScale;
            float x = playerScreenX + espModule.healthBarXOffset.getValue() - 20 * currentScale;
            float y = playerScreenY + espModule.healthBarYOffset.getValue();

            float healthPercentage = dummyPlayer.getHealth() / dummyPlayer.getMaxHealth();
            float filledHeight = healthBarRenderHeight * healthPercentage;
            Color healthColor = ColorUtils.getHealthColor(dummyPlayer);

            RenderUtils.drawRect(x, y + (healthBarRenderHeight - filledHeight), x + healthBarRenderWidth, y + healthBarRenderHeight, healthColor.getRGB());
            RenderUtils.drawRectOutline(x, y, x + healthBarRenderWidth, y + healthBarRenderHeight, 0.5f, Color.BLACK.getRGB());

            if (draggingElement == 0) {
                espModule.healthBarXOffset.setValue((float) (mouseX - getX() - dragOffsetX + 20 * currentScale));
                espModule.healthBarYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }

        // 2. Armor Bar
        if (espModule.armorBar.getValue()) {
            float armorBarRenderWidth = 25f * currentScale;
            float armorBarRenderHeight = 2.5f * currentScale;
            float x = playerScreenX + espModule.armorBarXOffset.getValue() - 12.5f * currentScale;
            float y = playerScreenY + espModule.armorBarYOffset.getValue() + 35 * currentScale;

            float armorPercentage = (float) dummyPlayer.getTotalArmorValue() / 20.0f;
            float filledWidth = armorBarRenderWidth * armorPercentage;

            RenderUtils.drawRect(x, y, x + filledWidth, y + armorBarRenderHeight, Color.CYAN.getRGB());
            RenderUtils.drawRectOutline(x, y, x + armorBarRenderWidth, y + armorBarRenderHeight, 0.5f, Color.BLACK.getRGB());

            if (draggingElement == 1) {
                espModule.armorBarXOffset.setValue((float) (mouseX - getX() - dragOffsetX + 12.5f * currentScale));
                espModule.armorBarYOffset.setValue((float) (mouseY - getY() - dragOffsetY - 35 * currentScale));
            }
        }

        // 3. Nametag
        if (espModule.renderBox.getValue() || true) { // Always show for preview
            String nameText = EnumChatFormatting.WHITE + "DummyPlayer";
            String healthText = String.format("%.1f" + EnumChatFormatting.RED + " \u2764", dummyPlayer.getHealth());
            float fontScale = 0.8f * currentScale;
            FontRenderer font = Fonts.INTER_MEDIUM.get((int)(10 * fontScale));

            double healthWidth = font.getStringWidth(healthText) + 3;
            double nameWidth = font.getStringWidth(nameText) + 3;
            double gap = 2 * currentScale;
            double combinedWidth = healthWidth + gap + nameWidth;
            double groupLeft = playerScreenX - (combinedWidth / 2.0) + espModule.nametagXOffset.getValue();
            double rectTop = playerScreenY - 15 * currentScale + espModule.nametagYOffset.getValue();
            double rectBottom = rectTop + 10 * currentScale;

            RenderUtils.drawRect((float) groupLeft, (float) rectTop, (float) (groupLeft + healthWidth), (float) rectBottom, new Color(0, 0, 0, 120).getRGB());
            font.drawStringWithShadow(healthText, (float) (groupLeft + 1.5f), (float) (rectTop + 2f * currentScale), -1);

            double nameLeftX = groupLeft + healthWidth + gap;
            RenderUtils.drawRect((float) nameLeftX, (float) rectTop, (float) (nameLeftX + nameWidth), (float) rectBottom, new Color(0, 0, 0, 120).getRGB());
            font.drawStringWithShadow(nameText, (float) (nameLeftX + 1.5f), (float) (rectTop + 2f * currentScale), -1);

            if (draggingElement == 2) {
                espModule.nametagXOffset.setValue((float) (mouseX - getX() - dragOffsetX + (combinedWidth / 2.0)));
                espModule.nametagYOffset.setValue((float) (mouseY - getY() - dragOffsetY + 15 * currentScale));
            }
        }

        // 4. Held Item
        float xItems = playerScreenX + 20 * currentScale;
        float yItems = playerScreenY;
        
        if (espModule.armorBar.getValue()) {
            float itemBoxSize = 16f * currentScale;
            float x = xItems + espModule.heldItemXOffset.getValue();
            float y = yItems + espModule.heldItemYOffset.getValue();

            ItemStack mainHandItem = dummyPlayer.getHeldItem();
            if (mainHandItem != null) {
                RenderUtils.renderItemStack(mainHandItem, (int) x, (int) y, currentScale);
            }

            if (draggingElement == 4) {
                espModule.heldItemXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
                espModule.heldItemYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }

        // 5. Armor Items
        if (espModule.armorBar.getValue()) {
            float armorBoxSize = 16f * currentScale;
            float x = xItems + 20 * currentScale + espModule.armorItemsXOffset.getValue();
            float y = yItems + espModule.armorItemsYOffset.getValue();

            for (int i = 0; i < 4; i++) {
                ItemStack armor = dummyPlayer.inventory.armorInventory[i];
                if (armor != null) {
                    float armorY = y + (3 - i) * (armorBoxSize + 2 * currentScale);
                    RenderUtils.renderItemStack(armor, (int) x, (int) armorY, currentScale);
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
        if (!espModule.showEspPreview.getValue()) return;

        // Reset button
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

            // Check elements for dragging
            // Health Bar
            float healthBarX = playerScreenX + espModule.healthBarXOffset.getValue() - 20 * currentScale;
            float healthBarY = playerScreenY + espModule.healthBarYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, healthBarX, healthBarY, healthBarX + 5 * currentScale, healthBarY + 30 * currentScale)) {
                draggingElement = 0;
                dragOffsetX = (int) (mouseX - healthBarX);
                dragOffsetY = (int) (mouseY - healthBarY);
                return;
            }

            // Armor Bar
            float armorBarX = playerScreenX + espModule.armorBarXOffset.getValue() - 12.5f * currentScale;
            float armorBarY = playerScreenY + espModule.armorBarYOffset.getValue() + 35 * currentScale;
            if (MouseUtils.isHovered(mouseX, mouseY, armorBarX, armorBarY, armorBarX + 25 * currentScale, armorBarY + 5 * currentScale)) {
                draggingElement = 1;
                dragOffsetX = (int) (mouseX - armorBarX);
                dragOffsetY = (int) (mouseY - armorBarY);
                return;
            }

            // Nametag
            float fontScale = 0.8f * currentScale;
            FontRenderer font = Fonts.INTER_MEDIUM.get((int)(10 * fontScale));
            String healthText = String.format("%.1f" + EnumChatFormatting.RED + " \u2764", dummyPlayer.getHealth());
            double combinedWidth = (font.getStringWidth(healthText) + 3) + (2 * currentScale) + (font.getStringWidth("DummyPlayer") + 3);
            float nametagDragX = (float) (playerScreenX - (combinedWidth / 2.0)) + espModule.nametagXOffset.getValue();
            float nametagDragY = playerScreenY - 15 * currentScale + espModule.nametagYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, nametagDragX, nametagDragY, nametagDragX + (float)combinedWidth, nametagDragY + 10 * currentScale)) {
                draggingElement = 2;
                dragOffsetX = (int) (mouseX - nametagDragX);
                dragOffsetY = (int) (mouseY - nametagDragY);
                return;
            }
        }

        // Widget dragging
        super.mouseClicked(mouseX, mouseY, mouseButton);
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
        espModule.heldItemXOffset.setValue(0f);
        espModule.heldItemYOffset.setValue(0f);
        espModule.armorItemsXOffset.setValue(0f);
        espModule.armorItemsYOffset.setValue(0f);
    }
}
