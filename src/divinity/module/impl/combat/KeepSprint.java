package divinity.module.impl.combat;

import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.NumberProperty;

public class KeepSprint extends Module {

    public final NumberProperty<?> motionX = new NumberProperty<>("Motion X", 0.6, 0.1, 1.0, 0.1);
    public final NumberProperty<?> motionZ = new NumberProperty<>("Motion Z", 0.6, 0.1, 1.0, 0.1);
    public final BooleanProperty preventStopSprint = new BooleanProperty("Keep Sprint", true);

    public KeepSprint(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(preventStopSprint, motionX, motionZ);
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
