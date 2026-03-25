package divinity.utils.simulation;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.List;

public class Simulation {

    private static final Minecraft mc = Minecraft.getMinecraft();

    /**
     * Simulates 'ticks' ticks ahead, using the player's current inputs.
     */
    public PlayerSimulationSnapshot[] getSimulatedPositions(EntityPlayerSP player, int ticks) {
        World world = mc.theWorld;

        // Copy initial state
        Vec3 pos = new Vec3(player.posX, player.posY, player.posZ);
        Vec3 motion = new Vec3(player.motionX, player.motionY, player.motionZ);
        float yaw = player.rotationYaw;
        boolean sprint = player.isSprinting();
        float fallDist = player.fallDistance;
        float stepHeight = player.stepHeight;
        boolean onGround = player.onGround;

        // Snapshot held inputs once
        float forward = player.moveForward;
        float strafe = player.moveStrafing;
        boolean jump = player.movementInput.jump;
        boolean sneakIn = player.movementInput.sneak;

        PlayerSimulationSnapshot[] snaps = new PlayerSimulationSnapshot[ticks];

        for (int i = 0; i < ticks; i++) {
            snaps[i] = new PlayerSimulationSnapshot(
                    new Vec3(pos.xCoord, pos.yCoord, pos.zCoord),
                    fallDist,
                    new Vec3(motion.xCoord, motion.yCoord, motion.zCoord),
                    onGround
            );

            // 1) gravity
            if (!onGround && !isInsideLiquid(world, pos)) {
                motion = motion.addVector(0, -0.08, 0);
            }

            // 2) friction
            motion = motion.multiply(onGround ? getBlockFriction(world, new BlockPos(pos).down()) * 0.91 : 0.98, onGround ? 1 : 0.98, onGround ? getBlockFriction(world, new BlockPos(pos).down()) * 0.91 : 0.98);
            // 3) movement input
            motion = motion.addVector(-Math.sin(Math.toRadians(yaw)) * forward * (onGround ? 0.1 : 0.02) * (sprint ? 1.3 : 1), 0, Math.cos(Math.toRadians(yaw)) * forward * (onGround ? 0.1 : 0.02) * (sprint ? 1.3 : 1));
            motion = motion.addVector(Math.cos(Math.toRadians(yaw)) * strafe * (onGround ? 0.1 : 0.02) * (sprint ? 1.3 : 1), 0, Math.sin(Math.toRadians(yaw)) * strafe * (onGround ? 0.1 : 0.02) * (sprint ? 1.3 : 1));

            // 4) jump
            if (onGround && jump) {
                motion = new Vec3(motion.xCoord, 0.42, motion.zCoord);
            }

            // 5) sneaking dampens descent
            if (sneakIn && motion.yCoord < 0) {
                motion = new Vec3(motion.xCoord, motion.yCoord * 0.5, motion.zCoord);
            }

            // 6) collision + step‑up
            CollisionResult cr = collideAndStep(world,
                    new AxisAlignedBB(
                            pos.xCoord, pos.yCoord, pos.zCoord,
                            pos.xCoord + player.width,
                            pos.yCoord + player.height,
                            pos.zCoord + player.width
                    ),
                    motion,
                    stepHeight
            );
            pos = cr.pos;
            motion = cr.motion;
            onGround = cr.onGround;

            // 7) fall distance
            fallDist = onGround ? 0 : fallDist - (float) Math.min(0, motion.yCoord);
        }

        return snaps;
    }

    private boolean isInsideLiquid(World world, Vec3 pos) {
        IBlockState s = world.getBlockState(new BlockPos(pos));
        return s.getBlock() instanceof BlockLiquid;
    }

    private double getBlockFriction(World world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        return s.getBlock().slipperiness;
    }

    /**
     * Performs AABB collision along X, Z, Y axes and a simple step‑up check.
     */
    private CollisionResult collideAndStep(World world, AxisAlignedBB box, Vec3 mot, float stepHt) {
        List<AxisAlignedBB> list = world.getCollisionBoxes(box.offset(mot.xCoord, mot.yCoord, mot.zCoord));

        // slide X
        for (AxisAlignedBB aabb : list) {
            mot = new Vec3(mot.xCoord, mot.yCoord, mot.zCoord);
            mot = mot.subtract(new Vec3(mot.xCoord - aabb.calculateXOffset(box, mot.xCoord), 0, 0));
        }
        box = box.offset(mot.xCoord, 0, 0);

        // slide Z
        for (AxisAlignedBB aabb : list) {
            mot = new Vec3(mot.xCoord, mot.yCoord, mot.zCoord);
            mot = mot.subtract(new Vec3(0, 0, mot.zCoord - aabb.calculateZOffset(box, mot.zCoord)));
        }
        box = box.offset(0, 0, mot.zCoord);

        // slide Y
        boolean onGround = false;
        for (AxisAlignedBB aabb : list) {
            double yOff = aabb.calculateYOffset(box, mot.yCoord);
            box = box.offset(0, yOff, 0);
            if (yOff < 0) onGround = true;
        }

        // step‑up
        if (!onGround && stepHt > 0) {
            AxisAlignedBB stepBox = box.offset(0, stepHt, 0);
            List<AxisAlignedBB> coll = world.getCollisionBoxes(stepBox.offset(mot.xCoord, mot.yCoord, mot.zCoord));
            AxisAlignedBB stepMoved = stepBox;
            for (AxisAlignedBB aabb : coll) {
                double yOff = aabb.calculateYOffset(stepMoved, mot.yCoord);
                stepMoved = stepMoved.offset(0, yOff, 0);
            }
            if (stepMoved.maxY < box.maxY + 0.001) {
                box = stepMoved.offset(0, -stepHt, 0);
                onGround = true;
            }
        }

        Vec3 newPos = new Vec3(box.minX, box.minY, box.minZ);
        Vec3 newMot = new Vec3(
                mot.xCoord * (box.minX == box.minX + mot.xCoord ? 0 : 1),
                mot.yCoord,
                mot.zCoord * (box.minZ == box.minZ + mot.zCoord ? 0 : 1)
        );
        return new CollisionResult(newPos, newMot, onGround);
    }

    private static class CollisionResult {
        final Vec3 pos;
        final Vec3 motion;
        final boolean onGround;

        CollisionResult(Vec3 p, Vec3 m, boolean g) {
            pos = p;
            motion = m;
            onGround = g;
        }
    }

    public static class PlayerSimulationSnapshot {
        public final Vec3 position, motion;
        public final float fallDistance;
        public final boolean onGround;

        public PlayerSimulationSnapshot(Vec3 pos, float fd, Vec3 mot, boolean g) {
            position = pos;
            fallDistance = fd;
            motion = mot;
            onGround = g;
        }
    }
}
