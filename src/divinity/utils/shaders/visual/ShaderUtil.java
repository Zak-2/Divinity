package divinity.utils.shaders.visual;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL14.glBlendFuncSeparate;
import static org.lwjgl.opengl.GL20.*;

public class ShaderUtil {

    public static final String VERTEX_SHADER =
            "#version 120\n" +
                    "\n" +
                    "void main() {\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}";

    private static final String ROUNDED_QUAD_FRAG_SHADER =
            "#version 120\n" +
                    "uniform float width;\n" +
                    "uniform float height;\n" +
                    "uniform float radius;\n" +
                    "uniform vec4 color;\n" +
                    "\n" +
                    "float SDRoundedRect(vec2 p, vec2 b, float r) {\n" +
                    "    vec2 q = abs(p) - b + r;\n" +
                    "    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 size = vec2(width, height);\n" +
                    "    vec2 pixel = gl_TexCoord[0].st * size;\n" +
                    "    vec2 centre = 0.5 * size;\n" +
                    "    float b = SDRoundedRect(pixel - centre, centre, radius);\n" +
                    "    float a = 1.0 - smoothstep(0, 1.0, b);\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * a);\n" +
                    "}";

    private static final GLShader ROUNDED_QUAD_SHADER = new GLShader(VERTEX_SHADER, ROUNDED_QUAD_FRAG_SHADER) {
        @Override
        public void setupUniforms() {
            this.setupUniform("width");
            this.setupUniform("height");
            this.setupUniform("color");
            this.setupUniform("radius");
        }
    };
    private static final String ROUNDED_QUAD_OUTLINE_FRAG =
            "#version 120\n" +
                    "uniform float width;\n" +
                    "uniform float height;\n" +
                    "uniform float radius;\n" +
                    "uniform float thickness;\n" +
                    "uniform vec4 color;\n" +
                    "\n" +
                    "// SDF for rounded rect centered at origin\n" +
                    "float sdRoundedRect(vec2 p, vec2 halfSize, float r) {\n" +
                    "    vec2 d = abs(p) - halfSize;\n" +
                    "    float outsideDist = length(max(d, vec2(0.0)));\n" +
                    "    float insideDist  = min(max(d.x, d.y), 0.0);\n" +
                    "    return outsideDist + insideDist - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    // pixel‑space coords\n" +
                    "    vec2 sz = vec2(width, height);\n" +
                    "    vec2 uv = gl_TexCoord[0].st;\n" +
                    "    vec2 pos = uv * sz - 0.5 * sz;\n" +
                    "\n" +
                    "    // compute half‑extents without corner radius\n" +
                    "    vec2 halfBox = 0.5 * sz - vec2(radius);\n" +
                    "\n" +
                    "    // signed distance to shape edge\n" +
                    "    float d = sdRoundedRect(pos, halfBox, radius);\n" +
                    "\n" +
                    "    // outline distance from centerline\n" +
                    "    float halfTh = thickness * 0.5;\n" +
                    "    float dist   = abs(d) - halfTh;\n" +
                    "\n" +
                    "    // antialiasing span\n" +
                    "    float aa = fwidth(dist);\n" +
                    "\n" +
                    "    // smoothstep around dist == 0\n" +
                    "    float alpha = clamp(0.5 - dist/aa, 0.0, 1.0);\n" +
                    "\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";
    private static final GLShader ROUNDED_QUAD_OUTLINE_SHADER =
            new GLShader(VERTEX_SHADER, ROUNDED_QUAD_OUTLINE_FRAG) {
                @Override
                public void setupUniforms() {
                    setupUniform("width");
                    setupUniform("height");
                    setupUniform("radius");
                    setupUniform("thickness");
                    setupUniform("color");
                }
            };
    private static final String ROUNDED_QUAD_FRAG_SHADER_VARYING =
            "#version 120\n" +
                    "uniform float width;\n" +
                    "uniform float height;\n" +
                    "uniform float radiusTL;\n" +
                    "uniform float radiusTR;\n" +
                    "uniform float radiusBR;\n" +
                    "uniform float radiusBL;\n" +
                    "uniform vec4 color;\n" +
                    "\n" +
                    "float SDRoundedCorner(vec2 p, float r) {\n" +
                    "    return length(p) - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec2 size = vec2(width, height);\n" +
                    "    vec2 pixel = gl_TexCoord[0].st * size;\n" +
                    "    float dist = 0.0;\n" +
                    "\n" +
                    "    if (pixel.x < radiusTL && pixel.y < radiusTL) {\n" + // Top-left
                    "        dist = SDRoundedCorner(pixel - vec2(radiusTL, radiusTL), radiusTL);\n" +
                    "    } else if (pixel.x > width - radiusTR && pixel.y < radiusTR) {\n" + // Top-right
                    "        dist = SDRoundedCorner(pixel - vec2(width - radiusTR, radiusTR), radiusTR);\n" +
                    "    } else if (pixel.x < radiusBL && pixel.y > height - radiusBL) {\n" + // Bottom-left
                    "        dist = SDRoundedCorner(pixel - vec2(radiusBL, height - radiusBL), radiusBL);\n" +
                    "    } else if (pixel.x > width - radiusBR && pixel.y > height - radiusBR) {\n" + // Bottom-right
                    "        dist = SDRoundedCorner(pixel - vec2(width - radiusBR, height - radiusBR), radiusBR);\n" +
                    "    }\n" +
                    "\n" +
                    "    float a = 1.0 - smoothstep(0.0, 1.0, dist);\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * a);\n" +
                    "}";
    private static final GLShader ROUNDED_QUAD_SHADER_VARYING = new GLShader(VERTEX_SHADER, ROUNDED_QUAD_FRAG_SHADER_VARYING) {
        @Override
        public void setupUniforms() {
            this.setupUniform("width");
            this.setupUniform("height");
            this.setupUniform("radiusTL");
            this.setupUniform("radiusTR");
            this.setupUniform("radiusBR");
            this.setupUniform("radiusBL");
            this.setupUniform("color");
        }
    };
    private static final String CIRCLE_FRAG_SHADER =
            "#version 120\n" +
                    "uniform float diameter;\n" +
                    "uniform vec4 color;\n" +
                    "\n" +
                    "// Returns distance from point p to circle of radius r centered at origin\n" +
                    "float sdCircle(vec2 p, float r) {\n" +
                    "    return length(p) - r;\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    // UV coords in [0,1]\n" +
                    "    vec2 uv = gl_TexCoord[0].st;\n" +
                    "    float r = diameter * 0.5;\n" +
                    "    // remap to [-r, +r]\n" +
                    "    vec2 pos = uv * diameter - vec2(r);\n" +
                    "\n" +
                    "    float d = sdCircle(pos, r);\n" +
                    "    if (d > 0.0) {\n" +
                    "        // outside the circle → drop the fragment\n" +
                    "        discard;\n" +
                    "    }\n" +
                    "    // Optional: 1px smooth edge\n" +
                    "  //float alpha = 1.0;\n" +
                    "    float alpha = 1.0 - smoothstep(0.0, 1.0, d);\n" +
                    "\n" +
                    "    gl_FragColor = vec4(color.rgb, color.a * alpha);\n" +
                    "}";
    private static final GLShader CIRCLE_SHADER = new GLShader(VERTEX_SHADER, CIRCLE_FRAG_SHADER) {
        @Override
        public void setupUniforms() {
            setupUniform("diameter");
            setupUniform("color");
        }
    };

