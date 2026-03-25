package divinity.module.impl.player;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.utils.player.RotationUtils;
import divinity.utils.player.inventory.InventoryUtils;
import divinity.utils.player.rotation.RotationHandler;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

public class NoFall extends Module {

    private boolean rotated;
    private boolean switched;
    private int originalSlot = -1;

    public NoFall(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        setSuffix("MLG");
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (event.isPost()) return;

        if (mc.thePlayer.onGround || mc.thePlayer.isInWater()) {

            if (switched && originalSlot != -1) {
                mc.thePlayer.inventory.currentItem = originalSlot;
                originalSlot = -1;
                switched = false;
            }

            rotated = false;
            return;
        }

        if (isAboutToTakeDamage()) {
            if (!switched) findAndSwapToWaterBucket();

            if (switched && !rotated) rotate();

            if (rotated) placeWater();
        }
    }

    private boolean isAboutToTakeDamage() {
        if (mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) return false;

        if (mc.theWorld == null) return false;

        double fallDistance = mc.thePlayer.fallDistance;

        if (fallDistance < 3.5) return false;

        Vec3 start = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 end = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY - 10.0, mc.thePlayer.posZ);
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(start, end, false, true, false);

        if (mop == null || mop.hitVec == null) return false;

        double distanceToGround = mc.thePlayer.posY - mop.hitVec.yCoord;

        if (distanceToGround < 2.0) return false;

        double velocityY = mc.thePlayer.motionY;
        double timeToHit = 0;
        double currentY = 0;
        double currentVelocity = velocityY;

        while (timeToHit < 20 && currentY < distanceToGround) {
            currentVelocity -= 0.08;
            currentVelocity *= 0.98;
            currentY -= currentVelocity;

            timeToHit++;

            if (currentY >= distanceToGround) break;
        }

        return timeToHit <= 4 && timeToHit > 0;
    }

    private void findAndSwapToWaterBucket() {
        int bestSlot = -1;

        for (int i = InventoryUtils.ONLY_HOT_BAR_BEGIN; i <= InventoryUtils.END; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && stack.getItem() instanceof ItemBucket) {
                if (stack.getMetadata() == 0) {
                    bestSlot = i;
                    break;
                }
            }
        }

        if (bestSlot != -1) {
            originalSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = bestSlot - 36;
            switched = true;
        }
    }

    private void rotate() {
        Vec3 start = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ);
        Vec3 end = new Vec3(mc.thePlayer.posX, mc.thePlayer.posY - 6.0, mc.thePlayer.posZ);
        MovingObjectPosition mop = mc.theWorld.rayTraceBlocks(start, end, false, true, false);

        if (mop != null && mop.hitVec != null) {
            Vec3 hitVec = mop.hitVec;

            float[] rotations = RotationUtils.getRotations(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.getEyeHeight(), mc.thePlayer.posZ), hitVec);

            ClientManager.getInstance().getRotationHandler().submit(this, new RotationHandler.RotationRequest(rotations[0], rotations[1], RotationHandler.RotationPriority.ONE, 2));
        }

        rotated = true;
    }

    private void placeWater() {
        ItemStack heldItem = mc.thePlayer.getHeldItem();

        if (heldItem == null || !(heldItem.getItem() instanceof ItemBucket)) return;

        mc.thePlayer.sendQueue.addToSendQueue(new C08PacketPlayerBlockPlacement(heldItem));

        rotated = false;
    }

    private void retrieveWater() {

    }

    @Override
    public void onEnable() {
        rotated = false;
        switched = false;
        originalSlot = -1;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (switched && originalSlot != -1) {
            mc.thePlayer.inventory.currentItem = originalSlot;
            originalSlot = -1;
            switched = false;
        }

        rotated = false;
        super.onDisable();
    }
}
