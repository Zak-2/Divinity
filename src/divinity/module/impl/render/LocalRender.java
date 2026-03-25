package divinity.module.impl.render;

import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;

public class LocalRender extends Module {
    private final BooleanProperty noWalls = new BooleanProperty("No Walls", true);

    public LocalRender(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(noWalls);
    }

    public boolean isNoWalls() {
        return isState() && noWalls.getValue();
    }
}