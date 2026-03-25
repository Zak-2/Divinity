package divinity.module.impl.render.element.impl;

import divinity.ClientManager;
import divinity.handler.PlayerTrackerHandler;
import divinity.module.impl.render.element.GuiEditElement;
import divinity.module.impl.render.element.core.DraggableElement;
import divinity.module.impl.render.element.core.graphicbox.GraphicBoxField;
import divinity.module.impl.world.hypixel.MurderMystery;
import divinity.utils.ColorUtils;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.FontRenderer;
import divinity.utils.font.Fonts;
import divinity.utils.player.PlayerUtils;
import divinity.utils.server.HypixelUtils;
import divinity.voice.VoiceClient;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

import java.awt.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PlayerListElement extends DraggableElement {

    private EntityPlayer contextMenuPlayer = null;
    private float contextMenuX = 0;
    private float contextMenuY = 0;
    private boolean clickConsumed = false;
    private static final float CONTEXT_MENU_WIDTH = 120f;
    private static final float CONTEXT_MENU_ITEM_HEIGHT = 20f;

    private static final float OUTER_PAD = 7f;
    private static final float HEADER_H = 26f;
    private static final float ROW_H = 14f;
    private static final float RADIUS = 10f;
    private static final float ICON_S = 10f;

    private boolean resizing = false;
    private float resizeStartX = 0;
    private float resizeStartY = 0;
    private float resizeStartWidth = 0;

    private static final float RESIZE_HANDLE_SIZE = 20f;

    private int lastMouseX = 0;
    private int lastMouseY = 0;

    private float scaleFactor = 1.0f;
    private static final float MIN_SCALE = 0.6f;
    private static final float MAX_SCALE = 2.0f;

    private static final List<GraphicBoxField<EntityPlayer>> FIELDS = Arrays.asList(
            new GraphicBoxField<>("Player", player -> {
                String username = null;
                try {
                    username = null;
                } catch (Exception ignored) {
                }

                String baseName = player.getGameProfile().getName();

                boolean isVcActive = ClientManager.getInstance().getModuleManager().proximityChat != null
                        && ClientManager.getInstance().getModuleManager().proximityChat.isState()
                        && ClientManager.getInstance().getModuleManager().proximityChat.espSpeaking.getValue()
                        && VoiceClient.getInstance().isUserSpeaking(player.getName());

                StringBuilder result = new StringBuilder(baseName);

                if (isVcActive) {
                    result.append(" [VC]");
                }

                if (username != null) {
                    result.append(" (").append(username).append(")");
                }

                return result.toString();

            }, player -> player == mc.thePlayer ? ClientManager.getInstance().getMainColor().getRGB() : ColorUtils.getColorRGB(EnumChatFormatting.GRAY)),
            new GraphicBoxField<>("HP", player -> String.valueOf((int) Math.ceil(player.getHealth())), player -> {
                float healthPercent = player.getMaxHealth() > 0 ? player.getHealth() / player.getMaxHealth() : 0;
                if (healthPercent > 0.66f) return ColorUtils.getColorRGB(EnumChatFormatting.GREEN);
                if (healthPercent > 0.33f) return ColorUtils.getColorRGB(EnumChatFormatting.YELLOW);
                return ColorUtils.getColorRGB(EnumChatFormatting.RED);
            }),
            new GraphicBoxField<>("Dist", player -> {
                if (player == mc.thePlayer || mc.thePlayer == null) return "0";
                return Math.round(mc.thePlayer.getDistanceToEntity(player)) + " blocks";
            }, player -> ColorUtils.getColorRGB(EnumChatFormatting.GRAY)),
            new GraphicBoxField<>("Kills", player -> {
                try {
                    return String.valueOf(PlayerTrackerHandler.getKillsForPlayer(player));
                } catch (Exception e) {
                    return "?";
                }
            }, player -> ColorUtils.getColorRGB(EnumChatFormatting.GRAY)),
            new GraphicBoxField<>("Team", player -> {
                if (HypixelUtils.isInLobby()) return "None";

                final String gameMode = HypixelUtils.getHypixelGameMode();

                switch (gameMode) {
                    case "BedWars":
                        return PlayerUtils.getTeamColorName(player);
                    case "SkyWars":
                    case "Pit":
                        return (player == mc.thePlayer) ? "Self" : "Enemy";
                    case "Duels":
                        return (player == mc.thePlayer) ? "Self" : "Opponent";
                    case "MurderMystery":
                        MurderMystery mmModule = (MurderMystery) ClientManager.getInstance().getModuleManager().get(MurderMystery.class);
                        if (mmModule != null && mmModule.isState()) {
                            if (mmModule.getDetectedMurderer() != null && player == mmModule.getDetectedMurderer())
                                return "Murderer";
                            ItemStack heldItem = player.getHeldItem();
                            if (heldItem != null && heldItem.getItem() == Items.bow) return "Detective";
                            return "Innocent";
                        } else return "Unknown";
                    default:
                        return "Unknown";
                }
            }, player -> {
                if (HypixelUtils.isInLobby()) return ColorUtils.getColorRGB(EnumChatFormatting.GRAY);

                final String gameMode = HypixelUtils.getHypixelGameMode();
                final EnumChatFormatting teamFormat = PlayerUtils.getPlayerTeamColor(player);

                if (gameMode.equals("BedWars") && teamFormat != null) return ColorUtils.getColorRGB(teamFormat);
                else if (gameMode.equals("SkyWars") || gameMode.equals("Pit"))
                    return (player == mc.thePlayer) ? ColorUtils.getColorRGB(EnumChatFormatting.WHITE) : ColorUtils.getColorRGB(EnumChatFormatting.RED);
                else if (gameMode.equals("Duels"))
                    return (player == mc.thePlayer) ? ColorUtils.getColorRGB(EnumChatFormatting.WHITE) : ColorUtils.getColorRGB(EnumChatFormatting.YELLOW);
                else if (gameMode.equals("MurderMystery")) {
                    MurderMystery mmModule = (MurderMystery) ClientManager.getInstance().getModuleManager().get(MurderMystery.class);
                    if (mmModule != null && mmModule.isState()) {
                        if (mmModule.getDetectedMurderer() != null && player == mmModule.getDetectedMurderer())
                            return ColorUtils.getColorRGB(EnumChatFormatting.RED);
                        ItemStack heldItem = player.getHeldItem();
                        if (heldItem != null && heldItem.getItem() == Items.bow)
                            return ColorUtils.getColorRGB(EnumChatFormatting.BLUE);
                        return ColorUtils.getColorRGB(EnumChatFormatting.GREEN);
                    } else return ColorUtils.getColorRGB(EnumChatFormatting.GRAY);
                }
                return ColorUtils.getColorRGB(EnumChatFormatting.GRAY);
            }),
            new GraphicBoxField<>("Hacker", player -> player == mc.thePlayer ? "" : "?", player -> ColorUtils.getColorRGB(EnumChatFormatting.GRAY))
    );

    private List<EntityPlayer> playersToDisplay = Collections.emptyList();

    public PlayerListElement(int x, int y) {
        super(x, y, 10, 10);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    public boolean consumeClick() {
        boolean v = clickConsumed;
        clickConsumed = false;
        return v;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (getX() < 1) setX(1);
        if (getY() < 1) setY(1);

        boolean inGui = mc.currentScreen != null;
        if (inGui || (mouseX != 0 || mouseY != 0)) {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        int effectiveMouseX = lastMouseX;
        int effectiveMouseY = lastMouseY;

        if (resizing) {
            float deltaX = effectiveMouseX - resizeStartX;
            float deltaY = effectiveMouseY - resizeStartY;

            float delta = (deltaX + deltaY) / 2.0f;
            float scaleChange = delta / 150f;
            float newScale = resizeStartWidth + scaleChange;
            scaleFactor = Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale));
        }

        updateAndSortPlayers();

        if (mc.theWorld == null || playersToDisplay.isEmpty()) {
            this.setWidth(10);
            this.setHeight(10);
            super.drawScreen(mouseX, mouseY, partialTicks);
            return;
        }

        int scaledHeaderFontSize = (int)(14 * scaleFactor);
        int scaledFontSize = (int)(12 * scaleFactor);
        FontRenderer headerFont = Fonts.INTER_MEDIUM.get(scaledHeaderFontSize);
        FontRenderer font = Fonts.INTER_MEDIUM.get(scaledFontSize);

        int rows = Math.min(playersToDisplay.size(), 24);
        int cols = FIELDS.size();

        float scaledOuterPad = OUTER_PAD * scaleFactor;
        float scaledHeaderH = HEADER_H * scaleFactor;
        float scaledRowH = ROW_H * scaleFactor;
        float scaledRadius = RADIUS * scaleFactor;
        float scaledIconS = ICON_S * scaleFactor;

        float[] colW = new float[cols];
        for (int c = 0; c < cols; c++) {
            GraphicBoxField<EntityPlayer> field = FIELDS.get(c);
            float max = font.getStringWidth(field.title) + 10f * scaleFactor;
            for (int r = 0; r < rows; r++) {
                String v = safe(field.valueFunc.apply(playersToDisplay.get(r)));
                float w = font.getStringWidth(v) + 10f * scaleFactor;
                if (c == 0) w += scaledIconS + 6f * scaleFactor;
                if (w > max) max = w;
            }
            colW[c] = Math.max(36f * scaleFactor, max);
        }

        float w = 0;
        for (float cw : colW) w += cw;
        w += scaledOuterPad * 2f;
        float h = scaledOuterPad * 2f + scaledHeaderH + rows * scaledRowH;

        float x = getX();
        float y = getY();

        int bg = RenderUtils.reAlpha(new Color(9, 9, 14).getRGB(), 0.78f);
        int border = RenderUtils.reAlpha(0xFFFFFFFF, 0.09f);
        ShaderUtils.drawRoundRect(x - 0.6f, y - 0.6f, x + w + 0.6f, y + h + 0.6f, scaledRadius + 0.6f, border);
        ShaderUtils.drawRoundRect(x, y, x + w, y + h, scaledRadius, bg);
        ShaderUtils.drawGradientRect(x + scaledOuterPad, y + scaledOuterPad + 13f * scaleFactor, x + w - scaledOuterPad, y + scaledOuterPad + 14.5f * scaleFactor,
                RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.85f),
                RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.85f),
                true
        );

        String title = "Players";
        String count = String.valueOf(rows);
        headerFont.drawStringWithShadow(title, x + scaledOuterPad, y + scaledOuterPad + 4f * scaleFactor, 0xFFFFFFFF);
        float countW = headerFont.getStringWidth(count);
        headerFont.drawStringWithShadow(count, x + w - scaledOuterPad - countW, y + scaledOuterPad + 4f * scaleFactor, RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 1f));

        float headerY = y + scaledOuterPad + 16f * scaleFactor;
        float listY = y + scaledOuterPad + scaledHeaderH;

        float cx = x + scaledOuterPad;
        int headerCol = RenderUtils.reAlpha(new Color(210, 210, 215).getRGB(), 0.75f);
        for (int c = 0; c < cols; c++) {
            String t = FIELDS.get(c).title;
            float tw = font.getStringWidth(t);
            float tx;
            if (c == 0) tx = cx + 6f * scaleFactor;
            else tx = cx + colW[c] - tw - 6f * scaleFactor;
            font.drawStringWithShadow(t, tx, headerY + 3 * scaleFactor, headerCol);
            cx += colW[c];
            if (c < cols - 1) {
                RenderUtils.drawRect(cx - 0.5f, headerY - 2f * scaleFactor, cx + 0.5f, y + h - scaledOuterPad, RenderUtils.reAlpha(0xFFFFFFFF, 0.05f));
            }
        }
        RenderUtils.drawRect(x + scaledOuterPad, listY - 1f, x + w - scaledOuterPad, listY, RenderUtils.reAlpha(0xFFFFFFFF, 0.06f));

        for (int r = 0; r < rows; r++) {
            EntityPlayer p = playersToDisplay.get(r);
            float rowY = listY + r * scaledRowH;
            int alt = RenderUtils.reAlpha(0xFFFFFFFF, (r % 2 == 0 ? 0.03f : 0.00f));
            if ((alt >>> 24) != 0) RenderUtils.drawRect(x + scaledOuterPad, rowY, x + w - scaledOuterPad, rowY + scaledRowH, alt);

            if (p == mc.thePlayer) {
                RenderUtils.drawGradientRectHorizontal(x + scaledOuterPad, rowY, x + w - scaledOuterPad, rowY + scaledRowH,
                        RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.10f),
                        RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.10f)
                );
            }
            else if (ClientManager.getInstance().getFriendManager().isFriend(p.getName())) {
                RenderUtils.drawGradientRectHorizontal(x + scaledOuterPad, rowY, x + w - scaledOuterPad, rowY + scaledRowH,
                        RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.08f),
                        RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.08f)
                );
            }

            float colX = x + scaledOuterPad;

            float textY = rowY + (scaledRowH - font.getHeight()) / 2f + 0.5f * scaleFactor;

            for (int c = 0; c < cols; c++) {
                GraphicBoxField<EntityPlayer> field = FIELDS.get(c);
                String value = safe(field.valueFunc.apply(p));
                int color = field.valueColorFunc.apply(p);
                if (c == 0) {
                    float headX = colX + 4f * scaleFactor;
                    float headY = rowY + (scaledRowH - scaledIconS) / 2f;
                    drawHead(p, headX, headY, scaledIconS);
                    float textX = colX + 4f * scaleFactor + scaledIconS + 5f * scaleFactor;
                    drawPlayerValue(font, value, textX, textY, color, scaleFactor);
                } else {
                    float tw = font.getStringWidth(value);
                    float tx = colX + colW[c] - tw - 6f * scaleFactor;
                    font.drawStringWithShadow(value, tx, textY, color);
                }
                colX += colW[c];
            }
        }

        this.setWidth((int) w);
        this.setHeight((int) h);

        boolean isInChat = mc.currentScreen instanceof net.minecraft.client.gui.GuiChat;
        boolean isInEditMode = mc.currentScreen instanceof GuiEditElement;

        if (isInEditMode || isInChat) {
            float handleSize = RESIZE_HANDLE_SIZE * scaleFactor;
            float handleX = x + w - handleSize;
            float handleY = y + h - handleSize;

            boolean isHoveringHandle = MouseUtils.isHovered(effectiveMouseX, effectiveMouseY, handleX, handleY, x + w, y + h);
            boolean active = isHoveringHandle || resizing;

            int cornerColor = active ? 0xCCFFFFFF : 0x50FFFFFF;

            float bracketSize = 6f * scaleFactor;
            float thickness = 1.5f * scaleFactor;

            RenderUtils.drawRect(x + w - thickness, y + h - bracketSize, x + w, y + h, cornerColor);
            RenderUtils.drawRect(x + w - bracketSize, y + h - thickness, x + w - thickness, y + h, cornerColor);
        }

        if (contextMenuPlayer != null) {
            drawContextMenu(mouseX, mouseY, font, scaleFactor);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawPlayerValue(FontRenderer font, String full, float x, float y, int baseColor, float scale) {
        if (full.contains("[VC]") || full.contains("(")) {
            String remaining = full;
            float currentX = x;

            int vcIndex = remaining.indexOf("[VC]");
            int parenIndex = remaining.indexOf("(");

            int firstSpecialIndex = -1;
            if (vcIndex != -1 && parenIndex != -1) {
                firstSpecialIndex = Math.min(vcIndex, parenIndex);
            } else if (vcIndex != -1) {
                firstSpecialIndex = vcIndex;
            } else if (parenIndex != -1) {
                firstSpecialIndex = parenIndex;
            }

            if (firstSpecialIndex > 0) {
                String playerName = remaining.substring(0, firstSpecialIndex).trim();
                font.drawStringWithShadow(playerName, currentX, y, baseColor);
                currentX += font.getStringWidth(playerName + " ");
                remaining = remaining.substring(firstSpecialIndex);
            }

            if (remaining.startsWith("[VC]")) {
                font.drawStringWithShadow(EnumChatFormatting.AQUA + "[VC]", currentX, y, 0xFFFFFFFF);
                currentX += font.getStringWidth("[VC] ");
                remaining = remaining.substring(4).trim();
            }

            if (remaining.startsWith("(") && remaining.endsWith(")")) {
                font.drawStringGradient(remaining, currentX, y,
                        ClientManager.getInstance().getMainColor().getRGB(),
                        ClientManager.getInstance().getSecondaryColor().getRGB());
                return;
            }

            if (!remaining.isEmpty()) {
                font.drawStringWithShadow(remaining, currentX, y, baseColor);
            }
            return;
        }

        font.drawStringWithShadow(full, x, y, baseColor);
    }

    private void drawHead(EntityPlayer player, float x, float y, float size) {
        if (!(player instanceof AbstractClientPlayer)) {
            return;
        }
        AbstractClientPlayer ap = (AbstractClientPlayer) player;
        ResourceLocation skin = ap.getLocationSkin();
        if (skin == null) {
            return;
        }
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(skin);
        GlStateManager.color(1f, 1f, 1f, 1f);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 8f, 8f, 8, 8, (int) size, (int) size, 64f, 64f);
        Gui.drawScaledCustomSizeModalRect((int) x, (int) y, 40f, 8f, 8, 8, (int) size, (int) size, 64f, 64f);
    }

    private String getPlayerTeamOrRoleString(EntityPlayer player) {
        if (HypixelUtils.isInLobby()) return "None";

        final String gameMode = HypixelUtils.getHypixelGameMode();

        switch (gameMode) {
            case "BedWars":
                return PlayerUtils.getTeamColorName(player);
            case "SkyWars":
            case "Pit":
                return (player == mc.thePlayer) ? "Self (Combat)" : "Enemy";
            case "Duels":
                return (player == mc.thePlayer) ? "Self (Duels)" : "Opponent";
            case "MurderMystery":
                MurderMystery mmModule = (MurderMystery) ClientManager.getInstance().getModuleManager().get(MurderMystery.class);
                if (mmModule != null && mmModule.isState()) {
                    if (mmModule.getDetectedMurderer() != null && player == mmModule.getDetectedMurderer())
                        return "MM Murderer";
                    ItemStack heldItem = player.getHeldItem();
                    if (heldItem != null && heldItem.getItem() == Items.bow) return "MM Detective";
                    return "MM Innocent";
                } else return "MM Unknown Role";
            default:
                return "zzz_Unknown";
        }
    }

    private void updateAndSortPlayers() {
        if (mc.theWorld == null || mc.thePlayer == null) {
            playersToDisplay = Collections.emptyList();
            return;
        }

        final EntityPlayerSP localPlayer = mc.thePlayer;

        final List<EntityPlayer> filteredPlayers = mc.theWorld.playerEntities.stream().filter(player -> isOnTab(player) && !isNPC(player)).sorted((p1, p2) -> {
            boolean p1IsSelf = (p1 == localPlayer);
            boolean p2IsSelf = (p2 == localPlayer);

            if (p1IsSelf && !p2IsSelf) return -1;
            else if (!p1IsSelf && p2IsSelf) return 1;
            else if (p1IsSelf) return 0;
            else {
                int teamCompare = getPlayerTeamOrRoleString(p1).compareTo(getPlayerTeamOrRoleString(p2));
                if (teamCompare != 0) return teamCompare;
                return String.CASE_INSENSITIVE_ORDER.compare(p1.getName(), p2.getName());
            }
        }).collect(Collectors.toList());

        if (filteredPlayers.size() > 24) playersToDisplay = filteredPlayers.subList(0, 24);
        else playersToDisplay = filteredPlayers;
    }

    private boolean isOnTab(final EntityPlayer player) {
        if (player == null || mc.getNetHandler() == null || mc.getNetHandler().getPlayerInfoMap() == null) return false;

        final String targetName = player.getName();

        for (final NetworkPlayerInfo info : mc.getNetHandler().getPlayerInfoMap()) {
            if (info != null && info.getGameProfile() != null && targetName.equals(info.getGameProfile().getName()))
                return true;
        }
        return false;
    }

    private boolean isNPC(final EntityPlayer player) {
        return player != null && player.getDisplayName() != null && player.getDisplayName().getFormattedText().startsWith("\2478[NPC]");
    }

    private void drawContextMenu(int mouseX, int mouseY, FontRenderer font, float scale) {
        if (contextMenuPlayer == null) return;

        boolean isFriend = ClientManager.getInstance().getFriendManager().isFriend(contextMenuPlayer.getName());

        String[] menuItems = {
                isFriend ? "Remove Friend" : "Add Friend",
                "Copy Name",
                "View Stats",
                "Close"
        };

        float scaledMenuWidth = CONTEXT_MENU_WIDTH * scale;
        float scaledItemHeight = CONTEXT_MENU_ITEM_HEIGHT * scale;
        float menuHeight = menuItems.length * scaledItemHeight;

        RenderUtils.drawRect(
                contextMenuX,
                contextMenuY,
                contextMenuX + scaledMenuWidth,
                contextMenuY + menuHeight,
                RenderUtils.reAlpha(0xFF000000, 0.95f)
        );

        RenderUtils.drawRectOutline(
                contextMenuX,
                contextMenuY,
                contextMenuX + scaledMenuWidth,
                contextMenuY + menuHeight,
                1f,
                RenderUtils.reAlpha(0xFFFFFFFF, 0.3f)
        );

        for (int i = 0; i < menuItems.length; i++) {
            float itemY = contextMenuY + (i * scaledItemHeight);

            boolean hovered = MouseUtils.isHovered(mouseX, mouseY, contextMenuX, itemY,
                    contextMenuX + scaledMenuWidth, itemY + scaledItemHeight);

            if (hovered) {
                RenderUtils.drawGradientRectHorizontal(
                        contextMenuX + 1,
                        itemY,
                        contextMenuX + scaledMenuWidth - 1,
                        itemY + scaledItemHeight,
                        RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.2f),
                        RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.2f)
                );
            }

            if (i < menuItems.length - 1) {
                RenderUtils.drawRect(
                        contextMenuX + 5,
                        itemY + scaledItemHeight - 0.5f,
                        contextMenuX + scaledMenuWidth - 5,
                        itemY + scaledItemHeight,
                        RenderUtils.reAlpha(0xFFFFFFFF, 0.15f)
                );
            }

            int textColor = hovered ? 0xFFFFFFFF : RenderUtils.reAlpha(0xFFFFFFFF, 0.8f);
            float textX = contextMenuX + (scaledMenuWidth - font.getStringWidth(menuItems[i])) / 2f;
            float textY = itemY + (scaledItemHeight - font.getHeight()) / 2f;
            font.drawStringWithShadow(menuItems[i], textX, textY, textColor);
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        clickConsumed = false;
        if (contextMenuPlayer != null) {
            clickConsumed = true;
            float scaledMenuHeight = 4 * CONTEXT_MENU_ITEM_HEIGHT * scaleFactor;
            float scaledMenuWidth = CONTEXT_MENU_WIDTH * scaleFactor;

            if (MouseUtils.isHovered(mouseX, mouseY, contextMenuX, contextMenuY,
                    contextMenuX + scaledMenuWidth, contextMenuY + scaledMenuHeight)) {

                int clickedItem = (int)((mouseY - contextMenuY) / (CONTEXT_MENU_ITEM_HEIGHT * scaleFactor));
                handleContextMenuClick(clickedItem);
                contextMenuPlayer = null;
                return;
            } else {
                contextMenuPlayer = null;
                return;
            }
        }

        boolean isInChat = mc.currentScreen instanceof GuiChat;
        boolean isInEditMode = mc.currentScreen instanceof GuiEditElement;

        if (mouseButton == 0 && (isInEditMode || isInChat)) {
            if (mc.theWorld != null && !playersToDisplay.isEmpty()) {
                float handleSize = RESIZE_HANDLE_SIZE * scaleFactor;
                float handleX = getX() + getWidth() - handleSize;
                float handleY = getY() + getHeight() - handleSize;

                if (MouseUtils.isHovered(mouseX, mouseY, handleX, handleY, getX() + getWidth(), getY() + getHeight())) {
                    resizing = true;
                    setDragging(false);
                    resizeStartX = mouseX;
                    resizeStartY = mouseY;
                    resizeStartWidth = scaleFactor;
                    clickConsumed = true;
                    return;
                }
            }
        }

        if (!resizing) {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mc.theWorld == null || playersToDisplay.isEmpty()) {
            return;
        }

        int scaledHeaderFontSize = (int)(14 * scaleFactor);
        int scaledFontSize = (int)(12 * scaleFactor);
        FontRenderer font = Fonts.INTER_MEDIUM.get(scaledFontSize);

        int rows = Math.min(playersToDisplay.size(), 24);
        int cols = FIELDS.size();

        float scaledOuterPad = OUTER_PAD * scaleFactor;
        float scaledHeaderH = HEADER_H * scaleFactor;
        float scaledRowH = ROW_H * scaleFactor;
        float scaledIconS = ICON_S * scaleFactor;

        float[] colW = new float[cols];
        for (int c = 0; c < cols; c++) {
            GraphicBoxField<EntityPlayer> field = FIELDS.get(c);
            float max = font.getStringWidth(field.title) + 10f * scaleFactor;
            for (EntityPlayer p : playersToDisplay) {
                String value = safe(field.valueFunc.apply(p));
                float valueWidth = font.getStringWidth(value);
                if (c == 0) valueWidth += scaledIconS + 9f * scaleFactor;
                max = Math.max(max, valueWidth + 12f * scaleFactor);
            }
            colW[c] = max;
        }

        float w = scaledOuterPad;
        for (float cw : colW) w += cw;
        w += scaledOuterPad;

        float h = scaledOuterPad + scaledHeaderH + (rows * scaledRowH) + scaledOuterPad;

        float x = getX();
        float y = getY();
        float listY = y + scaledOuterPad + scaledHeaderH;

        if (MouseUtils.isHovered(mouseX, mouseY, x + scaledOuterPad, listY,
                x + w - scaledOuterPad, listY + (rows * scaledRowH))) {

            clickConsumed = true;

            int clickedRow = (int)((mouseY - listY) / scaledRowH);

            if (clickedRow >= 0 && clickedRow < playersToDisplay.size()) {
                EntityPlayer clickedPlayer = playersToDisplay.get(clickedRow);

                if (clickedPlayer != mc.thePlayer) {
                    if (mouseButton == 1 || (isInChat && mouseButton == 0)) {
                        contextMenuPlayer = clickedPlayer;
                        contextMenuX = mouseX;
                        contextMenuY = mouseY;

                        float scaledMenuWidth = CONTEXT_MENU_WIDTH * scaleFactor;
                        float scaledMenuHeight = (4 * CONTEXT_MENU_ITEM_HEIGHT) * scaleFactor;

                        if (contextMenuX + scaledMenuWidth > mc.displayWidth) {
                            contextMenuX = mc.displayWidth - scaledMenuWidth;
                        }
                        if (contextMenuY + scaledMenuHeight > mc.displayHeight) {
                            contextMenuY = mc.displayHeight - scaledMenuHeight;
                        }
                    }
                }
            }
        }
    }

    private void handleContextMenuClick(int itemIndex) {
        if (contextMenuPlayer == null) return;

        String playerName = contextMenuPlayer.getName();

        switch (itemIndex) {
            case 0: // Add/Remove Friend
                if (ClientManager.getInstance().getFriendManager().isFriend(playerName)) {
                    ClientManager.getInstance().getFriendManager().remove(playerName);
                    ClientManager.getInstance().getNotificationManager().addNotification(
                            "Removed Friend",
                            playerName + " is removed from friends",
                            3000
                    );
                } else {
                    ClientManager.getInstance().getFriendManager().addFriend(playerName);
                    ClientManager.getInstance().getNotificationManager().addNotification(
                            "Added Friend",
                            playerName + " is now a friend",
                            3000
                    );
                }
                break;

            case 1: // Copy Name
                try {
                    java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(playerName);
                    java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
                    ClientManager.getInstance().getNotificationManager().addNotification(
                            "Copied",
                            "Copied " + playerName + " to clipboard",
                            2000
                    );
                } catch (Exception e) {
                    ClientManager.getInstance().getNotificationManager().addNotification(
                            "Error",
                            "Failed to copy name",
                            2000
                    );
                }
                break;

            case 2: // View Stats
                ClientManager.getInstance().getNotificationManager().addNotification(
                        "Stats",
                        "Stats feature coming soon!",
                        2000
                );
                break;

            case 3: // Close
                break;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        resizing = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (keyCode == 1 && contextMenuPlayer != null) {
            contextMenuPlayer = null;
        }
        super.keyTyped(typedChar, keyCode);
    }
}