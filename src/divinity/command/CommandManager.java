package divinity.command;

import divinity.Core;
import divinity.command.impl.Bind;
import divinity.command.impl.Config;
import divinity.command.impl.Help;
import divinity.utils.ClientUtils;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

public class CommandManager implements Core {
    @Getter
    private final HashMap<String[], Command> commands;
    private final String prefix;

    public CommandManager() {
        commands = new HashMap();
        prefix = "-";
    }

    @Override
    public void initialize() {
        commands.put(new String[]{"help", "h"}, new Help());
        commands.put(new String[]{"bind", "b", "set"}, new Bind());
        commands.put(new String[]{"config", "c", "configs"}, new Config());
    }

    @Override
    public void shutdown() {

    }

    public boolean processCommand(String rawMessage) {
        if (!rawMessage.startsWith(prefix)) {
            return false;
        }
        boolean safe = rawMessage.split(prefix).length > 1;
        if (safe) {
            String splitted = rawMessage.split(prefix)[1];
            String[] args = splitted.split(" ");
            Command command = getCommand(args[0]);
            if (command != null) {
                if (!command.run(args)) {
                    ClientUtils.sendChatMessage(command.usage());
                }
            } else {
                ClientUtils.sendChatMessage("Try -help.");
            }
        } else {
            ClientUtils.sendChatMessage("Try -help.");
        }
        return true;
    }

    private Command getCommand(String name) {
        for (Map.Entry entry : commands.entrySet()) {
            String[] key = (String[]) entry.getKey();
            for (String s : key) {
                if (s.equalsIgnoreCase(name)) {
                    return (Command) entry.getValue();
                }
            }
        }
        return null;
    }
}
