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

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (!espModule.showEspPreview.getValue()) return;

        if (dummyPlayer == null && mc.theWorld != null) initDummyPlayer();

        ShaderUtils.drawRoundRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 5, new Color(9, 9, 14, 200).getRGB());
        Fonts.INTER_MEDIUM.get(16).drawCenteredStringWithShadow(getName(), getX() + getWidth() / 2f, getY() + 5, -1);

        if (dummyPlayer != null) {
            // Orientation Fix: Ensure correct GL states for 1.8.9
            GlStateManager.enableDepth();
            GlStateManager.depthMask(true);
            GlStateManager.enableAlpha();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(770, 771);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            // Draw player model with correct orientation (fixed rotation)
            drawPlayerModel(getX() + getWidth() / 2f, getY() + getHeight() / 2f + 45, 45, partialTicks, dummyPlayer);

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableDepth();
            
            renderESPElements(mouseX, mouseY, partialTicks);
        }

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
        GlStateManager.scale(-scale, scale, scale); // Orientation Fix: Removed negative scale on Y to prevent upside down
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

    private void renderESPElements(int mouseX, int mouseY, float partialTicks) {
        float playerScreenX = getX() + getWidth() / 2f;
        float playerScreenY = getY() + getHeight() / 2f + 20 - 30;
        float currentScale = espModule.espPreviewScale.getValue();

        // 1. Health Bar
        if (espModule.armorBar.getValue()) {
            float hbW = 2.5f * currentScale;
            float hbH = 30f * currentScale;
            float x = playerScreenX - 25 * currentScale + espModule.healthBarXOffset.getValue();
            float y = playerScreenY + espModule.healthBarYOffset.getValue();

            float healthPercentage = dummyPlayer.getHealth() / dummyPlayer.getMaxHealth();
            float filledHeight = hbH * healthPercentage;
            RenderUtils.drawRect(x, y + (hbH - filledHeight), x + hbW, y + hbH, ColorUtils.getHealthColor(dummyPlayer).getRGB());
            RenderUtils.drawRectOutline(x, y, x + hbW, y + hbH, 0.5f, Color.BLACK.getRGB());

            if (draggingElement == 0) {
                espModule.healthBarXOffset.setValue((float) (mouseX - getX() - dragOffsetX + 25 * currentScale));
                espModule.healthBarYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }

        // 2. Armor Bar
        if (espModule.armorBar.getValue()) {
            float abW = 30f * currentScale;
            float abH = 2.5f * currentScale;
            float x = playerScreenX - 15 * currentScale + espModule.armorBarXOffset.getValue();
            float y = playerScreenY + 40 * currentScale + espModule.armorBarYOffset.getValue();

            float armorPercentage = (float) dummyPlayer.getTotalArmorValue() / 20.0f;
            RenderUtils.drawRect(x, y, x + abW * armorPercentage, y + abH, Color.CYAN.getRGB());
            RenderUtils.drawRectOutline(x, y, x + abW, y + abH, 0.5f, Color.BLACK.getRGB());

            if (draggingElement == 1) {
                espModule.armorBarXOffset.setValue((float) (mouseX - getX() - dragOffsetX + 15 * currentScale));
                espModule.armorBarYOffset.setValue((float) (mouseY - getY() - dragOffsetY - 40 * currentScale));
            }
        }

        // 3. Nametag (Using Local Player Name)
        String nameText = mc.thePlayer != null ? mc.thePlayer.getName() : "You";
        String healthText = String.format("%.1f" + EnumChatFormatting.RED + " \u2764", dummyPlayer.getHealth());
        FontRenderer font = Fonts.INTER_MEDIUM.get((int)(10 * currentScale));
        double hW = font.getStringWidth(healthText) + 3;
        double nW = font.getStringWidth(nameText) + 3;
        double combinedW = hW + 2 * currentScale + nW;
        double groupL = playerScreenX - (combinedW / 2.0) + espModule.nametagXOffset.getValue();
        double rectT = playerScreenY - 15 * currentScale + espModule.nametagYOffset.getValue();
        double rectB = rectT + 10 * currentScale;

        RenderUtils.drawRect((float) groupL, (float) rectT, (float) (groupL + hW), (float) rectB, new Color(0, 0, 0, 120).getRGB());
        font.drawStringWithShadow(healthText, (float) (groupL + 1.5f), (float) (rectT + 2f * currentScale), -1);
        double nL = groupL + hW + 2 * currentScale;
        RenderUtils.drawRect((float) nL, (float) rectT, (float) (nL + nW), (float) rectB, new Color(0, 0, 0, 120).getRGB());
        font.drawStringWithShadow(nameText, (float) (nL + 1.5f), (float) (rectT + 2f * currentScale), -1);

        if (draggingElement == 2) {
            espModule.nametagXOffset.setValue((float) (mouseX - getX() - dragOffsetX + (combinedW / 2.0)));
            espModule.nametagYOffset.setValue((float) (mouseY - getY() - dragOffsetY + 15 * currentScale));
        }

        // 4. Items
        float itemXBase = playerScreenX + 25 * currentScale;
        float itemYBase = playerScreenY;
        
        // Held Item
        ItemStack held = dummyPlayer.getHeldItem();
        if (held != null) {
            float x = itemXBase + espModule.heldItemXOffset.getValue();
            float y = itemYBase + espModule.heldItemYOffset.getValue();
            RenderUtils.renderItemStack(held, (int) x, (int) y, currentScale);
            if (draggingElement == 4) {
                espModule.heldItemXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
                espModule.heldItemYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
            }
        }

        // Armor Items
        float armorX = itemXBase + 20 * currentScale + espModule.armorItemsXOffset.getValue();
        float armorY = itemYBase + espModule.armorItemsYOffset.getValue();
        for (int i = 0; i < 4; i++) {
            ItemStack armor = dummyPlayer.inventory.armorInventory[i];
            if (armor != null) {
                float y = armorY + (3 - i) * (18 * currentScale);
                RenderUtils.renderItemStack(armor, (int) armorX, (int) y, currentScale);
            }
        }
        if (draggingElement == 5) {
            espModule.armorItemsXOffset.setValue((float) (mouseX - getX() - dragOffsetX));
            espModule.armorItemsYOffset.setValue((float) (mouseY - getY() - dragOffsetY));
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!espModule.showEspPreview.getValue()) return;

        float rbX = getX() + getWidth() / 2f - RESET_BUTTON_WIDTH / 2f;
        float rbY = getY() + getHeight() - RESET_BUTTON_HEIGHT - 5;
        if (MouseUtils.isHovered(mouseX, mouseY, rbX, rbY, rbX + RESET_BUTTON_WIDTH, rbY + RESET_BUTTON_HEIGHT)) {
            if (mouseButton == 0) { resetOffsets(); return; }
        }

        if (mouseButton == 0) {
            float pX = getX() + getWidth() / 2f;
            float pY = getY() + getHeight() / 2f + 20 - 30;
            float scale = espModule.espPreviewScale.getValue();

            // Check elements for dragging (Bounding Box logic)
            // Health Bar
            float hX = pX - 25 * scale + espModule.healthBarXOffset.getValue();
            float hY = pY + espModule.healthBarYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, hX, hY, hX + 5 * scale, hY + 30 * scale)) {
                draggingElement = 0; dragOffsetX = (int) (mouseX - hX); dragOffsetY = (int) (mouseY - hY); return;
            }

            // Armor Bar
            float aX = pX - 15 * scale + espModule.armorBarXOffset.getValue();
            float aY = pY + 40 * scale + espModule.armorBarYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, aX, aY, aX + 30 * scale, aY + 5 * scale)) {
                draggingElement = 1; dragOffsetX = (int) (mouseX - aX); dragOffsetY = (int) (mouseY - aY); return;
            }

            // Nametag
            FontRenderer font = Fonts.INTER_MEDIUM.get((int)(10 * scale));
            String name = mc.thePlayer != null ? mc.thePlayer.getName() : "You";
            String health = String.format("%.1f" + EnumChatFormatting.RED + " \u2764", dummyPlayer.getHealth());
            double combinedW = (font.getStringWidth(health) + 3) + (2 * scale) + (font.getStringWidth(name) + 3);
            float nX = (float) (pX - (combinedW / 2.0)) + espModule.nametagXOffset.getValue();
            float nY = pY - 15 * scale + espModule.nametagYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, nX, nY, nX + (float)combinedW, nY + 10 * scale)) {
                draggingElement = 2; dragOffsetX = (int) (mouseX - nX); dragOffsetY = (int) (mouseY - nY); return;
            }

            // Held Item
            float hIX = pX + 25 * scale + espModule.heldItemXOffset.getValue();
            float hIY = pY + espModule.heldItemYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, hIX, hIY, hIX + 16 * scale, hIY + 16 * scale)) {
                draggingElement = 4; dragOffsetX = (int) (mouseX - hIX); dragOffsetY = (int) (mouseY - hIY); return;
            }

            // Armor Items
            float aIX = pX + 45 * scale + espModule.armorItemsXOffset.getValue();
            float aIY = pY + espModule.armorItemsYOffset.getValue();
            if (MouseUtils.isHovered(mouseX, mouseY, aIX, aIY, aIX + 16 * scale, aIY + 72 * scale)) {
                draggingElement = 5; dragOffsetX = (int) (mouseX - aIX); dragOffsetY = (int) (mouseY - aIY); return;
            }
        }
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
