package divinity.gui.click.components.module;

import divinity.gui.click.components.Component;
import divinity.module.impl.render.ESP;
import divinity.module.impl.render.element.impl.ESPPreviewWidget;
import net.minecraft.client.Minecraft;

public class ESPPreviewComponent extends Component {

    private final ESP espModule;
    private final ESPPreviewWidget previewWidget;

    public ESPPreviewComponent(ESP espModule, float x, float y, float width, float height, float gap) {
        super(x, y, width, height, gap);
        this.espModule = espModule;
        this.previewWidget = new ESPPreviewWidget(espModule);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY) {
        if (!isVisible()) return;
        previewWidget.drawScreen(mouseX, mouseY, getX() + 5, getY() + 5, getWidth() - 10, getHeight() - 10, Minecraft.getMinecraft().timer.renderPartialTicks);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (!isVisible()) return;
        previewWidget.mouseClicked(mouseX, mouseY, mouseButton, getX() + 5, getY() + 5, getWidth() - 10, getHeight() - 10);
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY, int state) {
        if (!isVisible()) return;
        previewWidget.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    public boolean isVisible() {
        return espModule.showEspPreview.getValue();
    }
}
