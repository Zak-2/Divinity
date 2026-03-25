package divinity.module.property.impl;

import com.google.gson.internal.Primitives;
import divinity.module.property.Property;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Supplier;

@Getter
public class NumberProperty<T extends Number> extends Property<T> {

    private final T min;
    private final T max;
    private final T inc;

    public NumberProperty(String name, T value, T min, T max, T inc) {
        super(name, value);
        this.min = min;
        this.max = max;
        this.inc = inc;
    }

    public NumberProperty(String name, T value, T min, T max, T inc, Supplier<Boolean> supplier) {
        super(name, value, supplier);
        this.min = min;
        this.max = max;
        this.inc = inc;
    }


    @Override
    public void setValue(T value) {
        super.setValue(clamp(cast((Class<T>) getValue().getClass(), roundToPlace(value.doubleValue(), 2)), min, max));
    }

    private double roundToPlace(double value, int places) {
        if (places < 0) {
            throw new IllegalArgumentException();
        }
        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.UP);
        return bd.doubleValue();
    }

    public <T extends Number> T clamp(final T value, final T min, final T max) {
        return (((Comparable) value).compareTo(min) < 0) ? min : ((((Comparable) value).compareTo(max) > 0) ? max : value);
    }

    public <T extends Number, V extends Number> T cast(Class<T> numberClass, final V value) {
        numberClass = Primitives.wrap(numberClass);
        Object casted;
        if (numberClass == Byte.class) {
            casted = value.byteValue();
        } else if (numberClass == Short.class) {
            casted = value.shortValue();
        } else if (numberClass == Integer.class) {
            casted = value.intValue();
        } else if (numberClass == Long.class) {
            casted = value.longValue();
        } else if (numberClass == Float.class) {
            casted = value.floatValue();
        } else {
            if (numberClass != Double.class) {
                throw new ClassCastException(String.format("%s cannot be casted to %s", value.getClass(), numberClass));
            }
            casted = value.doubleValue();
        }
        return (T) casted;
    }

}
