package divinity.gui.click.windows;

import divinity.ClientManager;
import divinity.gui.click.components.CategoryButtonComponent;
import divinity.gui.click.components.ModuleButtonComponent;
import divinity.gui.click.components.module.ColorPickerContainer;
import divinity.gui.click.components.module.DropdownComponent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.Fonts;
import divinity.utils.shaders.visual.BlurUtil;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.*;
import java.util.List;

public class MainWindow extends Window {

    private final Map<Category, Integer> categoryScrollPositions = new HashMap<>();
    public CategoryButtonComponent selectedCategory = null;
    public ModuleButtonComponent selectedModule = null;
    public DropdownComponent dropdownComponent;
    public ColorPickerContainer pickerContainer;
    public boolean allowClick;
    private List<CategoryButtonComponent> categoryButtons;
    private List<ModuleButtonComponent> moduleButtons;
    private List<ModulePropertyWindow> propertyWindows;
    private int totalContentHeight = 0;

    public MainWindow(float x, float y, float width, float height) {
        super(x, y, width, height);
        setDraggable(true);
        initCategoryButtons();
        selectedCategory = categoryButtons.get(0);
        allowClick = true;
        for (Category category : Category.values()) {
            categoryScrollPositions.put(category, 0);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        BlurUtil.blurArea(getX(), getY(), getWidth(), getHeight());
        ShaderUtils.drawRoundRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 5, new Color(9, 9, 14, 200).getRGB());

        RenderUtils.drawVerticalLine(getX() + 130, getY(), getY() + getHeight() - 36, .5f, new Color(21, 20, 27).getRGB());
        RenderUtils.drawHorizontalLine(getX() + 1, getX() + getWidth() - 1, getY() + getHeight() - 36, .5f, new Color(21, 20, 27).getRGB());
        RenderUtils.drawHorizontalLine(getX() + 1, getX() + getWidth() - 1, getY() + 46, .5f, new Color(21, 20, 27).getRGB());

        Fonts.INTER_MEDIUM.get(26).drawCenteredStringWithShadow(ClientManager.getInstance().getName(), getX() + 65, getY() + 12, -1);
        Fonts.INTER_MEDIUM.get(16).drawCenteredStringWithShadow("Version: " + ClientManager.getInstance().getVersion(), getX() + 65, getY() + 32, Color.GRAY.getRGB());

        drawCategoryButtons(mouseX, mouseY);

        if (selectedCategory != null) {
            if (selectedCategory.getCategory() == null) {
                drawConfigPage(mouseX, mouseY);
            } else {
                drawModuleButtons(mouseX, mouseY);
                if (selectedModule != null) {
                    ModulePropertyWindow m = propertyWindows.stream()
                            .filter(pw -> pw.module.equals(selectedModule.module))
                            .findFirst().orElse(null);

                    if (m != null) {
                        m.setX(getX() + 134);
                        m.setY(getY() + 50);
                        m.drawScreen(mouseX, mouseY);
                        Fonts.INTER_MEDIUM.get(16).drawStringWithShadow(selectedModule.module.getName(), getX() + 134, getY() + 24, -1);
                        Fonts.INTER_MEDIUM.get(16).drawStringWithShadow("Bind:" + EnumChatFormatting.GRAY + "[" + Keyboard.getKeyName(selectedModule.module.getKeyBind()) + "]", getX() + 134, getY() + 36, -1);
                    }
                }
            }
        }

        if (dropdownComponent != null) dropdownComponent.drawScreen(mouseX, mouseY);
        if (pickerContainer != null) pickerContainer.drawScreen(mouseX, mouseY);

        allowClick = dropdownComponent == null && pickerContainer == null;
        super.drawScreen(mouseX, mouseY);
    }

    private void drawConfigPage(int mouseX, int mouseY) {
        float listX = getX() + 135;
        float listY = getY() + 50;
        ShaderUtils.drawRoundRect(listX, listY, getX() + getWidth() - 10, getY() + getHeight() - 50, 4, new Color(15, 15, 20).getRGB());
        Fonts.INTER_MEDIUM.get(20).drawCenteredStringWithShadow("Configs", listX + (getX() + getWidth() - 10 - listX) / 2, listY + 10, -1);
        Fonts.INTER_MEDIUM.get(16).drawCenteredStringWithShadow("No configurations found.", listX + (getX() + getWidth() - 10 - listX) / 2, listY + 50, Color.GRAY.getRGB());
    }

    private void initCategoryButtons() {
        categoryButtons = new ArrayList<>();
        moduleButtons = new ArrayList<>();
        propertyWindows = new ArrayList<>();
        for (Category value : Category.values()) {
            categoryButtons.add(new CategoryButtonComponent(value, this, 0, 0, 60, 26));
            for (Module module : ClientManager.getInstance().getModuleManager().getModules(value)) {
                moduleButtons.add(new ModuleButtonComponent(module, this, 0, 0, 120, 16));
                propertyWindows.add(new ModulePropertyWindow(module, this, 0, 0, getWidth() - 138, getHeight() - 86));
            }
        }
        categoryButtons.add(new CategoryButtonComponent("Configs", this, 0, 0, 60, 26));
    }

