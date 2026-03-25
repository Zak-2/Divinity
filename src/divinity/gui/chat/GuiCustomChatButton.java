package divinity.gui.chat;

import divinity.ClientManager;
import divinity.utils.AnimationUtils;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.gui.GuiButton;

import java.awt.*;

public class GuiCustomChatButton extends GuiButton {

    private float hoverAnimation = 1;

    public GuiCustomChatButton(int buttonId, int x, int y, String buttonText) {
        super(buttonId, x, y, buttonText);
        visible = true;
    }

    public GuiCustomChatButton(int buttonId, int x, int y, int width, String buttonText) {
        super(buttonId, x, y, width, 20, buttonText);
        visible = true;
    }

    public GuiCustomChatButton(int buttonId, int x, int y, int widthIn, int heightIn, String buttonText) {
        super(buttonId, x, y, widthIn, heightIn, buttonText);
        visible = true;
    }

    @Override
    public void drawButton(Minecraft mc, int mouseX, int mouseY) {
        if (visible) {
            boolean hover = MouseUtils.isHovered(xPosition, yPosition, xPosition + width, yPosition + height);
            hoverAnimation = AnimationUtils.getAnimationState(hoverAnimation, hover ? 3 : 2, 10f);

            RenderUtils.drawRoundedRect(xPosition, yPosition, width, height, 2, new Color(20, 20, 20, 150).getRGB());

            Color highlightColor = ClientManager.getInstance().getMainColor().darker();
            RenderUtils.drawRect(xPosition + width / 2f - Fonts.INTER_MEDIUM.get(12).getStringWidth(displayString) / 2f,
                    yPosition + height - hoverAnimation + 1,
                    xPosition + width / 2f + Fonts.INTER_MEDIUM.get(12).getStringWidth(displayString) / 2f,
                    yPosition + height - 1,
                    highlightColor.getRGB());

            Fonts.INTER_MEDIUM.get(12).drawCenteredStringGradient(displayString,
                    xPosition + width / 2f,
                    yPosition + height / 2f - Fonts.INTER_MEDIUM.get(12).getHeight() / 2f + 1f,
                    ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());
        }
    }

    @Override
    public void playPressSound(SoundHandler soundHandlerIn) {

    }
}