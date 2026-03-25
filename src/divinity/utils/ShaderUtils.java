package divinity.utils;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public class ShaderUtils {
    private static final String VERTEX_SHADER =
            "#version 120\n" +
                    "void main() {\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "}";
    private static final String ROUNDED_RECT_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform vec2 location;\n" +
                    "uniform vec2 rectSize;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "float roundSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b, 0.0)) - r;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 rectHalf = rectSize * 0.5;\n" +
                    "    float smoothedAlpha = (1.0 - smoothstep(0.0, 1.0, roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1.0, radius))) * color.a;\n" +
                    "    gl_FragColor = vec4(color.rgb, smoothedAlpha);\n" +
                    "}";
    private static final String CIRCLE_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform vec2 rectSize;\n" +
                    "uniform vec4 color;\n" +
                    "uniform float radius;\n" +
                    "void main() {\n" +
                    "    vec2 center = rectSize * 0.5;\n" +
                    "    vec2 pos = gl_TexCoord[0].st * rectSize;\n" +
                    "    float dist = length(pos - center);\n" +
                    "    float alpha = (1.0 - smoothstep(radius - 1.0, radius + 1.0, dist)) * color.a;\n" +
                    "    gl_FragColor = vec4(color.rgb, alpha);\n" +
                    "}";
    private static final String GRADIENT2_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform vec4 color1;\n" +
                    "uniform vec4 color2;\n" +
                    "uniform int direction;\n" +
                    "void main() {\n" +
                    "    float t = direction == 0 ? gl_TexCoord[0].s : gl_TexCoord[0].t;\n" +
                    "    gl_FragColor = mix(color1, color2, t);\n" +
                    "}";
    private static final String GRADIENT3_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform vec4 color1;\n" +
                    "uniform vec4 color2;\n" +
                    "uniform vec4 color3;\n" +
                    "uniform int direction;\n" +
                    "void main() {\n" +
                    "    float t = direction == 0 ? gl_TexCoord[0].s : gl_TexCoord[0].t;\n" +
                    "    vec4 color;\n" +
                    "    if(t < 0.5) {\n" +
                    "        color = mix(color1, color2, t * 2.0);\n" +
                    "    } else {\n" +
                    "        color = mix(color2, color3, (t - 0.5) * 2.0);\n" +
                    "    }\n" +
                    "    gl_FragColor = color;\n" +
                    "}";
    private static final String BLUR_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform vec4 color;\n" +
                    "uniform vec2 rectSize;\n" +
                    "uniform float blurRadius;\n" +
                    "void main() {\n" +
                    "    vec2 halfSize = rectSize * 0.5;\n" +
                    "    vec2 p = gl_TexCoord[0].st * rectSize - halfSize;\n" +
                    "    vec2 d = abs(p) - (halfSize - blurRadius);\n" +
                    "    float dist = length(max(d, 0.0)) + min(max(d.x, d.y), 0.0);\n" +
                    "    float alpha = 1.0 - smoothstep(-blurRadius, blurRadius, dist);\n" +
                    "    gl_FragColor = vec4(color.rgb, alpha * color.a);\n" +
                    "}";
    private static final String ROUNDED_BLUR_FRAGMENT_SHADER =
            "#version 120\n" +
                    "uniform vec4 color;\n" +
                    "uniform vec2 rectSize;\n" +
                    "uniform float radius;\n" +
                    "uniform float blurRadius;\n" +
                    "float roundSDF(vec2 p, vec2 b, float r) {\n" +
                    "    return length(max(abs(p) - b + r, 0.0)) - r;\n" +
                    "}\n" +
                    "void main() {\n" +
                    "    vec2 rectHalf = rectSize * 0.5;\n" +
                    "    vec2 p = gl_TexCoord[0].st * rectSize - rectHalf;\n" +
                    "    float dist = roundSDF(p, rectHalf - radius, radius);\n" +
                    "    float alpha = 1.0 - smoothstep(-blurRadius, blurRadius, dist);\n" +
                    "    gl_FragColor = vec4(color.rgb, alpha * color.a);\n" +
                    "}";
    private static int roundedRectProgram;
    private static int circleProgram;
    private static int gradient2Program;
    private static int gradient3Program;
    private static int blurProgram;
    private static int roundedBlurProgram;

    public static void init() {
        try {
            roundedRectProgram = createShaderProgram(VERTEX_SHADER, ROUNDED_RECT_FRAGMENT_SHADER);
            circleProgram = createShaderProgram(VERTEX_SHADER, CIRCLE_FRAGMENT_SHADER);
            gradient2Program = createShaderProgram(VERTEX_SHADER, GRADIENT2_FRAGMENT_SHADER);
            gradient3Program = createShaderProgram(VERTEX_SHADER, GRADIENT3_FRAGMENT_SHADER);
            blurProgram = createShaderProgram(VERTEX_SHADER, BLUR_FRAGMENT_SHADER);
            roundedBlurProgram = createShaderProgram(VERTEX_SHADER, ROUNDED_BLUR_FRAGMENT_SHADER);
            System.out.println("ShaderUtils initialized successfully");
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to initialize ShaderUtils", e);
        }
    }

    private static int createShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        int vertShader = createShader(vertexShaderSource, GL20.GL_VERTEX_SHADER);
        int fragShader = createShader(fragmentShaderSource, GL20.GL_FRAGMENT_SHADER);

        int program = GL20.glCreateProgram();
        GL20.glAttachShader(program, vertShader);
        GL20.glAttachShader(program, fragShader);
        GL20.glLinkProgram(program);

        if (GL20.glGetProgrami(program, GL20.GL_LINK_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetProgramInfoLog(program, 1024);
            GL20.glDeleteShader(vertShader);
            GL20.glDeleteShader(fragShader);
            throw new RuntimeException("Shader program linking failed: " + log);
        }

        GL20.glDeleteShader(vertShader);
        GL20.glDeleteShader(fragShader);
        return program;
    }

    private static int createShader(String source, int type) {
        int shader = GL20.glCreateShader(type);
        GL20.glShaderSource(shader, source);
        GL20.glCompileShader(shader);

        if (GL20.glGetShaderi(shader, GL20.GL_COMPILE_STATUS) == GL11.GL_FALSE) {
            String log = GL20.glGetShaderInfoLog(shader, 1024);
            GL20.glDeleteShader(shader);
            throw new RuntimeException("Shader compilation failed: " + log);
        }

        return shader;
    }

    private static void drawQuads(float x, float y, float x2, float y2) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(x, y2);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(x2, y2);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(x2, y);
        GL11.glEnd();
    }

    // Existing drawRoundRect method using new program system
    public static void drawRoundRect(float x, float y, float x2, float y2, float radius, int color) {
        if (roundedRectProgram == 0) return;

        float width = x2 - x;
        float height = y2 - y;

        float a = (color >> 24 & 0xFF) / 255.0f;
        float r = (color >> 16 & 0xFF) / 255.0f;
        float g = (color >> 8 & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        GL20.glUseProgram(roundedRectProgram);

        int uLocation = GL20.glGetUniformLocation(roundedRectProgram, "location");
        int uRectSize = GL20.glGetUniformLocation(roundedRectProgram, "rectSize");
        int uColor = GL20.glGetUniformLocation(roundedRectProgram, "color");
        int uRadius = GL20.glGetUniformLocation(roundedRectProgram, "radius");

        GL20.glUniform2f(uLocation, x, y);
        GL20.glUniform2f(uRectSize, width, height);
        GL20.glUniform4f(uColor, r, g, b, a);
        GL20.glUniform1f(uRadius, radius);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        drawQuads(x, y, x2, y2);

        GlStateManager.enableTexture2D();
        GL20.glUseProgram(0);
    }

    public static void drawCircle(float centerX, float centerY, float radius, int color) {
        if (circleProgram == 0) return;

        float diameter = radius * 2;
        float x = centerX - radius;
        float y = centerY - radius;
        float x2 = x + diameter;
        float y2 = y + diameter;

        float a = (color >> 24 & 0xFF) / 255.0f;
        float r = (color >> 16 & 0xFF) / 255.0f;
        float g = (color >> 8 & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;

        GL20.glUseProgram(circleProgram);

        int uLocation = GL20.glGetUniformLocation(circleProgram, "location");
        int uRectSize = GL20.glGetUniformLocation(circleProgram, "rectSize");
        int uColor = GL20.glGetUniformLocation(circleProgram, "color");
        int uRadius = GL20.glGetUniformLocation(circleProgram, "radius");

        GL20.glUniform2f(uLocation, x, y);
        GL20.glUniform2f(uRectSize, diameter, diameter);
        GL20.glUniform4f(uColor, r, g, b, a);
        GL20.glUniform1f(uRadius, radius);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        drawQuads(x, y, x2, y2);

        GlStateManager.enableTexture2D();
        GL20.glUseProgram(0);
    }

    public static void drawGradientRect(float x, float y, float x2, float y2, int color1, int color2, boolean horizontal) {
        if (gradient2Program == 0) return;

        float width = x2 - x;
        float height = y2 - y;

        float[] a1 = unpackColor(color1);
        float[] a2 = unpackColor(color2);

        GL20.glUseProgram(gradient2Program);

        int uLocation = GL20.glGetUniformLocation(gradient2Program, "location");
        int uRectSize = GL20.glGetUniformLocation(gradient2Program, "rectSize");
        int uColor1 = GL20.glGetUniformLocation(gradient2Program, "color1");
        int uColor2 = GL20.glGetUniformLocation(gradient2Program, "color2");
        int uDirection = GL20.glGetUniformLocation(gradient2Program, "direction");

        GL20.glUniform2f(uLocation, x, y);
        GL20.glUniform2f(uRectSize, width, height);
        GL20.glUniform4f(uColor1, a1[0], a1[1], a1[2], a1[3]);
        GL20.glUniform4f(uColor2, a2[0], a2[1], a2[2], a2[3]);
        GL20.glUniform1i(uDirection, horizontal ? 0 : 1);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        drawQuads(x, y, x2, y2);

        GlStateManager.enableTexture2D();
        GL20.glUseProgram(0);
    }

    public static void drawTripleGradientRect(float x, float y, float x2, float y2, int color1, int color2, int color3, boolean horizontal) {
        if (gradient3Program == 0) return;

        float width = x2 - x;
        float height = y2 - y;

        float[] a1 = unpackColor(color1);
        float[] a2 = unpackColor(color2);
        float[] a3 = unpackColor(color3);

        GL20.glUseProgram(gradient3Program);

        int uLocation = GL20.glGetUniformLocation(gradient3Program, "location");
        int uRectSize = GL20.glGetUniformLocation(gradient3Program, "rectSize");
        int uColor1 = GL20.glGetUniformLocation(gradient3Program, "color1");
        int uColor2 = GL20.glGetUniformLocation(gradient3Program, "color2");
        int uColor3 = GL20.glGetUniformLocation(gradient3Program, "color3");
        int uDirection = GL20.glGetUniformLocation(gradient3Program, "direction");

        GL20.glUniform2f(uLocation, x, y);
        GL20.glUniform2f(uRectSize, width, height);
        GL20.glUniform4f(uColor1, a1[0], a1[1], a1[2], a1[3]);
        GL20.glUniform4f(uColor2, a2[0], a2[1], a2[2], a2[3]);
        GL20.glUniform4f(uColor3, a3[0], a3[1], a3[2], a3[3]);
        GL20.glUniform1i(uDirection, horizontal ? 0 : 1);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        drawQuads(x, y, x2, y2);

        GlStateManager.enableTexture2D();
        GL20.glUseProgram(0);
    }

    public static void drawBlurRect(float x, float y, float x2, float y2, float blurRadius, int color) {
        if (blurProgram == 0) return;

        float width = x2 - x;
        float height = y2 - y;
        float[] col = unpackColor(color);

        GL20.glUseProgram(blurProgram);

        int uLocation = GL20.glGetUniformLocation(blurProgram, "location");
        int uRectSize = GL20.glGetUniformLocation(blurProgram, "rectSize");
        int uColor = GL20.glGetUniformLocation(blurProgram, "color");
        int uBlurRadius = GL20.glGetUniformLocation(blurProgram, "blurRadius");

        GL20.glUniform2f(uLocation, x, y);
        GL20.glUniform2f(uRectSize, width, height);
        GL20.glUniform4f(uColor, col[0], col[1], col[2], col[3]);
        GL20.glUniform1f(uBlurRadius, blurRadius);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        drawQuads(x, y, x2, y2);

        GlStateManager.enableTexture2D();
        GL20.glUseProgram(0);
    }

    public static void drawRoundedBlurRect(float x, float y, float x2, float y2, float cornerRadius, float blurRadius, int color) {
        if (roundedBlurProgram == 0) return;

        float width = x2 - x;
        float height = y2 - y;
        float[] col = unpackColor(color);

        GL20.glUseProgram(roundedBlurProgram);

        int uLocation = GL20.glGetUniformLocation(roundedBlurProgram, "location");
        int uRectSize = GL20.glGetUniformLocation(roundedBlurProgram, "rectSize");
        int uColor = GL20.glGetUniformLocation(roundedBlurProgram, "color");
        int uRadius = GL20.glGetUniformLocation(roundedBlurProgram, "radius");
        int uBlurRadius = GL20.glGetUniformLocation(roundedBlurProgram, "blurRadius");

        GL20.glUniform2f(uLocation, x, y);
        GL20.glUniform2f(uRectSize, width, height);
        GL20.glUniform4f(uColor, col[0], col[1], col[2], col[3]);
        GL20.glUniform1f(uRadius, cornerRadius);
        GL20.glUniform1f(uBlurRadius, blurRadius);

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        drawQuads(x, y, x2, y2);

        GlStateManager.enableTexture2D();
        GL20.glUseProgram(0);
    }

    private static float[] unpackColor(int color) {
        return new float[]{
                (color >> 16 & 0xFF) / 255.0f,
                (color >> 8 & 0xFF) / 255.0f,
                (color & 0xFF) / 255.0f,
                (color >> 24 & 0xFF) / 255.0f
        };
    }
}