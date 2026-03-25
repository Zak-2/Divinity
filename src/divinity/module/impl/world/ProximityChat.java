package divinity.module.impl.world;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.GameTickEvent;
import divinity.event.impl.render.RenderGuiEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.ModeProperty;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import divinity.voice.VoiceClient;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.util.List;

public class ProximityChat extends Module {

    public final BooleanProperty espSpeaking = new BooleanProperty("ESP Speaking", true);
    private final VoiceClient voice = VoiceClient.getInstance();
    private final ModeProperty microphone;
    private final ModeProperty activation = new ModeProperty("Activation", "Push To Talk", "Push To Talk", "Open Mic");
    private final ModeProperty pttKey = new ModeProperty("PTT Key", "V", () -> activation.isMode("Push To Talk"), "V", "B", "C", "G", "H", "X", "Z", "Left Alt", "Left Shift", "Left Control", "Mouse4", "Mouse5");
    private final NumberProperty<Double> range = new NumberProperty<>("Range", 32.0, 5.0, 128.0, 1.0);
    private final NumberProperty<Double> volume = new NumberProperty<>("Volume", 1.0, 0.0, 2.0, 0.01);
    private final BooleanProperty indicator = new BooleanProperty("Indicator", true);
    private final ModeProperty indicatorPos = new ModeProperty("Indicator Pos", "Bottom Left", indicator::getValue, "Bottom Left", "Bottom Right");

    public ProximityChat(String name, String[] aliases, Category category) {
        super(name, aliases, category);

        List<String> mics = voice.listMicrophones();
        String[] micModes = mics.toArray(new String[0]);
        microphone = new ModeProperty("Microphone", micModes.length > 0 ? micModes[0] : "Default", micModes);

        addProperty(microphone, activation, pttKey, range, volume, indicator, indicatorPos, espSpeaking);
    }

    private static boolean isKeyDown(int key) {
        if (key == -2) return org.lwjgl.input.Mouse.isButtonDown(3);
        if (key == -3) return org.lwjgl.input.Mouse.isButtonDown(4);
        if (key <= 0) return false;
        return Keyboard.isKeyDown(key);
    }

    private static int keyCode(String name) {
        if (name == null) return Keyboard.KEY_V;
        switch (name) {
            case "V":
                return Keyboard.KEY_V;
            case "B":
                return Keyboard.KEY_B;
            case "C":
                return Keyboard.KEY_C;
            case "G":
                return Keyboard.KEY_G;
            case "H":
                return Keyboard.KEY_H;
            case "X":
                return Keyboard.KEY_X;
            case "Z":
                return Keyboard.KEY_Z;
            case "Left Alt":
                return Keyboard.KEY_LMENU;
            case "Left Shift":
                return Keyboard.KEY_LSHIFT;
            case "Left Control":
                return Keyboard.KEY_LCONTROL;
            case "Mouse4":
                return -2;
            case "Mouse5":
                return -3;
        }
        return Keyboard.KEY_V;
    }

    @Override
    public void onEnable() {
        voice.start();
    }

    @Override
    public void onDisable() {
        voice.stop();
    }

    @EventListener
    public void onTick(GameTickEvent e) {
        if (mc.thePlayer == null) return;

        voice.setMicrophoneName(microphone.getValue());
        voice.setRange(range.getValue());
        voice.setVolume(volume.getValue());

        boolean openMicMode = activation.isMode("Open Mic");
        voice.setOpenMic(openMicMode);

        if (!openMicMode) {
            int key = keyCode(pttKey.getValue());
            boolean down = isKeyDown(key);
            voice.setPttPressed(down);
        } else {
            voice.setPttPressed(false);
        }

        voice.tickPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.dimension, range.getValue());
    }

    @EventListener
    public void onRender(RenderGuiEvent e) {
        if (!indicator.getValue()) return;
        if (!voice.isLocalSpeaking()) return;

        ScaledResolution sr = e.getSr();
        String text = "SPEAKING";
        int w = Fonts.INTER_MEDIUM.get(12).getStringWidth(text) + 10;
        int h = 16;

        int x = indicatorPos.isMode("Bottom Right") ? sr.getScaledWidth() - w - 3 : -3;
        int y = sr.getScaledHeight() - h - 7;

        int bg = new Color(0, 0, 0, 140).getRGB();
        int fg = new Color(255, 255, 255).getRGB();
        int live = new Color(0, 220, 140).getRGB();

        Fonts.INTER_MEDIUM.get(12).drawStringGradient(text, x + 10, y + 4.5f, ClientManager.getInstance().getMainColor().getRGB(), live);
    }
}
