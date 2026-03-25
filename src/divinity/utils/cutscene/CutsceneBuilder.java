package divinity.utils.cutscene;

import divinity.gui.cutscene.CutsceneGui;

public class CutsceneBuilder {
    private String audioPath;
    private String frameBasePath;
    private int frameCount;
    private int frameRate = 30;
    private boolean canSkip = true;

    public CutsceneBuilder audio(String audioPath) {
        this.audioPath = audioPath;
        return this;
    }

    public CutsceneBuilder frames(String frameBasePath, int frameCount) {
        this.frameBasePath = frameBasePath;
        this.frameCount = frameCount;
        return this;
    }

    public CutsceneBuilder frameRate(int fps) {
        this.frameRate = fps;
        return this;
    }

    public CutsceneBuilder skippable(boolean canSkip) {
        this.canSkip = canSkip;
        return this;
    }

    public CutsceneGui build() {
        if (audioPath == null || frameBasePath == null || frameCount <= 0) {
            throw new IllegalStateException("Audio path, frame base path, and frame count must be set");
        }
        return new CutsceneGui(audioPath, frameBasePath, frameCount, frameRate, canSkip);
    }
}