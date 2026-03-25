package divinity.module.impl.render;

import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.ModeProperty;
import divinity.module.property.impl.NumberProperty;

public class ItemAnimations extends Module {

    public final ModeProperty blockModeProperty = new ModeProperty("Block Mode", "Tap", "OG", "Winter", "Tap");
    public NumberProperty<?> speedProperty = new NumberProperty<>("Swing Speed", 1.0, 0.5, 3.0, 0.1);

    public ItemAnimations(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(blockModeProperty, speedProperty);
    }
}
