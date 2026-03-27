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

/**
 * ESP Preview Component.
 * This is no longer a standalone widget but a component rendered inside the ESP module's settings.
 */
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

        // Draw background
        ShaderUtils.drawRoundRect(x, y, x + width, y + height, 5, new Color(15, 15, 20, 255).getRGB());
        Fonts.INTER_MEDIUM.get(16).drawCenteredStringWithShadow("ESP Preview", x + width / 2f, y + 5, -1);

        if (dummyPlayer != null) {
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            // Draw player model
            drawPlayerModel(x + width / 2f, y + height / 2f + 45, 45, partialTicks, dummyPlayer);

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            
            renderESPElements(mouseX, mouseY, x, y, width, height, partialTicks);
        }

        // Reset button
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

    private void renderESPElements(int mouseX, int mouseY, float x, float y, float width, float height, float partialTicks) {
        float playerCenterX = x + width / 2f;
        float playerCenterY = y + height / 2f + 45;
        float scale = espModule.espPreviewScale.getValue();

        float espBaseX = playerCenterX;
        float espBaseY = playerCenterY - 60;

        // Box ESP Rendering in Preview
        if (espModule.renderBox.getValue()) {
            float boxW = 35 * scale;
            float boxH = 65 * scale;
            float boxX = espBaseX - boxW / 2f;
            float boxY = espBaseY;
            int boxColor = espModule.boxColor.getValue().getRGB();

            if (espModule.boxType.getValue().equals("Full")) {
                RenderUtils.drawRectOutline(boxX, boxY, boxX + boxW, boxY + boxH, 1f, boxColor);
            } else {
                float perc = 0.2f;
                // Corners
                RenderUtils.drawRect(boxX, boxY, boxX + boxW * perc, boxY + 1, boxColor);
                RenderUtils.drawRect(boxX, boxY, boxX + 1, boxY + boxH * perc, boxColor);
                RenderUtils.drawRect(boxX + boxW * (1 - perc), boxY, boxX + boxW, boxY + 1, boxColor);
                RenderUtils.drawRect(boxX + boxW - 1, boxY, boxX + boxW, boxY + boxH * perc, boxColor);
                RenderUtils.drawRect(boxX, boxY + boxH - 1, boxX + boxW * perc, boxY + boxH, boxColor);
                RenderUtils.drawRect(boxX, boxY + boxH * (1 - perc), boxX + 1, boxY + boxH, boxColor);
                RenderUtils.drawRect(boxX + boxW * (1 - perc), boxY + boxH - 1, boxX + boxW, boxY + boxH, boxColor);
                RenderUtils.drawRect(boxX + boxW - 1, boxY + boxH * (1 - perc), boxX + boxW, boxY + boxH, boxColor);
            }
        }

        // Health Bar
        float hbW = 2.5f * scale;
        float hbH = 65f * scale;
        float hbX = espBaseX - 22 * scale + espModule.healthBarXOffset.getValue();
        float hbY = espBaseY + espModule.healthBarYOffset.getValue();
        RenderUtils.drawRect(hbX, hbY, hbX + hbW, hbY + hbH, ColorUtils.getHealthColor(dummyPlayer).getRGB());
        RenderUtils.drawRectOutline(hbX, hbY, hbX + hbW, hbY + hbH, 0.5f, Color.BLACK.getRGB());
        if (draggingElement == 0) {
            espModule.healthBarXOffset.setValue(mouseX - (espBaseX - 22 * scale) - dragOffsetX);
            espModule.healthBarYOffset.setValue(mouseY - espBaseY - dragOffsetY);
        }

        // Armor Bar
        if (espModule.armorBar.getValue()) {
            float abW = 35f * scale;
            float abH = 2.5f * scale;
            float abX = espBaseX - abW / 2f + espModule.armorBarXOffset.getValue();
            float abY = espBaseY + 68 * scale + espModule.armorBarYOffset.getValue();
            RenderUtils.drawRect(abX, abY, abX + abW, abY + abH, Color.CYAN.getRGB());
            RenderUtils.drawRectOutline(abX, abY, abX + abW, abY + abH, 0.5f, Color.BLACK.getRGB());
            if (draggingElement == 1) {
                espModule.armorBarXOffset.setValue(mouseX - (espBaseX - abW / 2f) - dragOffsetX);
                espModule.armorBarYOffset.setValue(mouseY - (espBaseY + 68 * scale) - dragOffsetY);
            }
        }

        // Nametag
        String name = mc.thePlayer.getName();
        FontRenderer font = Fonts.INTER_MEDIUM.get((int)(10 * scale));
        float nW = font.getStringWidth(name) + 4;
        float nX = espBaseX - nW / 2f + espModule.nametagXOffset.getValue();
        float nY = espBaseY - 12 * scale + espModule.nametagYOffset.getValue();
        RenderUtils.drawRect(nX, nY, nX + nW, nY + 10 * scale, new Color(0, 0, 0, 150).getRGB());
        font.drawStringWithShadow(name, nX + 2, nY + 2 * scale, -1);
        if (draggingElement == 2) {
            espModule.nametagXOffset.setValue(mouseX - (espBaseX - nW / 2f) - dragOffsetX);
            espModule.nametagYOffset.setValue(mouseY - (espBaseY - 12 * scale) - dragOffsetY);
        }

        // Items
        ItemStack held = dummyPlayer.getHeldItem();
        if (held != null) {
            float hiX = espBaseX + 20 * scale + espModule.heldItemXOffset.getValue();
            float hiY = espBaseY + 20 * scale + espModule.heldItemYOffset.getValue();
            RenderUtils.renderItemStack(held, (int) hiX, (int) hiY, scale);
            if (draggingElement == 4) {
                espModule.heldItemXOffset.setValue(mouseX - (espBaseX + 20 * scale) - dragOffsetX);
                espModule.heldItemYOffset.setValue(mouseY - (espBaseY + 20 * scale) - dragOffsetY);
            }
        }

        float aiX = espBaseX + 20 * scale + espModule.armorItemsXOffset.getValue();
        float aiY = espBaseY + 40 * scale + espModule.armorItemsYOffset.getValue();
        for (int i = 0; i < 4; i++) {
            ItemStack armor = dummyPlayer.inventory.armorInventory[i];
            if (armor != null) {
                RenderUtils.renderItemStack(armor, (int) aiX, (int) (aiY + (3 - i) * 16 * scale), scale);
            }
        }
        if (draggingElement == 5) {
            espModule.armorItemsXOffset.setValue(mouseX - (espBaseX + 20 * scale) - dragOffsetX);
            espModule.armorItemsYOffset.setValue(mouseY - (espBaseY + 40 * scale) - dragOffsetY);
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
            float playerCenterY = y + height / 2f + 45;
            float scale = espModule.espPreviewScale.getValue();
            float espBaseX = playerCenterX;
            float espBaseY = playerCenterY - 60;

            // Check elements for dragging
            // Health Bar
            float hX = espBaseX - 22 * scale + espModule.healthBarXOffset.getValue();
            float hY = espBaseY + espModule.healthBarYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, hX, hY, hX + 5 * scale, hY + 65 * scale)) {
                draggingElement = 0; dragOffsetX = mouseX - hX; dragOffsetY = mouseY - hY; return;
            }

            // Armor Bar
            float abX = espBaseX - 17.5f * scale + espModule.armorBarXOffset.getValue();
            float abY = espBaseY + 68 * scale + espModule.armorBarYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, abX, abY, abX + 35 * scale, abY + 5 * scale)) {
                draggingElement = 1; dragOffsetX = mouseX - abX; dragOffsetY = mouseY - abY; return;
            }

            // Nametag
            float nW = Fonts.INTER_MEDIUM.get((int)(10 * scale)).getStringWidth(mc.thePlayer.getName()) + 4;
            float nX = espBaseX - nW / 2f + espModule.nametagXOffset.getValue();
            float nY = espBaseY - 12 * scale + espModule.nametagYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, nX, nY, nX + nW, nY + 10 * scale)) {
                draggingElement = 2; dragOffsetX = mouseX - nX; dragOffsetY = mouseY - nY; return;
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
