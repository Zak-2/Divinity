package divinity.module.impl.render.element.impl;

import divinity.ClientManager;
import divinity.module.impl.render.ESP;
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

public class ESPPreviewWidget {

    private final Minecraft mc = Minecraft.getMinecraft();
    private final ESP espModule;
    private EntityOtherPlayerMP dummyPlayer;

    private int draggingElement = -1; // -1: none, 0: healthBar, 1: armorBar, 2: nametag, 4: heldItem, 5: armorItems
    private float dragOffsetX, dragOffsetY;

    private static final int RESET_BUTTON_HEIGHT = 15;
    private static final int RESET_BUTTON_WIDTH = 80;

    public ESPPreviewWidget(ESP espModule) {
        this.espModule = espModule;
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

            dummyPlayer.inventory.armorInventory[3] = new ItemStack(Items.diamond_helmet);
            dummyPlayer.inventory.armorInventory[2] = new ItemStack(Items.diamond_chestplate);
            dummyPlayer.inventory.armorInventory[1] = new ItemStack(Items.diamond_leggings);
            dummyPlayer.inventory.armorInventory[0] = new ItemStack(Items.diamond_boots);
            dummyPlayer.inventory.mainInventory[dummyPlayer.inventory.currentItem] = new ItemStack(Items.diamond_sword);
        }
    }

    public void drawScreen(int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        if (!espModule.showEspPreview.getValue()) return;

        if (dummyPlayer == null && mc.theWorld != null) initDummyPlayer();

        ShaderUtils.drawRoundRect(x, y, x + width, y + height, 5, new Color(15, 15, 20, 255).getRGB());
        Fonts.INTER_MEDIUM.get(16).drawCenteredStringWithShadow("ESP Preview", x + width / 2f, y + 5, -1);

        if (dummyPlayer != null) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            // Use a standard scale that matches in-game proportions better
            float playerModelScale = 35.0f; 
            drawPlayerModel(x + width / 2f, y + height / 2f + 40, playerModelScale, partialTicks, dummyPlayer);

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            
            renderESPElements(mouseX, mouseY, x, y, width, height, playerModelScale);
        }

        float rbX = x + width / 2f - RESET_BUTTON_WIDTH / 2f;
        float rbY = y + height - RESET_BUTTON_HEIGHT - 5;
        boolean hoveredReset = MouseUtils.isHovered(mouseX, mouseY, rbX, rbY, rbX + RESET_BUTTON_WIDTH, rbY + RESET_BUTTON_HEIGHT);
        ShaderUtils.drawRoundRect(rbX, rbY, rbX + RESET_BUTTON_WIDTH, rbY + RESET_BUTTON_HEIGHT, 3, hoveredReset ? ClientManager.getInstance().getSecondaryColor().getRGB() : new Color(50, 50, 50).getRGB());
        Fonts.INTER_MEDIUM.get(12).drawCenteredString("Reset Positions", rbX + RESET_BUTTON_WIDTH / 2f, rbY + RESET_BUTTON_HEIGHT / 2f - Fonts.INTER_MEDIUM.get(12).getHeight() / 2f, -1);
    }

    private void drawPlayerModel(float x, float y, float scale, float partialTicks, EntityLivingBase entity) {
        GlStateManager.enableColorMaterial();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 50.0F);
        GlStateManager.scale(-scale, scale, scale);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        
        RenderHelper.enableStandardItemLighting();
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

    private void renderESPElements(int mouseX, int mouseY, float x, float y, float width, float height, float modelScale) {
        float playerCenterX = x + width / 2f;
        float playerCenterY = y + height / 2f + 40;
        
        // Define the 2D box bounds that would normally surround the player in-game
        // These bounds are used as the reference for all ESP elements
        float boxWidth = 30.0f;
        float boxHeight = 60.0f;
        float posX = playerCenterX - boxWidth / 2f;
        float posY = playerCenterY - boxHeight;
        float endPosX = playerCenterX + boxWidth / 2f;
        float endPosY = playerCenterY;

        int black = new Color(0, 0, 0, 255).getRGB();

        // 1. Box ESP
        if (espModule.renderBox.getValue()) {
            int boxColor = espModule.boxColor.getValue().getRGB();
            if (espModule.boxType.getValue().equals("Full")) {
                RenderUtils.drawRectOutline(posX, posY, endPosX, endPosY, 0.5f, boxColor);
            } else {
                float perc = 0.2f;
                RenderUtils.drawRect(posX - 0.5f, posY, posX, posY + (endPosY - posY) * perc, boxColor);
                RenderUtils.drawRect(posX - 0.5f, endPosY - (endPosY - posY) * perc, posX, endPosY, boxColor);
                RenderUtils.drawRect(posX - 0.5f, posY, posX + (endPosX - posX) * perc, posY + 0.5f, boxColor);
                RenderUtils.drawRect(endPosX - (endPosX - posX) * perc, posY, endPosX, posY + 0.5f, boxColor);
                RenderUtils.drawRect(endPosX - 0.5f, posY, endPosX, posY + (endPosY - posY) * perc, boxColor);
                RenderUtils.drawRect(endPosX - 0.5f, endPosY - (endPosY - posY) * perc, endPosX, endPosY, boxColor);
                RenderUtils.drawRect(posX, endPosY - 0.5f, posX + (endPosX - posX) * perc, endPosY, boxColor);
                RenderUtils.drawRect(endPosX - (endPosX - posX) * perc, endPosY - 0.5f, endPosX - 0.5f, endPosY, boxColor);
            }
        }

        // 2. Health Bar
        float hX = posX - 3.5f + espModule.healthBarXOffset.getValue();
        float hY = posY + espModule.healthBarYOffset.getValue();
        float hXEnd = posX - 1f + espModule.healthBarXOffset.getValue();
        float hYEnd = endPosY + espModule.healthBarYOffset.getValue();
        float healthPerc = dummyPlayer.getHealth() / dummyPlayer.getMaxHealth();
        RenderUtils.drawRect(hX, hYEnd - (hYEnd - hY) * healthPerc, hXEnd, hYEnd, ColorUtils.getHealthColor(dummyPlayer).getRGB());
        RenderUtils.drawRectOutline(hX, hY, hXEnd, hYEnd, 0.5f, black);
        
        if (draggingElement == 0) {
            espModule.healthBarXOffset.setValue(mouseX - (posX - 3.5f) - dragOffsetX);
            espModule.healthBarYOffset.setValue(mouseY - posY - dragOffsetY);
        }

        // 3. Armor Bar
        if (espModule.armorBar.getValue()) {
            float aX = posX + espModule.armorBarXOffset.getValue();
            float aY = endPosY + 1.5f + espModule.armorBarYOffset.getValue();
            float aXEnd = endPosX + espModule.armorBarXOffset.getValue();
            float aYEnd = endPosY + 3.5f + espModule.armorBarYOffset.getValue();
            float armorPerc = (float) dummyPlayer.getTotalArmorValue() / 20.0f;
            RenderUtils.drawRect(aX, aY, aX + (aXEnd - aX) * armorPerc, aYEnd, Color.CYAN.getRGB());
            RenderUtils.drawRectOutline(aX, aY, aXEnd, aYEnd, 0.5f, black);
            
            if (draggingElement == 1) {
                espModule.armorBarXOffset.setValue(mouseX - posX - dragOffsetX);
                espModule.armorBarYOffset.setValue(mouseY - (endPosY + 1.5f) - dragOffsetY);
            }
        }

        // 4. Nametag
        String nameText = mc.thePlayer.getName();
        String healthText = String.format("%.1f", dummyPlayer.getHealth());
        FontRenderer font = Fonts.INTER_MEDIUM.get(10);
        float nameW = font.getStringWidth(nameText) + 4;
        float healthW = font.getStringWidth(healthText) + 4;
        float combinedW = nameW + healthW + 2;
        float nX = (posX + (endPosX - posX) / 2f - combinedW / 2f) + espModule.nametagXOffset.getValue();
        float nY = posY - 12 + espModule.nametagYOffset.getValue();
        RenderUtils.drawRect(nX, nY, nX + combinedW, nY + 10, new Color(0, 0, 0, 150).getRGB());
        font.drawStringWithShadow(nameText, nX + 2, nY + 2, -1);
        font.drawStringWithShadow(healthText, nX + nameW + 2, nY + 2, ColorUtils.getHealthColor(dummyPlayer).getRGB());
        
        if (draggingElement == 2) {
            espModule.nametagXOffset.setValue(mouseX - (posX + (endPosX - posX) / 2f - combinedW / 2f) - dragOffsetX);
            espModule.nametagYOffset.setValue(mouseY - (posY - 12) - dragOffsetY);
        }

        // 5. Held Item
        ItemStack held = dummyPlayer.getHeldItem();
        if (held != null) {
            float hiX = endPosX + 2 + espModule.heldItemXOffset.getValue();
            float hiY = posY + espModule.heldItemYOffset.getValue();
            RenderUtils.renderItemStack(held, (int) hiX, (int) hiY, 1.0f);
            if (draggingElement == 4) {
                espModule.heldItemXOffset.setValue(mouseX - (endPosX + 2) - dragOffsetX);
                espModule.heldItemYOffset.setValue(mouseY - posY - dragOffsetY);
            }
        }

        // 6. Armor Items
        float aiX = endPosX + 2 + espModule.armorItemsXOffset.getValue();
        float aiY = posY + 18 + espModule.armorItemsYOffset.getValue();
        for (int i = 0; i < 4; i++) {
            ItemStack armor = dummyPlayer.inventory.armorInventory[i];
            if (armor != null) {
                RenderUtils.renderItemStack(armor, (int) aiX, (int) (aiY + (3 - i) * 16), 1.0f);
            }
        }
        if (draggingElement == 5) {
            espModule.armorItemsXOffset.setValue(mouseX - (endPosX + 2) - dragOffsetX);
            espModule.armorItemsYOffset.setValue(mouseY - (posY + 18) - dragOffsetY);
        }
    }

    public void mouseClicked(int mouseX, int mouseY, int mouseButton, float x, float y, float width, float height) {
        if (!espModule.showEspPreview.getValue()) return;

        float rbX = x + width / 2f - RESET_BUTTON_WIDTH / 2f;
        float rbY = y + height - RESET_BUTTON_HEIGHT - 5;
        if (MouseUtils.isHovered(mouseX, mouseY, rbX, rbY, rbX + RESET_BUTTON_WIDTH, rbY + RESET_BUTTON_HEIGHT)) {
            if (mouseButton == 0) { resetOffsets(); return; }
        }

        if (mouseButton == 0) {
            float playerCenterX = x + width / 2f;
            float playerCenterY = y + height / 2f + 40;
            float boxWidth = 30.0f;
            float boxHeight = 60.0f;
            float posX = playerCenterX - boxWidth / 2f;
            float posY = playerCenterY - boxHeight;
            float endPosX = playerCenterX + boxWidth / 2f;
            float endPosY = playerCenterY;

            // Health Bar
            float hX = posX - 3.5f + espModule.healthBarXOffset.getValue();
            float hY = posY + espModule.healthBarYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, hX, hY, hX + 2.5f, hY + 60f)) {
                draggingElement = 0; dragOffsetX = mouseX - hX; dragOffsetY = mouseY - hY; return;
            }

            // Armor Bar
            float aX = posX + espModule.armorBarXOffset.getValue();
            float aY = endPosY + 1.5f + espModule.armorBarYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, aX, aY, aX + 30f, aY + 2.5f)) {
                draggingElement = 1; dragOffsetX = mouseX - aX; dragOffsetY = mouseY - aY; return;
            }

            // Nametag
            String nameText = mc.thePlayer.getName();
            String healthText = String.format("%.1f", dummyPlayer.getHealth());
            FontRenderer font = Fonts.INTER_MEDIUM.get(10);
            float combinedW = font.getStringWidth(nameText) + font.getStringWidth(healthText) + 6;
            float nX = (posX + (endPosX - posX) / 2f - combinedW / 2f) + espModule.nametagXOffset.getValue();
            float nY = posY - 12 + espModule.nametagYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, nX, nY, nX + combinedW, nY + 10)) {
                draggingElement = 2; dragOffsetX = mouseX - nX; dragOffsetY = mouseY - nY; return;
            }

            // Held Item
            float hiX = endPosX + 2 + espModule.heldItemXOffset.getValue();
            float hiY = posY + espModule.heldItemYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, hiX, hiY, hiX + 16, hiY + 16)) {
                draggingElement = 4; dragOffsetX = mouseX - hiX; dragOffsetY = mouseY - hiY; return;
            }

            // Armor Items
            float aiX = endPosX + 2 + espModule.armorItemsXOffset.getValue();
            float aiY = posY + 18 + espModule.armorItemsYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, aiX, aiY, aiX + 16, aiY + 64)) {
                draggingElement = 5; dragOffsetX = mouseX - aiX; dragOffsetY = mouseY - aiY; return;
            }
        }
    }

    public void mouseReleased(int mouseX, int mouseY, int state) {
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
