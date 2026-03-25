package divinity.utils;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;

public class ColorUtils {
    public static int getColorRGB(EnumChatFormatting color) {
        if (color == null) return 0xFFFFFF;

        switch (color) {
            case BLACK:
                return 0x000000;
            case DARK_BLUE:
                return 0x0000AA;
            case DARK_GREEN:
                return 0x00AA00;
            case DARK_AQUA:
                return 0x00AAAA;
            case DARK_RED:
                return 0xAA0000;
            case DARK_PURPLE:
                return 0xAA00AA;
            case GOLD:
                return 0xFFAA00;
            case GRAY:
                return 0xAAAAAA;
            case DARK_GRAY:
                return 0x555555;
            case BLUE:
                return 0x5555FF;
            case GREEN:
                return 0x55FF55;
            case AQUA:
                return 0x55FFFF;
            case RED:
                return 0xFF5555;
            case LIGHT_PURPLE:
                return 0xFF55FF;
            case YELLOW:
                return 0xFFFF55;
            default:
                return 0xFFFFFF;
        }
    }

    public static Color gradientWave(Color color1, Color color2, double offset) {
        if (offset > 1.0D) {
            double left = offset % 1.0D;
            int off = (int) offset;
            offset = (off % 2 == 0) ? left : (1.0D - left);
        }
        double inverse_percent = 1.0D - offset;
        int redPart = (int) (color1.getRed() * inverse_percent + color2.getRed() * offset);
        int greenPart = (int) (color1.getGreen() * inverse_percent + color2.getGreen() * offset);
        int bluePart = (int) (color1.getBlue() * inverse_percent + color2.getBlue() * offset);
        return new Color(redPart, greenPart, bluePart);
    }

    public static Color getHealthColor(EntityLivingBase entityLivingBase) {
        float health = entityLivingBase.getHealth();
        float[] fractions = new float[]{0.0F, 0.15f, .55F, 0.7f, .9f};
        Color[] colors = new Color[]{new Color(133, 0, 0), Color.RED.darker(), Color.ORANGE.darker(), Color.YELLOW.darker(), Color.GREEN.darker()};
        float progress = health / entityLivingBase.getMaxHealth();
        return health >= 0.0f ? blendColors(fractions, colors, progress).brighter() : colors[0];
    }

    private static Color blendColors(float[] fractions, Color[] colors, float progress) {
        if (fractions.length == colors.length) {
            int[] indices = getFractionIndices(fractions, progress);
            float[] range = {fractions[indices[0]], fractions[indices[1]]};
            Color[] colorRange = {colors[indices[0]], colors[indices[1]]};
            float max = range[1] - range[0];
            float value = progress - range[0];
            float weight = value / max;
            Color color = blend(colorRange[0], colorRange[1], (1.0F - weight));
            return color;
        }
        throw new IllegalArgumentException("Fractions and colours must have equal number of elements");
    }

    private static int[] getFractionIndices(float[] fractions, float progress) {
        int[] range = new int[2];
        int startPoint;
        for (startPoint = 0; startPoint < fractions.length && fractions[startPoint] <= progress; startPoint++) ;
        if (startPoint >= fractions.length)
            startPoint = fractions.length - 1;
        range[0] = startPoint - 1;
        range[1] = startPoint;
        return range;
    }

    public static Color blend(Color color1, Color color2, double ratio) {
        float r = (float) ratio;
        float ir = 1.0F - r;
        float[] rgb1 = color1.getColorComponents(new float[3]);
        float[] rgb2 = color2.getColorComponents(new float[3]);
        float red = rgb1[0] * r + rgb2[0] * ir;
        float green = rgb1[1] * r + rgb2[1] * ir;
        float blue = rgb1[2] * r + rgb2[2] * ir;
        if (red < 0.0F) {
            red = 0.0F;
        } else if (red > 255.0F) {
            red = 255.0F;
        }
        if (green < 0.0F) {
            green = 0.0F;
        } else if (green > 255.0F) {
            green = 255.0F;
        }
        if (blue < 0.0F) {
            blue = 0.0F;
        } else if (blue > 255.0F) {
            blue = 255.0F;
        }
        Color color3 = null;
        try {
            color3 = new Color(red, green, blue);
        } catch (IllegalArgumentException illegalArgumentException) {
        }
        return color3;
    }

    public enum Colors {
        BLACK(-16711423),
        BLUE(-12028161),
        DARKBLUE(-12621684),
        GREEN(-9830551),
        DARKGREEN(-9320847),
        WHITE(-65794),
        AQUA(-7820064),
        DARKAQUA(-12621684),
        GREY(-9868951),
        DARKGREY(-14342875),
        RED(-65536),
        DARKRED(-8388608),
        ORANGE(-29696),
        DARKORANGE(-2263808),
        YELLOW(-256),
        DARKYELLOW(-2702025),
        MAGENTA(-18751),
        DARKMAGENTA(-2252579);

        public int c;

        Colors(int co) {
            this.c = co;
        }
    }
}
