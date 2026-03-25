package divinity.gui.alt.util;

public enum AltAuthStatus {
    UNVERIFIED("\247cUnverified"),
    PROCESSING("\247eLogging in"),
    VERIFIED("\247aAvailable");

    public final String displayName;

    AltAuthStatus(String displayName) {
        this.displayName = displayName;
    }
}