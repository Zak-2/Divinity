package divinity.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Mouse;

public final class MouseUtils {
    public static boolean isHovered(float x, float y, float width, float height) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - (Mouse.getY() * sr.getScaledHeight() / mc.displayHeight);
        return mouseX >= x && mouseX <= width && mouseY >= y && mouseY <= height;
    }

    public static boolean isHovered(int mouseX, int mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= width && mouseY >= y && mouseY <= height;
    }
}