    public static void glDrawRoundedQuad(final double x, final double y,
                                         final float width, final float height,
                                         final float radius,
                                         final int color) {

        final boolean restore = glEnableBlend();

        final boolean alphaTest = glIsEnabled(GL_ALPHA_TEST);
        if (alphaTest) glDisable(GL_ALPHA_TEST);

        glUseProgram(ROUNDED_QUAD_SHADER.getProgram());
        glUniform1f(ROUNDED_QUAD_SHADER.getUniformLocation("width"), width);
        glUniform1f(ROUNDED_QUAD_SHADER.getUniformLocation("height"), height);
        glUniform1f(ROUNDED_QUAD_SHADER.getUniformLocation("radius"), radius);
        glUniform4f(ROUNDED_QUAD_SHADER.getUniformLocation("color"),
                (color >> 16 & 0xFF) / 255.f,
                (color >> 8 & 0xFF) / 255.f,
                (color & 0xFF) / 255.f,
                (color >> 24 & 0xFF) / 255.f);

        glDisable(GL_TEXTURE_2D);

        glBegin(GL_QUADS);

        {
            glTexCoord2f(0.f, 0.f);
            glVertex2d(x, y);

            glTexCoord2f(0.f, 1.f);
            glVertex2d(x, y + height);

            glTexCoord2f(1.f, 1.f);
            glVertex2d(x + width, y + height);

            glTexCoord2f(1.f, 0.f);
            glVertex2d(x + width, y);
        }

        glEnd();

        glUseProgram(0);

        glEnable(GL_TEXTURE_2D);

        if (alphaTest) glEnable(GL_ALPHA_TEST);

        glRestoreBlend(restore);
    }

