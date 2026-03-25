package divinity.utils.cutscene;

import divinity.gui.cutscene.CutsceneGui;
import net.minecraft.client.Minecraft;

public class CutsceneManager {
    public static boolean hasPlayed = false;


    public static void playCutscene(String audioFile, String frameFolder, int frameCount) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(new CutsceneGui(audioFile, frameFolder, frameCount));
    }

    public static void playCutscene(CutsceneGui cutscene) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.displayGuiScreen(cutscene);
    }

    public static CutsceneBuilder builder() {
        return new CutsceneBuilder();
    }

    public static void cleanup() {
        MP3AudioPlayer.cleanup();
    }

    public static void playIntroCutscene() {
        CutsceneGui intro = builder()
                .audio("intro_music.wav")
                .frames("intro_sequence", 450)
                .frameRate(30)
                .skippable(true)
                .build();
        playCutscene(intro);
    }

    public static void playUnskippableEnding() {
        CutsceneGui ending = builder()
                .audio("ending_music.wav")
                .frames("ending_sequence", 600)
                .frameRate(30)
                .skippable(false)
                .build();
        playCutscene(ending);
    }
}