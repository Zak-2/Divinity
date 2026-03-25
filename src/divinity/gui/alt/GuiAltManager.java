package divinity.gui.alt;

import divinity.ClientManager;
import divinity.gui.alt.util.AltAuthResult;
import divinity.gui.alt.util.AltAuthStatus;
import divinity.gui.alt.util.SavedAltData;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import divinity.utils.shaders.ShaderMenu;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static divinity.gui.alt.util.MicrosoftAuthenticator.Logger;


public class GuiAltManager extends GuiScreen {
    private static final AtomicBoolean isPickingCookieFile = new AtomicBoolean(false);
    private static final long ANIMATION_DURATION_MS = 800L;
    private static final int PANEL_VERTICAL_PADDING = 12;
    private static final int TITLE_AREA_HEIGHT = 55;
    private static final int PANEL_RADIUS = 10;
    public static GuiAltList altListGui;
    private final GuiScreen parentScreen;
    private final int panelOuterColor = new Color(21, 20, 27, 150).getRGB();
    private final int textColor = new Color(220, 220, 220).getRGB();
    private final int buttonColor = new Color(40, 40, 50).getRGB();
    private final int buttonHoverColor = new Color(60, 60, 75).getRGB();

    private long lastEntryClickTime = 0L;
    private int lastEntryClickIndex = -1;
    private static final long DOUBLE_CLICK_THRESHOLD_MS = 300L;

    private long openTime;
    private boolean initialized, deletingAlt;
    private int hoveredButton = -1;
    private float hoverOpacity = 0;
    private long lastHoverTime = 0;

    private float panelX1, panelY1, panelX2, panelY2;
    private float titleY, separatorLineY;

    public GuiAltManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    public static void addAlt(SavedAltData alt) {
        if (altListGui != null) {
            altListGui.addAlt(alt);
        }
    }

    public static void removeAlt(int idx) {
        if (altListGui != null) {
            altListGui.entries.remove(idx);
        }
    }

