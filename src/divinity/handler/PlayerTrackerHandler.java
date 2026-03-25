package divinity.handler;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.ChatEvent;
import divinity.event.impl.packet.PacketEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.server.S01PacketJoinGame;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerTrackerHandler {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Map<EntityPlayer, Integer> playerKills = new HashMap<>();

    private static final Pattern KILL_MESSAGE_PATTERN = Pattern.compile(
            ".*? was (?:killed|shot|thrown off a cliff|thrown into the void|shocked|final killed) by (\\w+).*"
    );

    public static int getKillsForPlayer(EntityPlayer player) {
        return playerKills.getOrDefault(player, 0);
    }

    @EventListener
    public void onEvent(ChatEvent event) {
        String message = event.getMessage();
        Matcher matcher = KILL_MESSAGE_PATTERN.matcher(message);
        if (matcher.matches()) {
            String killerName = matcher.group(1).replaceAll("[^a-zA-Z0-9_]", "").trim();
            if (killerName.isEmpty()) return;
            EntityPlayer killer = mc.theWorld.getPlayerEntityByName(killerName);
            if (killer == null) {
                return;
            }
            playerKills.put(killer, playerKills.getOrDefault(killer, 0) + 1);
        }
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        if (event.isReceiving()) {
            if (event.getPacket() instanceof S01PacketJoinGame) {
                if (!playerKills.isEmpty()) {
                    playerKills.clear();
                    ClientManager.getInstance().getNotificationManager().addNotification("Player Tracker", "Cleared data", 2000);
                }
            }
        }
    }
}
