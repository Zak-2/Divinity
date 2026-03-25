package divinity.utils.font;

import java.awt.*;

@FunctionalInterface
public interface GradientApplier {
    Color colour(int i);
}