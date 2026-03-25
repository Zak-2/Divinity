package divinity.utils.math;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import org.lwjgl.util.vector.Vector2f;

import java.math.BigDecimal;
import java.math.MathContext;
import java.security.SecureRandom;
import java.text.DecimalFormat;
import java.util.Random;

public final class MathUtils {
    public static final Random rng = new Random();
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.######");

    private MathUtils() {
    }

    public static Vector2f getRealAngle(EntityPlayerSP player) {
        return new Vector2f(player.lastReportedYaw, player.lastReportedPitch);
    }

    public static Vec3 getLookVector(float yaw, float pitch) {
        float pi180 = (float) (Math.PI / 180.0);
        float f = MathHelper.cos(-yaw * pi180 - (float) Math.PI);
        float f1 = MathHelper.sin(-yaw * pi180 - (float) Math.PI);
        float f2 = -MathHelper.cos(-pitch * pi180);
        float f3 = MathHelper.sin(-pitch * pi180);
        return new Vec3((f1 * f2), f3, (f * f2)).normalize();
    }

    public static Vector2f getAnglesTo(Vector2f srcAngle, Vec3 src, Vec3 dst) {
        Vec3 offset = dst.subtract(src);
        double distXZ = offset.horizontalLength();
        double _180pi = 180.0 / Math.PI;

        float yaw = MathHelper.wrapAngleTo180_float((float) (MathHelper.atan2(offset.zCoord, offset.xCoord) * _180pi) - 90.0F);
        float wrappedSrcYaw = MathHelper.wrapAngleTo180_float(srcAngle.x);
        float wrappedDiff = MathHelper.wrapAngleTo180_float(yaw - wrappedSrcYaw);
        yaw = srcAngle.x + getGCDFixed(wrappedDiff);

        float pitch = MathHelper.wrapAngleTo180_float(((float) (-(MathHelper.atan2(offset.yCoord, distXZ) * _180pi))));
        pitch = srcAngle.y + getGCDFixed(pitch - srcAngle.y);
        pitch = MathHelper.clamp_float(pitch, -90.0f, 90.0f);

        return new Vector2f(yaw, pitch);
    }

    public static float getGCDFixed(float angle) {
        float a = Minecraft.getMinecraft().gameSettings.mouseSensitivity * 0.6F + 0.2F;
        float b = a * a * a * 8.0F;
        float c = (float) ((double) b * 0.15D);
        return (float) roundToMultiple(angle, c);
    }

    public static double roundToMultiple(double value, double inc) {
        return Math.round(value / inc) * inc;
    }

    public static int getRandomInRange(int min, int max) {
        SecureRandom random = new SecureRandom();
        int range = max - min;
        int scaled = random.nextInt() * range;
        int shifted = scaled + min;
        if (shifted > max) {
            shifted = max;
        }
        return shifted;
    }

    public static <T extends Number> T clamp(T value, T minimum, T maximum) {
        if (value instanceof Integer) {
            if (value.intValue() > maximum.intValue()) {
                value = maximum;
            } else if (value.intValue() < minimum.intValue()) {
                value = minimum;
            }
        } else if (value instanceof Float) {
            if (value.floatValue() > maximum.floatValue()) {
                value = maximum;
            } else if (value.floatValue() < minimum.floatValue()) {
                value = minimum;
            }
        } else if (value instanceof Double) {
            if (value.doubleValue() > maximum.doubleValue()) {
                value = maximum;
            } else if (value.doubleValue() < minimum.doubleValue()) {
                value = minimum;
            }
        } else if (value instanceof Long) {
            if (value.longValue() > maximum.longValue()) {
                value = maximum;
            } else if (value.longValue() < minimum.longValue()) {
                value = minimum;
            }
        } else if (value instanceof Short) {
            if (value.shortValue() > maximum.shortValue()) {
                value = maximum;
            } else if (value.shortValue() < minimum.shortValue()) {
                value = minimum;
            }
        } else if (value instanceof Byte) {
            if (value.byteValue() > maximum.byteValue()) {
                value = maximum;
            } else if (value.byteValue() < minimum.byteValue()) {
                value = minimum;
            }
        }

        return value;
    }

    public static double roundToDecimalPlace(double value, double inc) {
        final double halfOfInc = inc / 2.0D;
        final double floored = StrictMath.floor(value / inc) * inc;
        if (value >= floored + halfOfInc)
            return new BigDecimal(StrictMath.ceil(value / inc) * inc, MathContext.DECIMAL64).
                    stripTrailingZeros()
                    .doubleValue();
        else
            return new BigDecimal(floored, MathContext.DECIMAL64)
                    .stripTrailingZeros()
                    .doubleValue();
    }
}
