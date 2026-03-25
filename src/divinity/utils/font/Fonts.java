package divinity.utils.font;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

import java.awt.*;
import java.io.InputStream;

public enum Fonts {
    INTER_MEDIUM("Inter_28pt-Medium");

    private final String file;
    private final Int2ObjectMap<FontRenderer> fontMap = new Int2ObjectArrayMap<>();

    Fonts(String file) {
        this.file = file;
    }

    public FontRenderer get(int size) {
        return this.fontMap.computeIfAbsent(size, font -> {
            try {
                return create(this.file, size, true);
            } catch (Exception var5) {
                throw new RuntimeException("Unable to load font: " + this, var5);
            }
        });
    }

    public FontRenderer get(int size, boolean antiAlias) {
        return this.fontMap.computeIfAbsent(size, font -> {
            try {
                return create(this.file, size, antiAlias);
            } catch (Exception var5) {
                throw new RuntimeException("Unable to load font: " + this, var5);
            }
        });
    }

    public FontRenderer create(String file, float size, boolean antiAlias) {
        try (InputStream in = Preconditions.checkNotNull(Fonts.class.getResourceAsStream("/assets/minecraft/divinity/fonts/" + file + ".ttf"), "Font resource is null")) {
            Font font = Font.createFont(0, in).deriveFont(Font.PLAIN, size);
            if (font != null) {
                return new FontRenderer(font, antiAlias);
            } else {
                throw new RuntimeException("Failed to create font");
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create font", ex);
        }
    }
}