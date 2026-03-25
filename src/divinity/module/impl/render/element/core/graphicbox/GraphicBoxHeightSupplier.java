package divinity.module.impl.render.element.core.graphicbox;

@FunctionalInterface
public interface GraphicBoxHeightSupplier {
    float get(
            float outlineWidth,
            float separatorWidth,
            float header
    );
}