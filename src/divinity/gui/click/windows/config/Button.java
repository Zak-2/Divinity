package divinity.gui.click.windows.config;

import divinity.ClientManager;
import divinity.utils.MouseUtils;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

public class Button extends Element<String, ConfigFrame> {

    public Button(String object, ConfigFrame parent, int x, int y, int width, int height) {
        super(object, parent, x, y, width, height);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawBorderedRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 1, new Color(11, 10, 15).getRGB(), isHovered(mouseX, mouseY) ? ClientManager.getInstance().getMainColor().getRGB() : new Color(16, 16, 21).getRGB());
        Fonts.INTER_MEDIUM.get(14).drawCenteredStringWithShadow(getObject(), getX() + getWidth() / 2, getY() + 5, -1);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (MouseUtils.isHovered(getX(), getY(), getX() + getWidth(), getY() + getHeight())) {
            switch (getObject()) {
                case "Add":
                    if (getParent().customInput.getText().length() > 0) {
                        // save
                        getParent().addConfigs();
                    }
                    break;
                case "Remove":
                    if (Objects.nonNull(getParent().currentConfig)) {
                        if (getParent().getCurrentTab().getObject().equalsIgnoreCase("cloud")) {

                        } else {
                            Path toDelete = ClientManager.getInstance().getCLIENT_DIR().resolve("configs").resolve(getParent().currentConfig.getObject());
                            if (toDelete.toFile().exists()) {
                                toDelete.toFile().delete();
                            }
                            try {
                                Path configsPath = ClientManager.getInstance().getCLIENT_DIR().resolve("configs");
                                if (Files.exists(configsPath)) {
                                    try (Stream<Path> stream = Files.list(configsPath)) {
                                        String[] array = stream.filter(file -> !Files.isDirectory(file))
                                                .map(Path::getFileName)
                                                .map(Path::toString)
                                                .toArray(String[]::new);

                                        getParent().configs = array;
                                        getParent().configEntries.clear();
                                        getParent().initializeEntries(getParent().configs);
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    break;
                case "Upload":
                    if (Objects.nonNull(getParent().currentConfig)) {
                        uploadConfigToCloud(getParent().currentConfig.getObject(), "");
                    }
                    break;
                case "Share":
                    break;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void uploadConfigToCloud(String configName, String uploaderName) {
        // TODO impl
    }
}