    public static void glDrawRoundedQuadOutline(
            double x, double y,
            float width, float height,
            float radius, float thickness,
            int color
    ) {
        boolean restore = glEnableBlend();
        boolean alphaTest = glIsEnabled(GL_ALPHA_TEST);
        if (alphaTest) glDisable(GL_ALPHA_TEST);

        glUseProgram(ROUNDED_QUAD_OUTLINE_SHADER.getProgram());
        glUniform1f(ROUNDED_QUAD_OUTLINE_SHADER.getUniformLocation("width"), width);
        glUniform1f(ROUNDED_QUAD_OUTLINE_SHADER.getUniformLocation("height"), height);
        glUniform1f(ROUNDED_QUAD_OUTLINE_SHADER.getUniformLocation("radius"), radius);
        glUniform1f(ROUNDED_QUAD_OUTLINE_SHADER.getUniformLocation("thickness"), thickness);
        glUniform4f(
                ROUNDED_QUAD_OUTLINE_SHADER.getUniformLocation("color"),
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f
        );

        glDisable(GL_TEXTURE_2D);
        glBegin(GL_QUADS);
        glTexCoord2f(0f, 0f);
        glVertex2d(x, y);
        glTexCoord2f(0f, 1f);
        glVertex2d(x, y + height);
        glTexCoord2f(1f, 1f);
        glVertex2d(x + width, y + height);
        glTexCoord2f(1f, 0f);
        glVertex2d(x + width, y);
        glEnd();

        glUseProgram(0);
        glEnable(GL_TEXTURE_2D);
        if (alphaTest) glEnable(GL_ALPHA_TEST);
        glRestoreBlend(restore);
    }

    public static void glDrawRoundedQuadVarying(final double x, final double y,
                                                final float width, final float height,
                                                final float radiusTL, final float radiusTR,
                                                final float radiusBR, final float radiusBL,
                                                final int color) {

        final boolean restore = glEnableBlend();

        final boolean alphaTest = glIsEnabled(GL_ALPHA_TEST);

        if (alphaTest) glDisable(GL_ALPHA_TEST);

        glUseProgram(ROUNDED_QUAD_SHADER_VARYING.getProgram());
        glUniform1f(ROUNDED_QUAD_SHADER_VARYING.getUniformLocation("width"), width);
        glUniform1f(ROUNDED_QUAD_SHADER_VARYING.getUniformLocation("height"), height);
        glUniform1f(ROUNDED_QUAD_SHADER_VARYING.getUniformLocation("radiusTL"), radiusTL);
        glUniform1f(ROUNDED_QUAD_SHADER_VARYING.getUniformLocation("radiusTR"), radiusTR);
        glUniform1f(ROUNDED_QUAD_SHADER_VARYING.getUniformLocation("radiusBR"), radiusBR);
        glUniform1f(ROUNDED_QUAD_SHADER_VARYING.getUniformLocation("radiusBL"), radiusBL);
        glUniform4f(ROUNDED_QUAD_SHADER_VARYING.getUniformLocation("color"), (color >> 16 & 0xFF) / 255.f, (color >> 8 & 0xFF) / 255.f, (color & 0xFF) / 255.f, (color >> 24 & 0xFF) / 255.f);

        glDisable(GL_TEXTURE_2D);

        glBegin(GL_QUADS);

        {
            glTexCoord2f(0.f, 0.f);
            glVertex2d(x, y);

            glTexCoord2f(0.f, 1.f);
            glVertex2d(x, y + height);

            glTexCoord2f(1.f, 1.f);
            glVertex2d(x + width, y + height);

            glTexCoord2f(1.f, 0.f);
            glVertex2d(x + width, y);
        }

        glEnd();

        glUseProgram(0);

        glEnable(GL_TEXTURE_2D);

        if (alphaTest) glEnable(GL_ALPHA_TEST);

        glRestoreBlend(restore);
    }

    public static void glDrawCircle(double x, double y, float radius, int color) {
        float diameter = radius * 2f;

        boolean blendWas = glIsEnabled(GL_BLEND);

        if (!blendWas) {
            glEnable(GL_BLEND);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO);
        }

        boolean alphaTest = glIsEnabled(GL_ALPHA_TEST);

        if (alphaTest) glDisable(GL_ALPHA_TEST);

        glDisable(GL_TEXTURE_2D);

        glUseProgram(CIRCLE_SHADER.getProgram());
        glUniform1f(CIRCLE_SHADER.getUniformLocation("diameter"), diameter);
        glUniform4f(
                CIRCLE_SHADER.getUniformLocation("color"),
                ((color >> 16) & 0xFF) / 255f,
                ((color >> 8) & 0xFF) / 255f,
                (color & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f
        );

        glBegin(GL_QUADS);
        glTexCoord2f(0f, 0f);
        glVertex2d(x, y);
        glTexCoord2f(0f, 1f);
        glVertex2d(x, y + diameter);
        glTexCoord2f(1f, 1f);
        glVertex2d(x + diameter, y + diameter);
        glTexCoord2f(1f, 0f);
        glVertex2d(x + diameter, y);
        glEnd();

        glUseProgram(0);
        glEnable(GL_TEXTURE_2D);
        if (alphaTest) glEnable(GL_ALPHA_TEST);
        if (!blendWas) glDisable(GL_BLEND);
    }

    public static boolean glEnableBlend() {
        final boolean wasEnabled = glIsEnabled(GL_BLEND);

        if (!wasEnabled) {
            glEnable(GL_BLEND);
            glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        }

        return wasEnabled;
    }

    public static void glRestoreBlend(final boolean wasEnabled) {
        if (!wasEnabled) {
            glDisable(GL_BLEND);
        }
    }
}