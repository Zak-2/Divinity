package divinity.utils.server;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

public class ServerUtils {
    private final static Minecraft mc = Minecraft.getMinecraft();

    public static boolean isOnHypixel() {
        if (mc == null || mc.getCurrentServerData() == null) {
            return false;
        }

        ServerData serverData = mc.getCurrentServerData();
        String serverIP = serverData.serverIP.toLowerCase();

        return serverIP.contains("hypixel");
    }

    public static boolean isInTabList(EntityLivingBase entity) {
        for (NetworkPlayerInfo n : mc.getNetHandler().getPlayerInfoMap()) {
            if (entity.getName().equals(n.getGameProfile().getName())) {
                return true;
            }
        }
        return false;
    }
}
