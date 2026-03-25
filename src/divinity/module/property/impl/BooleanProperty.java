package divinity.module.property.impl;


import divinity.module.property.Property;

import java.util.function.Supplier;

public class BooleanProperty extends Property<Boolean> {

    public BooleanProperty(String name, Boolean value) {
        super(name, value);
    }

    public BooleanProperty(String name, Boolean value, Supplier<Boolean> supplier) {
        super(name, value, supplier);
    }

    public void toggle() {
        setValue(!getValue());
    }

}