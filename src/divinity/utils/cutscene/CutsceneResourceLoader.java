package divinity.utils.cutscene;

import java.io.*;
import java.nio.file.Files;

public class CutsceneResourceLoader {
    public static File loadFileFromResources(String resourcePath, String tempPrefix, String tempSuffix) throws IOException {
        InputStream inputStream = CutsceneResourceLoader.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            throw new FileNotFoundException("Failed to find file at path " + resourcePath + " in resources.");
        }
        File tempFile = File.createTempFile(tempPrefix, tempSuffix);
        tempFile.deleteOnExit();
        try (OutputStream outputStream = new BufferedOutputStream(Files.newOutputStream(tempFile.toPath()))) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } finally {
            inputStream.close();
        }
        return tempFile;
    }

    public static File loadAudioFromResources(String audioPath) throws IOException {
        String extension = audioPath.substring(audioPath.lastIndexOf('.'));
        return loadFileFromResources("/assets/minecraft/divinity/audio/" + audioPath, "cutscene_audio", extension);
    }
}