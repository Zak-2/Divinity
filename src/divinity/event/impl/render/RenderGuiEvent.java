package divinity.event.impl.render;

import divinity.event.base.Event;
import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;

@Getter
public class RenderGuiEvent extends Event {

    private final int width;
    private final int height;
    private final float partialTicks;
    private final ScaledResolution sr;

    public RenderGuiEvent(ScaledResolution sr, float partialTicks) {
        this.sr = sr;
        this.partialTicks = partialTicks;
        this.width = getSr().getScaledWidth();
        this.height = getSr().getScaledHeight();
    }
}