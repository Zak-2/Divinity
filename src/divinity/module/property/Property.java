package divinity.module.property;

import java.util.function.Supplier;

public class Property<T> {

    private final String name;
    private final Supplier<Boolean> supplier;
    private T value;

    public Property(String name, T value) {
        this.name = name;
        this.value = value;
        this.supplier = () -> true;
    }

    public Property(String name, T value, Supplier<Boolean> supplier) {
        this.name = name;
        this.value = value;
        this.supplier = supplier;
    }

    public String getName() {
        return name;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public boolean isVisible() {
        return supplier.get();
    }

}