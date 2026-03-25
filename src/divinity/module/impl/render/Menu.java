package divinity.module.impl.render;

import divinity.ClientManager;
import divinity.module.Category;
import divinity.module.Module;
import org.lwjgl.input.Keyboard;

public class Menu extends Module {

    public Menu(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        setKeyBind(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnable() {
        mc.displayGuiScreen(ClientManager.getInstance().getClickable());
        toggle();
        super.onEnable();
    }
}
