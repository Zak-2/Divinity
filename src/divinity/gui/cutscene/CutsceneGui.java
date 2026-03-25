package divinity.gui.cutscene;

import divinity.utils.cutscene.CutsceneFrameManager;
import divinity.utils.cutscene.CutsceneManager;
import divinity.utils.cutscene.CutsceneResourceLoader;
import divinity.utils.cutscene.MP3AudioPlayer;
import divinity.utils.font.Fonts;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.IOException;

public class CutsceneGui extends GuiScreen {
    private static final long AUDIO_INIT_DELAY = 200;
    private static final long SKIP_TEXT_FADE_DURATION = 6000;
    private final CutsceneFrameManager frameManager;
    private final long frameDelay;
    private final boolean useAudioSync = true;
    private final String audioResourcePath;
    private final boolean showSkipText = true;
    private int frameRate = 30;
    private long cutsceneStartTime;
    private boolean audioStarted = false;
    private boolean cutsceneFinished = false;
    private boolean wasFullscreen = false;
    private float originalMusicVolume = 1.0f;
    private float originalSoundVolume = 1.0f;
    private boolean isFirstFrame = true;
    private File audioFile;
    private boolean canSkip = true;
    private long skipTextFadeStartTime = -1;

    public CutsceneGui(String audioPath, String frameBasePath, int frameCount) {
        this(audioPath, frameBasePath, frameCount, 30, true);
    }

    public CutsceneGui(String audioPath, String frameBasePath, int frameCount, int fps, boolean canSkip) {
        this.audioResourcePath = audioPath;
        this.frameRate = fps;
        this.frameDelay = 1000 / frameRate;
        this.canSkip = canSkip;
        this.frameManager = new CutsceneFrameManager(frameBasePath, frameCount);
        loadAudio();
    }

    private void loadAudio() {
        try {
            audioFile = CutsceneResourceLoader.loadAudioFromResources(audioResourcePath);
        } catch (Exception e) {
            System.err.println("Failed to load audio: " + e.getMessage());
            audioFile = null;
        }
    }

    @Override
    public void initGui() {
        super.initGui();
        if (cutsceneStartTime == 0) {
            cutsceneStartTime = System.currentTimeMillis();
            wasFullscreen = mc.isFullScreen();
            originalMusicVolume = mc.gameSettings.getSoundLevel(SoundCategory.MUSIC);
            originalSoundVolume = mc.gameSettings.getSoundLevel(SoundCategory.MASTER);
            /*if (!mc.isFullScreen()) {
                mc.toggleFullscreen();
            }*/
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, 0.0f);
            mc.gameSettings.setSoundLevel(SoundCategory.AMBIENT, 0.0f);
            mc.gameSettings.setSoundLevel(SoundCategory.BLOCKS, 0.2f);
            mc.gameSettings.setSoundLevel(SoundCategory.PLAYERS, 0.2f);
            mc.gameSettings.setSoundLevel(SoundCategory.WEATHER, 0.1f);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xFF000000);
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - cutsceneStartTime;

        if (!audioStarted && elapsedTime >= AUDIO_INIT_DELAY && audioFile != null) {
            MP3AudioPlayer.playMP3(audioFile);
            audioStarted = true;
            cutsceneStartTime = currentTime - AUDIO_INIT_DELAY;
            elapsedTime = AUDIO_INIT_DELAY;
        }

        int currentFrame;
        if (useAudioSync && MP3AudioPlayer.isPlaying()) {
            long audioPosition = MP3AudioPlayer.getPlaybackPosition();
            currentFrame = (int) (audioPosition / frameDelay);
        } else {
            if (elapsedTime >= AUDIO_INIT_DELAY) {
                currentFrame = (int) ((elapsedTime - AUDIO_INIT_DELAY) / frameDelay);
            } else {
                currentFrame = 0;
            }
        }

        if (elapsedTime < AUDIO_INIT_DELAY) {
            currentFrame = 0;
            if (isFirstFrame) {
                isFirstFrame = false;
            }
        }

        if (currentFrame >= frameManager.getTotalFrames() && audioStarted) {
            if (!cutsceneFinished) {
                onCutsceneEnd();
                cutsceneFinished = true;
            }
            return;
        }

        ResourceLocation frameTexture = frameManager.getFrame(currentFrame);
        if (frameTexture != null) {
            renderFrame(frameTexture);
        } else {
            drawRect(100, 100, 200, 200, 0xFFFF0000);
            fontRendererObj.drawString("Frame " + currentFrame, 110, 110, 0xFFFFFF);
        }

