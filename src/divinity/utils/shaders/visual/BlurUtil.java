package divinity.utils.shaders.visual;

import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GLContext;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.BufferUtils.createFloatBuffer;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;

public final class BlurUtil {

    public static final String VERTEX_SHADER =
            "#version 120\n" +
                    "void main() {\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}";
    private static final String BLUR_FRAG_SHADER =
            "#version 120\n" +
                    "\n" +
                    "uniform sampler2D texture;\n" +
                    "uniform sampler2D texture2;\n" +
                    "uniform vec2 texelSize;\n" +
                    "uniform vec2 direction;\n" +
                    "uniform float radius;\n" +
                    "uniform float weights[256];\n" +
                    "\n" +
                    "uniform float width;\n" +
                    "uniform float height;\n" +
                    "uniform float radiusTL;\n" +
                    "uniform float radiusTR;\n" +
                    "uniform float radiusBR;\n" +
                    "uniform float radiusBL;\n" +
                    "\n" +
                    "float SDRoundedCorner(vec2 p, float r) {\n" +
                    "    return length(p) - r;\n" +
                    "}\n" +
                    "\n" +
                    "float roundedMask(vec2 uv) {\n" +
                    "    vec2 size = vec2(width, height);\n" +
                    "    vec2 pixel = uv * size;\n" +
                    "    float dist = 0.0;\n" +
                    "\n" +
                    "    if (pixel.x < radiusTL && pixel.y < radiusTL) {\n" +
                    "        dist = SDRoundedCorner(pixel - vec2(radiusTL, radiusTL), radiusTL);\n" +
                    "    } else if (pixel.x > width - radiusTR && pixel.y < radiusTR) {\n" +
                    "        dist = SDRoundedCorner(pixel - vec2(width - radiusTR, radiusTR), radiusTR);\n" +
                    "    } else if (pixel.x < radiusBL && pixel.y > height - radiusBL) {\n" +
                    "        dist = SDRoundedCorner(pixel - vec2(radiusBL, height - radiusBL), radiusBL);\n" +
                    "    } else if (pixel.x > width - radiusBR && pixel.y > height - radiusBR) {\n" +
                    "        dist = SDRoundedCorner(pixel - vec2(width - radiusBR, height - radiusBR), radiusBR);\n" +
                    "    }\n" +
                    "\n" +
                    "    return 1.0 - smoothstep(0.0, 1.0, dist);\n" +
                    "}\n" +
                    "\n" +
                    "void main() {\n" +
                    "    vec4 color = vec4(0.0);\n" +
                    "    vec2 uv = gl_TexCoord[0].st;\n" +
                    "\n" +
                    "    // skip where mask is transparent on horizontal pass\n" +
                    "    if (direction.y == 0 && texture2D(texture2, uv).a == 0.0) return;\n" +
                    "\n" +
                    "    for (float f = -radius; f <= radius; f++) {\n" +
                    "        color += texture2D(texture, uv + f * texelSize * direction) * weights[int(abs(f))];\n" +
                    "    }\n" +
                    "\n" +
                    "    float mask = roundedMask(uv);\n" +
                    "    color.a *= mask;\n" +
                    "\n" +
                    "    gl_FragColor = color;\n" +
                    "}";
    private static final GLShader blurShader = new GLShader(VERTEX_SHADER, BLUR_FRAG_SHADER) {
        @Override
        public void setupUniforms() {
            this.setupUniform("texture");
            this.setupUniform("texture2");
            this.setupUniform("texelSize");
            this.setupUniform("radius");
            this.setupUniform("direction");
            this.setupUniform("weights");
            this.setupUniform("width");
            this.setupUniform("height");
            this.setupUniform("radiusTL");
            this.setupUniform("radiusTR");
            this.setupUniform("radiusBR");
            this.setupUniform("radiusBL");
        }

        @Override
        public void updateUniforms() {
            final float radius = 20f;

            glUniform1i(getUniformLocation("texture"), 0);
            glUniform1i(getUniformLocation("texture2"), 20);
            glUniform1f(getUniformLocation("radius"), radius);

            FloatBuffer buf = createFloatBuffer(256);

            for (int i = 0; i <= radius / 2f; i++) {
                buf.put(calculateGaussianOffset(i, radius / 4f));
            }

            buf.rewind();

            glUniform1(getUniformLocation("weights"), buf);
            glUniform2f(getUniformLocation("texelSize"), 1f / Display.getWidth(), 1f / Display.getHeight());
        }
    };
    private static final List<double[]> blurAreas = new ArrayList<>();
    public static boolean disableBlur;
    private static Framebuffer framebuffer, framebufferRender;

