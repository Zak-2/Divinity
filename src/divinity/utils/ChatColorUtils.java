package divinity.utils;

import java.awt.*;
import java.util.Map;

public class ChatColorUtils {

    private static final String[] MINECRAFT_COLORS = {
            "§0", "§1", "§2", "§3", "§4", "§5", "§6", "§7",
            "§8", "§9", "§a", "§b", "§c", "§d", "§e", "§f"
    };

    private static final Color[] MINECRAFT_RGB_COLORS = {
            new Color(0, 0, 0),       // Black (§0)
            new Color(0, 0, 170),     // Dark Blue (§1)
            new Color(0, 170, 0),     // Dark Green (§2)
            new Color(0, 170, 170),   // Dark Aqua (§3)
            new Color(170, 0, 0),     // Dark Red (§4)
            new Color(170, 0, 170),   // Dark Purple (§5)
            new Color(255, 170, 0),   // Gold (§6)
            new Color(170, 170, 170), // Gray (§7)
            new Color(85, 85, 85),    // Dark Gray (§8)
            new Color(85, 85, 255),   // Blue (§9)
            new Color(85, 255, 85),   // Green (§a)
            new Color(85, 255, 255), // Aqua (§b)
            new Color(255, 85, 85),  // Red (§c)
            new Color(255, 85, 255), // Light Purple (§d)
            new Color(255, 255, 85), // Yellow (§e)
            new Color(255, 255, 255) // White (§f)
    };

    public static String toMinecraftColorCode(Color color) {
        double minDistance = Double.MAX_VALUE;
        String closestColorCode = "§f";

        for (int i = 0; i < MINECRAFT_RGB_COLORS.length; i++) {
            double distance = colorDistance(color, MINECRAFT_RGB_COLORS[i]);
            if (distance < minDistance) {
                minDistance = distance;
                closestColorCode = MINECRAFT_COLORS[i];
            }
        }

        return closestColorCode;
    }

    private static double colorDistance(Color c1, Color c2) {
        int redDiff = c1.getRed() - c2.getRed();
        int greenDiff = c1.getGreen() - c2.getGreen();
        int blueDiff = c1.getBlue() - c2.getBlue();
        return Math.sqrt(redDiff * redDiff + greenDiff * greenDiff + blueDiff * blueDiff);
    }

    public static String formatWithColors(String message, Map<String, Color> colorMap) {
        for (Map.Entry<String, Color> entry : colorMap.entrySet()) {
            String placeholder = entry.getKey();
            Color color = entry.getValue();
            String minecraftColorCode = toMinecraftColorCode(color);
            message = message.replace(placeholder, minecraftColorCode);
        }
        return message;
    }
}