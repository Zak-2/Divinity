package divinity.module.impl.world;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.HandleInputEvent;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.world.LoadWorldEvent;
import divinity.event.impl.world.PreventBlockBreakEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.utils.player.RotationUtils;
import divinity.utils.player.rotation.RequireRotationPriority;
import divinity.utils.player.rotation.RotationHandler;
import net.minecraft.block.BlockBed;
import net.minecraft.block.material.Material;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.util.EnumSet;

public class Breaker extends Module {

    private long lastBreakFinishTime = 0;
    public BlockPos currentTarget = null;
    private EnumFacing lastBreakFace = null;
    private BlockPos previousTargetHalf = null;
    private final double MAX_REACH_DISTANCE = 4.5;
    private BlockPos[] enemyBedPositions = new BlockPos[0];

    public Breaker(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onLoadWorld(LoadWorldEvent event) {
        resetRuntimeState();
        enemyBedPositions = new BlockPos[0];
        previousTargetHalf = null;
    }

    @EventListener
    @RequireRotationPriority
    public void onEvent(PreventBlockBreakEvent event) {
        if (currentTarget != null) event.setCancelled(true);
    }

    @EventListener
    @RequireRotationPriority
    public void onEvent(HandleInputEvent event) {
        handleDestruction();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (event.isPre()) {
            updateTargetBlock();
            return;
        }
        if (currentTarget != null) rotateToBreak(event);
    }

    private void handleDestruction() {
        breakBlock();
    }

    private void updateTargetBlock() {
        searchForEnemyBeds();

        if (currentTarget != null && !isBlockBroken(currentTarget)) {
            if (isWhitelistedBed(currentTarget)) {
                currentTarget = null;
                return;
            }
            return;
        }

        currentTarget = null;

        if (enemyBedPositions.length != 2) return;

        Vec3 playerPosition = mc.thePlayer.getPositionVector();
        double reachSquared = MAX_REACH_DISTANCE * MAX_REACH_DISTANCE;

        if (previousTargetHalf != null && !isBlockBroken(previousTargetHalf)) {
            if (isWhitelistedBed(previousTargetHalf)) {
                previousTargetHalf = null;
            } else if (playerPosition.squareDistanceTo(RotationUtils.toVecCenter(previousTargetHalf)) <= reachSquared) {
                currentTarget = previousTargetHalf;
                return;
            }
        }

        BlockPos closestHalf = getClosestBedHalf(playerPosition);

        if (isWhitelistedBed(closestHalf)) {
            return;
        }

        if (playerPosition.squareDistanceTo(RotationUtils.toVecCenter(closestHalf)) > reachSquared) return;

        EnumFacing directionToHalf = calculateDirectionToBlock(closestHalf);
        BlockPos blocker = closestHalf.offset(directionToHalf);

        if (!isBlockBroken(blocker)) {
            double distanceSquared = playerPosition.squareDistanceTo(RotationUtils.toVecCenter(blocker));
            if (distanceSquared <= reachSquared) {
                currentTarget = blocker;
                previousTargetHalf = closestHalf;
                return;
            }
        }

        currentTarget = closestHalf;
        previousTargetHalf = closestHalf;
    }

    private void breakBlock() {
        if (System.currentTimeMillis() - lastBreakFinishTime < 300) return;

        if (currentTarget == null) return;

        if (mc.thePlayer.getDistanceSqToCenter(currentTarget) > MAX_REACH_DISTANCE * MAX_REACH_DISTANCE) {
            currentTarget = null;
            return;
        }

        if (isWhitelistedBed(currentTarget)) {
            currentTarget = null;
            return;
        }

        if (!mc.playerController.getIsHittingBlock()) {
            mc.playerController.clickBlock(currentTarget, lastBreakFace);
        }

        if (mc.playerController.onPlayerDamageBlock(currentTarget, lastBreakFace)) {
            mc.thePlayer.swingItem();

            if (mc.theWorld.getBlockState(currentTarget).getBlock().getMaterial() == Material.air) {
                lastBreakFinishTime = System.currentTimeMillis();
                currentTarget = null;
            }
        }
    }

    private void searchForEnemyBeds() {
        BlockPos playerPos = mc.thePlayer.getPosition();
        double bestDistance = Double.MAX_VALUE;
        double reachSquared = MAX_REACH_DISTANCE * MAX_REACH_DISTANCE;

        enemyBedPositions = new BlockPos[0];

        for (int dx = -(int) Math.ceil(MAX_REACH_DISTANCE); dx <= (int) Math.ceil(MAX_REACH_DISTANCE); dx++) {
            for (int dy = -(int) Math.ceil(MAX_REACH_DISTANCE); dy <= (int) Math.ceil(MAX_REACH_DISTANCE); dy++) {
                for (int dz = -(int) Math.ceil(MAX_REACH_DISTANCE); dz <= (int) Math.ceil(MAX_REACH_DISTANCE); dz++) {
                    BlockPos pos = playerPos.add(dx, dy, dz);

                    if (!(mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed)) continue;

                    if (isWhitelistedBed(pos)) continue;

                    double distanceSquared = playerPos.distanceSq(pos);
                    if (distanceSquared > reachSquared) continue;

                    if (distanceSquared < bestDistance) {
                        BlockPos[] halves = findBedHalves(pos);
                        if (halves.length == 2 && !isWhitelistedBed(halves[0]) && !isWhitelistedBed(halves[1])) {
                            enemyBedPositions = halves;
                            bestDistance = distanceSquared;
                        }
                    }
                }
            }
        }
    }

    private BlockPos[] findBedHalves(BlockPos pos) {
        for (EnumFacing direction : EnumSet.of(EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST)) {
            BlockPos neighbor = pos.offset(direction);
            if (mc.theWorld.getBlockState(neighbor).getBlock() instanceof BlockBed) {
                if (pos.getX() < neighbor.getX() || pos.getZ() < neighbor.getZ()) {
                    return new BlockPos[]{pos, neighbor};
                } else {
                    return new BlockPos[]{neighbor, pos};
                }
            }
        }
        return new BlockPos[0];
    }

    private boolean isBlockBroken(BlockPos pos) {
        return mc.theWorld.getBlockState(pos).getBlock().getMaterial() == Material.air;
    }

    private BlockPos getClosestBedHalf(Vec3 playerPosition) {
        if (enemyBedPositions.length != 2) return null;
        double distance1 = playerPosition.squareDistanceTo(RotationUtils.toVecCenter(enemyBedPositions[0]));
        double distance2 = playerPosition.squareDistanceTo(RotationUtils.toVecCenter(enemyBedPositions[1]));
        return (distance1 <= distance2) ? enemyBedPositions[0] : enemyBedPositions[1];
    }

    private EnumFacing calculateDirectionToBlock(BlockPos pos) {
        Vec3 origin = RotationUtils.getHitOrigin(mc.thePlayer);
        Vec3 center = RotationUtils.toVecCenter(pos);
        Vec3 difference = center.subtract(origin);
        return EnumFacing.getFacingFromVector((float) difference.xCoord, (float) difference.yCoord, (float) difference.zCoord).getOpposite();
    }

    private void rotateToBreak(UpdateEvent event) {
        if (currentTarget == null) return;

        Vec3 origin = RotationUtils.getHitOrigin(mc.thePlayer);

        Vec3 center = RotationUtils.toVecCenter(currentTarget);

        Vec3 difference = center.subtract(origin);

        lastBreakFace = EnumFacing.getFacingFromVector((float) difference.xCoord, (float) difference.yCoord, (float) difference.zCoord).getOpposite();

        Vec3 hitPosition = center.addVector(lastBreakFace.getDirectionVec().getX() * 0.5, lastBreakFace.getDirectionVec().getY() * 0.5, lastBreakFace.getDirectionVec().getZ() * 0.5);

        float[] rotations = RotationUtils.getRotations(new float[]{event.getYaw(), event.getPitch()}, origin, hitPosition, 45.f);

        ClientManager.getInstance().getRotationHandler().submit(this, new RotationHandler.RotationRequest(rotations[0], rotations[1], RotationHandler.RotationPriority.FOUR));
    }

    private boolean isWhitelistedBed(BlockPos pos) {
        return ClientManager.getInstance().getModuleManager().defender.isBed(pos);
    }

    private void resetRuntimeState() {
        currentTarget = null;
        lastBreakFinishTime = 0;
        enemyBedPositions = new BlockPos[0];
        previousTargetHalf = null;
    }

    @Override
    public void onEnable() {
        resetRuntimeState();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetRuntimeState();
        super.onDisable();
    }
}