    private BlurUtil() {
    }

    public static void blurArea(double x, double y, double w, double h) {
        ContextCapabilities cc = GLContext.getCapabilities();
        if (disableBlur || !cc.OpenGL20) return;
        blurAreas.add(new double[]{x, y, w, h, 0, 0, 0, 0});
    }

    public static void blurArea(double x, double y, double w, double h, double TL, double TR, double BR, double BL) {
        ContextCapabilities cc = GLContext.getCapabilities();
        if (disableBlur || !cc.OpenGL20) return;
        blurAreas.add(new double[]{x, y, w, h, TL, TR, BR, BL});
    }

    public static void onRenderGameOverlay(Framebuffer mcFb, ScaledResolution sr) {
        if (framebuffer == null || framebufferRender == null || blurAreas.isEmpty()) return;

        framebufferRender.framebufferClear();
        framebufferRender.bindFramebuffer(false);

        for (double[] area : blurAreas) {
            ShaderUtil.glDrawRoundedQuadVarying(area[0], area[1], (float) area[2], (float) area[3], (float) area[4], (float) area[5], (float) area[6], (float) area[7], 0xFF << 24);
        }

        double[] area = blurAreas.get(0);

        blurAreas.clear();

        boolean blend = ShaderUtil.glEnableBlend();

        // horizontal
        framebuffer.bindFramebuffer(false);
        blurShader.use();
        setMaskUniforms(area);
        setDirection(1);
        glDrawFramebuffer(sr, mcFb);
        glUseProgram(0);

        // vertical
        mcFb.bindFramebuffer(false);
        blurShader.use();
        setMaskUniforms(area);
        setDirection(0);
        glActiveTexture(GL_TEXTURE20);
        glBindTexture(GL_TEXTURE_2D, framebufferRender.framebufferTexture);
        glActiveTexture(GL_TEXTURE0);
        glDrawFramebuffer(sr, framebuffer);
        glUseProgram(0);

        ShaderUtil.glRestoreBlend(blend);
    }

    private static void setMaskUniforms(double[] area) {
        glUniform1f(blurShader.getUniformLocation("width"), (float) area[2]);
        glUniform1f(blurShader.getUniformLocation("height"), (float) area[3]);
        glUniform1f(blurShader.getUniformLocation("radiusTL"), (float) area[4]);
        glUniform1f(blurShader.getUniformLocation("radiusTR"), (float) area[5]);
        glUniform1f(blurShader.getUniformLocation("radiusBR"), (float) area[6]);
        glUniform1f(blurShader.getUniformLocation("radiusBL"), (float) area[7]);
    }

    private static void setDirection(int pass) {
        glUniform2f(blurShader.getUniformLocation("direction"), 1 - pass, pass);
    }

    private static void glDrawFramebuffer(ScaledResolution sr, Framebuffer fb) {
        glBindTexture(GL_TEXTURE_2D, fb.framebufferTexture);
        glBegin(GL_QUADS);
        glTexCoord2f(0, 1);
        glVertex2i(0, 0);
        glTexCoord2f(0, 0);
        glVertex2i(0, sr.getScaledHeight());
        glTexCoord2f(1, 0);
        glVertex2i(sr.getScaledWidth(), sr.getScaledHeight());
        glTexCoord2f(1, 1);
        glVertex2i(sr.getScaledWidth(), 0);
        glEnd();
    }

    public static void onFrameBufferResize(int w, int h) {
        if (framebuffer != null) framebuffer.deleteFramebuffer();
        if (framebufferRender != null) framebufferRender.deleteFramebuffer();
        framebuffer = new Framebuffer(w, h, false);
        framebufferRender = new Framebuffer(w, h, false);
    }

    private static float calculateGaussianOffset(float x, float sigma) {
        float p = x / sigma;
        return (float) (1.0 / (Math.abs(sigma) * 2.50662827463) * Math.exp(-0.5 * p * p));
    }
}