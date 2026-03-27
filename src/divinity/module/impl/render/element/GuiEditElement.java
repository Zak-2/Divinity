package divinity.module.impl.render.element;

import divinity.module.impl.render.element.core.DraggableElement;
import divinity.module.impl.render.ESP;
import divinity.module.impl.render.element.impl.ESPPreviewWidget;
import divinity.module.impl.render.element.impl.PlayerListElement;
import divinity.utils.RenderUtils;
import net.minecraft.client.gui.GuiScreen;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiEditElement extends GuiScreen {

    public static final List<DraggableElement> draggableElements = new ArrayList<>();


    public GuiEditElement() {
        draggableElements.add(new PlayerListElement(1, 85));
        draggableElements.add(new ESPPreviewWidget((ESP) ClientManager.getInstance().getModuleManager().get(ESP.class), 1, 10, 200, 250));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        RenderUtils.drawRect(0, 0, width, height, new Color(0, 0, 0, 120).getRGB());
        draggableElements.forEach(draggableElement -> draggableElement.drawScreen(mouseX, mouseY, partialTicks));
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        draggableElements.forEach(draggableElement -> draggableElement.mouseClicked(mouseX, mouseY, mouseButton));
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        draggableElements.forEach(draggableElement -> draggableElement.mouseReleased(mouseX, mouseY, state));
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() throws IOException {
        draggableElements.forEach(DraggableElement::handleMouseInput);
        super.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        draggableElements.forEach(draggableElement -> draggableElement.keyTyped(typedChar, keyCode));
        super.keyTyped(typedChar, keyCode);
    }
}