package divinity.module.impl.movement;

import divinity.event.base.EventListener;
import divinity.event.impl.input.MoveInputOptionsEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.utils.player.LocalPlayerUtils;

public class Speed extends Module {

    // Crazy Speed I Hate Black People
    public Speed(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onEvent(MoveInputOptionsEvent event) {
        setSuffix("Sprint Jump");

        if (mc.thePlayer.onGround && LocalPlayerUtils.isMoving()) {
            event.setJumping(true);
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}