package divinity.gui.alt.util;

import divinity.ClientManager;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;

import java.util.Base64;

public final class SavedAltData {
    public String profileID;
    public String cachedUsername;
    public String emailOrRefreshTokenOrCookie; //email,refreshToken,cookie
    public String password;
    public SavedLoginType type;

    public String accessToken;
    public AltAuthStatus authStatus = AltAuthStatus.UNVERIFIED;
    public AltBanStatus banStatus = AltBanStatus.UNKNOWN;

    public SavedAltData() {
    }

    public SavedAltData(String profileID, String cachedUsername, String emailOrRefreshTokenOrCookie, String password, SavedLoginType type) {
        this.profileID = profileID;
        this.cachedUsername = cachedUsername;
        this.emailOrRefreshTokenOrCookie = emailOrRefreshTokenOrCookie;
        this.password = password;
        this.type = type;
    }

    public static SavedAltData parseFromLine(String line) {
        if (line.isEmpty()) return null;
        char type = line.charAt(0);
        line = line.substring(1);
        String[] args = line.split(":", type == 'e' ? 4 : type == 'o' ? 2 : 3);
        switch (type) {
            case 'e': // email/pass
                return new SavedAltData(args[args.length - 1], args[args.length - 2], args[args.length - 3], args[args.length - 4], SavedLoginType.CREDENTIALS);
            case 'r': // refresh token
                return new SavedAltData(args[args.length - 1], args[args.length - 2], new String(Base64.getDecoder().decode(args[args.length - 3])), null, SavedLoginType.REFRESH_TOKEN);
            case 'c': // cookie
                return new SavedAltData(args[args.length - 1], args[args.length - 2], new String(Base64.getDecoder().decode(args[args.length - 3])), null, SavedLoginType.COOKIE);
            default: // cracked
            case 'o':
                return new SavedAltData(args[args.length - 1], args[args.length - 2], null, null, SavedLoginType.REFRESH_TOKEN);
        }
    }

    public AltAuthResult login() {
        switch (type) {
            case REFRESH_TOKEN:
                return ClientManager.getInstance().getAltManager().authenticator.loginWithRefreshToken(this, emailOrRefreshTokenOrCookie);
            case COOKIE:
                return ClientManager.getInstance().getAltManager().authenticator.loginWithCookie(this, emailOrRefreshTokenOrCookie);
            case CREDENTIALS:
                return ClientManager.getInstance().getAltManager().authenticator.loginWithCredentials(this, emailOrRefreshTokenOrCookie, password);
            case CRACKED:
            default:
                return AltAuthResult.SUCCESS;
        }
    }

    public void saveLine(StringBuilder builder) {
        builder.append(type.type);
        switch (type.type) {
            case 'e':
                builder.append(password).append(":");
            case 'r':
            case 'c':
                builder.append(type.type == 'e' ? emailOrRefreshTokenOrCookie : Base64.getEncoder().encodeToString(emailOrRefreshTokenOrCookie.getBytes())).append(":");
            case 'o':
                builder.append(cachedUsername).append(":");
                builder.append(profileID);
                break;
        }
    }

    public void setMcSession() {
        Minecraft.getMinecraft().session = new Session(cachedUsername, profileID, accessToken, "mojang");
    }
}