package divinity.utils.cutscene;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public class CutsceneFrameManager {
    @Getter
    private final int totalFrames;
    private final String frameBasePath;
    private ResourceLocation[] frames;

    public CutsceneFrameManager(String frameBasePath, int frameCount) {
        this.frameBasePath = frameBasePath;
        this.totalFrames = frameCount;
        loadFrames();
    }

    private void loadFrames() {
        frames = new ResourceLocation[totalFrames];
        for (int i = 0; i < totalFrames; i++) {
            String frameName = String.format("frame_%03d.jpg", i + 1);
            frames[i] = new ResourceLocation("minecraft", "divinity/cutscenes/" + frameBasePath + "/" + frameName);
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(frames[i]);
            } catch (Exception e) {
                System.err.println("Failed to load frame: " + frames[i].toString());
            }
        }
    }

    public ResourceLocation getFrame(int frameIndex) {
        if (frameIndex >= 0 && frameIndex < totalFrames) {
            return frames[frameIndex];
        }
        return null;
    }

}