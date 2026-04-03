package divinity.handler;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.GameTickEvent;
import divinity.event.impl.render.RenderGuiEvent;

public class UnquantifiedHandler {

    @EventListener
    public void onEvent(GameTickEvent event) {
        // Auth and licensing checks removed
    }

    @EventListener
    public void onEvent(RenderGuiEvent event) {
        // IRC status overlay removed
    }
}
