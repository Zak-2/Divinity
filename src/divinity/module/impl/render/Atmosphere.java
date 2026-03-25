package divinity.module.impl.render;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.render.RenderWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.ColorProperty;
import divinity.module.property.impl.ModeProperty;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.RenderUtils;
import divinity.utils.shaders.visual.BloomUtil;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Atmosphere extends Module {

    private final BooleanProperty worldTime = new BooleanProperty("World Time", true);
    private final ModeProperty timeMode = new ModeProperty("Time Mode", "Static", worldTime::getValue, "Static", "Real");
    private final NumberProperty<Integer> worldTimeSlider = new NumberProperty<>("Time", 12500, 0, 24000, 1000, () -> worldTime.getValue() && timeMode.isMode("Static"));

    private final ModeProperty weatherMode = new ModeProperty("Weather", "Normal", "Normal", "Clear", "Rain", "Thunder", "Snow");

    private final ModeProperty fogMode = new ModeProperty("Fog", "Normal", "Normal", "None", "Clear", "Override");
    private final NumberProperty<Double> fogStart = new NumberProperty<>("Fog Start", 0.0, 0.0, 1024.0, 1.0, () -> fogMode.isMode("Override"));
    private final NumberProperty<Double> fogEnd = new NumberProperty<>("Fog End", 256.0, 1.0, 2048.0, 1.0, () -> fogMode.isMode("Override"));
    private final ColorProperty fogColor = new ColorProperty("Fog Color", new Color(180, 200, 255, 255), () -> fogMode.isMode("Override"));
    private final ColorProperty skyColor = new ColorProperty("Sky Color", new Color(180, 200, 255, 255), () -> fogMode.isMode("Override"));

    private final BooleanProperty particles = new BooleanProperty("Particles", false);
    private final BooleanProperty particleGradient = new BooleanProperty("Gradient Color", false);
    private final ModeProperty particleMode = new ModeProperty("Particle Mode", "Pyramids", particles::getValue, "Dodecahedrons", "Stars", "Pyramids", "Cubes");

    private final NumberProperty<Integer> particleMax = new NumberProperty<>("Particle Amount", 100, 0, 400, 1, particles::getValue);
    private final NumberProperty<Double> particleSpawnRate = new NumberProperty<>("Spawn Rate", 55.0, 0.0, 200.0, 1.0, particles::getValue);
    private final NumberProperty<Double> particleRange = new NumberProperty<>("Range", 12.0, 0.5, 12.0, 0.1, particles::getValue);
    private final NumberProperty<Double> particleSize = new NumberProperty<>("Size", 0.10, 0.05, 0.9, 0.01, particles::getValue);
    private final NumberProperty<Double> particleLife = new NumberProperty<>("Life", 2.2, 0.5, 10.0, 0.1, particles::getValue);
    private final NumberProperty<Integer> particleAlpha = new NumberProperty<>("Alpha", 255, 0, 255, 1, particles::getValue);
    private final NumberProperty<Double> lineWidth = new NumberProperty<>("Line Width", 1.0, 1.0, 4.0, 0.1, particles::getValue);

    private final BooleanProperty bloom = new BooleanProperty("Bloom", true, particles::getValue);
    private final NumberProperty<Double> bloomBoost = new NumberProperty<>("Bloom Boost", 1.6, 1.0, 3.0, 0.1, () -> particles.getValue() && bloom.getValue());

    private final ColorProperty particleColor = new ColorProperty("Particle Color", ClientManager.getInstance().getMainColor(), particles::getValue);
    private final ColorProperty particleSecColor = new ColorProperty("Particle Secondary Color", ClientManager.getInstance().getSecondaryColor(), () -> particles.getValue() && particleGradient.getValue());

    private final List<AtmoParticle> particleList = new ArrayList<>();
    private final Random rng = new Random();

    private long lastFrameTime = -1L;
    private double spawnAccumulator = 0.0;

    public Atmosphere(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(
                worldTime, timeMode, worldTimeSlider,
                weatherMode,
                fogMode, fogStart, fogEnd, fogColor, skyColor,
                particles, particleGradient, particleMode, particleMax, particleSpawnRate, particleRange, particleSize, particleLife, particleAlpha, lineWidth,
                bloom, bloomBoost,
                particleColor, particleSecColor
        );
    }

    @Override
    public void onEnable() {
        particleList.clear();
        lastFrameTime = -1L;
        spawnAccumulator = 0.0;
    }

    @Override
    public void onDisable() {
        particleList.clear();
    }

    public boolean updateWorldTime() {
        return isState() && worldTime.getValue();
    }

    public long getWorldTime() {
        if (!updateWorldTime()) return 0L;

        if (timeMode.isMode("Real")) {
            LocalTime now = LocalTime.now();
            int seconds = now.toSecondOfDay();
            double fraction = seconds / 86400.0;
            return (long) ((fraction * 24000.0 + 18000.0) % 24000.0);
        }

        return worldTimeSlider.getValue().longValue();
    }

    public boolean overrideWeather() {
        return isState() && !weatherMode.isMode("Normal");
    }

    public boolean overrideRaining() {
        return overrideWeather() && (weatherMode.isMode("Rain") || weatherMode.isMode("Thunder") || weatherMode.isMode("Snow"));
    }

    public boolean overrideThundering() {
        return overrideWeather() && weatherMode.isMode("Thunder");
    }

    public float getRainStrengthOverride(float partialTicks) {
        if (!overrideWeather()) return -1.0F;
        return overrideRaining() ? 1.0F : 0.0F;
    }

    public float getThunderStrengthOverride(float partialTicks) {
        if (!overrideWeather()) return -1.0F;
        return overrideThundering() ? 1.0F : 0.0F;
    }

    public boolean forceSnow() {
        return overrideWeather() && weatherMode.isMode("Snow");
    }

    public boolean overrideFog() {
        return isState() && !fogMode.isMode("Normal");
    }

    public boolean fogNone() {
        return isState() && fogMode.isMode("None");
    }

    public boolean fogClear() {
        return isState() && fogMode.isMode("Clear");
    }

    public boolean fogOverride() {
        return isState() && fogMode.isMode("Override");
    }

    public float getFogStartDistance() {
        return fogStart.getValue().floatValue();
    }

    public float getFogEndDistance() {
        float end = fogEnd.getValue().floatValue();
        float start = fogStart.getValue().floatValue();
        return Math.max(end, start + 0.1F);
    }

    public Vec3 getOverrideFogColor() {
        Color c = fogColor.getValue();
        return new Vec3(c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0);
    }

    public Vec3 getOverrideSkyColor() {
        Color c = skyColor.getValue();
        return new Vec3(c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0);
    }

    @EventListener
    public void onRenderWorld(RenderWorldEvent event) {
        if (!particles.getValue()) return;
        if (mc.theWorld == null || mc.thePlayer == null) return;

        long now = System.nanoTime();
        if (lastFrameTime == -1L) lastFrameTime = now;
        double dt = (now - lastFrameTime) / 1_000_000_000.0;
        lastFrameTime = now;

        if (dt <= 0.0 || dt > 0.5) dt = 0.016;

        updateParticles(dt);

        int max = particleMax.getValue();
        if (max > 0) {
            spawnAccumulator += dt * particleSpawnRate.getValue();
            while (spawnAccumulator >= 1.0 && particleList.size() < max) {
                spawnParticle();
                spawnAccumulator -= 1.0;
            }
        }

        float pt = event.getPartialTicks();

        renderParticlesWorld(pt, false);

        if (bloom.getValue()) {
            BloomUtil.bloom(() -> renderParticlesToBloom(pt));
        }
    }

    private void updateParticles(double dt) {
        Iterator<AtmoParticle> it = particleList.iterator();
        while (it.hasNext()) {
            AtmoParticle p = it.next();
            p.age += dt;
            if (p.age >= p.life) {
                it.remove();
                continue;
            }

            double drift = 0.18;
            p.vx += Math.sin(p.seed + p.age * 1.6) * drift * dt * 0.15;
            p.vz += Math.cos(p.seed + p.age * 1.4) * drift * dt * 0.15;
            p.vy += 0.06 * dt;

            p.x += p.vx * dt;
            p.y += p.vy * dt;
            p.z += p.vz * dt;
            p.rot += (float) (p.rotSpeed * dt);
        }
    }

    private void spawnParticle() {
        double range = particleRange.getValue();
        double px = mc.thePlayer.posX;
        double py = mc.thePlayer.posY + mc.thePlayer.getEyeHeight() - 0.25;
        double pz = mc.thePlayer.posZ;

        double ox = (rng.nextDouble() * 2.0 - 1.0) * range;
        double oz = (rng.nextDouble() * 2.0 - 1.0) * range;
        double oy = (rng.nextDouble() * 2.0 - 1.0) * (range * 0.45);

        double x = px + ox;
        double y = py + oy;
        double z = pz + oz;

        double vx = (rng.nextDouble() * 2.0 - 1.0) * 0.10;
        double vy = rng.nextDouble() * 0.06;
        double vz = (rng.nextDouble() * 2.0 - 1.0) * 0.10;

        float size = particleSize.getValue().floatValue() * (0.65F + rng.nextFloat() * 0.8F);
        float life = particleLife.getValue().floatValue() * (0.75F + rng.nextFloat() * 0.6F);
        float rot = rng.nextFloat() * 360.0F;
        float rotSpeed = (rng.nextFloat() * 2.0F - 1.0F) * 70.0F;

        particleList.add(new AtmoParticle(x, y, z, vx, vy, vz, size, life, rot, rotSpeed, rng.nextDouble() * 10.0));
    }

    private void renderParticlesToBloom(float partialTicks) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        mc.entityRenderer.setupCameraTransform(partialTicks, 2);

        renderParticlesWorld(partialTicks, true);

        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    private void renderParticlesWorld(float partialTicks, boolean bloomPass) {
        if (particleList.isEmpty()) return;

        Color base = particleColor.getValue();
        Color sec = particleGradient.getValue() ? particleSecColor.getValue() : base;

        float br = base.getRed() / 255.0F;
        float bg = base.getGreen() / 255.0F;
        float bb = base.getBlue() / 255.0F;

        float sr = sec.getRed() / 255.0F;
        float sg = sec.getGreen() / 255.0F;
        float sb = sec.getBlue() / 255.0F;

        float aBase = particleAlpha.getValue() / 255.0F;
        float lw = lineWidth.getValue().floatValue();

        if (bloomPass) {
            aBase *= bloomBoost.getValue().floatValue();
            lw *= 1.35F;
        }

        GL11.glPushMatrix();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);

        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
        GL11.glLineWidth(lw);

        for (AtmoParticle p : particleList) {
            float t = (float) (p.age / p.life);
            float fadeIn = Math.min(1.0F, t * 3.0F);
            float fadeOut = 1.0F - t;
            float a = aBase * fadeIn * fadeOut;

            if (a <= 0.002F) continue;

            switch (particleMode.getValue()) {
                case "Pyramids":
                    renderPyramidWire(p, br, bg, bb, sr, sg, sb, a);
                    break;
                case "Cubes":
                    renderCubeWire(p, br, bg, bb, sr, sg, sb, a);
                    break;
                case "Stars":
                    renderPentagrammicPrism(p, br, bg, bb, sr, sg, sb, a);
                    break;
                case "Dodecahedrons":
                    renderDodecahedron(p, br, bg, bb, sr, sg, sb, a);
                    break;
            }
        }

        GL11.glLineWidth(1.0F);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_BLEND);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);

        GL11.glPopMatrix();

        GL11.glColor4f(1F, 1F, 1F, 1F);
    }

    // helper methods (put these in your class)
    private float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }

    private void glColorLerp(float br, float bg, float bb, float sr, float sg, float sb, float a, float t) {
        t = clamp01(t);
        float r = br * (1.0f - t) + sr * t;
        float g = bg * (1.0f - t) + sg * t;
        float b = bb * (1.0f - t) + sb * t;
        GL11.glColor4f(r, g, b, a);
    }

    // -------------------- Pyramid (wire) --------------------
    private void renderPyramidWire(AtmoParticle p, float br, float bg, float bb, float sr, float sg, float sb, float a) {
        double rx = p.x - RenderManager.renderPosX;
        double ry = p.y - RenderManager.renderPosY;
        double rz = p.z - RenderManager.renderPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(rx, ry, rz);

        RenderManager rm = mc.getRenderManager();
        GL11.glRotatef(-RenderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(p.rot, 0.0F, 0.0F, 1.0F);

        float s = p.size;
        float halfS = s * 0.5f;

        float[] xs = new float[] { -halfS, halfS, halfS, -halfS, 0.0f }; // 4 base corners + apex x
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (float x : xs) { if (x < minX) minX = x; if (x > maxX) maxX = x; }
        float range = maxX - minX;
        if (range <= 1e-6f) range = 1f; // avoid div by zero

        float x1 = -halfS, z1 = -halfS;
        float x2 = halfS,  z2 = -halfS;
        float x3 = halfS,  z3 = halfS;
        float x4 = -halfS, z4 = halfS;
        float apexY = s;

        // base loop (per-vertex color)
        GL11.glBegin(GL11.GL_LINE_LOOP);
        float[] bx = { x1, x2, x3, x4 };
        float[] bz = { z1, z2, z3, z4 };
        for (int i = 0; i < 4; i++) {
            float vx = bx[i];
            float t = (vx - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,t);
            GL11.glVertex3f(vx, 0, bz[i]);
        }
        GL11.glEnd();

        // edges to apex
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 4; i++) {
            float vx = bx[i];
            float t1 = (vx - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,t1);
            GL11.glVertex3f(vx, 0, bz[i]);

            float tApex = (0.0f - minX) / range; // apex x = 0
            glColorLerp(br,bg,bb,sr,sg,sb,a,tApex);
            GL11.glVertex3f(0, apexY, 0);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    // -------------------- Cube (wire) --------------------
    private void renderCubeWire(AtmoParticle p, float br, float bg, float bb, float sr, float sg, float sb, float a) {
        double rx = p.x - RenderManager.renderPosX;
        double ry = p.y - RenderManager.renderPosY;
        double rz = p.z - RenderManager.renderPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(rx, ry, rz);

        RenderManager rm = mc.getRenderManager();
        GL11.glRotatef(-RenderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(p.rot, 0.0F, 0.0F, 1.0F);

        float s = p.size * 0.55F;
        // 8 corners relative to center
        float[][] v = new float[][] {
                { -s, -s, -s }, {  s, -s, -s }, {  s, -s,  s }, { -s, -s,  s },
                { -s,  s, -s }, {  s,  s, -s }, {  s,  s,  s }, { -s,  s,  s }
        };

        // compute minX/maxX in local coords:
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (int i = 0; i < v.length; i++) {
            if (v[i][0] < minX) minX = v[i][0];
            if (v[i][0] > maxX) maxX = v[i][0];
        }
        float range = maxX - minX;
        if (range <= 1e-6f) range = 1f;

        // edges between vertices (pairs)
        int[][] edges = {
                {0,1},{1,2},{2,3},{3,0}, // bottom
                {4,5},{5,6},{6,7},{7,4}, // top
                {0,4},{1,5},{2,6},{3,7}  // verticals
        };

        GL11.glBegin(GL11.GL_LINES);
        for (int[] e : edges) {
            int i = e[0], j = e[1];
            float t1 = (v[i][0] - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,t1);
            GL11.glVertex3f(v[i][0], v[i][1], v[i][2]);

            float t2 = (v[j][0] - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,t2);
            GL11.glVertex3f(v[j][0], v[j][1], v[j][2]);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    private void renderPentagrammicPrism(AtmoParticle p, float br, float bg, float bb, float sr, float sg, float sb, float a) {
        double rx = p.x - RenderManager.renderPosX;
        double ry = p.y - RenderManager.renderPosY;
        double rz = p.z - RenderManager.renderPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(rx, ry, rz);

        RenderManager rm = mc.getRenderManager();
        GL11.glRotatef(-RenderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(p.rot, 0.0F, 0.0F, 1.0F);

        float outerRadius = p.size;
        float depth = outerRadius * 0.3f;
        float innerRadius = outerRadius * 0.4f;

        float[][] frontStar = new float[10][3];
        float[][] backStar = new float[10][3];

        for (int i = 0; i < 10; i++) {
            float radius = (i % 2 == 0) ? outerRadius : innerRadius;
            float angle = (float) (Math.PI * i / 5.0f - Math.PI / 2.0f);

            frontStar[i][0] = (float) (radius * Math.cos(angle));
            frontStar[i][1] = (float) (radius * Math.sin(angle));
            frontStar[i][2] = depth / 2;

            backStar[i][0] = frontStar[i][0];
            backStar[i][1] = frontStar[i][1];
            backStar[i][2] = -depth / 2;
        }

        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (int i = 0; i < 10; i++) {
            float fx = frontStar[i][0];
            if (fx < minX) minX = fx;
            if (fx > maxX) maxX = fx;
            float bx = backStar[i][0];
            if (bx < minX) minX = bx;
            if (bx > maxX) maxX = bx;
        }
        float range = maxX - minX;
        if (range <= 1e-6f) range = 1f;

        // front loop
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 10; i++) {
            float vx = frontStar[i][0];
            float t = (vx - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,t);
            GL11.glVertex3f(frontStar[i][0], frontStar[i][1], frontStar[i][2]);
        }
        GL11.glEnd();

        // back loop
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i < 10; i++) {
            float vx = backStar[i][0];
            float t = (vx - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,t);
            GL11.glVertex3f(backStar[i][0], backStar[i][1], backStar[i][2]);
        }
        GL11.glEnd();

        // connectors
        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < 10; i++) {
            float fx = frontStar[i][0];
            float bx = backStar[i][0];
            float tF = (fx - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,tF);
            GL11.glVertex3f(frontStar[i][0], frontStar[i][1], frontStar[i][2]);

            float tB = (bx - minX) / range;
            glColorLerp(br,bg,bb,sr,sg,sb,a,tB);
            GL11.glVertex3f(backStar[i][0], backStar[i][1], backStar[i][2]);
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    // -------------------- Dodecahedron (wire by connecting nearby vertices) --------------------
    private void renderDodecahedron(AtmoParticle p, float br, float bg, float bb, float sr, float sg, float sb, float a) {
        double rx = p.x - RenderManager.renderPosX;
        double ry = p.y - RenderManager.renderPosY;
        double rz = p.z - RenderManager.renderPosZ;

        GL11.glPushMatrix();
        GL11.glTranslated(rx, ry, rz);

        RenderManager rm = mc.getRenderManager();
        GL11.glRotatef(-RenderManager.playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(rm.playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glRotatef(p.rot, 0.0F, 0.0F, 1.0F);

        float s = p.size;
        float phi = (float)((1.0 + Math.sqrt(5.0)) / 2.0);
        float[][] icosaVertices = {
                {-1, phi, 0}, {1, phi, 0}, {-1, -phi, 0}, {1, -phi, 0},
                {0, -1, phi}, {0, 1, phi}, {0, -1, -phi}, {0, 1, -phi},
                {phi, 0, -1}, {phi, 0, 1}, {-phi, 0, -1}, {-phi, 0, 1}
        };

        for (int i = 0; i < icosaVertices.length; i++) {
            icosaVertices[i][0] *= s * 0.3f;
            icosaVertices[i][1] *= s * 0.3f;
            icosaVertices[i][2] *= s * 0.3f;
        }

        int[][] icosaFaces = {
                {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
                {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
                {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
                {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}
        };

        float[][] dodecaVertices = new float[20][3];
        for (int i = 0; i < icosaFaces.length; i++) {
            int v1 = icosaFaces[i][0];
            int v2 = icosaFaces[i][1];
            int v3 = icosaFaces[i][2];
            dodecaVertices[i][0] = (icosaVertices[v1][0] + icosaVertices[v2][0] + icosaVertices[v3][0]) / 3.0f;
            dodecaVertices[i][1] = (icosaVertices[v1][1] + icosaVertices[v2][1] + icosaVertices[v3][1]) / 3.0f;
            dodecaVertices[i][2] = (icosaVertices[v1][2] + icosaVertices[v2][2] + icosaVertices[v3][2]) / 3.0f;
        }

        // compute minX/maxX
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        for (int i = 0; i < dodecaVertices.length; i++) {
            float vx = dodecaVertices[i][0];
            if (vx < minX) minX = vx;
            if (vx > maxX) maxX = vx;
        }
        float range = maxX - minX;
        if (range <= 1e-6f) range = 1f;

        float connectionThreshold = s * 0.35f;
        float thrSq = connectionThreshold * connectionThreshold;

        GL11.glBegin(GL11.GL_LINES);
        for (int i = 0; i < dodecaVertices.length; i++) {
            for (int j = i + 1; j < dodecaVertices.length; j++) {
                float dx = dodecaVertices[i][0] - dodecaVertices[j][0];
                float dy = dodecaVertices[i][1] - dodecaVertices[j][1];
                float dz = dodecaVertices[i][2] - dodecaVertices[j][2];
                float distSq = dx*dx + dy*dy + dz*dz;
                if (distSq < thrSq) {
                    float t1 = (dodecaVertices[i][0] - minX) / range;
                    glColorLerp(br,bg,bb,sr,sg,sb,a,t1);
                    GL11.glVertex3f(dodecaVertices[i][0], dodecaVertices[i][1], dodecaVertices[i][2]);

                    float t2 = (dodecaVertices[j][0] - minX) / range;
                    glColorLerp(br,bg,bb,sr,sg,sb,a,t2);
                    GL11.glVertex3f(dodecaVertices[j][0], dodecaVertices[j][1], dodecaVertices[j][2]);
                }
            }
        }
        GL11.glEnd();

        GL11.glPopMatrix();
    }

    private static final class AtmoParticle {
        private double x;
        private double y;
        private double z;

        private double vx;
        private double vy;
        private double vz;

        private final float size;
        private final float life;
        private double age;

        private float rot;
        private final float rotSpeed;

        private final double seed;

        private AtmoParticle(double x, double y, double z, double vx, double vy, double vz, float size, float life, float rot, float rotSpeed, double seed) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.size = size;
            this.life = life;
            this.rot = rot;
            this.rotSpeed = rotSpeed;
            this.seed = seed;
            this.age = 0.0;
        }
    }
}
