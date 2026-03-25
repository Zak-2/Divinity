package divinity.module.impl.world;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.GameTickEvent;
import divinity.module.Category;
import divinity.module.Module;
import net.minecraft.item.ItemBlock;

public class FastPlace extends Module {

    public FastPlace(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onEvent(GameTickEvent event) {
        if (mc.gameSettings.keyBindUseItem.isKeyDown() && mc.thePlayer.getCurrentEquippedItem() != null && mc.thePlayer.getCurrentEquippedItem().getItem() instanceof ItemBlock) {
            if (mc.rightClickDelayTimer > 0) mc.rightClickDelayTimer = 0;
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
