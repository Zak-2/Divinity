package divinity.utils.shaders.visual;

import divinity.utils.shaders.RenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GLContext;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;

public final class BloomUtil {

    private static final String VERTEX =
            "#version 120\n" +
                    "void main() {\n" +
                    "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
                    "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
                    "}";

    private static final String BLUR =
            "#version 120\n" +
                    "uniform sampler2D texture;\n" +
                    "uniform vec2 texelSize;\n" +
                    "uniform vec2 direction;\n" +
                    "uniform vec4 colour;\n" +
                    "void main() {\n" +
                    "    vec2 uv = gl_TexCoord[0].st;\n" +
                    "    vec4 res = texture2D(texture, uv) * 0.227027;\n" +
                    "    res += texture2D(texture, uv + direction * texelSize * 1.0) * 0.1945946;\n" +
                    "    res += texture2D(texture, uv - direction * texelSize * 1.0) * 0.1945946;\n" +
                    "    res += texture2D(texture, uv + direction * texelSize * 2.0) * 0.1216216;\n" +
                    "    res += texture2D(texture, uv - direction * texelSize * 2.0) * 0.1216216;\n" +
                    "    res += texture2D(texture, uv + direction * texelSize * 3.0) * 0.054054;\n" +
                    "    res += texture2D(texture, uv - direction * texelSize * 3.0) * 0.054054;\n" +
                    "    res += texture2D(texture, uv + direction * texelSize * 4.0) * 0.016216;\n" +
                    "    res += texture2D(texture, uv - direction * texelSize * 4.0) * 0.016216;\n" +
                    "    gl_FragColor = vec4(res.rgb * colour.rgb, res.a * colour.a);\n" +
                    "}";

    private static float currentTexelX;
    private static float currentTexelY;
    private static float currentDirX;
    private static float currentDirY;
    private static float currentStrength;

    private static final GLShader blurShader = new GLShader(VERTEX, BLUR) {
        @Override
        public void setupUniforms() {
            setupUniform("texture");
            setupUniform("texelSize");
            setupUniform("direction");
            setupUniform("colour");
            glUniform1i(getUniformLocation("texture"), 0);
        }

        @Override
        public void updateUniforms() {
            glUniform2f(getUniformLocation("texelSize"), currentTexelX, currentTexelY);
            glUniform2f(getUniformLocation("direction"), currentDirX, currentDirY);
            glUniform4f(getUniformLocation("colour"), 1.0f, 1.0f, 1.0f, currentStrength);
        }
    };

    private static Framebuffer mask;
    private static Framebuffer down;
    private static Framebuffer ping;
    private static Framebuffer pong;

    public static boolean disableBloom;

    public static int downsample = 2;
    public static float strength = 1.0f;

    private static final List<RenderCallback> renders = new ArrayList<>();

    private BloomUtil() {
    }

    private static boolean supported() {
        ContextCapabilities cc = GLContext.getCapabilities();
        return cc.OpenGL20;
    }

    public static void bloom(RenderCallback render) {
        if (disableBloom || !supported()) return;
        renders.add(render);
    }

    public static void drawAndBloom(RenderCallback render) {
        render.render();
        if (disableBloom || !supported()) return;
        renders.add(render);
    }

    public static void onRenderGameOverlay(Framebuffer mcFramebuffer) {
        if (disableBloom || !supported()) {
            renders.clear();
            return;
        }
        if (renders.isEmpty()) return;

        Minecraft mc = Minecraft.getMinecraft();
        ensureBuffers(mc.displayWidth, mc.displayHeight);

        glPushAttrib(GL_ALL_ATTRIB_BITS);

        int prevMatrixMode = glGetInteger(GL_MATRIX_MODE);

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();

        try {
            mask.framebufferClear();
            mask.bindFramebuffer(false);
            glViewport(0, 0, mask.framebufferWidth, mask.framebufferHeight);
            beginNDC();

            for (RenderCallback cb : renders) cb.render();
            renders.clear();

            endNDC();

            int smallW = down.framebufferWidth;
            int smallH = down.framebufferHeight;

            down.framebufferClear();
            down.bindFramebuffer(false);
            glViewport(0, 0, smallW, smallH);
            beginNDC();
            drawTextureNDC(mask.framebufferTexture);
            endNDC();

            ping.framebufferClear();
            ping.bindFramebuffer(false);
            glViewport(0, 0, smallW, smallH);
            beginNDC();
            blurPass(down.framebufferTexture, smallW, smallH, 1.0f, 0.0f);
            endNDC();

            pong.framebufferClear();
            pong.bindFramebuffer(false);
            glViewport(0, 0, smallW, smallH);
            beginNDC();
            blurPass(ping.framebufferTexture, smallW, smallH, 0.0f, 1.0f);
            endNDC();

            mcFramebuffer.bindFramebuffer(false);
            glViewport(0, 0, mc.displayWidth, mc.displayHeight);

            glDisable(GL_DEPTH_TEST);
            glDepthMask(false);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE);

            beginNDC();
            drawTextureNDC(pong.framebufferTexture);
            endNDC();

            glDepthMask(true);

            mask.framebufferClear();
            down.framebufferClear();
            ping.framebufferClear();
            pong.framebufferClear();

            mcFramebuffer.bindFramebuffer(false);
            glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        } finally {
            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();

            glMatrixMode(prevMatrixMode);

            glPopAttrib();
        }
    }

    public static void onFrameBufferResize(int width, int height) {
        delete();
        allocate(width, height);
    }

    private static void ensureBuffers(int width, int height) {
        if (mask == null) {
            allocate(width, height);
            return;
        }
        if (mask.framebufferWidth != width || mask.framebufferHeight != height) {
            delete();
            allocate(width, height);
        }
    }

    private static void allocate(int width, int height) {
        int ds = Math.max(1, downsample);
        int w = Math.max(1, width / ds);
        int h = Math.max(1, height / ds);

        mask = new Framebuffer(width, height, false);
        down = new Framebuffer(w, h, false);
        ping = new Framebuffer(w, h, false);
        pong = new Framebuffer(w, h, false);
    }

    private static void delete() {
        if (mask != null) mask.deleteFramebuffer();
        if (down != null) down.deleteFramebuffer();
        if (ping != null) ping.deleteFramebuffer();
        if (pong != null) pong.deleteFramebuffer();
        mask = null;
        down = null;
        ping = null;
        pong = null;
    }

    private static void blurPass(int texture, int w, int h, float dirX, float dirY) {
        currentTexelX = 1.0f / w;
        currentTexelY = 1.0f / h;
        currentDirX = dirX;
        currentDirY = dirY;
        currentStrength = strength;

        blurShader.use();
        drawTextureNDC(texture);
        glUseProgram(0);
    }

    private static void beginNDC() {
        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(-1.0, 1.0, -1.0, 1.0, -1.0, 1.0);

        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();

        glDisable(GL_CULL_FACE);
        glEnable(GL_TEXTURE_2D);
    }

    private static void endNDC() {
        glMatrixMode(GL_MODELVIEW);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glPopMatrix();
        glMatrixMode(GL_MODELVIEW);
    }

    private static void drawTextureNDC(int texture) {
        glBindTexture(GL_TEXTURE_2D, texture);

        glBegin(GL_QUADS);

        glTexCoord2f(0, 1);
        glVertex2f(-1, -1);

        glTexCoord2f(0, 0);
        glVertex2f(-1, 1);

        glTexCoord2f(1, 0);
        glVertex2f(1, 1);

        glTexCoord2f(1, 1);
        glVertex2f(1, -1);

        glEnd();
    }
}