        if (canSkip && showSkipText) {
            drawSkipText();
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void renderFrame(ResourceLocation frameTexture) {
        try {
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(frameTexture);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);

            float sourceAspect = 16.0f / 9.0f;
            float screenAspect = (float) width / height;
            int renderWidth, renderHeight;
            int x, y;
            if (screenAspect > sourceAspect) {
                renderHeight = height;
                renderWidth = (int) (height * sourceAspect);
                x = (width - renderWidth) / 2;
                y = 0;
            } else {
                renderWidth = width;
                renderHeight = (int) (width / sourceAspect);
                x = 0;
                y = (height - renderHeight) / 2;
            }

            Tessellator tessellator = Tessellator.getInstance();
            WorldRenderer worldrenderer = tessellator.getWorldRenderer();
            worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX);
            worldrenderer.pos(x, y + renderHeight, 0.0D).tex(0.0D, 1.0D).endVertex();
            worldrenderer.pos(x + renderWidth, y + renderHeight, 0.0D).tex(1.0D, 1.0D).endVertex();
            worldrenderer.pos(x + renderWidth, y, 0.0D).tex(1.0D, 0.0D).endVertex();
            worldrenderer.pos(x, y, 0.0D).tex(0.0D, 0.0D).endVertex();
            tessellator.draw();
            GL11.glDisable(GL11.GL_BLEND);
        } catch (Exception e) {
            System.err.println("Error rendering frame " + frameTexture.toString() + ": " + e.getMessage());
            drawRect(50, 50, 200, 100, 0xFFFF0000);
            fontRendererObj.drawString("Frame Error", 60, 60, 0xFFFFFF);
        }
    }

    private void drawSkipText() {
        String skipText = "Press ESC or click to skip";
        if (skipTextFadeStartTime == -1) {
            skipTextFadeStartTime = System.currentTimeMillis();
        }
        long currentTime = System.currentTimeMillis();
        long elapsedFadeTime = currentTime - skipTextFadeStartTime;
        float fadeProgress = Math.min(1.0f, (float) elapsedFadeTime / SKIP_TEXT_FADE_DURATION);

        int alpha = (int) (fadeProgress * 150);
        if (alpha <= 0) {
            return;
        }

        int baseColor = 0xFFFFFF;
        int colorWithAlpha = (alpha << 24) | (baseColor & 0x00FFFFFF);

        int textWidth = Fonts.INTER_MEDIUM.get(16).getStringWidth(skipText);
        int x = width - textWidth - 10;
        int y = height - 20;

        int bgColor = 0x000000;
        int bgWithAlpha = ((alpha / 2) << 24) | (bgColor & 0x00FFFFFF);
        drawRect(x - 5, y - 2, x + textWidth + 5, y + 12, bgWithAlpha);

        Fonts.INTER_MEDIUM.get(16).drawStringWithShadow(skipText, x, y, colorWithAlpha);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (canSkip && keyCode == 1) {
            onCutsceneEnd();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (canSkip) {
            onCutsceneEnd();
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void onCutsceneEnd() {
        MP3AudioPlayer.stopAudio();
        cutsceneFinished = true;
        if (mc.isFullScreen() && !wasFullscreen) {
            mc.toggleFullscreen();
        }
        mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, originalMusicVolume);
        mc.gameSettings.setSoundLevel(SoundCategory.MASTER, originalSoundVolume);
        mc.gameSettings.setSoundLevel(SoundCategory.AMBIENT, originalSoundVolume);
        mc.gameSettings.setSoundLevel(SoundCategory.BLOCKS, originalSoundVolume);
        mc.gameSettings.setSoundLevel(SoundCategory.PLAYERS, originalSoundVolume);
        mc.gameSettings.setSoundLevel(SoundCategory.WEATHER, originalSoundVolume);
        CutsceneManager.hasPlayed = true;
        mc.displayGuiScreen(null);
    }

    @Override
    public void onGuiClosed() {
        if (!cutsceneFinished) {
            if (mc.isFullScreen() && !wasFullscreen) {
                mc.toggleFullscreen();
            }
            mc.gameSettings.setSoundLevel(SoundCategory.MUSIC, originalMusicVolume);
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, originalSoundVolume);
            mc.gameSettings.setSoundLevel(SoundCategory.AMBIENT, originalSoundVolume);
            mc.gameSettings.setSoundLevel(SoundCategory.BLOCKS, originalSoundVolume);
            mc.gameSettings.setSoundLevel(SoundCategory.PLAYERS, originalSoundVolume);
            mc.gameSettings.setSoundLevel(SoundCategory.WEATHER, originalSoundVolume);
        }
        MP3AudioPlayer.stopAudio();
        super.onGuiClosed();
    }
}