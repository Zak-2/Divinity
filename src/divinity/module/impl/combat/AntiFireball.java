package divinity.module.impl.combat;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.HandleInputEvent;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.utils.player.RotationUtils;
import divinity.utils.player.rotation.RequireRotationPriority;
import divinity.utils.player.rotation.RotationHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityLargeFireball;
import net.minecraft.util.Vec3;

public class AntiFireball extends Module {

    public Entity currentTarget;

    public AntiFireball(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    @RequireRotationPriority
    public void onEvent(HandleInputEvent event) {
        if (mc.thePlayer.getDistanceToEntity(currentTarget) <= mc.playerController.getBlockReachDistance()) {
            mc.clickMouse();
            mc.leftClickCounter = 0;
        }
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPre()) return;

        Entity closest = null;
        double closestDistance = 8.F;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (!(entity instanceof EntityLargeFireball)) continue;

            EntityLargeFireball fb = (EntityLargeFireball) entity;

            if (!fb.isEntityAlive()) continue;

            double distance = mc.thePlayer.getDistanceToEntity(fb);

            if (distance <= closestDistance) {
                closestDistance = distance;
                closest = fb;
            }
        }

        currentTarget = closest;

        if (currentTarget != null) {
            final Vec3 origin = RotationUtils.getHitOrigin(mc.thePlayer);
            final Vec3 vec = currentTarget.getPositionEyes(1.0F);
            final float[] rotations = RotationUtils.getRotations(new float[]{event.getYaw(), event.getPitch()}, origin, vec, 45.f);
            ClientManager.getInstance().getRotationHandler().submit(this, new RotationHandler.RotationRequest(rotations[0], rotations[1], RotationHandler.RotationPriority.ZERO, 2));
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        super.onDisable();
    }
}
