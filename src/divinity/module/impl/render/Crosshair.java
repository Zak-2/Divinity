package divinity.module.impl.render;


import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.render.RenderGuiEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.ColorProperty;
import divinity.module.property.impl.ModeProperty;
import divinity.utils.RenderUtils;

import java.awt.*;

public class Crosshair extends Module {

    public final BooleanProperty hideCrosshair = new BooleanProperty("Hide Crosshair", false);
    private final ModeProperty crosshairType = new ModeProperty("Crosshair Type", "T", "T", "Cross");
    private final ColorProperty colorProperty = new ColorProperty("Color", ClientManager.getInstance().getMainColor());

    public Crosshair(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(hideCrosshair, crosshairType, colorProperty);
    }

    @EventListener
    public void onRender(RenderGuiEvent e) {
        if (hideCrosshair.getValue()) return;

        int screenWidth = e.getWidth() / 2;
        int screenHeight = e.getHeight() / 2;
        int size = 3;
        Color color = colorProperty.getValue();

        switch (crosshairType.getValue()) {
            case "T":
                drawT(screenWidth, screenHeight, size, color);
                break;
            case "Cross":
                drawCross(screenWidth, screenHeight, size, color);
                break;
        }
    }

    private void drawT(int x, int y, int size, Color color) {
        RenderUtils.drawRect(x - size, y, x + size, y + 1, color.getRGB());
        RenderUtils.drawRect(x - .5f, y, x + .5f, y + size, color.getRGB());
    }

    private void drawCross(int x, int y, int size, Color color) {
        RenderUtils.drawRect(x - size, y, x + size, y + 1, color.getRGB());
        RenderUtils.drawRect(x - .5f, y - size, x + .5f, y + size + 1, color.getRGB());
    }

    @Override
    public void onEnable() {
        // TODO FIX ME
        if (ClientManager.getInstance().getModuleManager().get(ESP.class).isState()) {
            ClientManager.getInstance().getModuleManager().get(ESP.class).toggle();
            ClientManager.getInstance().getModuleManager().get(ESP.class).toggle();
        }
        super.onEnable();
    }
}


