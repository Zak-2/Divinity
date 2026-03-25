package divinity.event.impl.render;

import divinity.event.base.Event;
import lombok.Getter;
import net.minecraft.client.gui.ScaledResolution;

@Getter
public class RenderWorldEvent extends Event {
    private final float partialTicks;
    private final ScaledResolution scaledResolution;

    public RenderWorldEvent(float partialTicks, ScaledResolution scaledResolution) {
        this.partialTicks = partialTicks;
        this.scaledResolution = scaledResolution;
    }
}
