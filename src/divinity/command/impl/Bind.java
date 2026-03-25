package divinity.command.impl;

import divinity.ClientManager;
import divinity.command.Command;
import divinity.module.Module;
import divinity.utils.ClientUtils;
import org.lwjgl.input.Keyboard;

import java.util.Objects;

public class Bind implements Command {


    @Override
    public boolean run(String[] args) {
        if (args.length == 3) {
            Module module = ClientManager.getInstance().getModuleManager().getWithAlias(args[1]);
            if (Objects.nonNull(module)) {
                module.setKeyBind(Keyboard.getKeyIndex(args[2].toUpperCase()));
                ClientUtils.sendChatMessage(module.getName() + " has been bound to " + args[2] + ".");
            } else {
                ClientUtils.sendChatMessage(args[1] + " not found.");
            }
            return true;
        }
        return false;
    }

    @Override
    public String usage() {
        return "bind: -bind [module] [key]";
    }
}