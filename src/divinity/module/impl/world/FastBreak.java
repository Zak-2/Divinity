package divinity.module.impl.world;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.GameTickEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.player.RotationUtils;

public class FastBreak extends Module {

    private final NumberProperty<?> breakAtPercentage = new NumberProperty<>("Break At Percent", 0.25f, 0.0f, 1.0f, 0.1f);

    public FastBreak(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(breakAtPercentage);
    }

    @EventListener
    public void onEvent(GameTickEvent event) {
        setSuffix(String.valueOf(breakAtPercentage.getValue()));
        if (mc.playerController.curBlockDamageMP > breakAtPercentage.getValue().floatValue() && (RotationUtils.isPointedBlock(mc))) mc.playerController.curBlockDamageMP = 1.f;
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
