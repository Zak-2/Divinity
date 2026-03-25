package divinity.gui.alt.util;

public enum SavedLoginType {
    CRACKED('o'), CREDENTIALS('e'), COOKIE('c'), REFRESH_TOKEN('r');

    final char type;

    SavedLoginType(char type) {
        this.type = type;
    }
}