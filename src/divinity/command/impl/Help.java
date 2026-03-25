package divinity.command.impl;

import divinity.ClientManager;
import divinity.command.Command;
import divinity.utils.ClientUtils;

public class Help implements Command {

    @Override
    public boolean run(String[] args) {
        for (Command command : ClientManager.getInstance().getCommandManager().getCommands().values()) {
            ClientUtils.sendChatMessage(command.usage());
        }
        return true;
    }

    @Override
    public String usage() {
        return "help: prints list of available commands";
    }
}
