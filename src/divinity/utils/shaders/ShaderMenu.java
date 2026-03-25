package divinity.utils.shaders;

import divinity.ClientManager;
import net.minecraft.client.gui.ScaledResolution;
import org.apache.commons.io.IOUtils;
import org.lwjgl.opengl.ARBFragmentShader;
import org.lwjgl.opengl.ARBShaderObjects;
import org.lwjgl.opengl.ARBVertexShader;
import org.lwjgl.opengl.GL20;

import java.io.InputStream;
import java.util.HashMap;

public class ShaderMenu extends GLSLShader {
    private final String[] shaderFiles = {"aurora.frag", "descent.frag", "sunset.frag"};
    private float time;
    private int currentShaderIndex = 0;

    public ShaderMenu() {
        super("aurora.frag");
    }

    public void switchShader() {
        currentShaderIndex = (currentShaderIndex + 1) % shaderFiles.length;
        loadShader(shaderFiles[currentShaderIndex]);
    }

    public void loadShader(String shaderFile) {
        if (this.program != 0) {
            GL20.glDeleteProgram(this.program);
            this.program = 0;
        }
        this.uniformsMap = new HashMap<>();
        this.time = 0;

        int vertexShaderID, fragmentShaderID;
        try {
            final InputStream vertexStream = getClass().getResourceAsStream("/assets/minecraft/divinity/vertex/vertex.vert");
            if (vertexStream == null) {
                System.err.println("Vertex shader file not found: /assets/minecraft/divinity/vertex/vertex.vert");
                return;
            }
            vertexShaderID = createShader(IOUtils.toString(vertexStream), ARBVertexShader.GL_VERTEX_SHADER_ARB);
            IOUtils.closeQuietly(vertexStream);

            final InputStream fragmentStream = getClass().getResourceAsStream("/assets/minecraft/divinity/shaders/" + shaderFile);
            if (fragmentStream == null) {
                System.err.println("Fragment shader file not found: /assets/minecraft/divinity/shaders/" + shaderFile);
                return;
            }
            fragmentShaderID = createShader(IOUtils.toString(fragmentStream), ARBFragmentShader.GL_FRAGMENT_SHADER_ARB);
            IOUtils.closeQuietly(fragmentStream);
        } catch (final Exception e) {
            System.err.println("Error loading shader: " + shaderFile);
            e.printStackTrace();
            return;
        }

        if (vertexShaderID == 0 || fragmentShaderID == 0) {
            System.err.println("Failed to create vertex or fragment shader for: " + shaderFile);
            return;
        }

        this.program = ARBShaderObjects.glCreateProgramObjectARB();
        if (this.program == 0) {
            System.err.println("Failed to create shader program for: " + shaderFile);
            return;
        }

        ARBShaderObjects.glAttachObjectARB(this.program, vertexShaderID);
        ARBShaderObjects.glAttachObjectARB(this.program, fragmentShaderID);
        ARBShaderObjects.glLinkProgramARB(this.program);
        ARBShaderObjects.glValidateProgramARB(this.program);

        if (ARBShaderObjects.glGetObjectParameteriARB(this.program, ARBShaderObjects.GL_OBJECT_LINK_STATUS_ARB) == 0) {
            System.err.println("Shader program linking failed for: " + shaderFile + "\n" + getLogInfo(this.program));
            GL20.glDeleteProgram(this.program);
            this.program = 0;
            return;
        }

        setupUniforms();
    }

    @Override
    public void setupUniforms() {
        setupUniform("iResolution");
        setupUniform("iTime");
    }

    @Override
    public void updateUniforms() {
        if (this.program == 0) {
            return;
        }
        final ScaledResolution scaledResolution = new ScaledResolution(mc);

        final int resolutionID = getUniform("iResolution");
        if (resolutionID > -1) {
            GL20.glUniform2f(resolutionID, (float) scaledResolution.getScaledWidth() * 2, (float) scaledResolution.getScaledHeight() * 2);
        }

        final int timeID = getUniform("iTime");
        if (timeID > -1) {
            GL20.glUniform1f(timeID, time);
        }
        this.time += 0.002f * ClientManager.getInstance().delta;
    }
}