package divinity.module.impl.movement;

import divinity.event.base.EventListener;
import divinity.event.impl.input.MoveInputOptionsEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.utils.move.MoveUtils;
import net.minecraft.client.settings.KeyBinding;

public class Sprint extends Module {

    public Sprint(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onEvent(MoveInputOptionsEvent event) {
        if (MoveUtils.canSprint(mc.thePlayer) && !mc.thePlayer.isSprinting()) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
    }
}