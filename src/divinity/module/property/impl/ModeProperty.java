package divinity.module.property.impl;

import divinity.module.property.Property;
import lombok.Getter;

import java.util.Arrays;
import java.util.function.Supplier;

@Getter
public class ModeProperty extends Property<String> {

    private final String[] modes;

    public ModeProperty(String name, String value, String... modes) {
        super(name, value);
        this.modes = modes;
    }

    public ModeProperty(String name, String value, Supplier<Boolean> supplier, String... modes) {
        super(name, value, supplier);
        this.modes = modes;
    }

    public boolean isMode(String name) {
        boolean isValid = Arrays.asList(modes).contains(name);
        return isValid && getValue().equalsIgnoreCase(name);
    }

    public void next() {
        int index = getCurrentIndex();
        if (index != -1) {
            setValue(modes[(index + 1) % modes.length]);
        }
    }

    public void previous() {
        int index = getCurrentIndex();
        if (index != -1) {
            setValue(modes[(index - 1 + modes.length) % modes.length]);
        }
    }

    private int getCurrentIndex() {
        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(getValue())) {
                return i;
            }
        }
        return -1;
    }
}
