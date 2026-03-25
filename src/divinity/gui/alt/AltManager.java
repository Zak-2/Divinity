package divinity.gui.alt;


import divinity.ClientManager;
import divinity.gui.alt.util.MicrosoftAuthenticator;
import divinity.gui.alt.util.SavedAltData;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static divinity.gui.alt.util.MicrosoftAuthenticator.Logger;


public final class AltManager {
    private static final Path ALTS_FILE = ClientManager.getInstance().getCLIENT_DIR().resolve("alts");
    private static final SecretKeySpec CLIENT_KEY_AES256 = new SecretKeySpec("76191320242773677418164513742126".getBytes(), "AES");
    public final List<SavedAltData> savedAlts = new CopyOnWriteArrayList<>();
    public MicrosoftAuthenticator authenticator;
    private boolean shouldResaveAltsFile;

    // TODO: Communicate all banned ips for all
    //https://checkip.amazonaws.com/

    public AltManager() {
        init();
    }

    public static String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, CLIENT_KEY_AES256);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | InvalidKeyException |
                 BadPaddingException e) {
            Logger.error("Unable to encrypt alt data");
            return null;
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, CLIENT_KEY_AES256);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedData));
            return new String(decryptedBytes);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException |
                 BadPaddingException e) {
            Logger.error("Unable to decrypt alt data");
            return null;
        }
    }

    private void init() {
        loadAltsFile();

        if (Files.exists(ALTS_FILE))
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(this::saveAltsFile, 10, 5, TimeUnit.SECONDS);

        // FIXME: GET OWN CLIENT_ID, CLIENT_SECRET
        authenticator = new MicrosoftAuthenticator(
                this::addAlt,
                "9fbc7315-7200-4b2b-a655-bb38c865da17",
                "Bzn8Q~YryydJsydgnnxHgJq.NM3Oo4.AEEohLbBb");
    }

    public void addAlt(SavedAltData alt) {
        savedAlts.add(alt);
        shouldResaveAltsFile = true;
        GuiAltManager.addAlt(alt);
        ClientManager.getInstance().getNotificationManager().addNotification("Added alt", "AltManager successfully added alt " + alt.cachedUsername, 3000);
    }

    public void removeAlt(int altIdx) {
        savedAlts.remove(altIdx);
        shouldResaveAltsFile = true;
        GuiAltManager.removeAlt(altIdx);
        ClientManager.getInstance().getNotificationManager().addNotification("Success", "Account removed", 2000);
    }

    private void saveAltsFile() {
        if (!shouldResaveAltsFile) {
            return;
        }

        StringWriter writer = new StringWriter();

        for (SavedAltData alt : savedAlts) {
            StringBuilder sb = new StringBuilder();
            alt.saveLine(sb);
            writer.write(sb.toString());
            writer.write('\n');
        }

        String plaintext = writer.toString();
        String encrypted = encrypt(plaintext);

        if (encrypted == null) {
            return;
        }

        try (BufferedWriter bw = Files.newBufferedWriter(ALTS_FILE, StandardCharsets.UTF_8)) {
            bw.write(encrypted);
        } catch (IOException e) {
            Logger.error("Unable to write alts");
        }
    }

    private void loadAltsFile() {
        if (Files.notExists(ALTS_FILE)) {
            try {
                Files.createFile(ALTS_FILE);
            } catch (IOException e) {
                Logger.error("Unable to create alts");
            }
        } else {
            try {
                String encrypted = new String(Files.readAllBytes(ALTS_FILE), StandardCharsets.UTF_8);

                String plaintext = decrypt(encrypted);

                if (plaintext != null) {
                    try (BufferedReader reader = new BufferedReader(new StringReader(plaintext))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            SavedAltData data = SavedAltData.parseFromLine(line);
                            savedAlts.add(data);
                        }
                    }
                }
            } catch (IOException e) {
                Logger.error("Unable to read alts.enc");
            }
        }
    }
}
