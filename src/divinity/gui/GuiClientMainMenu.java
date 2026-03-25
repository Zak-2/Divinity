package divinity.gui;

import divinity.ClientManager;
import divinity.gui.alt.GuiAltManager;
import divinity.gui.cutscene.CutsceneGui;
import divinity.utils.RenderUtils;
import divinity.utils.cutscene.CutsceneManager;
import divinity.utils.font.Fonts;
import divinity.utils.shaders.ShaderMenu;
import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;

import java.awt.*;
import java.io.IOException;


public class GuiClientMainMenu extends GuiScreen {

    private static final long ANIMATION_DURATION_MS = 800L;
    private static final long BUTTON_STAGGER_DELAY_MS = 60L;
    private static final int PANEL_HORIZONTAL_PADDING = 15;
    private static final int PANEL_VERTICAL_PADDING = 12;
    private static final int TITLE_AREA_HEIGHT = 55;

    private long openTime;

    private float panelX1, panelY1, panelX2, panelY2;
    private float titleY, versionY, separatorLineY;

    @Override
    public void initGui() {
        this.openTime = System.currentTimeMillis();
        buttonList.clear();

        String[] displayNames = {"Select World", "Play Multiplayer", "Alt Manager", "Options", "Shutdown"};
        int mainButtonGap = 4;
        int mainButtonWidth = 120;
        int mainButtonHeight = 18;
        int totalButtonBlockHeight = (displayNames.length * mainButtonHeight) + ((displayNames.length - 1) * mainButtonGap);

        int startX = (this.width / 2) - (mainButtonWidth / 2);
        int startY = (this.height / 2) - (totalButtonBlockHeight / 2) + (TITLE_AREA_HEIGHT / 2) - 10; // Position buttons correctly

        for (int i = 0; i < displayNames.length; i++) {
            buttonList.add(new GuiCustomButton(i, startX, startY, mainButtonWidth, mainButtonHeight, displayNames[i], i * BUTTON_STAGGER_DELAY_MS));
            startY += mainButtonHeight + mainButtonGap;
        }

        GuiButton firstButton = buttonList.get(0);
        GuiButton lastButton = buttonList.get(displayNames.length - 1);
        panelX1 = firstButton.xPosition - PANEL_HORIZONTAL_PADDING;
        panelX2 = firstButton.xPosition + firstButton.width + PANEL_HORIZONTAL_PADDING;
        panelY1 = firstButton.yPosition - PANEL_VERTICAL_PADDING - TITLE_AREA_HEIGHT;
        panelY2 = lastButton.yPosition + lastButton.height + PANEL_VERTICAL_PADDING;

        titleY = panelY1 + 22;
        versionY = titleY + 28;
        separatorLineY = firstButton.yPosition - (PANEL_VERTICAL_PADDING / 2f);

        int bottomY = this.height - 28;
        int shaderWidth = Fonts.INTER_MEDIUM.get(18).getStringWidth("Switch Shader") + 20;
        int cutsceneWidth = Fonts.INTER_MEDIUM.get(18).getStringWidth("Replay Cutscene") + 20;
        int totalBottomWidth = shaderWidth + 6 + cutsceneWidth;
        int bottomStartX = (this.width - totalBottomWidth) / 2;

        buttonList.add(new GuiCustomButton(10, bottomStartX, bottomY, shaderWidth, 20, "Switch Shader", displayNames.length * BUTTON_STAGGER_DELAY_MS));
        buttonList.add(new GuiCustomButton(11, bottomStartX + shaderWidth + 6, bottomY, cutsceneWidth, 20, "Replay Cutscene", (displayNames.length + 1) * BUTTON_STAGGER_DELAY_MS));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        long elapsedTime = System.currentTimeMillis() - openTime;
        float easedProgress = easeOutQuint(Math.min(1.0f, (float) elapsedTime / ANIMATION_DURATION_MS));

        if (ClientManager.getInstance().shaderMenu == null) ClientManager.getInstance().shaderMenu = new ShaderMenu();
        ClientManager.getInstance().shaderMenu.renderShader(this.width, this.height);

        GlStateManager.pushMatrix();
        GlStateManager.translate(0, 40.0f * (1.0f - easedProgress), 0);
        drawCentralPanel(easedProgress);
        GlStateManager.popMatrix();

        drawCornerText(easedProgress);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCentralPanel(float alpha) {
        int panelAlpha = (int) (150 * alpha);
        int elementAlpha = (int) (255 * alpha);

        RenderUtils.drawRoundRect(panelX1, panelY1, panelX2, panelY2, 10, new Color(21, 20, 27, panelAlpha).getRGB());
        RenderUtils.drawRoundRect(panelX1 + 0.5f, panelY1 + 0.5f, panelX2 - 0.5f, panelY2 - 0.5f, 10, new Color(9, 9, 14, panelAlpha).getRGB());

        RenderUtils.drawHorizontalLine(panelX1 + 10, panelX2 - 10, separatorLineY, 1, new Color(21, 20, 27, elementAlpha).getRGB());

        Fonts.INTER_MEDIUM.get(40).drawCenteredStringWithShadow(ClientManager.getInstance().getName(), (float) width / 2, titleY, applyAlpha(-1, alpha));
        Fonts.INTER_MEDIUM.get(18).drawCenteredStringWithShadow("Version: " + ClientManager.getInstance().getVersion(), (float) width / 2, versionY, applyAlpha(Color.GRAY.getRGB(), alpha));
    }

    private void drawCornerText(float alpha) {
        float xOffset = 30.0f * (1.0f - alpha);

        String timeText = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        int timeWidth = Fonts.INTER_MEDIUM.get(18).getStringWidth(timeText);
        Fonts.INTER_MEDIUM.get(18).drawStringWithShadow(timeText, width - timeWidth - 4 + xOffset, 8, applyAlpha(-1, alpha));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(new GuiSelectWorld(this));
                break;
            case 1:
                mc.displayGuiScreen(new GuiMultiplayer(this));
                break;
            case 2:
                mc.displayGuiScreen(new GuiAltManager(this));
                break;
            case 3:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case 4:
                mc.shutdown();
                break;
            case 10:
                if (ClientManager.getInstance().shaderMenu != null)
                    ClientManager.getInstance().shaderMenu.switchShader();
                break;
            case 11:
                CutsceneGui cutscene = CutsceneManager.builder().audio("cutscene_audio.wav").frames("car", 834).frameRate(25).build();
                CutsceneManager.playCutscene(cutscene);
                break;
        }
    }

    private float easeOutQuint(float x) {
        return (float) (1 - Math.pow(1 - x, 5));
    }

    private int applyAlpha(int color, float alpha) {
        Color c = new Color(color, true);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * alpha)).getRGB();
    }
}