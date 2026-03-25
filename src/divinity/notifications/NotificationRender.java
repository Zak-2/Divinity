package divinity.notifications;

import divinity.ClientManager;
import divinity.utils.ShaderUtils;
import divinity.utils.font.FontRenderer;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;

import java.awt.*;
import java.util.ArrayList;

public class NotificationRender {
    private static final float NOTIFICATION_HEIGHT = 36;
    private static final float HORIZONTAL_PADDING = 6;
    private static final float ICON_AREA_SIZE = 24;
    private static final float LINE_SPACING = 2;
    private static final float ICON_TEXT_GAP = 8;
    private static final float CORNER_RADIUS = 10f;
    private static final float TOP_MARGIN = 10;
    private static final float NOTIFICATION_GAP = 5;
    private static final Color BACKGROUND_COLOR = new Color(0x1a1a1e);
    private static final Color ICON_BACKGROUND_COLOR = new Color(0x1f1f24);
    private static final Color ICON_COLOR_SUCCESS = new Color(152, 223, 175, 255);
    private static final Color TITLE_TEXT_COLOR = new Color(225, 226, 230, 255);
    private static final Color DESCRIPTION_TEXT_COLOR = new Color(160, 162, 170, 255);
    public final Minecraft mc = Minecraft.getMinecraft();
    private FontRenderer titleFont;
    private FontRenderer descriptionFont;
    private FontRenderer iconFont;

    public void onRender() {
        if (titleFont == null) titleFont = Fonts.INTER_MEDIUM.get(16);
        if (descriptionFont == null) descriptionFont = Fonts.INTER_MEDIUM.get(14);
        if (iconFont == null) iconFont = Fonts.INTER_MEDIUM.get(22);

        ArrayList<Notification> notifs = ClientManager.getInstance().getNotificationManager().getNotifications();
        if (notifs == null || notifs.isEmpty()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        final int screenWidth = sr.getScaledWidth();

        for (int i = notifs.size() - 1; i >= 0; i--) {
            Notification n = notifs.get(i);
            if (n.isFinished()) {
                ClientManager.getInstance().getNotificationManager().delete(i);
                continue;
            }

            float duration = (float) n.getLifeTime() / (float) n.getDuration();
            float fadeIn = 0.1f;
            float fadeOut = 0.94f;
            float animationProgress = (duration < fadeIn) ? (duration / fadeIn) : ((duration > fadeOut) ? 1.0f - ((duration - fadeOut) / (1.0f - fadeOut)) : 1.0f);

            float titleWidth = titleFont.getStringWidth(n.getTitle());
            float descWidth = descriptionFont.getStringWidth(n.getDescription());
            float textWidth = Math.max(titleWidth, descWidth);
            float notificationWidth = HORIZONTAL_PADDING + ICON_AREA_SIZE + ICON_TEXT_GAP + textWidth + HORIZONTAL_PADDING;


            float xPos = (screenWidth / 2f) - (notificationWidth / 2f);

            float targetY = TOP_MARGIN + (i * (NOTIFICATION_HEIGHT + NOTIFICATION_GAP));

            float startY = -NOTIFICATION_HEIGHT;


            float currentY = startY + (targetY - startY) * animationProgress;

            ShaderUtils.drawRoundRect(xPos, currentY, xPos + notificationWidth, currentY + NOTIFICATION_HEIGHT, CORNER_RADIUS, BACKGROUND_COLOR.getRGB());

            float iconAreaX = xPos + HORIZONTAL_PADDING;
            float iconAreaY = currentY + (NOTIFICATION_HEIGHT / 2f) - (ICON_AREA_SIZE / 2f);
            ShaderUtils.drawRoundRect(iconAreaX, iconAreaY, iconAreaX + ICON_AREA_SIZE, iconAreaY + ICON_AREA_SIZE, 8f, ICON_BACKGROUND_COLOR.getRGB());

            String iconChar = "✓";
            int iconColor = ICON_COLOR_SUCCESS.getRGB();
            iconFont.drawCenteredString(iconChar, iconAreaX + ICON_AREA_SIZE / 2f, iconAreaY + ICON_AREA_SIZE / 2f - 4, iconColor);

            float textBlockHeight = titleFont.getHeight() + LINE_SPACING + descriptionFont.getHeight();
            float textX = iconAreaX + ICON_AREA_SIZE + ICON_TEXT_GAP;
            float textBlockY = currentY + (NOTIFICATION_HEIGHT / 2f) - (textBlockHeight / 2f);

            titleFont.drawString(n.getTitle(), textX, textBlockY, TITLE_TEXT_COLOR.getRGB());
            descriptionFont.drawString(n.getDescription(), textX, textBlockY + titleFont.getHeight() + 4, DESCRIPTION_TEXT_COLOR.getRGB());
        }
    }
}