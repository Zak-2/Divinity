package divinity.module.property.impl;


import divinity.module.property.Property;

import java.util.function.Supplier;

public class StringProperty extends Property<String> {

    public StringProperty(String name, String value) {
        super(name, value);
    }

    public StringProperty(String name, String value, Supplier<Boolean> supplier) {
        super(name, value, supplier);
    }
}
