package divinity.module.impl.render;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.render.RenderGuiEvent;
import divinity.gui.click.Clickable;
import divinity.gui.click.UiSection;
import divinity.gui.tab.TabGui;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.impl.render.element.GuiEditElement;
import divinity.module.impl.render.element.core.DraggableElement;
import divinity.module.impl.render.element.impl.PlayerListElement;
import divinity.module.property.impl.ColorProperty;
import divinity.module.property.impl.ModeProperty;
import divinity.module.property.impl.MultiSelectProperty;
import divinity.utils.ColorUtils;
import divinity.utils.RenderUtils;
import divinity.utils.ShaderUtils;
import divinity.utils.font.FontRenderer;
import divinity.utils.font.Fonts;
import hogoshi.util.Easings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.EnumChatFormatting;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class HUD extends Module {

    private final TabGui tabGui = new TabGui(this);
    private final List<Module> animatedModules = new ArrayList<>();
    private final ModeProperty layout = new ModeProperty("Layout", "Classic", "Split", "Classic");
    //private final BooleanProperty tabGuiEnabled = new BooleanProperty("TabGUI", true);
    private final ColorProperty primaryColor = new ColorProperty("Primary", getMainColor());
    private final ColorProperty secondaryColor = new ColorProperty("Secondary", getSecondaryColor());
    private final MultiSelectProperty widgets = new MultiSelectProperty("Widgets", new String[]{"Player List"}, new String[]{"Player List"});
    private final MultiSelectProperty listSettings = new MultiSelectProperty("Arraylist Settings", new String[]{"Hide Visuals", "Background", "Encase"}, new String[]{"Background", "Encase"});
    public int totalHeight;

    public HUD(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        setState(true);
        addProperty(layout, primaryColor, secondaryColor, listSettings, widgets);
    }

    @EventListener
    public void onRender(RenderGuiEvent event) {
        syncColors();

        drawWatermark(event);

        float usedHeight = drawModuleList(event);

        totalHeight = (int) usedHeight;

        drawDraggables();

        //if (tabGuiEnabled.getValue()) tabGui.drawTabGUI();
    }

    public boolean isSplitLayout() {
        return layout.isMode("Split");
    }

    private void syncColors() {
        ClientManager cm = ClientManager.getInstance();
        if (!cm.getMainColor().equals(primaryColor.getValue())) cm.setMainColor(primaryColor.getValue());
        if (!cm.getSecondaryColor().equals(secondaryColor.getValue())) cm.setSecondaryColor(secondaryColor.getValue());
    }

    private void drawWatermark(RenderGuiEvent event) {
        ClientManager cm = ClientManager.getInstance();
        String name = cm.getName();
        String sub = cm.getVersion() + " " + cm.getBuildType().name().toLowerCase();

        FontRenderer title = Fonts.INTER_MEDIUM.get(18);
        FontRenderer small = Fonts.INTER_MEDIUM.get(12);

        float pad = 7f;
        float iconS = 10f;
        float gap = 7f;

        float textW = Math.max(title.getStringWidth(name), small.getStringWidth(sub));
        float w = pad + iconS + gap + textW + pad;
        float h = pad + title.getHeight() + 1f + small.getHeight() + pad - 1f;

        float margin = 8f;
        float x = isSplitLayout() ? event.getWidth() - margin - w : margin;
        float y = margin;

        int bg = RenderUtils.reAlpha(new Color(9, 9, 14).getRGB(), 0.72f);
        int border = RenderUtils.reAlpha(0xFFFFFFFF, 0.10f);
        int inner = RenderUtils.reAlpha(new Color(13, 13, 18).getRGB(), 0.55f);

        ShaderUtils.drawRoundRect(x - 0.5f, y - 0.5f, x + w + 0.5f, y + h + 0.5f, 10.5f, border);
        ShaderUtils.drawRoundRect(x, y, x + w, y + h, 10f, bg);
        ShaderUtils.drawRoundRect(x + 1f, y + 1f, x + w - 1f, y + h - 1f, 9f, inner);

        float iconX = x + pad;
        float iconY = y + (h - iconS) / 2f;

        ShaderUtils.drawRoundRect(iconX, iconY, iconX + iconS, iconY + iconS, 3.5f, RenderUtils.reAlpha(new Color(16, 16, 22).getRGB(), 0.9f));
        ShaderUtils.drawGradientRect(iconX + 1.5f, iconY + 1.5f, iconX + iconS - 1.5f, iconY + iconS - 1.5f,
                RenderUtils.reAlpha(primaryColor.getValue().getRGB(), 0.9f),
                RenderUtils.reAlpha(secondaryColor.getValue().getRGB(), 0.9f),
                false
        );

        float tx = iconX + iconS + gap;
        float ty = y + pad - 1f;

        title.drawStringGradient(name, tx, ty, primaryColor.getValue().getRGB(), secondaryColor.getValue().getRGB());
        small.drawStringWithShadow(sub, tx, ty + title.getHeight() + 1.5f, RenderUtils.reAlpha(new Color(190, 190, 195).getRGB(), 0.75f));
    }

    private void updateAnimatedModules() {
        for (Module m : ClientManager.getInstance().getModuleManager().getModules()) {
            if (listSettings.isSelected("Hide Visuals") && m.getCategory() == Category.RENDER) {
                continue;
            }
            if (m.isState() && !animatedModules.contains(m)) animatedModules.add(m);
        }

        animatedModules.removeIf(m -> {
            boolean xDone = m.getModuleAnimation().getXPosAnimation().isDone();
            return !m.isState() && xDone;
        });
    }

    private float drawModuleList(RenderGuiEvent event) {
        int SPACING = 3;
        float half = SPACING / 2f;
        int fontH = getFont().getHeight();
        int screenW = event.getWidth();

        updateAnimatedModules();

        List<Module> modulesToRender = new ArrayList<>();

        for (Module m : animatedModules) {
            if (listSettings.isSelected("Hide Visuals") && m.getCategory() == Category.RENDER) {
                continue;
            }
            modulesToRender.add(m);
        }

        modulesToRender.sort((m1, m2) -> Float.compare(
                getFont().getStringWidth(getModuleString(m2)),
                getFont().getStringWidth(getModuleString(m1))
        ));

        for (int i = 0; i < modulesToRender.size(); i++) {
            Module module = modulesToRender.get(i);
            String text = getModuleString(module);
            float textW = getFont().getStringWidth(text);

            float targetX;
            if (isSplitLayout()) {
                // Left side alignment
                targetX = module.isState() ? 1 : -textW - 5;
            } else {
                // Right side alignment
                targetX = module.isState() ? screenW - 1 - textW : screenW + 5;
            }

            float targetY = half + i * (fontH + SPACING);

            module.animate(targetX, targetY, 0.15f, Easings.CIRC_OUT);

            float xPos = (float) module.getModuleAnimation().getXPosAnimation().getValue();
            float yPos = (float) module.getModuleAnimation().getYPosAnimation().getValue();

            float left = xPos - half;
            float right = xPos + textW + half;
            float top = yPos - half;
            float bottom = yPos + fontH + half;

            if (listSettings.isSelected("Background"))
                RenderUtils.drawRect(left, top, right, bottom, new Color(0, 0, 0, 50).getRGB());

            getFont().drawStringWithShadow(text, xPos, yPos, getColor(i, modulesToRender.size()));

            if (listSettings.isSelected("Encase")) {
                int color = getColor(i, modulesToRender.size());

                if (isSplitLayout()) {
                    // Left side - border on right edge
                    RenderUtils.drawRect(right - 1f, top, right, bottom, color);
                } else {
                    // Right side - border on left edge
                    RenderUtils.drawRect(left, top, left + 1f, bottom, color);
                }

                float nextEdge;

                if (i < modulesToRender.size() - 1 && modulesToRender.get(i + 1).isState()) {
                    float nextXPos = (float) modulesToRender.get(i + 1).getModuleAnimation().getXPosAnimation().getValue();
                    float nextTextW = getFont().getStringWidth(getModuleString(modulesToRender.get(i + 1)));

                    if (isSplitLayout()) {
                        nextEdge = nextXPos + nextTextW + half;
                    } else {
                        nextEdge = nextXPos - half;
                    }
                } else if (i == modulesToRender.size() - 1 && module.isState()) {
                    nextEdge = isSplitLayout() ? left : right;
                } else {
                    nextEdge = isSplitLayout() ? right : left;
                }

                if (isSplitLayout()) {
                    if (nextEdge < right) RenderUtils.drawRect(nextEdge, bottom, right, bottom + 1f, color);
                } else {
                    if (nextEdge > left) RenderUtils.drawRect(left, bottom, nextEdge, bottom + 1f, color);
                }
            }
        }

        return half + modulesToRender.size() * (fontH + SPACING) - SPACING / 2f;
    }

    private void drawDraggables() {
        for (DraggableElement e : GuiEditElement.draggableElements) {
            if (e instanceof PlayerListElement) {
                e.setEnabled(widgets.isSelected("Player List"));
            }
        }


        boolean isChatOpen = Minecraft.getMinecraft().currentScreen instanceof GuiChat;
        boolean isEditing = (Minecraft.getMinecraft().currentScreen instanceof Clickable && ClientManager.getInstance().getClickable().section == UiSection.Widgets)
                || Minecraft.getMinecraft().currentScreen instanceof GuiEditElement;

        if (!isChatOpen && !isEditing) {
            for (DraggableElement e : GuiEditElement.draggableElements) {
                if (e.isEnabled()) {
                    e.drawScreen(0, 0, 0);
                }
            }
        }
    }

    private String getModuleString(Module module) {
        String suffix = module.getSuffix();
        return suffix.isEmpty() ? module.getName() : module.getName() + " " + EnumChatFormatting.GRAY + suffix;
    }

    private FontRenderer getFont() {
        return Fonts.INTER_MEDIUM.get(18);
    }

    private int getColor(int index, int total) {
        double phase = (System.currentTimeMillis() / 10.0) / 100.0 + (double) index / total;
        return ColorUtils.gradientWave(primaryColor.getValue(), secondaryColor.getValue(), Math.abs(phase)).getRGB();
    }

    private Color getMainColor() {
        return ClientManager.getInstance().getMainColor();
    }

    private Color getSecondaryColor() {
        return ClientManager.getInstance().getSecondaryColor();
    }
}