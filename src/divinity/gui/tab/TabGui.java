package divinity.gui.tab;

import divinity.ClientManager;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.impl.render.HUD;
import divinity.module.property.Property;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.ModeProperty;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.FontRenderer;
import divinity.utils.font.Fonts;
import hogoshi.Animation;
import hogoshi.util.Easings;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.lang3.text.WordUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public final class TabGui {

    private static final float MARGIN = 8f;
    private static final float PANEL_RADIUS = 10f;
    private static final float PANEL_PAD = 7f;
    private static final float HEADER_H = 18f;
    private static final float ROW_H = 14f;
    private static final float PROP_ROW_H = 16f;
    private static final float GAP = 7f;

    private static TabGui tabUI;
    @Getter
    private final HUD hud;
    private final Animation categoryYAnimation;
    private final Animation moduleYAnimation;
    private final Animation moduleWindowAnimation;
    private final Animation propertyYAnimation;
    private final Animation propertyWindowAnimation;
    private final Animation dimAnimation;
    private final List<PropertyComponent> propertyComponents;
    private Section section;
    private int categoryIndex, moduleIndex, propertyIndex;
    private long lastKeyPressTime;

    public TabGui(HUD hud) {
        tabUI = this;
        this.hud = hud;
        this.section = Section.CATEGORIES;
        this.categoryIndex = 0;
        this.moduleIndex = 0;
        this.propertyIndex = 0;
        this.categoryYAnimation = new Animation();
        this.moduleYAnimation = new Animation();
        this.moduleWindowAnimation = new Animation();
        this.propertyYAnimation = new Animation();
        this.propertyWindowAnimation = new Animation();
        this.dimAnimation = new Animation();
        this.propertyComponents = new ArrayList<>();
        this.categoryYAnimation.animate(0, 0, Easings.SLIDE, true);
        this.moduleYAnimation.animate(0, 0, Easings.SLIDE, true);
        this.moduleWindowAnimation.animate(-150, 0, Easings.SLIDE, true);
        this.propertyYAnimation.animate(0, 0, Easings.SLIDE, true);
        this.propertyWindowAnimation.animate(-150, 0, Easings.SLIDE, true);
        this.dimAnimation.animate(0.6f, 0, Easings.SLIDE, true);
        this.lastKeyPressTime = System.currentTimeMillis();
    }

    public static TabGui getInstance() {
        return tabUI;
    }

    public void drawTabGUI() {
        categoryYAnimation.update();
        moduleYAnimation.update();
        moduleWindowAnimation.update();
        propertyYAnimation.update();
        propertyWindowAnimation.update();
        dimAnimation.update();

        float alpha = (float) dimAnimation.getValue();
        int border = RenderUtils.reAlpha(0xFFFFFFFF, 0.08f * alpha);
        int bg = RenderUtils.reAlpha(new Color(9, 9, 14).getRGB(), 0.65f * alpha);
        int bg2 = RenderUtils.reAlpha(new Color(13, 13, 18).getRGB(), 0.55f * alpha);

        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        float screenW = sr.getScaledWidth();

        FontRenderer titleFont = Fonts.INTER_MEDIUM.get(15);
        FontRenderer rowFont = Fonts.INTER_MEDIUM.get(16);

        float catW = 92f;
        for (Category value : Category.values()) {
            catW = Math.max(catW, rowFont.getStringWidth(WordUtils.capitalizeFully(value.name())) + PANEL_PAD * 2 + 12);
        }
        float catH = HEADER_H + 4f + Category.values().length * ROW_H + PANEL_PAD;

        float modW = 0f;
        float modH = 0f;
        List<Module> modules = null;
        if (section == Section.MODULES || section == Section.PROPERTIES) {
            modules = getModules(Category.values()[categoryIndex]);
            modW = 80f;
            for (Module module : modules) {
                modW = Math.max(modW, rowFont.getStringWidth(module.getName()) + PANEL_PAD * 2 + 22);
            }
            modH = HEADER_H + 4f + modules.size() * ROW_H + PANEL_PAD;
        }

        float propW = 0f;
        float propH = 0f;
        List<Property> properties = null;
        if (section == Section.PROPERTIES && modules != null && !modules.isEmpty()) {
            Module selectedModule = modules.get(moduleIndex);
            properties = getSupportedProperties(selectedModule);
            propW = 120f;
            for (Property property : properties) {
                float tw;
                if (property instanceof ModeProperty) {
                    tw = Fonts.INTER_MEDIUM.get(14).getStringWidth(property.getName()) + Fonts.INTER_MEDIUM.get(13).getStringWidth(getModeDisplayText((ModeProperty) property)) + PANEL_PAD * 2 + 44;
                } else {
                    tw = Fonts.INTER_MEDIUM.get(14).getStringWidth(property.getName()) + PANEL_PAD * 2 + 62;
                }
                propW = Math.max(propW, tw);
            }
            propH = HEADER_H + 4f + properties.size() * PROP_ROW_H + PANEL_PAD;
        }

        float totalW = catW;
        if (section == Section.MODULES || section == Section.PROPERTIES) totalW += GAP + modW;
        if (section == Section.PROPERTIES) totalW += GAP + propW;

        float baseX = hud != null && hud.isSplitLayout() ? (screenW - MARGIN - totalW) : MARGIN;
        float baseY = MARGIN;

        ShaderUtils.drawRoundRect(baseX - 0.5f, baseY - 0.5f, baseX + catW + 0.5f, baseY + catH + 0.5f, PANEL_RADIUS + 0.5f, border);
        ShaderUtils.drawRoundRect(baseX, baseY, baseX + catW, baseY + catH, PANEL_RADIUS, bg);
        ShaderUtils.drawRoundRect(baseX + 1f, baseY + 1f, baseX + catW - 1f, baseY + catH - 1f, PANEL_RADIUS - 1f, bg2);
        ShaderUtils.drawGradientRect(baseX + 10, baseY + HEADER_H - 2, baseX + catW - 10, baseY + HEADER_H - 1,
                RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.9f * alpha),
                RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.9f * alpha),
                true
        );

        titleFont.drawStringGradient("NAV", baseX + PANEL_PAD, baseY + 6, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());

        float catListY = baseY + HEADER_H + 4f;
        float catSelY = catListY + (float) categoryYAnimation.getValue();
        ShaderUtils.drawRoundRect(baseX + 4, catSelY + 0.5f, baseX + catW - 4, catSelY + ROW_H - 0.5f, 8f, RenderUtils.reAlpha(0xFFFFFFFF, 0.08f * alpha));
        ShaderUtils.drawGradientRect(baseX + 5, catSelY + 3, baseX + 7, catSelY + ROW_H - 3,
                RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.95f * alpha),
                RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.95f * alpha),
                false
        );

        float catTextY = catListY + (ROW_H - rowFont.getHeight()) / 2f;
        for (int i = 0; i < Category.values().length; i++) {
            Category value = Category.values()[i];
            rowFont.drawStringWithShadow(WordUtils.capitalizeFully(value.name()), baseX + PANEL_PAD + 6, catTextY + i * ROW_H, RenderUtils.reAlpha(-1, alpha));
        }

        if (section == Section.MODULES || section == Section.PROPERTIES) {
            if (modules == null) modules = getModules(Category.values()[categoryIndex]);

            float modX = baseX + catW + GAP + (float) moduleWindowAnimation.getValue();
            ShaderUtils.drawRoundRect(modX - 0.5f, baseY - 0.5f, modX + modW + 0.5f, baseY + modH + 0.5f, PANEL_RADIUS + 0.5f, border);
            ShaderUtils.drawRoundRect(modX, baseY, modX + modW, baseY + modH, PANEL_RADIUS, bg);
            ShaderUtils.drawRoundRect(modX + 1f, baseY + 1f, modX + modW - 1f, baseY + modH - 1f, PANEL_RADIUS - 1f, bg2);
            ShaderUtils.drawGradientRect(modX + 10, baseY + HEADER_H - 2, modX + modW - 10, baseY + HEADER_H - 1,
                    RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.9f * alpha),
                    RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.9f * alpha),
                    true
            );

            titleFont.drawStringWithShadow("MODULES", modX + PANEL_PAD, baseY + 6, RenderUtils.reAlpha(-1, alpha));

            float modListY = baseY + HEADER_H + 4f;
            float modSelY = modListY + (float) moduleYAnimation.getValue();
            ShaderUtils.drawRoundRect(modX + 4, modSelY + 0.5f, modX + modW - 4, modSelY + ROW_H - 0.5f, 8f, RenderUtils.reAlpha(0xFFFFFFFF, 0.08f * alpha));
            ShaderUtils.drawGradientRect(modX + 5, modSelY + 3, modX + 7, modSelY + ROW_H - 3,
                    RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.95f * alpha),
                    RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.95f * alpha),
                    false
            );

            float modTextY = modListY + (ROW_H - rowFont.getHeight()) / 2f;
            for (int i = 0; i < modules.size(); i++) {
                Module module = modules.get(i);
                int textCol = module.isState() ? RenderUtils.reAlpha(-1, alpha) : RenderUtils.reAlpha(new Color(170, 170, 170).getRGB(), alpha);
                rowFont.drawStringWithShadow(module.getName(), modX + PANEL_PAD + 6, modTextY + i * ROW_H, textCol);
                int dotCol = module.isState() ? RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.95f * alpha) : RenderUtils.reAlpha(new Color(30, 30, 40).getRGB(), 0.9f * alpha);
                float rowY = modListY + i * ROW_H;
                ShaderUtils.drawCircle(modX + modW - PANEL_PAD - 7, rowY + ROW_H / 2f, 2.8f, dotCol);
            }

            if (section == Section.PROPERTIES) {
                Module selectedModule = modules.get(moduleIndex);
                if (properties == null) properties = getSupportedProperties(selectedModule);

                float propX = modX + modW + GAP + (float) propertyWindowAnimation.getValue();
                ShaderUtils.drawRoundRect(propX - 0.5f, baseY - 0.5f, propX + propW + 0.5f, baseY + propH + 0.5f, PANEL_RADIUS + 0.5f, border);
                ShaderUtils.drawRoundRect(propX, baseY, propX + propW, baseY + propH, PANEL_RADIUS, bg);
                ShaderUtils.drawRoundRect(propX + 1f, baseY + 1f, propX + propW - 1f, baseY + propH - 1f, PANEL_RADIUS - 1f, bg2);
                ShaderUtils.drawGradientRect(propX + 10, baseY + HEADER_H - 2, propX + propW - 10, baseY + HEADER_H - 1,
                        RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.9f * alpha),
                        RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.9f * alpha),
                        true
                );

                titleFont.drawStringWithShadow("SETTINGS", propX + PANEL_PAD, baseY + 6, RenderUtils.reAlpha(-1, alpha));

                float propListY = baseY + HEADER_H + 4f;
                float propSelY = propListY + (float) propertyYAnimation.getValue();
                ShaderUtils.drawRoundRect(propX + 4, propSelY + 0.5f, propX + propW - 4, propSelY + PROP_ROW_H - 0.5f, 8f, RenderUtils.reAlpha(0xFFFFFFFF, 0.08f * alpha));
                ShaderUtils.drawGradientRect(propX + 5, propSelY + 3, propX + 7, propSelY + PROP_ROW_H - 3,
                        RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.95f * alpha),
                        RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.95f * alpha),
                        false
                );

                for (int i = 0; i < propertyComponents.size(); i++) {
                    PropertyComponent component = propertyComponents.get(i);
                    component.update();
                    component.draw(propX + 8, propListY + i * PROP_ROW_H, propW - 16, 0, 0, alpha);
                }
            }
        }
    }

    private String getModeDisplayText(ModeProperty property) {
        int all = property.getModes().length;
        return ModePropertyComponent.getNumber(property.getValue(), property.getModes()) + "/" + all + " " + property.getValue();
    }

    public void keyListener(int key) {
        if (key == Keyboard.KEY_UP || key == Keyboard.KEY_DOWN || key == Keyboard.KEY_LEFT || key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_RETURN) {
            lastKeyPressTime = System.currentTimeMillis();
            dimAnimation.animate(0.6f, 0.5, Easings.SLIDE, true);
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastKeyPressTime > 3000) {
            dimAnimation.animate(0.4f, 0.5, Easings.SLIDE, true);
        }

        if (section == Section.CATEGORIES) {
            int prevCategoryIndex = categoryIndex;
            if (key == Keyboard.KEY_DOWN) {
                if (categoryIndex < Category.values().length - 1) categoryIndex++;
                else categoryIndex = 0;
            } else if (key == Keyboard.KEY_UP) {
                if (categoryIndex == 0) categoryIndex = Category.values().length - 1;
                else categoryIndex--;
            }
            if (prevCategoryIndex != categoryIndex) {
                float targetY = ROW_H * categoryIndex;
                categoryYAnimation.animate(targetY, 0.15, Easings.SLIDE, true);
            }
            if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_RETURN) {
                if (!getModules(Category.values()[categoryIndex]).isEmpty()) {
                    section = Section.MODULES;
                    moduleIndex = 0;
                    moduleYAnimation.animate(0, 0.15, Easings.SLIDE, true);
                    moduleWindowAnimation.animate(0, 0.15, Easings.SLIDE, true);
                }
            }
        } else if (section == Section.MODULES) {
            int prevModuleIndex = moduleIndex;
            if (key == Keyboard.KEY_DOWN) {
                if (moduleIndex < getModules(Category.values()[categoryIndex]).size() - 1) moduleIndex++;
                else moduleIndex = 0;
            } else if (key == Keyboard.KEY_UP) {
                if (moduleIndex == 0) moduleIndex = getModules(Category.values()[categoryIndex]).size() - 1;
                else moduleIndex--;
            } else if (key == Keyboard.KEY_LEFT) {
                moduleIndex = 0;
                section = Section.CATEGORIES;
                moduleYAnimation.animate(0, 0.15, Easings.SLIDE, true);
                moduleWindowAnimation.animate(-150, 0.15, Easings.SLIDE, true);
            } else if (key == Keyboard.KEY_RIGHT) {
                Module selectedModule = getModules(Category.values()[categoryIndex]).get(moduleIndex);
                if (!getSupportedProperties(selectedModule).isEmpty()) {
                    section = Section.PROPERTIES;
                    propertyIndex = 0;
                    propertyYAnimation.animate(0, 0.15, Easings.SLIDE, true);
                    propertyWindowAnimation.animate(0, 0.15, Easings.SLIDE, true);
                    updatePropertyComponents(selectedModule);
                }
            } else if (key == Keyboard.KEY_RETURN) {
                if (!getModules(Category.values()[categoryIndex]).get(moduleIndex).getName().equalsIgnoreCase("HUD")) {
                    if (getModules(Category.values()[categoryIndex]).get(moduleIndex).isState()) {
                        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.5F));
                    } else {
                        Minecraft.getMinecraft().getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 2.0F));
                    }
                    getModules(Category.values()[categoryIndex]).get(moduleIndex).toggle();
                }
            }
            if (prevModuleIndex != moduleIndex) {
                float targetY = ROW_H * moduleIndex;
                moduleYAnimation.animate(targetY, 0.15, Easings.SLIDE, true);
            }
        } else if (section == Section.PROPERTIES) {
            int prevPropertyIndex = propertyIndex;
            if (key == Keyboard.KEY_DOWN) {
                Module selectedModule = getModules(Category.values()[categoryIndex]).get(moduleIndex);
                if (propertyIndex < getSupportedProperties(selectedModule).size() - 1) propertyIndex++;
                else propertyIndex = 0;
            } else if (key == Keyboard.KEY_UP) {
                if (propertyIndex == 0) {
                    Module selectedModule = getModules(Category.values()[categoryIndex]).get(moduleIndex);
                    propertyIndex = getSupportedProperties(selectedModule).size() - 1;
                } else propertyIndex--;
            } else if (key == Keyboard.KEY_LEFT) {
                section = Section.MODULES;
                propertyIndex = 0;
                propertyYAnimation.animate(0, 0.15, Easings.SLIDE, true);
                propertyWindowAnimation.animate(-150, 0.15, Easings.SLIDE, true);
                propertyComponents.clear();
            }
            if (prevPropertyIndex != propertyIndex) {
                float targetY = PROP_ROW_H * propertyIndex;
                propertyYAnimation.animate(targetY, 0.15, Easings.SLIDE, true);
            }
            if (key == Keyboard.KEY_RIGHT || key == Keyboard.KEY_RETURN) {
                Module selectedModule = getModules(Category.values()[categoryIndex]).get(moduleIndex);
                List<Property> props = getSupportedProperties(selectedModule);
                if (propertyIndex < props.size()) {
                    Property property = props.get(propertyIndex);
                    if (property instanceof BooleanProperty) ((BooleanProperty) property).toggle();
                    else if (property instanceof ModeProperty) ((ModeProperty) property).next();
                }
            }
        }
    }

    private List<Module> getModules(Category category) {
        List<Module> moduleList = new ArrayList<>();
        for (Module module : ClientManager.getInstance().getModuleManager().getModules()) {
            if (module.getCategory() == category) moduleList.add(module);
        }
        moduleList.sort((m1, m2) -> Math.round(Fonts.INTER_MEDIUM.get(18).getStringWidth(m2.getName())) - Math.round(Fonts.INTER_MEDIUM.get(18).getStringWidth(m1.getName())));
        return moduleList;
    }

    private List<Property> getSupportedProperties(Module module) {
        List<Property> supported = new ArrayList<>();
        for (Property property : module.getProperties()) {
            if (property instanceof BooleanProperty || property instanceof ModeProperty) {
                if (property.isVisible()) supported.add(property);
            }
        }
        return supported;
    }

    private void updatePropertyComponents(Module module) {
        propertyComponents.clear();
        List<Property> properties = getSupportedProperties(module);
        for (Property property : properties) {
            if (property instanceof BooleanProperty)
                propertyComponents.add(new BooleanPropertyComponent((BooleanProperty) property));
            else if (property instanceof ModeProperty)
                propertyComponents.add(new ModePropertyComponent((ModeProperty) property));
        }
    }

    private interface PropertyComponent {
        void update();

        void draw(float x, float y, float width, int mouseX, int mouseY, float alpha);
    }

    private static class BooleanPropertyComponent implements PropertyComponent {
        private final BooleanProperty property;
        private final Animation stateAnimation;

        public BooleanPropertyComponent(BooleanProperty property) {
            this.property = property;
            this.stateAnimation = new Animation();
            this.stateAnimation.animate(property.getValue() ? 1 : 0, 0, Easings.SLIDE, true);
        }

        @Override
        public void update() {
            stateAnimation.update();
            float target = property.getValue() ? 1 : 0;
            if (stateAnimation.getValue() != target) stateAnimation.animate(target, 0.15, Easings.SLIDE, true);
        }

        @Override
        public void draw(float x, float y, float width, int mouseX, int mouseY, float alpha) {
            FontRenderer nameFont = Fonts.INTER_MEDIUM.get(14);
            float textY = y + (PROP_ROW_H - nameFont.getHeight()) / 2f;
            nameFont.drawStringWithShadow(property.getName(), x + 2, textY, RenderUtils.reAlpha(-1, alpha));

            float trackW = 22f;
            float trackH = 10f;
            float trackX = x + width - trackW - 4;
            float trackY = y + (PROP_ROW_H - trackH) / 2f;

            int trackBg = RenderUtils.reAlpha(new Color(16, 16, 22).getRGB(), 0.85f * alpha);
            int trackBorder = RenderUtils.reAlpha(0xFFFFFFFF, 0.08f * alpha);
            int knobBg = RenderUtils.reAlpha(new Color(240, 240, 240).getRGB(), 0.9f * alpha);

            ShaderUtils.drawRoundRect(trackX - 0.5f, trackY - 0.5f, trackX + trackW + 0.5f, trackY + trackH + 0.5f, 5.5f, trackBorder);
            ShaderUtils.drawRoundRect(trackX, trackY, trackX + trackW, trackY + trackH, 5f, trackBg);
            if (property.getValue()) {
                ShaderUtils.drawGradientRect(trackX + 1.5f, trackY + 1.5f, trackX + trackW - 1.5f, trackY + trackH - 1.5f,
                        RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.9f * alpha),
                        RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.9f * alpha),
                        true
                );
            }

            float anim = (float) stateAnimation.getValue();
            float knobS = 8f;
            float knobX = trackX + 1.2f + anim * (trackW - knobS - 2.4f);
            ShaderUtils.drawRoundRect(knobX, trackY + 1f, knobX + knobS, trackY + 1f + knobS, 4f, knobBg);
        }
    }

    private static class ModePropertyComponent implements PropertyComponent {
        private final ModeProperty property;
        private final Animation textAnimation;

        public ModePropertyComponent(ModeProperty property) {
            this.property = property;
            this.textAnimation = new Animation();
            this.textAnimation.animate(getDisplayTextWidth(), 0, Easings.SLIDE, true);
        }

        private static int getNumber(String name, String[] array) {
            return IntStream.range(0, array.length)
                    .filter(i -> name.equals(array[i]))
                    .map(i -> i + 1)
                    .findFirst()
                    .orElse(-1);
        }

        @Override
        public void update() {
            textAnimation.update();
            float targetWidth = getDisplayTextWidth();
            if (textAnimation.getValue() != targetWidth) textAnimation.animate(targetWidth, 0.15, Easings.SLIDE, true);
        }

        @Override
        public void draw(float x, float y, float width, int mouseX, int mouseY, float alpha) {
            FontRenderer nameFont = Fonts.INTER_MEDIUM.get(14);
            FontRenderer valueFont = Fonts.INTER_MEDIUM.get(13);

            int all = property.getModes().length;
            String displayText = getNumber(property.getValue(), property.getModes()) + "/" + all + " " + property.getValue();

            float textY = y + (PROP_ROW_H - nameFont.getHeight()) / 2f;
            nameFont.drawStringWithShadow(property.getName(), x + 2, textY, RenderUtils.reAlpha(-1, alpha));

            float textW = valueFont.getStringWidth(displayText);
            float pillW = Math.max(56f, textW + 14f);
            float pillH = 11f;
            float pillX = x + width - pillW - 4;
            float pillY = y + (PROP_ROW_H - pillH) / 2f;

            int pBg = RenderUtils.reAlpha(new Color(16, 16, 22).getRGB(), 0.85f * alpha);
            int pBorder = RenderUtils.reAlpha(0xFFFFFFFF, 0.08f * alpha);

            ShaderUtils.drawRoundRect(pillX - 0.5f, pillY - 0.5f, pillX + pillW + 0.5f, pillY + pillH + 0.5f, 6.5f, pBorder);
            ShaderUtils.drawRoundRect(pillX, pillY, pillX + pillW, pillY + pillH, 6f, pBg);
            ShaderUtils.drawGradientRect(pillX + 6, pillY + pillH - 1.5f, pillX + pillW - 6, pillY + pillH - 0.5f,
                    RenderUtils.reAlpha(ClientManager.getInstance().getMainColor().getRGB(), 0.85f * alpha),
                    RenderUtils.reAlpha(ClientManager.getInstance().getSecondaryColor().getRGB(), 0.85f * alpha),
                    true
            );

            float valY = y + (PROP_ROW_H - valueFont.getHeight()) / 2f + 0.2f;
            valueFont.drawStringWithShadow(displayText, pillX + (pillW - textW) / 2f, valY, RenderUtils.reAlpha(-1, alpha));
        }

        private float getDisplayTextWidth() {
            int all = property.getModes().length;
            String displayText = getNumber(property.getValue(), property.getModes()) + "/" + all + " " + property.getValue();
            return Fonts.INTER_MEDIUM.get(13).getStringWidth(displayText);
        }
    }
}
