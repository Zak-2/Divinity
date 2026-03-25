package divinity.module.impl.render;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.base.EventPriority;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.packet.PacketEvent;
import divinity.event.impl.render.RenderGuiEvent;
import divinity.event.impl.render.RenderWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.impl.combat.Aura;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.ColorProperty;
import divinity.utils.RenderUtils;
import divinity.utils.math.TimerUtil;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.util.BlockPos;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

import static org.lwjgl.opengl.GL11.glColor4ub;

public class HitEffects extends Module {
    private final BooleanProperty hitMarker = new BooleanProperty("Hit Marker", true);
    private final BooleanProperty damageParticles = new BooleanProperty("Damage Particles", true);
    private final ColorProperty colorProperty = new ColorProperty("Particles Color", Color.LIGHT_GRAY, damageParticles::getValue);
    private final TimerUtil attackTimeOut = new TimerUtil();
    private final TimerUtil killTimeOut = new TimerUtil();
    private final ArrayList<hit> hits = new ArrayList<hit>();
    private double progress;
    private int color;
    private int lastAttackedEntity;
    private int toBeKilledEntity;
    private float lastHealth;
    private EntityLivingBase lastTarget = null;

    public HitEffects(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(hitMarker, damageParticles, colorProperty);
    }

    private static int removeAlphaComponent(int color) {
        int r = color >> 16 & 0xFF;
        int g = color >> 8 & 0xFF;
        int b = color & 0xFF;

        return ((r & 0xFF) << 16) |
                ((g & 0xFF) << 8) |
                (b & 0xFF) |
                ((0) << 24);
    }

