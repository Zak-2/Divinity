package divinity.utils.player;

import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSword;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;

import java.util.ArrayList;
import java.util.List;

public class LocalPlayerUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static double getBaseMoveSpeed() {
        double walkSpeed = 0.221;
        double modSprint = 0.27 / walkSpeed;
        double baseSpeed = walkSpeed;

        if (mc.thePlayer.isSprinting()) {
            baseSpeed *= modSprint;
        }

        if (isCollidingBlock(mc.thePlayer.getEntityBoundingBox())) {
            baseSpeed *= 0.09 / walkSpeed;
        }

        if (mc.thePlayer.isOnLadder()) {
            baseSpeed *= 0.17 / walkSpeed;
        }

        if (mc.theWorld.isAnyLiquid(mc.thePlayer.getEntityBoundingBox())) {
            baseSpeed *= 0.115 / walkSpeed;
        }

        PotionEffect effect = mc.thePlayer.getActivePotionEffect(Potion.moveSpeed);
        if (effect != null && effect.getAmplifier() >= 0 && effect.getDuration() >= 5) {
            baseSpeed *= 1.0 + 0.2 * (effect.getAmplifier() + 1);
        }
        return baseSpeed;
    }

    private static boolean isCollidingBlock(AxisAlignedBB bb) {
        List<AxisAlignedBB> bbs = new ArrayList<>();
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                if (mc.theWorld.isBlockLoaded(blockpos$mutableblockpos.set(k1, 64, l1))) {
                    for (int i2 = k - 1; i2 < l; ++i2) {
                        blockpos$mutableblockpos.set(k1, i2, l1);
                        IBlockState iBlockState1 = mc.theWorld.getBlockState(blockpos$mutableblockpos);
                        Block block = iBlockState1.getBlock();
                        if (block == Blocks.web) {
                            block.addCollisionBoxesToList(mc.theWorld, blockpos$mutableblockpos, iBlockState1, bb, bbs, mc.thePlayer);
                            if (!bbs.isEmpty())
                                return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static List<AxisAlignedBB> getBlockCollidingAABBs(AxisAlignedBB bb) {
        List<AxisAlignedBB> list = Lists.newArrayList();
        int i = MathHelper.floor_double(bb.minX);
        int j = MathHelper.floor_double(bb.maxX + 1.0D);
        int k = MathHelper.floor_double(bb.minY);
        int l = MathHelper.floor_double(bb.maxY + 1.0D);
        int i1 = MathHelper.floor_double(bb.minZ);
        int j1 = MathHelper.floor_double(bb.maxZ + 1.0D);
        BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                if (mc.theWorld.isBlockLoaded(blockpos$mutableblockpos.set(k1, 64, l1))) {
                    for (int i2 = k - 1; i2 < l; ++i2) {
                        blockpos$mutableblockpos.set(k1, i2, l1);
                        IBlockState iblockstate1 = mc.theWorld.getBlockState(blockpos$mutableblockpos);
                        iblockstate1.getBlock().addCollisionBoxesToList(mc.theWorld, blockpos$mutableblockpos, iblockstate1, bb, list, mc.thePlayer);
                    }
                }
            }
        }

        return list;
    }

    public static boolean isMoving() {
        return (mc.thePlayer.moveForward != 0.0F || mc.thePlayer.moveStrafing != 0.0F);
    }

    public static boolean isHoldingSword() {
        return mc.thePlayer.getHeldItem() != null && mc.thePlayer.getHeldItem().getItem() instanceof ItemSword;
    }

    public static float getMoveAngle() {
        return getMoveAngle(mc.thePlayer.moveForward, mc.thePlayer.moveStrafing, mc.thePlayer.rotationYaw);
    }

    public static float getMoveAngle(float forward, float strafe, float baseYaw) {
        float f = strafe * strafe + forward * forward;
        if (f >= 1.0e-4f) {
            float yaw = baseYaw;
            boolean reversed = forward < 0.0f;
            float strafingYaw = forward > 0.0f ? 45.0f : reversed ? -45.0f : 90.0f;

            if (reversed)
                yaw += 180.0f;
            if (strafe > 0.0f)
                yaw -= strafingYaw;
            else if (strafe < 0.0f)
                yaw += strafingYaw;

            return yaw;
        } else {
            return baseYaw;
        }
    }
}
