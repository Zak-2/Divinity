package divinity.module.impl.render.element.core.graphicbox;

import divinity.ClientManager;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;

import java.util.List;

public class GraphicBoxUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final float OUTLINE_WIDTH = 0.5f;
    private static final float SEPARATOR_WIDTH = 1.0f;
    private static final float HEADER = 10.0f;

    private GraphicBoxUtils() {

    }

    public static <T> float[] drawGraphicBox(
            List<GraphicBoxField<T>> fields,
            List<T> objects,
            float x,
            float y) {

        float[] dimensions = new float[2];

        float width = 0.0f;
        float margin = 2.0f;
        float spacing = margin * 3.0f;

        int fs = fields.size();
        int n = objects.size();

        String[][] values = new String[fs][n];
        double[] fieldWidths = new double[fs];

        for (int i = 0; i < fs; i++) {
            fieldWidths[i] = Fonts.INTER_MEDIUM.get(12).getStringWidth(fields.get(i).title) + spacing;
        }

        for (int j = 0; j < n; j++) {
            T object = objects.get(j);

            for (int i = 0; i < fs; i++) {
                values[i][j] = fields.get(i).valueFunc.apply(object);
                fieldWidths[i] = Math.max(fieldWidths[i], Fonts.INTER_MEDIUM.get(12).getStringWidth(values[i][j]) + spacing);
            }
        }

        float fieldWidthsAccumulator = 0.0f;

        for (int i = 0; i < fs; i++) {
            fieldWidthsAccumulator += (float) fieldWidths[i];
        }

        width = Math.max(width, fieldWidthsAccumulator);

        dimensions[0] = width;

        GraphicBoxUtils.drawGraphicBox(null, x, y, width,
                (outlineWidth, separatorWidth, header) -> (n + 1) * header + separatorWidth,
                (gbx, gby, gbwidth, gbheight, color, outlineWidth, separatorWidth, header) -> {
                    float xAccumulator = x + margin;
                    for (int i = 0; i < fs; i++) {
                        Fonts.INTER_MEDIUM.get(12).drawStringWithShadow(fields.get(i).title, xAccumulator, y + 4, 0xFFFFFFFF);
                        xAccumulator += (float) fieldWidths[i];
                    }

                    for (int i = 0; i < n; i++) {
                        float elementY = y + header + separatorWidth + i * header;

                        xAccumulator = x + margin;
                        for (int j = 0; j < fs; j++) {
                            if (j == 0 && i == 0 && fields.get(0).title.startsWith("Player")) {
                                Fonts.INTER_MEDIUM.get(12).drawStringGradient(values[j][i], xAccumulator, elementY + 3, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());
                            } else if (j == 0 && fields.get(0).title.startsWith("User")) {
                                Fonts.INTER_MEDIUM.get(12).drawStringGradient(values[j][i], xAccumulator, elementY + 3, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());
                            } else {
                                String fullUsername = values[j][i];
                                String username = null;

                                if (username != null && j == 0) {
                                    int startIndex = fullUsername.indexOf("(");
                                    int endIndex = fullUsername.indexOf(")");
                                    if (startIndex != -1 && endIndex != -1) {
                                        String partInBrackets = fullUsername.substring(startIndex, endIndex + 1);
                                        Fonts.INTER_MEDIUM.get(12).drawStringWithShadow(fullUsername.split(" \\(")[0], xAccumulator, elementY + 3, fields.get(j).valueColorFunc.apply(objects.get(i)));
                                        Fonts.INTER_MEDIUM.get(12).drawStringGradient("(" + username + ")", xAccumulator + Fonts.INTER_MEDIUM.get(12).getStringWidth(values[j][i]) - Fonts.INTER_MEDIUM.get(12).getStringWidth(partInBrackets), elementY + 3, ClientManager.getInstance().getMainColor().getRGB(), ClientManager.getInstance().getSecondaryColor().getRGB());
                                    }
                                } else {
                                    Fonts.INTER_MEDIUM.get(12).drawStringWithShadow(values[j][i], xAccumulator, elementY + 3, fields.get(j).valueColorFunc.apply(objects.get(i)));
                                }
                            }
                            xAccumulator += (float) fieldWidths[j];
                        }
                    }
                    dimensions[1] = gbheight;
                });

        return dimensions;
    }

    public static void drawGraphicBox(
            String title,
            float x,
            float y,
            float width,
            float height,
            GraphicBoxContent content) {

        int color = 0x44c17b;

        RenderUtils.glDrawFilledQuad(x, y, width, height, 0x80 << 24);

        RenderUtils.glDrawFilledQuad(x, y, width, HEADER, 0x80 << 24);

        if (title != null) {
            Fonts.INTER_MEDIUM.get(12).drawStringWithShadow(title, x + 2.0f, y + 2, 0xFFFFFFFF);
        }

        if (content != null) {
            content.drawContent(x, y, width, height, color, OUTLINE_WIDTH, SEPARATOR_WIDTH, HEADER);
        }
    }

    public static void drawGraphicBox(
            String title,
            float x,
            float y,
            float width,
            GraphicBoxHeightSupplier heightSupplier,
            GraphicBoxContent content
    ) {
        float height = heightSupplier.get(OUTLINE_WIDTH, SEPARATOR_WIDTH, HEADER);
        drawGraphicBox(title, x, y, width, height, content);
    }

}