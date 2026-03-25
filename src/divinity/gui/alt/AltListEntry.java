package divinity.gui.alt;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.util.UUIDTypeAdapter;
import divinity.ClientManager;
import divinity.gui.alt.util.SavedAltData;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiListExtended;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.resources.SkinManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.Map;
import java.util.UUID;

public final class AltListEntry implements GuiListExtended.IGuiListEntry {
    private static final Minecraft mc = Minecraft.getMinecraft();
    // --- Theme Constants ---
    private static final int accentColor = new Color(121, 105, 229).getRGB();
    private static final int selectedColor = new Color(30, 30, 40, 200).getRGB();
    private static final int hoveredColor = new Color(25, 25, 35, 150).getRGB();
    private static final int defaultColor = new Color(20, 20, 27, 100).getRGB();
    private static final int textColor = new Color(220, 220, 220).getRGB();
    private static final int deleteButtonColor = new Color(180, 50, 50).getRGB();
    private static final int deleteButtonHoverColor = new Color(200, 60, 60).getRGB();
    private ResourceLocation skinLocation = null;
    private boolean skinRequested = false;
    final SavedAltData savedData;
    private final GuiAltList parent;
    private boolean isHovered = false, xHover = false;

    public AltListEntry(GuiAltList parent, SavedAltData savedData) {
        this.parent = parent;
        this.savedData = savedData;
    }

    @Override
    public void setSelected(int p_178011_1_, int p_178011_2_, int p_178011_3_) {
    }

    private void drawTextureAt(float x, float y, ResourceLocation resourceLocation) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(resourceLocation);
        GlStateManager.enableBlend();
        Gui.drawScaledCustomSizeModalRect(Math.round(x), Math.round(y), 8, 8, 8, 8, 33, 33, 64.0f, 64.0f);
        GlStateManager.disableBlend();
    }

    @Override
    public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
        this.isHovered = mouseX >= x && mouseY >= y && mouseX <= x + listWidth && mouseY <= y + slotHeight;
        int backgroundColor, textColor, statusColor;

        if (isSelected) {
            backgroundColor = selectedColor;
            textColor = new Color(255, 255, 255).getRGB();
        } else if (isHovered) {
            backgroundColor = hoveredColor;
            textColor = new Color(240, 240, 240).getRGB();
        } else {
            backgroundColor = defaultColor;
            textColor = AltListEntry.textColor;
        }

        switch (savedData.authStatus) {
            case VERIFIED:
                statusColor = new Color(60, 200, 80).getRGB();
                break;
            case PROCESSING:
                statusColor = new Color(255, 180, 30).getRGB();
                break;
            case UNVERIFIED:
            default:
                statusColor = new Color(200, 60, 60).getRGB();
                break;
        }

        int padding = 2;

        RenderUtils.drawRect(x - 1, y - 1, x + listWidth - 4, y + slotHeight - padding + 4, backgroundColor);

        if (isSelected) RenderUtils.drawGradientRect(x, y, x + 3, y + slotHeight + 1, false, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());

        GameProfile gameProfile = new GameProfile(savedData.profileID == null ? UUID.randomUUID() : UUIDTypeAdapter.fromString(savedData.profileID), savedData.cachedUsername);

        if (!skinRequested) {
            skinRequested = true;

            Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> cached = mc.getSkinManager().loadSkinFromCache(gameProfile);
            if (cached != null && cached.containsKey(MinecraftProfileTexture.Type.SKIN)) {
                skinLocation = mc.getSkinManager().loadSkin(cached.get(MinecraftProfileTexture.Type.SKIN), MinecraftProfileTexture.Type.SKIN);
            } else {
                mc.getSkinManager().loadProfileTextures(gameProfile, (type, location, texture) -> {
                    if (type == MinecraftProfileTexture.Type.SKIN) skinLocation = mc.getSkinManager().loadSkin(texture, type);
                }, true);
            }
        }

        ResourceLocation resourceLocation;

        if (skinLocation != null) {
            resourceLocation = skinLocation;
        } else {
            UUID uuid = EntityPlayer.getUUID(gameProfile);
            resourceLocation = DefaultPlayerSkin.getDefaultSkin(uuid);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        final float targetOffset = isSelected ? 4f : 0f;
        drawTextureAt((float) x + targetOffset, y, resourceLocation);

        Fonts.INTER_MEDIUM.get(18).drawString(gameProfile.getName(), x + 42, y + 5, textColor);

        String statusText = "Status: " + savedData.authStatus.displayName;
        Fonts.INTER_MEDIUM.get(16).drawString(statusText, x + 42, y + 20, statusColor);

        // Draw delete button
        float buttonX = x + listWidth - 14;
        float buttonY = y + 1;
        float buttonSize = 8;

        Color buttonColor = xHover ? new Color(deleteButtonHoverColor) : new Color(deleteButtonColor);

        RenderUtils.drawRect(buttonX - 1, buttonY - 1, buttonX + buttonSize + 1, buttonY + buttonSize + 1, buttonColor.getRGB());

        float textWidth = Fonts.INTER_MEDIUM.get(16).getStringWidth("X");
        float textHeight = Fonts.INTER_MEDIUM.get(16).getHeight();

        float textX = buttonX + (buttonSize - textWidth) / 2;
        float textY = buttonY + (buttonSize - textHeight) / 2 + 0.7f;

        Fonts.INTER_MEDIUM.get(16).drawString("X", textX, textY, -1); // White text for contrast
        this.xHover = mouseX >= buttonX && mouseY >= buttonY && mouseX <= buttonX + buttonSize && mouseY <= buttonY + buttonSize;
    }

    @Override
    public boolean mousePressed(int slotIndex, int p_148278_2_, int p_148278_3_, int p_148278_4_, int p_148278_5_, int p_148278_6_) {
        parent.selectedEntry = slotIndex;
        if (isHovered && p_148278_4_ == 0 && xHover) {
            ClientManager.getInstance().getAltManager().removeAlt(slotIndex);
        }
        return false;
    }

    @Override
    public void mouseReleased(int slotIndex, int x, int y, int mouseEvent, int relativeX, int relativeY) {
    }
}