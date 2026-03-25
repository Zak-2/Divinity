package divinity.module.property.impl;


import divinity.module.property.Property;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class MultiSelectProperty extends Property<String[]> {

    private final List<String> selected;

    public MultiSelectProperty(String name, String[] value, String[] selected) {
        super(name, value);
        this.selected = new ArrayList<>(Arrays.asList(selected));
    }

    public MultiSelectProperty(String name, String[] value, Supplier<Boolean> supplier, String[] selected) {
        super(name, value, supplier);
        this.selected = new ArrayList<>(Arrays.asList(selected));
    }

    public boolean isSelected(String name) {
        return contains(selected, name);
    }

    public void activate(String name) {
        if (contains(Arrays.asList(getValue()), name) && !isSelected(name)) {
            selected.add(name);
        }
    }

    public void deActivate(String name) {
        selected.removeIf(s -> s.equalsIgnoreCase(name));
    }

    public void toggle(String name) {
        if (isSelected(name)) {
            deActivate(name);
        } else {
            activate(name);
        }
    }

    private boolean contains(List<String> list, String target) {
        return list.stream().anyMatch(s -> s.equalsIgnoreCase(target));
    }
}