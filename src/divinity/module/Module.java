package divinity.module;

import divinity.ClientManager;
import divinity.module.property.Property;
import hogoshi.PositionedAnimation;
import hogoshi.util.Easing;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class Module {
    protected final static Minecraft mc = Minecraft.getMinecraft();
    private final String name;
    private final String description;
    private final String[] aliases;
    private final Category category;
    private final List<Property> properties;
    private final PositionedAnimation moduleAnimation;
    private String suffix;
    private int keyBind;
    private boolean state;


    public Module(String name, String[] aliases, Category category) {
        this.name = name;
        this.aliases = aliases;
        this.description = "";
        this.suffix = "";
        this.keyBind = 0;
        this.category = category;
        properties = new ArrayList<>();
        this.moduleAnimation = new PositionedAnimation();
    }

    public void setState(boolean state) {
        this.state = state;
        if (this.state) {
            ClientManager.getInstance().getEventDispatcher().register(this);
            onEnable();
        } else {
            ClientManager.getInstance().getEventDispatcher().unregister(this);
            onDisable();
        }
    }

    public void addProperty(Property... properties) {
        this.properties.addAll(Arrays.asList(properties));
    }

    public void toggle() {
        setState(!state);
    }

    public void onEnable() {

    }

    public void onDisable() {

    }

    public void animate(float targetX, float targetY, double duration, Easing easing) {
        moduleAnimation.animate(targetX, targetY, duration, easing);
    }
}