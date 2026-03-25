package divinity.utils.move;

import divinity.event.impl.minecraft.UpdateEvent;
import divinity.utils.player.LocalPlayerUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

import static java.lang.Math.toRadians;

public class MoveUtils {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean canSprint(final EntityPlayer player) {
        return player.moveForward >= 0.8F
                && (player.getFoodStats().getFoodLevel() > 6 || player.capabilities.allowFlying)
                && !player.isPotionActive(Potion.blindness)
                && !player.isCollidedHorizontally
                && !player.isSneaking();
    }

    public static boolean isOverVoid(Minecraft mc) {
        AxisAlignedBB bb = mc.thePlayer.getEntityBoundingBox();
        double footY = bb.minY;

        int minX = MathHelper.floor_double(bb.minX);
        int maxX = MathHelper.floor_double(bb.maxX);
        int minZ = MathHelper.floor_double(bb.minZ);
        int maxZ = MathHelper.floor_double(bb.maxZ);

        int y = MathHelper.floor_double(footY) - 1;

        for (; y >= 0; y--) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos checkPos = new BlockPos(x, y, z);
                    IBlockState state = mc.theWorld.getBlockState(checkPos);
                    if (state.getBlock().getMaterial() != Material.air && state.getBlock().getCollisionBoundingBox(mc.theWorld, checkPos, state) != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static Vec3 predictPosition(EntityPlayerSP player) {
        double posX = player.posX;
        double posY = player.posY;
        double posZ = player.posZ;

        double vx = player.motionX;
        double vy = player.motionY;
        double vz = player.motionZ;

        final double gravity = 0.08D;
        final double drag = 0.98D;

        if (!player.onGround) {
            vy -= gravity;
            vx *= drag;
            vy *= drag;
            vz *= drag;
        }

        double predX = posX + vx;
        double predY = posY + vy;
        double predZ = posZ + vz;

        AxisAlignedBB predictedBB = player.getEntityBoundingBox().offset(predX - posX, predY - posY, predZ - posZ);
        List<AxisAlignedBB> colliding = mc.theWorld.getCollidingBoundingBoxes(player, predictedBB);

        if (!colliding.isEmpty()) {
            final double step = 0.01D;

            double[][] dirs = {
                    { step, 0.0D, 0.0D},
                    {-step, 0.0D, 0.0D},
                    {0.0D,  step, 0.0D},
                    {0.0D, -step, 0.0D},
                    {0.0D, 0.0D,  step},
                    {0.0D, 0.0D, -step}
            };

            boolean found = false;

            for (double[] d : dirs) {
                AxisAlignedBB adj = predictedBB.offset(d[0], d[1], d[2]);
                if (mc.theWorld.getCollidingBoundingBoxes(player, adj).isEmpty()) {
                    predX += d[0];
                    predY += d[1];
                    predZ += d[2];
                    found = true;
                    break;
                }
            }

            if (!found) return new Vec3(posX, posY, posZ);
        }

        return new Vec3(predX, predY, predZ);
    }
}
