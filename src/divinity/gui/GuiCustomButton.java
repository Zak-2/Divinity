package divinity.gui;

import divinity.utils.AnimationUtils;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;


public class GuiCustomButton extends GuiButton {

    private static final int CORNER_RADIUS = 10;
    private static final int FONT_SIZE = 16;
    private static final float BORDER_THICKNESS = 0.5f;
    private static final Color COLOR_BACKGROUND = new Color(16, 16, 21, 140);
    private static final Color COLOR_BORDER = new Color(11, 10, 15, 140);
    private static final Color COLOR_TEXT = Color.WHITE;
    private static final Color COLOR_GRADIENT_ACCENT = new Color(121, 105, 229);
    private static final Color COLOR_GRADIENT_BASE = new Color(9, 9, 14);

    private static final float HOVER_ANIMATION_SPEED_MS = 800f;
    private static final long ENTRANCE_ANIMATION_DURATION_MS = 500L;
    private static final float ENTRANCE_Y_OFFSET = 20.0f;
    private final long creationTime;
    private final long animationDelay;
    private float hoverAnimation = 0.0f;

    public GuiCustomButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText, long animationDelay) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        this.creationTime = System.currentTimeMillis();
        this.animationDelay = animationDelay;
    }

    private static float easeOutQuint(float x) {
        return (float) (1 - Math.pow(1 - x, 5));
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (!this.visible) return;

        long elapsedTime = System.currentTimeMillis() - creationTime - animationDelay;
        float rawProgress = Math.max(0, Math.min(1, (float) elapsedTime / ENTRANCE_ANIMATION_DURATION_MS));
        float easedProgress = easeOutQuint(rawProgress);
        float animatedYOffset = (1.0f - easedProgress) * ENTRANCE_Y_OFFSET;
        float animatedAlpha = easedProgress;

        boolean isHovered = mouseX >= this.xPosition && mouseY >= (this.yPosition + animatedYOffset) && mouseX < this.xPosition + this.width && mouseY < (this.yPosition + animatedYOffset) + this.height;
        this.hoverAnimation = AnimationUtils.getAnimationState(this.hoverAnimation, isHovered ? 255.0f : 0.0f, HOVER_ANIMATION_SPEED_MS);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, animatedYOffset, 0);
        GlStateManager.color(1, 1, 1, animatedAlpha); // This alpha affects textures, not necessarily raw draws

        int backgroundRGB = applyAlpha(COLOR_BACKGROUND.getRGB(), animatedAlpha);
        int borderRGB = applyAlpha(COLOR_BORDER.getRGB(), animatedAlpha);
        RenderUtils.drawRoundRect(xPosition, yPosition, xPosition + width, yPosition + height, CORNER_RADIUS, backgroundRGB);
        RenderUtils.drawRoundRect(xPosition + BORDER_THICKNESS, yPosition + BORDER_THICKNESS, xPosition + width - BORDER_THICKNESS, yPosition + height - BORDER_THICKNESS, CORNER_RADIUS, borderRGB);

        if (hoverAnimation > 1) {
            drawHoverEffect((int) (hoverAnimation * animatedAlpha));
        }

        int textRGB = applyAlpha(COLOR_TEXT.getRGB(), animatedAlpha);
        Fonts.INTER_MEDIUM.get(FONT_SIZE).drawCenteredString(this.displayString, this.xPosition + this.width / 2f, this.yPosition + (this.height - Fonts.INTER_MEDIUM.get(FONT_SIZE).getHeight()) / 2f + 1f, textRGB);

        GlStateManager.popMatrix();
    }

    private void drawHoverEffect(int alpha) {
        final float centerX = this.xPosition + this.width / 2.0f;
        final float centerY = this.yPosition + this.height / 2.0f;
        final float halfTextWidth = Fonts.INTER_MEDIUM.get(FONT_SIZE).getStringWidth(this.displayString) / 2.0f;
        final int linePadding = 3;

        int accentColor = new Color(COLOR_GRADIENT_ACCENT.getRed(), COLOR_GRADIENT_ACCENT.getGreen(), COLOR_GRADIENT_ACCENT.getBlue(), alpha).getRGB();
        int baseColor = new Color(COLOR_GRADIENT_BASE.getRed(), COLOR_GRADIENT_BASE.getGreen(), COLOR_GRADIENT_BASE.getBlue(), alpha).getRGB();

        RenderUtils.drawGradientRectHorizontal(this.xPosition, centerY, centerX - halfTextWidth - linePadding, centerY + 0.5f, baseColor, accentColor);
        RenderUtils.drawGradientRectHorizontal(centerX + halfTextWidth + linePadding, centerY, this.xPosition + this.width, centerY + 0.5f, accentColor, baseColor);
    }


    private int applyAlpha(int color, float alpha) {
        Color oldColor = new Color(color, true);
        return new Color(oldColor.getRed(), oldColor.getGreen(), oldColor.getBlue(), (int) (oldColor.getAlpha() * alpha)).getRGB();
    }
}