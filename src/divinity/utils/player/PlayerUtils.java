package divinity.utils.player;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.EnumChatFormatting;

public class PlayerUtils {
    public static String getTeamColorName(EntityLivingBase entity) {
        if (entity == null) return "None";

        Team entityTeam = entity.getTeam();
        if (entityTeam != null) {
            String formattedName = entityTeam.formatString(entityTeam.getRegisteredName());
            EnumChatFormatting color = extractColor(formattedName);

            if (color != null) {
                return getColorName(color);
            }
        }

        return "None";
    }

    public static EnumChatFormatting getPlayerTeamColor(EntityLivingBase entity) {
        if (entity == null) return null;
        Team entityTeam = entity.getTeam();
        if (entityTeam != null) {
            String formattedName = entityTeam.formatString(entityTeam.getRegisteredName());
            return extractColor(formattedName);
        }

        return null;
    }

    private static EnumChatFormatting extractColor(String formattedName) {
        if (formattedName == null || formattedName.isEmpty()) return null;
        for (EnumChatFormatting color : EnumChatFormatting.values()) {
            if (formattedName.startsWith(color.toString())) {
                return color;
            }
        }
        return null;
    }

    private static String getColorName(EnumChatFormatting color) {
        if (color == null) return "None";

        switch (color) {
            case BLACK:
                return "Black";
            case DARK_BLUE:
                return "Dark Blue";
            case DARK_GREEN:
                return "Dark Green";
            case DARK_AQUA:
                return "Dark Aqua";
            case DARK_RED:
                return "Dark Red";
            case DARK_PURPLE:
                return "Dark Purple";
            case GOLD:
                return "Gold";
            case GRAY:
                return "Gray";
            case DARK_GRAY:
                return "Dark Gray";
            case BLUE:
                return "Blue";
            case GREEN:
                return "Green";
            case AQUA:
                return "Aqua";
            case RED:
                return "Red";
            case LIGHT_PURPLE:
                return "Light Purple";
            case YELLOW:
                return "Yellow";
            case WHITE:
                return "White";
            default:
                return "Unknown";
        }
    }

    public static double healthPercentage(EntityLivingBase entity) {
        return entity.getHealth() / entity.getMaxHealth();
    }

    public static double armorPercentage(EntityLivingBase entity) {
        return entity.getTotalArmorValue() / 20D;
    }

}
