package divinity.module.impl.world.hypixel;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.packet.PacketEvent;
import divinity.event.impl.world.LoadWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.ModeProperty;
import divinity.utils.math.TimerUtil;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;

import java.util.Arrays;

public class AutoHypixel extends Module {

    private final BooleanProperty autoPlayProperty = new BooleanProperty("Auto Play", true);
    private final BooleanProperty leaveOnBanMSGProperty = new BooleanProperty("Panic Leave", true);
    private final ModeProperty gameTypeProperty = new ModeProperty("Mode", "Solo Normal", autoPlayProperty::getValue, "Solo Normal", "Teams Normal", "Solo Insane", "Teams Insane");
    private final TimerUtil banMSGDelay = new TimerUtil();

    public AutoHypixel(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onEvent(LoadWorldEvent event) {
        banMSGDelay.reset();
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        if (event.isReceiving()) {
            final Packet<?> packet = event.getPacket();

            if (packet instanceof S45PacketTitle) {
                S45PacketTitle s45 = (S45PacketTitle) packet;
                if (s45.getMessage() != null && autoPlayProperty.getValue()) {
                    String message = s45.getMessage().getFormattedText().toLowerCase();

                    if (message.contains("you died") || message.contains("game over")) {
                        ClientManager.getInstance().getNotificationManager().addNotification("You Lost", "Sending Into Next Game, Play Better Perhaps?", 2000L);
                        switch (gameTypeProperty.getValue()) {
                            case "Solo Normal":
                                mc.thePlayer.dispatchCommand("play solo_normal");
                                break;
                            case "Teams Normal":
                                mc.thePlayer.dispatchCommand("play teams_normal");
                                break;
                            case "Solo Insane":
                                mc.thePlayer.dispatchCommand("play solo_insane");
                                break;
                            case "Teams Insane":
                                mc.thePlayer.dispatchCommand("play teams_insane");
                                break;
                        }
                    } else if (message.contains("you win") || message.contains("victory")) {
                        ClientManager.getInstance().getNotificationManager().addNotification("You Won!", "Sending Into Next Game", 2000L);
                        switch (gameTypeProperty.getValue()) {
                            case "Solo Normal":
                                mc.thePlayer.dispatchCommand("play solo_normal");
                                break;
                            case "Teams Normal":
                                mc.thePlayer.dispatchCommand("play teams_normal");
                                break;
                            case "Solo Insane":
                                mc.thePlayer.dispatchCommand("play solo_insane");
                                break;
                            case "Teams Insane":
                                mc.thePlayer.dispatchCommand("play teams_insane");
                                break;
                        }
                    }
                }
            }
            if (packet instanceof S02PacketChat) {
                S02PacketChat s02 = (S02PacketChat) packet;
                if (leaveOnBanMSGProperty.getValue() && banMSGDelay.hasTimeElapsed(5000) && s02.getChatComponent().getUnformattedText().startsWith("A player has been removed from your game.")) {
                    mc.thePlayer.sendChatMessage("/hub");
                }
            }
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }

    private enum Gamemode {
        SOLO_NORMAL("Solo Normal"),
        TEAMS_NORMAL("Teams Normal"),
        SOLO_INSANE("Solo Insane"),
        TEAMS_INSANE("Teams Insane");

        private final String name;

        Gamemode(String name) {
            this.name = name;
        }

        public static Gamemode fromString(String name) {
            return Arrays.stream(values())
                    .filter(mode -> mode.name.equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}
