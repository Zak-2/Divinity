package divinity.module.impl.render.element.core.graphicbox;

@FunctionalInterface
public interface GraphicBoxContent {

    void drawContent(
            double gbx,
            double gby,
            float gbwidth,
            float gbheight,
            int color,
            float outlineWidth,
            float separatorWidth,
            float header
    );
}