    @EventListener(EventPriority.HIGHEST)
    public void onEvent(RenderGuiEvent event) {
        if (hitMarker.getValue() && progress > 0.0D) {
            final ScaledResolution resolution = event.getSr();

            final double xMiddle = resolution.getScaledWidth() / 2.0D;
            final double yMiddle = resolution.getScaledHeight() / 2.0D;

            GlStateManager.pushMatrix();
            GlStateManager.enableBlend();
            GlStateManager.disableTexture2D();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);

            progress = RenderUtils.linearAnimation(progress, 0.0D, 0.02D);
            int interpolatedColor = RenderUtils.fadeTo(
                    removeAlphaComponent(this.color),
                    this.color,
                    (float) progress
            );

            GlStateManager.translate(xMiddle, yMiddle, 0.0D);
            GlStateManager.rotate(45.0F, 0.0F, 0.0F, 1.0F);

            glColor4ub((byte) (interpolatedColor >> 16 & 0xFF), (byte) (interpolatedColor >> 8 & 0xFF),
                    (byte) (interpolatedColor & 0xFF), (byte) (interpolatedColor >> 24 & 0xFF));

            for (int i = 0; i < 4; i++) {
                drawHitMarker(2, 4, 1);
                if (i != 3)
                    GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }

            GlStateManager.enableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.popMatrix();
            GlStateManager.resetColor();
        }
    }

    @EventListener
    public void onEvent(RenderWorldEvent event) {
        if (damageParticles.getValue()) {
            for (hit h : hits) {
                if (h.isFinished()) {
                    hits.remove(h);
                } else {
                    h.onRender();
                }
            }
        }
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        Aura aura = ClientManager.getInstance().getModuleManager().aura;
        if (damageParticles.getValue()) {
            if (aura.target == null) {
                this.lastHealth = 20;
                lastTarget = null;
                return;
            }
            if (this.lastTarget == null || aura.target != this.lastTarget) {
                this.lastTarget = aura.target;
                this.lastHealth = aura.target.getHealth();
                return;
            }
            if (aura.target.getHealth() != this.lastHealth) {
                if (aura.target.getHealth() < this.lastHealth) {
                    this.hits.add(new hit(aura.target.getPosition().add(ThreadLocalRandom.current().nextDouble(-0.5,
                                    0.5), ThreadLocalRandom.current().nextDouble(1, 1.5),
                            ThreadLocalRandom.current().nextDouble(-0.5, 0.5)), this.lastHealth -
                            aura.target.getHealth()));
                }
                this.lastHealth = aura.target.getHealth();
            }
        }
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        if (event.isReceiving()) {
            if (event.getPacket() instanceof S19PacketEntityStatus) {
                S19PacketEntityStatus packetEntityStatus = (S19PacketEntityStatus) event.getPacket();
                final int entityId = packetEntityStatus.getEntityId();
                if (entityId == lastAttackedEntity || (!killTimeOut.hasTimeElapsed(50) && entityId == toBeKilledEntity)) {
                    switch (packetEntityStatus.getOpCode()) {
                        case 2:
                            color = ClientManager.getInstance().getMainColor().getRGB();
                            progress = 1.0D;
                            killTimeOut.reset();
                            toBeKilledEntity = lastAttackedEntity;
                            break;
                        case 3:
                            color = Color.red.getRGB();
                            progress = 1.0D;
                            toBeKilledEntity = -1;
                            break;
                    }
                    lastAttackedEntity = -1;
                }
            }
        } else if (event.isSending()) {
            if (event.getPacket() instanceof C02PacketUseEntity) {
                final C02PacketUseEntity packetUseEntity = (C02PacketUseEntity) event.getPacket();
                if (packetUseEntity.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    lastAttackedEntity = packetUseEntity.getEntityId();
                    attackTimeOut.reset();
                }
            } else if (event.getPacket() instanceof C03PacketPlayer) {
                if (lastAttackedEntity != -1 && attackTimeOut.hasTimeElapsed(500))
                    lastAttackedEntity = -1;
            }
        }
    }

    private void drawHitMarker(double xOffset, double length, double width) {
        final double halfWidth = width * 0.5D;

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2d(-(xOffset + length), -halfWidth);
        GL11.glVertex2d(-(xOffset + length), halfWidth);
        GL11.glVertex2d(-xOffset, halfWidth);
        GL11.glVertex2d(-xOffset, -halfWidth);
        GL11.glEnd();
    }


    class hit {
        private final BlockPos pos;
        private final double healthVal;
        private final long maxTime = 1000;
        private long startTime = System.currentTimeMillis();

        public hit(BlockPos pos, double healthVal) {
            this.startTime = System.currentTimeMillis();
            this.pos = pos;
            this.healthVal = healthVal;
        }

        public void onRender() {
            final double x = this.pos.getX() + (0) * mc.timer.renderPartialTicks - RenderManager.viewerPosX + 1.5;
            final double y = this.pos.getY() + (0) * mc.timer.renderPartialTicks - RenderManager.viewerPosY;
            final double z = this.pos.getZ() + (0) * mc.timer.renderPartialTicks - RenderManager.viewerPosZ;

            final float var10001 = (mc.gameSettings.thirdPersonView == 2) ? -1.0f : 1.0f;
            final double size = (2.5);
            GL11.glPushMatrix();
            GL11.glEnable(3042);
            GL11.glEnable(3042);
            GL11.glBlendFunc(770, 771);
            GL11.glEnable(2848);
            GL11.glDisable(3553);
            GL11.glDisable(2929);
            mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 0);
            GL11.glTranslated(x, y, z);
            GL11.glNormal3f(0.0f, 1.0f, 0.0f);
            GL11.glRotatef(-RenderManager.playerViewY, 0.0f, 1.0f, 0.0f);
            GL11.glRotatef(mc.getRenderManager().playerViewX, var10001, 0.0f, 0.0f);
            GL11.glScaled(-0.01666666753590107 * size, -0.01666666753590107 * size, 0.01666666753590107 * size);
            float sizePercentage;
            long timeLeft = (this.startTime + this.maxTime) - System.currentTimeMillis();
            float yPercentage = 0;
            if (timeLeft < 75) {
                sizePercentage = Math.min((float) timeLeft / 75F, 1F);
                yPercentage = Math.min((float) timeLeft / 75F, 1F);
            } else {
                sizePercentage = Math.min((float) (System.currentTimeMillis() - this.startTime) / 300F, 1F);
                yPercentage = Math.min((float) (System.currentTimeMillis() - this.startTime) / 600F, 1F);
            }
            GlStateManager.scale(0.8 * sizePercentage, 0.8 * sizePercentage, 0.8 * sizePercentage);
            Gui.drawRect(-100, -100, 100, 100, new Color(255, 0, 0, 0).getRGB());
            Color c = colorProperty.getValue();
            mc.fontRendererObj.drawStringWithShadow("-" + new DecimalFormat("#.#").format(this.healthVal), 0, -(yPercentage * 1), c.getRGB());
            GL11.glDisable(3042);
            GL11.glEnable(3553);
            GL11.glDisable(2848);
            GL11.glDisable(3042);
            GL11.glEnable(2929);
            GlStateManager.color(1.0f, 1.0f, 1.0f);
            GlStateManager.popMatrix();
        }

        public boolean isFinished() {
            return System.currentTimeMillis() - this.startTime >= maxTime;
        }
    }
}
