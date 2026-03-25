package divinity.gui.click.components;

import divinity.ClientManager;
import divinity.gui.click.windows.MainWindow;
import divinity.module.Category;
import divinity.utils.AnimationUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class CategoryButtonComponent extends Component {

    private final Category category;
    private final MainWindow mainWindow;
    private final String customName;
    private float yAnimation = 12f;
    private float categoryAnimation = 0;

    public CategoryButtonComponent(Category category, MainWindow mainWindow, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.category = category;
        this.mainWindow = mainWindow;
        this.customName = null;
    }

    public CategoryButtonComponent(String customName, MainWindow mainWindow, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.category = null;
        this.mainWindow = mainWindow;
        this.customName = customName;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        boolean active = mainWindow.selectedCategory == this;
        float targetY = active ? 6f : 12f;
        yAnimation = AnimationUtils.getAnimationState(yAnimation, targetY, 35f);
        categoryAnimation = AnimationUtils.getAnimationState(categoryAnimation, active ? getHeight() + 2 : 0, 100);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.prepareScissorbox(getX(), getY(), getX() + getWidth(), getY() + (int) categoryAnimation);

        float buttonX2 = getX() + getWidth();
        float buttonY2 = getY() + getHeight();

        ShaderUtils.drawRoundRect(getX(), getY(), buttonX2, buttonY2, 5, new Color(35, 35, 45).getRGB());
        ShaderUtils.drawRoundRect(getX() + 1f, getY() + 1f, buttonX2 - 1f, buttonY2 - 1f, 5, new Color(15, 15, 20, 200).getRGB());

        String subText = (category == null) ? "0 Configs" : ClientManager.getInstance().getModuleManager().getModules(getCategory()).size() + " Modules";
        Fonts.INTER_MEDIUM.get(12).drawCenteredStringWithShadow(subText, getX() + getWidth() / 2, getY() + getHeight() - 8, Color.GRAY.getRGB());

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        String name = (category != null) ? WordUtils.capitalizeFully(category.name()) : customName;
        Fonts.INTER_MEDIUM.get(15).drawCenteredStringWithShadow(
                name, getX() + getWidth() / 2, getY() + Math.round(yAnimation),
                active ? ClientManager.getInstance().getMainColor().getRGB() : -1
        );

        super.drawScreen(mouseX, mouseY);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (hovered(mouseX, mouseY)) {
            if (mainWindow.selectedCategory != this) {
                mainWindow.selectedCategory = this;
                mainWindow.selectedModule = null;
                mainWindow.dropdownComponent = null;
                mainWindow.pickerContainer = null;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    public Category getCategory() {
        return category;
    }
}