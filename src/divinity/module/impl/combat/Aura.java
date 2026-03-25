package divinity.module.impl.combat;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.friend.FriendManager;
import divinity.handler.BlinkHandler;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.ModeProperty;
import divinity.module.property.impl.MultiSelectProperty;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.player.LocalPlayerUtils;
import divinity.utils.player.RotationUtils;
import divinity.utils.player.rotation.RequireRotationPriority;
import divinity.utils.player.rotation.RotationHandler;
import divinity.utils.server.ServerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Aura extends Module {

    private int interactTick;
    private boolean isBlocking;
    public EntityLivingBase target;
    private final List<EntityLivingBase> validatedTargets = new ArrayList<>();
    private final BlinkHandler blinkHandler = ClientManager.getInstance().getBlinkHandler();
    private final NumberProperty<?> reach = new NumberProperty<>("Reach", 3.2, 3.0, 6.0, 0.1);
    private final ModeProperty sorting = new ModeProperty("Sorting", "Distance", "Distance", "Health", "Angle", "Armor", "HurtTime");
    private final MultiSelectProperty addons = new MultiSelectProperty("Addons", new String[]{"Auto Block", "Raytrace Attack", "Raytrace Block", "Aim Through Walls"}, new String[]{"Raytrace Attack", "Raytrace Block", "Aim Through Walls"});
    private final MultiSelectProperty allowedTargets = new MultiSelectProperty("Targets", new String[]{"Players", "Monsters", "Animals"}, new String[]{"Players"});

    public Aura(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(reach, sorting, addons, allowedTargets);
    }

    @EventListener
    @RequireRotationPriority
    public void onAttack(UpdateEvent event) {
        if (event.isPost()) return;

        if (target == null) {
            releaseBlock();
            return;
        }

        interactTick++;

        switch (interactTick) {
            case 1:
                releaseBlock();
                break;
            case 2:
                performAttack();
                applyBlock();
                interactTick = 0;
                break;
        }
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (event.isPost()) return;
        reset();
        updateCache();
        determineTarget();
        forceBlockAnimation();
        rotateToTarget(event);
    }

    private void reset() {
        target = null;
        validatedTargets.clear();
    }

    private void updateCache() {
        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (entity instanceof EntityLivingBase) {
                EntityLivingBase living = (EntityLivingBase) entity;
                if (mc.thePlayer.getDistanceToEntity(living) <= reach.getValue().floatValue() && isValid(living)) {
                    validatedTargets.add(living);
                }
            }
        }

        switch (sorting.getValue()) {
            case "HurtTime":
                validatedTargets.sort(Comparator.comparingDouble(e -> e.hurtTime));
                break;
            case "Distance":
                validatedTargets.sort(Comparator.comparingDouble(e -> mc.thePlayer.getDistanceToEntity(e)));
                break;
            case "Health":
                validatedTargets.sort(Comparator.comparingDouble(EntityLivingBase::getHealth));
                break;
            case "Angle":
                validatedTargets.sort(Comparator.comparingDouble(e -> {
                    double yawDiff = Math.abs(RotationUtils.calculateYawFromSrcToDst(
                            mc.thePlayer.rotationYaw,
                            mc.thePlayer.posX,
                            mc.thePlayer.posZ,
                            e.posX,
                            e.posZ
                    ) - mc.thePlayer.rotationYaw);
                    return yawDiff > 180 ? 360 - yawDiff : yawDiff;
                }));
                break;
            case "Armor":
                validatedTargets.sort(Comparator.comparingDouble(EntityLivingBase::getTotalArmorValue));
                break;
        }
    }

    private void determineTarget() {
        if (validatedTargets.isEmpty()) return;

        target = validatedTargets.get(0);
    }

    private void forceBlockAnimation() {
        if (addons.isSelected("Auto Block") && LocalPlayerUtils.isHoldingSword() && target != null)
            mc.getItemRenderer().fakeUseTicks = 1;
    }

    private void rotateToTarget(UpdateEvent event) {
        if (target == null) return;

        final Vec3 origin = RotationUtils.getHitOrigin(mc.thePlayer);
        final AxisAlignedBB box = RotationUtils.getHittableBoundingBox(target, 0.1);
        final Vec3 hitVec = RotationUtils.getAttackHitVec(mc, origin, box, RotationUtils.RotationsPoint.CHEST.getHitVec(origin, box), addons.isSelected("Aim Through Walls"), -1);

        if (hitVec == null) return;

        float[] rots = RotationUtils.getRotations(new float[]{event.getYaw(), event.getPitch()}, origin, hitVec, 45.F);

        ClientManager.getInstance().getRotationHandler().submit(this, new RotationHandler.RotationRequest(rots[0], rots[1], RotationHandler.RotationPriority.TWO));
    }

    private void performAttack() {
        if (target == null) return;
        if (mc.thePlayer.isUsingItem()) return;
        if (addons.isSelected("Raytrace Attack") && !RotationUtils.mouseOverEntity(mc, target)) return;
        mc.clickMouse();
    }

    private void applyBlock() {
        if (LocalPlayerUtils.isHoldingSword() && addons.isSelected("Auto Block") && !isBlocking) {
            final Vec3 hitVec = new Vec3(0, 1, 0);

            if (addons.isSelected("Raytrace Block") && RotationUtils.mouseOverEntity(mc, target)) {
                mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(target, hitVec));
                mc.thePlayer.sendQueue.addToSendQueue(new C02PacketUseEntity(target, C02PacketUseEntity.Action.INTERACT));
            }

            mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.getHeldItem()));
            isBlocking = true;
        }
    }

    private void releaseBlock() {
        if (isBlocking) {
            blinkHandler.setClientActive(this, true, 1);

            int originalSlot = mc.thePlayer.inventory.currentItem;
            int swapToSlot = (originalSlot + 1) % 9;

            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(swapToSlot));
            mc.thePlayer.sendQueue.addToSendQueue(new C09PacketHeldItemChange(originalSlot));
            mc.playerController.onStoppedUsingItem(mc.thePlayer);

            isBlocking = false;
        }
    }

    private boolean isValid(EntityLivingBase target) {
        final FriendManager manager = ClientManager.getInstance().getFriendManager();

        if (target == mc.thePlayer) return false;
        if (!target.isEntityAlive()) return false;
        if (target.isInvisible()) return false;
        if (!isAllowed(target)) return false;

        if (target instanceof EntityPlayer) {
            if (ServerUtils.isOnHypixel()) {
                if (!ServerUtils.isInTabList(target)) return false;
                if (manager.isSameTeamByName(target)) return false;
            }
            if (manager.isFriend(target.getName())) return false;
        }

        return true;
    }

    private boolean isAllowed(EntityLivingBase entity) {
        if (entity instanceof EntityPlayer) return allowedTargets.isSelected("Players");
        if (entity instanceof EntityMob) return allowedTargets.isSelected("Monsters");
        if (entity instanceof EntityAnimal) return allowedTargets.isSelected("Animals");
        return false;
    }

    @Override
    public void onEnable() {
        reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        reset();
        releaseBlock();
        super.onDisable();
    }
}