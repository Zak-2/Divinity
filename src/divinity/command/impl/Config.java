package divinity.command.impl;

import divinity.ClientManager;
import divinity.command.Command;

public class Config implements Command {


    @Override
    public boolean run(String[] args) {
        if (args.length == 3) {
            switch (args[1]) {
                case "load":
                    ClientManager.getInstance().getModuleManager().load("configs", args[2]);
                    ClientManager.getInstance().getNotificationManager().addNotification("Config loaded", args[2] + " has been loaded", 3000);
                    break;
                case "save":
                    ClientManager.getInstance().getModuleManager().save("configs", args[2]);
                    ClientManager.getInstance().getNotificationManager().addNotification("Config saved", args[2] + " has been saved", 3000);
                    break;
                default:
                    break;
            }
            return true;
        }
        return false;
    }

    @Override
    public String usage() {
        return "config: -config [load/save] [name]";
    }
}
