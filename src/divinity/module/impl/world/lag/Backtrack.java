package divinity.module.impl.world.lag;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.packet.PacketEvent;
import divinity.event.impl.render.RenderWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.RenderUtils;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S13PacketDestroyEntities;
import net.minecraft.network.play.server.S14PacketEntity;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import org.lwjgl.opengl.GL11;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Backtrack extends Module {


    private Entity real;
    private boolean forceUpdate;
    private EntityOtherPlayerMP fake;
    private final ConcurrentLinkedQueue<TimestampedPacket> packetQueue = new ConcurrentLinkedQueue<>();
    private final BooleanProperty renderFakeEnt = new BooleanProperty("Render Fake", true);
    private final NumberProperty<?> maxTime = new NumberProperty<>("Delay", 250L, 0L, 1000L, 10L);
    private final NumberProperty<?> minRange = new NumberProperty<>("MinRange", 2.5, 0.0, 6.0, 0.1);
    private final NumberProperty<?> maxRange = new NumberProperty<>("MaxRange", 4.0, 0.0, 6.0, 0.1);

    public Backtrack(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(renderFakeEnt, maxTime, minRange, maxRange);
    }


    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        fake = null;
        real = null;
        forceUpdate = false;
        releaseAllQueuedPackets();
        super.onDisable();
    }

    @EventListener
    public void onEvent(RenderWorldEvent event) {
        if (renderFakeEnt.getValue()) renderGhost(event);
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPost()) return;
        if (fake == null) return;
        tickEntity(fake);
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if (event.isReceiving()) {
            if (!isPlayingPacket(packet)) return;

            if (packetQueue.removeIf(tp -> tp.packet == packet)) return;

            handlePositionUpdate(packet);

            if (real == null) {
                releaseAllQueuedPackets();
                return;
            }

            if (fake.getPositionVector() != null && !forceUpdate) {
                double distance = distanceEyesToClosest(mc.thePlayer, fake);

                boolean tooClose = distance <= minRange.getValue().floatValue();
                boolean tooFar = distance >= maxRange.getValue().floatValue();

                if (tooClose || tooFar) {
                    forceUpdate = true;
                    real = null;
                }
            } else {
                forceUpdate = true;
            }

            long now = System.currentTimeMillis();
            long delay = maxTime.getValue().longValue();

            Iterator<TimestampedPacket> it = packetQueue.iterator();

            while (it.hasNext()) {
                TimestampedPacket tp = it.next();
                if (forceUpdate || now - tp.timestamp > delay) {
                    handlePacket(tp.packet);
                    it.remove();
                } else break;
            }

            if (real == null) fake = null;

            forceUpdate = false;

            packetQueue.offer(new TimestampedPacket(packet, now));
            event.setCancelled(true);
        } else {
            handleAttack(packet);
        }
    }

    private void handleAttack(final Packet<?> packet) {
        if (packet instanceof C02PacketUseEntity) {
            C02PacketUseEntity attack = (C02PacketUseEntity) packet;
            if (attack.getAction() == C02PacketUseEntity.Action.ATTACK) {
                final Entity entity = attack.getEntityFromWorld(mc.theWorld);

                if (entity != null && (real == null || real.getEntityId() != entity.getEntityId())) {
                    real = entity;
                    fake = new EntityOtherPlayerMP(mc.theWorld, ((EntityOtherPlayerMP) real).getGameProfile());
                    fake.setPosition(entity.posX, entity.posY, entity.posZ);
                    setServerPosition(fake, getServerPosition(real));
                }
            }
        }
    }

    private void handlePositionUpdate(final Packet<?> packet) {
        if (packet instanceof S12PacketEntityVelocity) handleEntityVelocityPacket(packet);
        if (packet instanceof S13PacketDestroyEntities) handleEntityDestroyPacket(packet);
        if (packet instanceof S14PacketEntity) handleEntityPacket(packet);
        if (packet instanceof S18PacketEntityTeleport) handleEntityTeleportPacket(packet);
    }

    private void handleEntityVelocityPacket(final Packet<?> packet) {
        final S12PacketEntityVelocity S12 = (S12PacketEntityVelocity) packet;
        if (real == null || S12.getEntityID() != real.getEntityId()) return;
        fake.motionX = S12.getMotionX() / 8000.0D;
        fake.motionY = S12.getMotionY() / 8000.0D;
        fake.motionZ = S12.getMotionZ() / 8000.0D;
    }

    private void handleEntityDestroyPacket(final Packet<?> packet) {
        final S13PacketDestroyEntities S13 = (S13PacketDestroyEntities) packet;

        for (int id : S13.getEntityIDs()) {
            if (id == real.getEntityId()) {
                real = null;
                break;
            }
        }
    }

    private void handleEntityPacket(final Packet<?> packet) {
        final S14PacketEntity s14 = (S14PacketEntity) packet;

        if (real == null || s14.getEntity(mc.theWorld) != real) return;

        createFakeEntity();

        final Vec3i serverPos = getServerPosition(fake);

        final int newX = serverPos.getX() + s14.func_149062_c();
        final int newY = serverPos.getY() + s14.func_149061_d();
        final int newZ = serverPos.getZ() + s14.func_149064_e();

        setServerPosition(fake, new Vec3i(newX, newY, newZ));

        double d0 = (double) newX / 32.0;
        double d1 = (double) newY / 32.0;
        double d2 = (double) newZ / 32.0;

        fake.setPositionAndRotation2(d0, d1, d2, fake.rotationYaw, fake.rotationPitch, 3, false);
    }

    private void handleEntityTeleportPacket(final Packet<?> packet) {
        final S18PacketEntityTeleport s18 = (S18PacketEntityTeleport) packet;

        if (real == null || s18.getEntityId() != real.getEntityId()) return;

        createFakeEntity();

        setServerPosition(fake, new Vec3i(s18.getX(), s18.getY(), s18.getZ()));

        final Vec3i serverPos = getServerPosition(fake);
        double d0 = (double) serverPos.getX() / 32.0;
        double d1 = (double) serverPos.getY() / 32.0;
        double d2 = (double) serverPos.getZ() / 32.0;
        float f = (float) (s18.getYaw() * 360) / 256.0F;
        float f1 = (float) (s18.getPitch() * 360) / 256.0F;

        if (Math.abs(fake.posX - d0) < 0.03125 && Math.abs(fake.posY - d1) < 0.015625 && Math.abs(fake.posZ - d2) < 0.03125) {
            fake.setPositionAndRotation2(fake.posX, fake.posY, fake.posZ, f, f1, 3, true);
        } else {
            fake.setPositionAndRotation2(d0, d1, d2, f, f1, 3, true);
        }
    }

    private void tickEntity(Entity entity) {
        entity.lastTickPosX = entity.posX;
        entity.lastTickPosY = entity.posY;
        entity.lastTickPosZ = entity.posZ;
        entity.prevRotationYaw = entity.rotationYaw;
        entity.prevRotationPitch = entity.rotationPitch;
        entity.ticksExisted++;

        if (entity.ridingEntity != null) entity.updateRidden();
        else entity.onUpdate();

        if (Double.isNaN(entity.posX) || Double.isInfinite(entity.posX)) entity.posX = entity.lastTickPosX;
        if (Double.isNaN(entity.posY) || Double.isInfinite(entity.posY)) entity.posY = entity.lastTickPosY;
        if (Double.isNaN(entity.posZ) || Double.isInfinite(entity.posZ)) entity.posZ = entity.lastTickPosZ;
        if (Double.isNaN(entity.rotationPitch) || Double.isInfinite(entity.rotationPitch))
            entity.rotationPitch = entity.prevRotationPitch;
        if (Double.isNaN(entity.rotationYaw) || Double.isInfinite(entity.rotationYaw))
            entity.rotationYaw = entity.prevRotationYaw;
    }

    private void renderGhost(RenderWorldEvent event) {
        if (fake == null || real == null || getServerPosition(fake).equals(getServerPosition(real))) return;

        float pt = event.getPartialTicks();

        double x = RenderUtils.interpolate(fake.prevPosX, fake.posX, pt) - RenderManager.renderPosX;
        double y = RenderUtils.interpolate(fake.prevPosY, fake.posY, pt) - RenderManager.renderPosY;
        double z = RenderUtils.interpolate(fake.prevPosZ, fake.posZ, pt) - RenderManager.renderPosZ;
        float yaw = (float) RenderUtils.interpolate(fake.prevRotationYaw, fake.rotationYaw, pt);

        mc.getTextureManager().bindTexture(((EntityOtherPlayerMP) real).getLocationSkin());
        GlStateManager.enableBlend();
        GlStateManager.enableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.f, 1.f, 1.f, 0.5f);
        mc.getRenderManager().doRenderEntity(fake, x, y, z, yaw, pt, false);
        GlStateManager.resetColor();
        GlStateManager.disableBlend();
    }

    private void createFakeEntity() {
        if (fake != null || real == null) return;
        fake = new EntityOtherPlayerMP(mc.theWorld, ((EntityOtherPlayerMP) real).getGameProfile());
        setServerPosition(fake, getServerPosition(real));
        fake.setPosition(real.posX, real.posY, real.posZ);
    }

    private Vec3i getServerPosition(Entity entity) {
        return new Vec3i(entity.serverPosX, entity.serverPosY, entity.serverPosZ);
    }

    private void setServerPosition(Entity entity, Vec3i pos) {
        entity.serverPosX = pos.getX();
        entity.serverPosY = pos.getY();
        entity.serverPosZ = pos.getZ();
    }

    private double distanceEyesToClosest(Entity self, Entity other) {
        final AxisAlignedBB box = other.getEntityBoundingBox();
        return self.getPositionEyes(1.0f).distanceTo(new Vec3(
                Math.max(box.minX, Math.min(box.maxX, self.posX)),
                Math.max(box.minY, Math.min(box.maxY, self.posY)),
                Math.max(box.minZ, Math.min(box.maxZ, self.posZ))
        ));
    }

    private void releaseAllQueuedPackets() {
        if (!packetQueue.isEmpty()) {
            for (TimestampedPacket tp : packetQueue) {
                handlePacket(tp.packet);
            }
            packetQueue.clear();
        }
    }

    private boolean isPlayingPacket(Packet<?> packet) {
        return EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet) != null;
    }

    private void handlePacket(Packet<?> packet) {
        mc.getNetHandler().getNetworkManager().wrapper$channelRead0(null, packet);
    }

    private static class TimestampedPacket {
        final Packet<?> packet;
        final long timestamp;

        TimestampedPacket(Packet<?> packet, long timestamp) {
            this.packet = packet;
            this.timestamp = timestamp;
        }
    }
}