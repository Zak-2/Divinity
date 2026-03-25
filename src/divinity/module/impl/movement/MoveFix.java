package divinity.module.impl.movement;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.player.JumpEvent;
import divinity.event.impl.player.MoveFlyingInputEvent;
import divinity.module.Category;
import divinity.module.Module;

public class MoveFix extends Module {

    public MoveFix(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onEvent(MoveFlyingInputEvent event) {
        event.setYaw(ClientManager.getInstance().getRotationHandler().lastYaw);
    }

    @EventListener
    public void onEvent(JumpEvent event) {
        event.setYaw(ClientManager.getInstance().getRotationHandler().lastYaw);
    }
}
