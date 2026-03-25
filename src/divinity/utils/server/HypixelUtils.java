package divinity.utils.server;

import net.minecraft.client.Minecraft;
import net.minecraft.scoreboard.Score;
import net.minecraft.scoreboard.ScoreObjective;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.scoreboard.Scoreboard;

public class HypixelUtils {
    private final static Minecraft mc = Minecraft.getMinecraft();


    public static boolean isOnHypixel() {
        if (mc.theWorld == null || mc.thePlayer == null) return false;

        if (!mc.thePlayer.getClientBrand().contains("Hypixel BungeeCord")) return false;

        if (!mc.getCurrentServerData().serverIP.toLowerCase().contains("hypixel.net")) {
            return false;
        }

        Scoreboard scoreboard = mc.theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);

        if (objective == null) return false;

        String title = objective.getDisplayName().toLowerCase();

        if (title.contains("hypixel")) {
            return true;
        }

        for (Score score : scoreboard.getSortedScores(objective)) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()).toLowerCase();

            if (line.contains("hypixel")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isInLobby() {
        if (Minecraft.getMinecraft().theWorld == null) return false;

        Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);

        if (objective == null) return false;

        for (Score score : scoreboard.getSortedScores(objective)) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName());

            if (line.toLowerCase().contains("tokens:")) {
                return true;
            }
        }

        return false;
    }

    public static String getHypixelGameMode() {
        if (Minecraft.getMinecraft().theWorld == null) return "Unknown";

        Scoreboard scoreboard = Minecraft.getMinecraft().theWorld.getScoreboard();
        ScoreObjective objective = scoreboard.getObjectiveInDisplaySlot(1);

        if (objective == null) return "Unknown";

        String title = objective.getDisplayName().toUpperCase();

        for (Score score : scoreboard.getSortedScores(objective)) {
            ScorePlayerTeam team = scoreboard.getPlayersTeam(score.getPlayerName());
            String line = ScorePlayerTeam.formatPlayerName(team, score.getPlayerName()).toUpperCase();

            if (title.contains("BED WARS") && line.toLowerCase().contains("red")) return "BedWars";
            if (title.contains("SKYWARS") && line.toLowerCase().contains("players left:")) return "SkyWars";
            if (title.contains("DUELS") && line.toLowerCase().contains("opponent")) return "Duels";
            if (title.contains("MURDER MYSTERY") && line.toLowerCase().contains("role:")) return "MurderMystery";
            if (title.contains("MEGA WALLS") && line.toLowerCase().contains("[r]")) return "MegaWalls";
            if (title.contains("THE HYPIXEL PIT")) return "Pit";
        }

        return "Unknown";
    }
}
