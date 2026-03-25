package divinity.gui.click.components.module;

import divinity.ClientManager;
import divinity.gui.click.components.Component;
import divinity.utils.AnimationUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.FontRenderer;
import divinity.utils.font.Fonts;
import divinity.utils.math.TimerUtil;
import net.minecraft.util.MathHelper;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorPickerContainer extends Component {

    private static final int PADDING = 3;
    private static final int COLOR_FIELD_SIZE = 80;
    private static final int SLIDER_HEIGHT = 6;
    private static final int TAB_HEIGHT = 16;
    private static final int PRESET_SIZE = 10;
    private static final int PRESET_COLUMNS = 5;
    private static final int PRESET_SPACING = 2;

    private static final List<Color> DEFAULT_PRESETS = Arrays.asList(
            new Color(255, 59, 48),    // Red
            new Color(255, 87, 34),    // Deep Orange
            new Color(255, 149, 0),    // Orange
            new Color(255, 195, 0),    // Vibrant Yellow
            new Color(255, 234, 0),    // Bright Yellow
            new Color(255, 204, 0),    // Golden Yellow
            new Color(204, 255, 0),    // Lime
            new Color(76, 217, 100),   // Green
            new Color(0, 230, 118),    // Bright Green
            new Color(52, 199, 89),    // Medium Green
            new Color(0, 200, 83),     // Darker Green
            new Color(0, 191, 165),    // Teal
            new Color(0, 188, 212),    // Cyan
            new Color(64, 224, 208),   // Turquoise
            new Color(90, 200, 250),   // Light Blue
            new Color(0, 122, 255),    // Blue
            new Color(30, 144, 255),   // Dodger Blue
            new Color(100, 149, 237),  // Cornflower Blue
            new Color(70, 130, 180),   // Steel Blue
            new Color(72, 61, 139),    // Dark Slate Blue
            new Color(88, 86, 214),    // Indigo
            new Color(138, 43, 226),   // Blue Violet
            new Color(153, 50, 204),   // Dark Orchid
            new Color(186, 85, 211),   // Medium Orchid
            new Color(218, 112, 214),  // Orchid
            new Color(221, 160, 221),  // Plum
            new Color(255, 105, 180),  // Hot Pink
            new Color(255, 20, 147),   // Deep Pink
            new Color(255, 92, 205),   // Light Fuchsia
            new Color(255, 159, 243),  // Pastel Pink
            new Color(255, 153, 204),  // Soft Pink
            new Color(255, 128, 171),  // Rose Pink
            new Color(255, 77, 136),   // Bright Rose
            new Color(240, 128, 128),  // Light Coral
            new Color(250, 128, 114),  // Salmon
            new Color(255, 160, 122),  // Light Salmon
            new Color(255, 182, 193),  // Light Pink
            new Color(255, 218, 185),  // Peach Puff
            new Color(255, 228, 181),  // Moccasin
            new Color(255, 239, 213),  // Papaya Whip
            new Color(255, 250, 205),  // Lemon Chiffon
            new Color(230, 230, 250),  // Lavender
            new Color(176, 224, 230),  // Powder Blue
            new Color(173, 216, 230),  // Light Blue
            new Color(152, 251, 152),  // Pale Green
            new Color(144, 238, 144),  // Light Green
            new Color(127, 255, 212),  // Aquamarine
            new Color(175, 238, 238),  // Pale Turquoise
            new Color(224, 255, 255),  // Light Cyan
            new Color(216, 191, 216)   // Thistle
    );


    private final TimerUtil timerUtils;
    private final ColorPickerComponent component;
    private final List<Color> customPresets = new ArrayList<>();
    private final float[] tabAnimations = new float[2];
    private boolean draggingHue, draggingAlpha, draggingColorField;
    private int activeTab = 0;
    private float hue, saturation, value;
    private int alpha;
    private int hexInputMode = 0;
    private StringBuilder hexInput = new StringBuilder();

    public ColorPickerContainer(ColorPickerComponent component, float x, float y, float width, float height) {
        super(x, y, width, height);
        this.component = component;
        this.timerUtils = new TimerUtil();
        refreshValues();
        for (int i = 0; i < 5; i++) {
            customPresets.add(DEFAULT_PRESETS.get(i));
        }
    }

    private void refreshValues() {
        Color color = component.property.getValue();
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        hue = hsb[0];
        saturation = hsb[1];
        value = hsb[2];
        alpha = component.property.getAlpha();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        setHeight(calculateContentHeight());
        ShaderUtils.drawRoundRect(getX(), getY(), getX() + getWidth(), getY() + getHeight(), 6, new Color(35, 35, 45).getRGB());
        ShaderUtils.drawRoundRect(getX() + 0.5f, getY() + 0.5f, getX() + getWidth() - 0.5f, getY() + getHeight() - 0.5f, 6, new Color(15, 15, 20, 200).getRGB());

        drawTabs(mouseX, mouseY);

        if (draggingHue) updateHue(mouseX);
        if (draggingAlpha) updateAlpha(mouseX);
        if (draggingColorField) updateSatVal(mouseX, mouseY);

        switch (activeTab) {
            case 0:
                drawPickerTab(mouseX, mouseY);
                break;
            case 1:
                drawPresetsTab(mouseX, mouseY);
                break;
        }

        super.drawScreen(mouseX, mouseY);
    }

    private int calculateContentHeight() {
        int baseHeight = TAB_HEIGHT + PADDING;
        switch (activeTab) {
            case 0:
                return baseHeight + COLOR_FIELD_SIZE + PADDING + (2 * (SLIDER_HEIGHT + PADDING)) + 3;
            case 1:
                int rows = (int) Math.ceil((activeTab == 1 ? DEFAULT_PRESETS.size() : customPresets.size()) / (float) PRESET_COLUMNS);
                //  return baseHeight + rows * (PRESET_SIZE + PRESET_SPACING) + PADDING;
                return 70;
        }
        return baseHeight;
    }

    private void drawTabs(int mouseX, int mouseY) {
        float tabWidth = getWidth() / 2;
        String[] tabNames = {"Picker", "Presets"};

        for (int i = 0; i < 2; i++) {
            float tabX = getX() + (i * tabWidth);
            boolean isActive = activeTab == i;
            tabAnimations[i] = AnimationUtils.getAnimationState(tabAnimations[i], isActive ? 255 : 80, 300);

            int alpha = (int) MathHelper.clamp_float(tabAnimations[i], 0, 255);
           /* RenderUtils.drawRoundRect(tabX + 1, getY() + 1, tabX + tabWidth - 1, getY() + TAB_HEIGHT - 1, 4,
                    new Color(121, 105, 229, alpha).getRGB());*/
            RenderUtils.drawRect(tabX + 1, getY() + TAB_HEIGHT - 1, tabX + tabWidth - 1, getY() + TAB_HEIGHT, new Color(ClientManager.getInstance().getMainColor().getRed(),
                    ClientManager.getInstance().getMainColor().getGreen(), ClientManager.getInstance().getMainColor().getBlue(), alpha).getRGB());


            float textWidth = Fonts.INTER_MEDIUM.get(12).getStringWidth(tabNames[i]);
            // Clamp text alpha to 0-255
            int textAlpha = (int) MathHelper.clamp_float(tabAnimations[i] * 0.8f + 80, 0, 255);
            Fonts.INTER_MEDIUM.get(12).drawStringWithShadow(tabNames[i], tabX + (tabWidth - textWidth) / 2, getY() + (TAB_HEIGHT - 2) / 2,
                    new Color(255, 255, 255, textAlpha).getRGB());
        }
    }

    private void drawPickerTab(int mouseX, int mouseY) {
        float yPos = getY() + TAB_HEIGHT + PADDING;

        drawColorField(getX() + PADDING, yPos, COLOR_FIELD_SIZE);

        Color currentColor = component.property.getValue();
        RenderUtils.drawRoundRect(getX() + PADDING * 2 + COLOR_FIELD_SIZE, yPos,
                getX() + getWidth() - PADDING, yPos + 15, 4, currentColor.getRGB());

        String hexText = String.format("#%02X%02X%02X%02X", currentColor.getRed(), currentColor.getGreen(),
                currentColor.getBlue(), alpha);
        Fonts.INTER_MEDIUM.get(9).drawStringWithShadow(hexInputMode == 0 ? hexText : "#" + hexInput + (System.currentTimeMillis() % 1000 > 500 ? "|" : ""),
                getX() + PADDING * 2 + COLOR_FIELD_SIZE + 3, yPos + 25, -1);

        yPos += COLOR_FIELD_SIZE + PADDING;
        float sliderWidth = getWidth() - (PADDING * 2);

        drawHueSlider(getX() + PADDING, yPos, sliderWidth, SLIDER_HEIGHT);
        yPos += SLIDER_HEIGHT + PADDING;

        drawAlphaSlider(getX() + PADDING, yPos, sliderWidth, SLIDER_HEIGHT);
    }

    private void drawPresetsTab(int mouseX, int mouseY) {
        float yPos = getY() + TAB_HEIGHT + PADDING;
        float xStart = getX() + PADDING;
        int column = 0;

        for (Color color : DEFAULT_PRESETS) {
            float presetX = xStart + (column * (PRESET_SIZE + PRESET_SPACING));
            if (presetX + PRESET_SIZE > getX() + getWidth() - PADDING) {
                column = 0;
                yPos += PRESET_SIZE + PRESET_SPACING;
                presetX = xStart;
            }
            RenderUtils.drawRoundRect(presetX, yPos, presetX + PRESET_SIZE, yPos + PRESET_SIZE, 3, color.getRGB());
            column++;
        }
    }

    private void drawColorField(float x, float y, float size) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                float sat = i / size;
                float val = 1 - (j / size);
                RenderUtils.drawRect(x + i, y + j, x + i + 1, y + j + 1, Color.HSBtoRGB(hue, sat, val));
            }
        }
        float satPos = x + saturation * size;
        float valPos = y + (1 - value) * size;
        RenderUtils.drawRect(satPos - 1, valPos - 1, satPos + 1, valPos + 1, new Color(255, 255, 255).getRGB());
    }

    private void drawHueSlider(float x, float y, float width, float height) {
        for (int i = 0; i < width; i++) {
            RenderUtils.drawRect(x + i, y, x + i + 1, y + height, Color.HSBtoRGB(i / width, 1, 1));
        }
        float thumbPos = x + hue * width;
        ShaderUtils.drawRoundRect(thumbPos - 1.5f, y - 1, thumbPos + 1.5f, y + height + 1, 1, -1);
    }

    private void drawAlphaSlider(float x, float y, float width, float height) {
        for (int i = 0; i < width; i += 4) {
            for (int j = 0; j < height; j += 4) {
                boolean isLight = ((i / 4) + (j / 4)) % 2 == 0;
                RenderUtils.drawRect(x + i, y + j, x + Math.min(i + 4, width), y + Math.min(j + 4, height),
                        isLight ? new Color(200, 200, 200).getRGB() : new Color(100, 100, 100).getRGB());
            }
        }
        Color baseColor = component.property.getValue();
        RenderUtils.drawHorizontalGradientSideways(x, y, x + width, y + height,
                new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 0).getRGB(),
                new Color(baseColor.getRed(), baseColor.getGreen(), baseColor.getBlue(), 255).getRGB());
        float thumbPos = x + (alpha / 255f) * width;
        RenderUtils.drawRect(thumbPos - 1, y - 1, thumbPos + 1, y + height + 1, ClientManager.getInstance().getMainColor().getRGB());
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!hovered(mouseX, mouseY, getX(), getY(), getX() + getWidth(), getY() + getHeight()) && timerUtils.hasTimeElapsed(250)) {
            component.mainWindow.pickerContainer = null;
            component.expanded = false;
            return;
        }

        float tabWidth = getWidth() / 2;
        for (int i = 0; i < 2; i++) {
            float tabX = getX() + (i * tabWidth);
            if (mouseX >= tabX && mouseX <= tabX + tabWidth && mouseY >= getY() && mouseY <= getY() + TAB_HEIGHT) {
                activeTab = i;
                return;
            }
        }

        switch (activeTab) {
            case 0:
                handlePickerTabClick(mouseX, mouseY, mouseButton);
                break;
            case 1:
                handlePresetsTabClick(mouseX, mouseY, mouseButton);
                break;
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void handlePickerTabClick(int mouseX, int mouseY, int mouseButton) {
        float yPos = getY() + TAB_HEIGHT + PADDING;

        if (mouseX >= getX() + PADDING && mouseX <= getX() + PADDING + COLOR_FIELD_SIZE &&
                mouseY >= yPos && mouseY <= yPos + COLOR_FIELD_SIZE) {
            draggingColorField = true;
            updateSatVal(mouseX, mouseY);
            return;
        }

        float hexX = getX() + PADDING * 2 + COLOR_FIELD_SIZE + 3;
        float hexY = yPos + 25;
        if (mouseX >= hexX && mouseX <= hexX + 50 && mouseY >= hexY && mouseY <= hexY + 8) {
            hexInputMode = 1;
            hexInput = new StringBuilder(String.format("%02X%02X%02X%02X",
                    component.property.getValue().getRed(),
                    component.property.getValue().getGreen(),
                    component.property.getValue().getBlue(),
                    alpha));
            return;
        } else {
            hexInputMode = 0;
        }

        yPos += COLOR_FIELD_SIZE + PADDING;
        float sliderWidth = getWidth() - (PADDING * 2);

        if (mouseX >= getX() + PADDING && mouseX <= getX() + PADDING + sliderWidth &&
                mouseY >= yPos && mouseY <= yPos + SLIDER_HEIGHT) {
            draggingHue = true;
            updateHue(mouseX);
            return;
        }
        yPos += SLIDER_HEIGHT + PADDING;

        if (mouseX >= getX() + PADDING && mouseX <= getX() + PADDING + sliderWidth &&
                mouseY >= yPos && mouseY <= yPos + SLIDER_HEIGHT) {
            draggingAlpha = true;
            updateAlpha(mouseX);
        }
    }

    private void handlePresetsTabClick(int mouseX, int mouseY, int mouseButton) {
        float yPos = getY() + TAB_HEIGHT + PADDING;
        float xStart = getX() + PADDING;
        int column = 0;

        for (Color color : DEFAULT_PRESETS) {
            float presetX = xStart + (column * (PRESET_SIZE + PRESET_SPACING));
            if (presetX + PRESET_SIZE > getX() + getWidth() - PADDING) {
                column = 0;
                yPos += PRESET_SIZE + PRESET_SPACING;
                presetX = xStart;
            }
            if (mouseX >= presetX && mouseX <= presetX + PRESET_SIZE &&
                    mouseY >= yPos && mouseY <= yPos + PRESET_SIZE) {
                setColorFromPreset(color);
                return;
            }
            column++;
        }
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        draggingHue = draggingAlpha = draggingColorField = false;
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if (hexInputMode == 1) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_RETURN) {
                if (hexInput.length() == 8) {
                    try {
                        int r = Integer.parseInt(hexInput.substring(0, 2), 16);
                        int g = Integer.parseInt(hexInput.substring(2, 4), 16);
                        int b = Integer.parseInt(hexInput.substring(4, 6), 16);
                        int a = Integer.parseInt(hexInput.substring(6, 8), 16);
                        component.property.setValue(new Color(r, g, b));
                        component.property.setAlpha(a);
                        refreshValues();
                    } catch (NumberFormatException e) {
                        refreshValues();
                    }
                }
                hexInputMode = 0;
            } else if (keyCode == Keyboard.KEY_BACK && hexInput.length() > 0) {
                hexInput.deleteCharAt(hexInput.length() - 1);
            } else if (isHexChar(typedChar) && hexInput.length() < 8) {
                hexInput.append(Character.toUpperCase(typedChar));
            }
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isHexChar(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private void updateHue(int mouseX) {
        hue = MathHelper.clamp_float((mouseX - (getX() + PADDING)) / (getWidth() - (PADDING * 2)), 0, 1);
        updateColor();
    }

    private void updateSatVal(int mouseX, int mouseY) {
        float yPos = getY() + TAB_HEIGHT + PADDING;
        saturation = MathHelper.clamp_float((mouseX - (getX() + PADDING)) / COLOR_FIELD_SIZE, 0, 1);
        value = 1 - MathHelper.clamp_float((mouseY - yPos) / COLOR_FIELD_SIZE, 0, 1);
        updateColor();
    }

    private void updateAlpha(int mouseX) {
        alpha = (int) (MathHelper.clamp_float((mouseX - (getX() + PADDING)) / (getWidth() - (PADDING * 2)), 0, 1) * 255);
        component.property.setAlpha(alpha);
        component.property.setValue(FontRenderer.reAlpha(component.property.getValue(), alpha));
    }

    private void updateColor() {
        Color newColor = Color.getHSBColor(hue, saturation, value);
        component.property.setValue(newColor);
        component.property.setAlpha(alpha);
    }

    private void setColorFromPreset(Color color) {
        component.property.setValue(color);
        alpha = color.getAlpha();
        component.property.setAlpha(alpha);
        refreshValues();
    }
}