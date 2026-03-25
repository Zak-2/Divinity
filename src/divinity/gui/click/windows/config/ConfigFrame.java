package divinity.gui.click.windows.config;

import divinity.ClientManager;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import lombok.Getter;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class ConfigFrame extends Frame {
    private final List<TabButton> tabs;
    private final List<Button> buttons;
    private final String[] tabNames = new String[]{"Cloud", "Local"};
    private final String[] btNames = new String[]{"Add", "Remove", "Upload", "Share"};
    private final int visibleAreaHeight = 118 - 30;
    private final String[] cloudConfigs = new String[0];
    private final String[] uploaderNames = new String[0];
    public List<ConfigEntry> configEntries;
    public String[] configs;
    public ConfigEntry currentConfig;
    public CustomInput customInput;
    public int scrollOffset = 0;
    private TabButton currentTab;
    private int totalContentHeight = 0;

    public ConfigFrame(Object object, int x, int y, int width, int height) {
        super(object, x, y, width, height);
        tabs = new ArrayList<>();
        buttons = new ArrayList<>();
        configEntries = new ArrayList<>();
        configs = new String[]{};
        int offset = 1;
        for (String tab : tabNames) {
            tabs.add(new TabButton(tab, this, getX() + offset, getY() + 16, 58, 13));
            offset += 59;
        }
        int xOffset = 2;
        int yOffset = 130;
        int counter = 0;
        for (String btName : btNames) {
            buttons.add(new Button(btName, this, getX() + xOffset, getY() + yOffset, 58, 14));
            xOffset += 58;
            counter++;
            if (counter == 2) {
                xOffset = 2;
                yOffset = 144;
            }
        }
        currentTab = tabs.get(1);

        refreshLocalConfigs();
    }

    public void initGui() {
        customInput = new CustomInput(Fonts.INTER_MEDIUM.get(14), getX() + 4, getY() + 118, getWidth() - 8, 10);
        addConfigs();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        customInput.xPosition = getX() + 4;
        customInput.yPosition = getY() + 119;
        ShaderUtils.drawRoundRect(getX() - 2 - .5f, getY() - .5f, getX() + getWidth() + 2 + .5f, getY() + getHeight() + 4 + .5f, 10, new Color(21, 20, 27).getRGB());
        RenderUtils.drawRoundRect(getX() - 2, getY(), getX() + getWidth() + 2, getY() + getHeight() + 4, 10, new Color(9, 9, 14).getRGB());
        RenderUtils.drawRect(getX() + 2, getY() + 16, getX() + getWidth() - 2, getY() + getHeight() - 2, new Color(11, 10, 15).getRGB());
        Fonts.INTER_MEDIUM.get(16).drawStringWithShadow("Configs", getX() + 3, getY() + 6, -1);

        int offset = 2;
        for (TabButton tab : tabs) {
            tab.update(getX() + offset, getY() + 16);
            tab.drawScreen(mouseX, mouseY, partialTicks);
            offset += 59;
        }
        int xOffset = 2;
        int yOffset = 131;
        int counter = 0;
        for (Button button : buttons) {
            button.update(getX() + xOffset, getY() + yOffset);
            button.drawScreen(mouseX, mouseY, partialTicks);
            xOffset += 59;
            counter++;
            if (counter == 2) {
                xOffset = 2;
                yOffset = 146;
            }
        }
        customInput.drawTextBox();

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.prepareScissorbox(getX(), getY() + 30, getX() + getWidth(), getY() + 118);
        int coffset = 30 - scrollOffset;
        if (currentTab.getObject().equalsIgnoreCase("cloud")) {
            for (int i = 0; i < cloudConfigs.length; i++) {
                String config = cloudConfigs[i];
                ConfigEntry configEntry = new ConfigEntry(config, this, getX() + 2, getY() + coffset, getWidth() - 4 - 6, 10);
                configEntry.drawScreen(mouseX, mouseY, partialTicks);

                if (isHovered(mouseX, mouseY, getX() + 2, getY() + coffset, getWidth() - 4 - 6, 10)) {
                    String uploaderName = uploaderNames[i];
                    RenderUtils.drawRect(mouseX + 5, mouseY + 5, mouseX + 5 + Fonts.INTER_MEDIUM.get(12).getStringWidth("Uploaded by: " + uploaderName) + 4, mouseY + 5 + 12, new Color(0, 0, 0, 150).getRGB());
                    Fonts.INTER_MEDIUM.get(12).drawStringWithShadow("Uploaded by: " + uploaderName, mouseX + 7, mouseY + 10, -1);
                }

                coffset += 10;
            }
        } else {
            for (ConfigEntry configEntry : configEntries) {
                configEntry.update(getX() + 2, getY() + coffset);
                configEntry.setWidth(getWidth() - 4 - 6);
                configEntry.drawScreen(mouseX, mouseY, partialTicks);
                coffset += 10;
            }
        }
        if (totalContentHeight > visibleAreaHeight) {
            int scrollbarHeight = (int) ((visibleAreaHeight) * (visibleAreaHeight / (float) totalContentHeight));
            int scrollbarY = getY() + 30 + (int) ((scrollOffset / (float) totalContentHeight) * visibleAreaHeight);
            RenderUtils.drawRect(getX() + getWidth() - 4, scrollbarY + 1, getX() + getWidth() - 2, scrollbarY + scrollbarHeight - 1, new Color(100, 100, 100).getRGB());
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        tabs.forEach(tabButton -> tabButton.mouseClicked(mouseX, mouseY, mouseButton));
        buttons.forEach(button -> button.mouseClicked(mouseX, mouseY, mouseButton));
        customInput.mouseClicked(mouseX, mouseY, mouseButton);

        if (MouseUtils.isHovered(getX(), getY() + 30, getX() + getWidth(), getY() + 118)) {
            if (currentTab.getObject().equalsIgnoreCase("cloud")) {
                for (int i = 0; i < cloudConfigs.length; i++) {
                    String config = cloudConfigs[i];
                    int yOffset = 30 + (i * 10);
                    if (isHovered(mouseX, mouseY, getX() + 2, getY() + yOffset, getWidth() - 4, 10)) {
                        if (mouseButton == 0) {
                            currentConfig = new ConfigEntry(config, this, getX() + 2, getY() + yOffset, getWidth() - 4, 10);
                            loadCloudConfig(config);
                        } else if (mouseButton == 2 || mouseButton == 1) {
                            loadCloudConfig(config);
                        }
                    }
                }
            } else {
                configEntries.forEach(configEntry -> configEntry.mouseClicked(mouseX, mouseY, mouseButton));
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() {
        int scrollAmount = Mouse.getEventDWheel();
        if (MouseUtils.isHovered(getX(), getY(), getX() + getWidth(), getY() + getHeight())) {
            if (scrollAmount != 0) {
                if (currentTab.getObject().equalsIgnoreCase("cloud")) {
                    if (cloudConfigs.length > 8) {
                        scrollOffset -= scrollAmount > 0 ? 10 : -10;
                        scrollOffset = Math.max(0, Math.min(totalContentHeight - visibleAreaHeight, scrollOffset));
                    }
                } else {
                    scrollOffset -= scrollAmount > 0 ? 10 : -10;
                    scrollOffset = Math.max(0, Math.min(totalContentHeight - visibleAreaHeight, scrollOffset));
                }
            }
        }
        super.handleMouseInput();
    }

    public boolean isHovered(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX > x && mouseX < x + width && mouseY > y && mouseY < y + height;
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        customInput.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    public void initializeEntries(String[] array) {
        configEntries.clear();
        int cOffset = 30;
        for (String config : array) {
            configEntries.add(new ConfigEntry(config, this, getX() + 4, getY() + cOffset, getWidth() - 8, 10));
            cOffset += 10;
        }
        totalContentHeight = array.length * 10;
    }

    public void setCurrentTab(TabButton currentTab) {
        if (currentTab.getObject().equalsIgnoreCase("cloud")) {
            fetchCloudConfigs();
        } else {
            refreshLocalConfigs();
        }
        this.currentTab = currentTab;
    }

    public void addConfigs() {
        try {
            Path configsPath = ClientManager.getInstance().getCLIENT_DIR().resolve("configs");
            if (Files.exists(configsPath)) {
                try (Stream<Path> stream = Files.list(configsPath)) {
                    String[] gay = stream.filter(file -> !Files.isDirectory(file))
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toArray(String[]::new);
                    configs = gay;
                    configEntries.clear();
                    initializeEntries(gay);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void refreshLocalConfigs() {
        try {
            Path configsPath = ClientManager.getInstance().getCLIENT_DIR().resolve("configs");
            if (Files.exists(configsPath)) {
                try (Stream<Path> stream = Files.list(configsPath)) {
                    configs = stream.filter(file -> !Files.isDirectory(file))
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .toArray(String[]::new);
                    initializeEntries(configs);
                }
            } else {
                configsPath.toFile().mkdirs();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchCloudConfigs() {
        //TODO IMPL
    }

    private void loadCloudConfig(String configName) {
        //TODO IMPL
    }

}