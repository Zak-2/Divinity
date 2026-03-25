package divinity.utils.cutscene;

import lombok.Getter;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MP3AudioPlayer {
    private static final ExecutorService audioExecutor = Executors.newSingleThreadExecutor();
    @Getter
    private static volatile boolean isPlaying = false;
    private static volatile boolean shouldStop = false;
    private static AudioInputStream audioInputStream;
    private static Clip audioClip;

    public static void playMP3(File audioFile) {
        audioExecutor.submit(() -> {
            try {
                stopAudio();
                if (!audioFile.exists() || !audioFile.canRead()) {
                    System.err.println("Audio file doesn't exist or can't be read: " + audioFile.getAbsolutePath());
                    return;
                }
                Thread.sleep(50);
                AudioInputStream stream;
                try {
                    stream = AudioSystem.getAudioInputStream(audioFile);
                } catch (UnsupportedAudioFileException e) {
                    System.err.println("Unsupported audio format: " + audioFile.getName());
                    System.err.println("Please convert MP3 to WAV format for compatibility");
                    return;
                }
                AudioFormat format = stream.getFormat();
                AudioFormat.Encoding targetEncoding = AudioFormat.Encoding.PCM_SIGNED;
                AudioFormat targetFormat = new AudioFormat(
                        targetEncoding,
                        format.getSampleRate(),
                        16,
                        format.getChannels(),
                        format.getChannels() * 2,
                        format.getSampleRate(),
                        false
                );
                if (!AudioSystem.isConversionSupported(targetFormat, format)) {
                    System.err.println("Audio conversion not supported for this format");
                    stream.close();
                    return;
                }
                audioInputStream = AudioSystem.getAudioInputStream(targetFormat, stream);
                audioClip = AudioSystem.getClip();
                audioClip.open(audioInputStream);
                audioClip.setFramePosition(0);
                isPlaying = true;
                shouldStop = false;
                audioClip.start();
                while (audioClip.isRunning() && !shouldStop) {
                    Thread.sleep(100);
                }
                isPlaying = false;
            } catch (Exception e) {
                System.err.println("Error playing audio: " + e.getMessage());
                e.getSuppressed();
                isPlaying = false;
            }
        });
    }

    public static void stopAudio() {
        shouldStop = true;
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            audioClip.close();
        }
        if (audioInputStream != null) {
            try {
                audioInputStream.close();
            } catch (IOException e) {
                e.getSuppressed();
            }
        }
        isPlaying = false;
    }

    public static long getPlaybackPosition() {
        if (audioClip != null && isPlaying) {
            return audioClip.getMicrosecondPosition() / 1000;
        }
        return 0;
    }

    public static void cleanup() {
        stopAudio();
        audioExecutor.shutdown();
    }
}