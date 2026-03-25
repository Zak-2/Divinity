package divinity.module.property.impl;


import divinity.module.property.Property;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;
import java.util.function.Supplier;

@Setter
@Getter
public final class ColorProperty extends Property<Color> {

    private float hue;
    private float saturation;
    private float brightness;
    private int alpha;

    public ColorProperty(String name, Color value) {
        super(name, value);
    }

    public ColorProperty(String name, Color value, Supplier<Boolean> supplier) {
        super(name, value, supplier);
    }

    @Override
    public Color getValue() {
        return super.getValue();
    }

    @Override
    public void setValue(Color value) {
        super.setValue(value);
    }

    public void setRGB(int rgb) {
        setValue(new Color(rgb));
    }

}