    @Override
    public void initGui() {
        if (!initialized) {
            initialized = true;
            altListGui = new GuiAltList(this.mc, this.width, this.height, 32, this.height - 64, 36);
            altListGui.setAlts(ClientManager.getInstance().getAltManager().savedAlts);
        } else {
            altListGui.setDimensions(this.width, this.height, 32, this.height - 64);
            altListGui.setAlts(ClientManager.getInstance().getAltManager().savedAlts);
        }

        this.openTime = System.currentTimeMillis();
        this.deletingAlt = false;
        this.buttonList.clear();

    /* =======================
       Top row (3 buttons)
       ======================= */
        int topButtonWidth = 100;
        int topButtonHeight = 20;
        int topGap = 10;
        int topCount = 3;

        int topRowWidth = (topButtonWidth * topCount) + (topGap * (topCount - 1));
        int topStartX = (this.width - topRowWidth) / 2;
        int topY = this.height - 52;

        this.buttonList.add(new GuiButton(1, topStartX, topY, topButtonWidth, topButtonHeight, "Login"));
        this.buttonList.add(new GuiButton(4, topStartX + (topButtonWidth + topGap), topY, topButtonWidth, topButtonHeight, "External login"));
        this.buttonList.add(new GuiButton(3, topStartX + 2 * (topButtonWidth + topGap), topY, topButtonWidth, topButtonHeight, "Cookie (clipboard)"));

    /* =========================
       Bottom row (4 buttons)
       ========================= */
        int bottomButtonWidth = 75;
        int bottomButtonHeight = 20;
        int bottomGap = 10;
        int bottomCount = 4;

        int bottomRowWidth = (bottomButtonWidth * bottomCount) + (bottomGap * (bottomCount - 1));
        int bottomStartX = (this.width - bottomRowWidth) / 2;
        int bottomY = this.height - 28;

        this.buttonList.add(new GuiButton(7, bottomStartX, bottomY, bottomButtonWidth, bottomButtonHeight, "Delete"));
        this.buttonList.add(new GuiButton(2, bottomStartX + (bottomButtonWidth + bottomGap), bottomY, bottomButtonWidth, bottomButtonHeight, "Email pass"));
        this.buttonList.add(new GuiButton(8, bottomStartX + 2 * (bottomButtonWidth + bottomGap), bottomY, bottomButtonWidth, bottomButtonHeight, "Cookie (file)"));
        this.buttonList.add(new GuiButton(0, bottomStartX + 3 * (bottomButtonWidth + bottomGap), bottomY, bottomButtonWidth, bottomButtonHeight, "Back"));

    /* =======================
       Panel stuff (unchanged)
       ======================= */
        float panelWidth = this.width * 0.75f;
        float panelHeight = this.height * 0.75f;
        panelX1 = (this.width / 2f) - (panelWidth / 2f);
        panelX2 = (this.width / 2f) + (panelWidth / 2f);
        panelY1 = (this.height / 2f) - (panelHeight / 2f);
        panelY2 = (this.height / 2f) + (panelHeight / 2f);

        titleY = panelY1 + 22;
        separatorLineY = panelY1 + TITLE_AREA_HEIGHT - (PANEL_VERTICAL_PADDING / 2f);

        super.initGui();
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
        drawAltList(mouseX, mouseY, partialTicks);
        drawButtons(mouseX, mouseY, easedProgress);
        drawButtonTooltips(mouseX, mouseY);

        GlStateManager.popMatrix();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawCentralPanel(float alpha) {
        RenderUtils.drawRoundRect(panelX1, panelY1, panelX2, panelY2, PANEL_RADIUS, applyAlpha(panelOuterColor, alpha));

        RenderUtils.drawHorizontalLineGradient(panelX1, panelX2 - 2, titleY + Fonts.INTER_MEDIUM.get(40).getHeight() + 5, 1.0F, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());

        Fonts.INTER_MEDIUM.get(40).drawCenteredStringWithShadow("Alt Manager", (float) width / 2, titleY, applyAlpha(-1, alpha));

        String sessionInfo = "Logged in as: " + mc.getSession().getUsername();
        Fonts.INTER_MEDIUM.get(18).drawString(sessionInfo, panelX1 + 10, separatorLineY + 15, applyAlpha(textColor, alpha));
    }

    private void drawAltList(int mouseX, int mouseY, float partialTicks) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.prepareScissorbox((int) panelX1, (int) (separatorLineY + 10), (int) panelX2, (int) (panelY2 - PANEL_VERTICAL_PADDING));
        GlStateManager.pushMatrix();
        altListGui.setDimensions(this.width, this.height, (int) (separatorLineY + 8), (int) (panelY2 - PANEL_VERTICAL_PADDING));
        altListGui.drawScreen(mouseX, mouseY, partialTicks);
        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private void drawButtons(int mouseX, int mouseY, float alpha) {
        for (GuiButton button : this.buttonList) {
            boolean isHovered = mouseX >= button.xPosition && mouseX <= button.xPosition + button.width && mouseY >= button.yPosition && mouseY <= button.yPosition + button.height;

            int currentColor = isHovered ? buttonHoverColor : buttonColor;

            if (button.id == 7) currentColor = new Color(170, 50, 50).getRGB();

            RenderUtils.drawRect(button.xPosition, button.yPosition, button.xPosition + button.width, button.yPosition + button.height, applyAlpha(currentColor, alpha));

            if (button.id != 7)
                RenderUtils.drawHorizontalLineGradient(button.xPosition, button.xPosition + button.width - 1, button.yPosition, 1.0F, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());

            if (isHovered) {
                RenderUtils.drawGradientRect(button.xPosition, button.yPosition, button.xPosition + button.width, button.yPosition + button.height, true, applyAlpha(button.id == 7 ? buttonHoverColor : ClientManager.getInstance().getMainColor().getRGB(), alpha * 0.25f), applyAlpha(button.id == 7 ? buttonHoverColor : ClientManager.getInstance().getSecondaryColor().getRGB(), alpha * 0.25f));
            }

            Fonts.INTER_MEDIUM.get(18).drawCenteredString(button.displayString, button.xPosition + button.width / 2f, button.yPosition + (button.height - 8) / 2f, applyAlpha(textColor, alpha));

            button.visible = false;
            button.enabled = true;
        }
    }

    private void updateHoverAnimation(int mouseX, int mouseY) {
        int newHoveredButton = -1;
        for (int i = 0; i < this.buttonList.size(); i++) {
            GuiButton button = this.buttonList.get(i);
            if (mouseX >= button.xPosition && mouseX <= button.xPosition + button.width && mouseY >= button.yPosition && mouseY <= button.yPosition + button.height) {
                newHoveredButton = i;
                break;
            }
        }

        if (newHoveredButton != hoveredButton) {
            hoveredButton = newHoveredButton;
            lastHoverTime = System.currentTimeMillis();
            hoverOpacity = 0;
        } else if (hoveredButton != -1) {
            long elapsed = System.currentTimeMillis() - lastHoverTime;
            hoverOpacity = Math.min(1.0f, elapsed / 300.0f);
        }
    }

    private void drawButtonTooltips(int mouseX, int mouseY) {
        if (hoveredButton != -1 && hoverOpacity > 0.7f) {
            GuiButton button = this.buttonList.get(hoveredButton);

            String tooltipText = null;
            switch (button.id) {
                case 1:
                    tooltipText = "Login with selected account";
                    break;
                case 2:
                    tooltipText = "Add email/password account";
                    break;
                case 3:
                    tooltipText = "Login using cookie from clipboard";
                    break;
                case 4:
                    tooltipText = "Start browser-based login";
                    break;
                case 7:
                    tooltipText = "Remove selected account";
                    break;
                case 8:
                    tooltipText = "Login with cookie from file";
                    break;
            }

            if (tooltipText != null) {
                int tooltipWidth = Fonts.INTER_MEDIUM.get(18).getStringWidth(tooltipText) + 10;
                int tooltipX = mouseX + 10;
                int tooltipY = mouseY - 25;

                if (tooltipX + tooltipWidth > width) {
                    tooltipX = width - tooltipWidth - 5;
                }

                RenderUtils.drawRoundRect(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + 20, 3,
                        new Color(20, 20, 30, 220).getRGB());

                Fonts.INTER_MEDIUM.get(18).drawCenteredString(tooltipText, tooltipX + tooltipWidth / 2f, tooltipY + 6, textColor);
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (!button.enabled) return;

        switch (button.id) {
            case 3: // Cookie from clipboard
                new Thread(() -> {
                    String cookie = getSystemClipboard();
                    if (cookie == null || cookie.isEmpty()) {
                        ClientManager.getInstance().getNotificationManager().addNotification("Error", "Clipboard is empty or invalid", 3000);
                        return;
                    }
                    SavedAltData data = new SavedAltData();
                    ClientManager.getInstance().getNotificationManager().addNotification("Processing", "Logging in with cookie...", 2000);
                    AltAuthResult result = ClientManager.getInstance().getAltManager().authenticator.loginWithCookie(data, cookie);
                    if (result == AltAuthResult.SUCCESS) {
                        ClientManager.getInstance().getAltManager().addAlt(data);
                    } else {
                        ClientManager.getInstance().getNotificationManager().addNotification("Login failed", "Unable to login with cookie", 3000);
                    }
                }).start();
                break;

            case 8: // Cookie from file
                if (!isPickingCookieFile.get()) {
                    isPickingCookieFile.set(true);
                    new Thread(() -> {
                        Frame frame = new Frame();
                        FileDialog cookieFd = new FileDialog(frame, "Select cookie file", FileDialog.LOAD);
                        cookieFd.setMultipleMode(true);
                        String userHome = System.getProperty("user.home");
                        Path downloads = Paths.get(userHome, "Downloads");
                        if (Files.exists(downloads)) {
                            cookieFd.setDirectory(downloads.toAbsolutePath().toString());
                        }
                        cookieFd.setVisible(true);
                        File[] files = cookieFd.getFiles();
                        isPickingCookieFile.set(false);

                        if (files.length == 0) {
                            ClientManager.getInstance().getNotificationManager().addNotification("Cancelled", "No file selected", 2000);
                            return;
                        }

                        for (File file : files) {
                            try {
                                List<String> cookies = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
                                SavedAltData data = new SavedAltData();
                                AltAuthResult result = ClientManager.getInstance().getAltManager().authenticator.loginWithCookie(data, cookies);
                                if (result == AltAuthResult.SUCCESS) {
                                    ClientManager.getInstance().getAltManager().addAlt(data);
                                } else {
                                    ClientManager.getInstance().getNotificationManager().addNotification("Login failed", "Unable to login with " + file.getName(), 3000);
                                }
                            } catch (IOException e) {
                                Logger.error("Unable to read cookie file: {}", file);
                                throw new RuntimeException(e);
                            }
                        }
                    }).start();
                }
                break;

            case 7: // Delete alt
                if (isSelectedEntryValid()) {
                    deletingAlt = true;
                    GuiYesNo guiyesno = new GuiYesNo(this, "Are you sure you would like to delete " +
                            altListGui.entries.get(altListGui.selectedEntry).savedData.cachedUsername + "?",
                            "You will not be able to undo this action.", "Delete", "Back", altListGui.selectedEntry);
                    this.mc.displayGuiScreen(guiyesno);
                } else {
                    ClientManager.getInstance().getNotificationManager().addNotification("Error", "No account selected", 2000);
                }
                break;

            case 0: // Back
                this.mc.displayGuiScreen(parentScreen);
                break;

            case 4: // External login
                ClientManager.getInstance().getAltManager().authenticator.startBrowserLogin(true);
                ClientManager.getInstance().getNotificationManager().addNotification("Copied URL", "Copied microsoft login URL to clipboard", 3000);
                break;

            case 1: // Login
                if (isSelectedEntryValid()) {
                    SavedAltData data = ClientManager.getInstance().getAltManager().savedAlts.get(altListGui.selectedEntry);
                    new Thread(() -> {
                        switch (data.authStatus) {
                            case UNVERIFIED: {
                                data.authStatus = AltAuthStatus.PROCESSING;
                                ClientManager.getInstance().getNotificationManager().addNotification("Logging in", "Authenticating " + data.cachedUsername, 2000);
                                AltAuthResult result = data.login();
                                if (result == AltAuthResult.SUCCESS) {
                                    data.authStatus = AltAuthStatus.VERIFIED;
                                } else {
                                    data.authStatus = AltAuthStatus.UNVERIFIED;
                                    ClientManager.getInstance().getNotificationManager().addNotification("Failed", "Could not login as " + data.cachedUsername, 3000);
                                }
                                break;
                            }
                            case VERIFIED:
                                data.setMcSession();
                                ClientManager.getInstance().getNotificationManager().addNotification("Success", "Logged in as " + data.cachedUsername, 3000);
                                break;
                            case PROCESSING:
                                ClientManager.getInstance().getNotificationManager().addNotification("Wait", "Already logging in...", 2000);
                                break;
                        }
                    }).start();
                } else {
                    ClientManager.getInstance().getNotificationManager().addNotification("Error", "No account selected", 2000);
                }
                break;
        }
        super.actionPerformed(button);
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (this.deletingAlt && result) {
            ClientManager.getInstance().getAltManager().removeAlt(id);
        }
        this.deletingAlt = false;
        this.mc.displayGuiScreen(this);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void handleMouseInput() throws IOException {
        altListGui.handleMouseInput();
        super.handleMouseInput();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        altListGui.mouseClicked(mouseX, mouseY, mouseButton);

        for (GuiButton button : this.buttonList) {
            if (mouseX >= button.xPosition && mouseX <= button.xPosition + button.width && mouseY >= button.yPosition && mouseY <= button.yPosition + button.height) {
                button.playPressSound(this.mc.getSoundHandler());
                this.actionPerformed(button);
            }
        }

        if (mouseButton == 0) { // left click only
            if (isSelectedEntryValid()) {
                int clickedIndex = altListGui.selectedEntry;
                long now = System.currentTimeMillis();

                if (clickedIndex == lastEntryClickIndex && (now - lastEntryClickTime) <= DOUBLE_CLICK_THRESHOLD_MS) {
                    // double click: trigger login
                    loginSelectedEntry();

                    // reset so a triple-click won't fire again
                    lastEntryClickIndex = -1;
                    lastEntryClickTime = 0L;
                } else {
                    // store for next click
                    lastEntryClickIndex = clickedIndex;
                    lastEntryClickTime = now;
                }
            } else {
                // clicked outside a valid entry: reset
                lastEntryClickIndex = -1;
                lastEntryClickTime = 0L;
            }
        }
    }

    private boolean isSelectedEntryValid() {
        return !ClientManager.getInstance().getAltManager().savedAlts.isEmpty() &&
                altListGui.selectedEntry >= 0 &&
                altListGui.selectedEntry < altListGui.entries.size();
    }

    public String getSystemClipboard() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);
        if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            try {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                Logger.error("Unable to get clipboard contents");
            }
        }
        return null;
    }

    private void loginSelectedEntry() {
        if (!isSelectedEntryValid()) {
            ClientManager.getInstance().getNotificationManager().addNotification("Error", "No account selected", 2000);
            return;
        }

        final SavedAltData data = ClientManager.getInstance().getAltManager().savedAlts.get(altListGui.selectedEntry);

        new Thread(() -> {
            switch (data.authStatus) {
                case UNVERIFIED: {
                    data.authStatus = AltAuthStatus.PROCESSING;
                    ClientManager.getInstance().getNotificationManager().addNotification("Logging in", "Authenticating " + data.cachedUsername, 2000);
                    AltAuthResult result = data.login();
                    if (result == AltAuthResult.SUCCESS) {
                        data.authStatus = AltAuthStatus.VERIFIED;
                    } else {
                        data.authStatus = AltAuthStatus.UNVERIFIED;
                        ClientManager.getInstance().getNotificationManager().addNotification("Failed", "Could not login as " + data.cachedUsername, 3000);
                    }
                    break;
                }
                case VERIFIED:
                    data.setMcSession();
                    ClientManager.getInstance().getNotificationManager().addNotification("Success", "Logged in as " + data.cachedUsername, 3000);
                    break;
                case PROCESSING:
                    ClientManager.getInstance().getNotificationManager().addNotification("Wait", "Already logging in...", 2000);
                    break;
            }
        }, "Alt-Login-Thread").start();
    }

    private float easeOutQuint(float x) {
        return (float) (1 - Math.pow(1 - x, 5));
    }

    private int applyAlpha(int color, float alpha) {
        Color c = new Color(color, true);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int) (c.getAlpha() * alpha)).getRGB();
    }
}