    private void drawCategoryButtons(int mouseX, int mouseY) {
        float totalWidth = categoryButtons.size() * 65;
        float startX = getX() + (getWidth() / 2) - (totalWidth / 2);
        for (CategoryButtonComponent categoryButton : categoryButtons) {
            categoryButton.setX(startX);
            categoryButton.setY(getY() + getHeight() - 30);
            categoryButton.drawScreen(mouseX, mouseY);
            startX += 65;
        }
    }

    private void drawModuleButtons(int mouseX, int mouseY) {
        Category cat = selectedCategory.getCategory();
        totalContentHeight = (int) moduleButtons.stream()
                .filter(m -> m.module.getCategory().equals(cat))
                .mapToDouble(m -> m.getHeight() + 2)
                .sum();

        int scrollY = categoryScrollPositions.getOrDefault(cat, 0);
        float moduleAreaY = getY() + 50;
        float moduleAreaHeight = getHeight() - 50 - 36;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.prepareScissorbox(getX() + 6, moduleAreaY, getX() + 126, moduleAreaY + moduleAreaHeight);

        int offset = -scrollY;
        for (ModuleButtonComponent moduleButton : moduleButtons) {
            if (moduleButton.module.getCategory().equals(cat)) {
                moduleButton.setX(getX() + 6);
                moduleButton.setY(getY() + 50 + offset);
                moduleButton.drawScreen(mouseX, mouseY);
                offset += moduleButton.getHeight() + 2;
            }
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (allowClick) {
            for (CategoryButtonComponent categoryButton : categoryButtons) {
                if (categoryButton.hovered(mouseX, mouseY)) {
                    selectedCategory = categoryButton;
                    selectedModule = null;
                    categoryButton.mouseClicked(mouseX, mouseY, mouseButton);
                    return;
                }
            }
        }

        if (selectedCategory != null && selectedCategory.getCategory() != null) {
            for (ModuleButtonComponent moduleButton : moduleButtons) {
                if (moduleButton.module.getCategory().equals(selectedCategory.getCategory())) {
                    if (moduleButton.hovered(mouseX, mouseY)) selectedModule = moduleButton;
                    moduleButton.mouseClicked(mouseX, mouseY, mouseButton);
                }
            }
            if (selectedModule != null && allowClick) {
                propertyWindows.stream()
                        .filter(pw -> pw.module.equals(selectedModule.module))
                        .findFirst()
                        .ifPresent(pw -> pw.mouseClicked(mouseX, mouseY, mouseButton));
            }
        }

        if (dropdownComponent != null) dropdownComponent.mouseClicked(mouseX, mouseY, mouseButton);
        if (pickerContainer != null) pickerContainer.mouseClicked(mouseX, mouseY, mouseButton);
        
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();

        int mouseWheel = Mouse.getEventDWheel();
        if (mouseWheel != 0) {
            boolean scrolledProperties = false;
            if (selectedModule != null) {
                ModulePropertyWindow m = propertyWindows.stream()
                        .filter(pw -> pw.module.equals(selectedModule.module))
                        .findFirst().orElse(null);

                if (m != null && m.isMouseOverWindow()) {
                    m.onScroll(mouseWheel);
                    scrolledProperties = true;
                }
            }

            if (!scrolledProperties && selectedCategory != null && selectedCategory.getCategory() != null) {
                Category cat = selectedCategory.getCategory();
                int scrollY = categoryScrollPositions.getOrDefault(cat, 0);
                scrollY = MathHelper.clamp_int(scrollY + (mouseWheel > 0 ? -20 : 20), 0, Math.max(0, totalContentHeight - (int) (getHeight() - 86)));
                categoryScrollPositions.put(cat, scrollY);
            }
        }
    }


    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (selectedModule != null) {
            propertyWindows.stream()
                    .filter(pw -> pw.module.equals(selectedModule.module))
                    .findFirst()
                    .ifPresent(pw -> pw.mouseReleased(mouseX, mouseY, state));
        }

        if (pickerContainer != null) {
            pickerContainer.mouseReleased(mouseX, mouseY, state);
        }
        
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (selectedCategory != null && selectedCategory.getCategory() != null) {
            for (ModuleButtonComponent moduleButton : moduleButtons) {
                if (moduleButton.module.getCategory().equals(selectedCategory.getCategory())) {
                    moduleButton.keyTyped(typedChar, keyCode);
                }
            }
        }

        if (selectedModule != null) {
            propertyWindows.stream()
                    .filter(pw -> pw.module.equals(selectedModule.module))
                    .findFirst()
                    .ifPresent(pw -> pw.keyTyped(typedChar, keyCode));
        }
        
        super.keyTyped(typedChar, keyCode);
    }
}
