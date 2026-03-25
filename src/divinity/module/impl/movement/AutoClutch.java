package divinity.module.impl.movement;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.HandleInputEvent;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.ModuleManager;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.player.RotationUtils;
import divinity.utils.player.inventory.InventoryUtils;
import divinity.utils.player.rotation.RotationHandler;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class AutoClutch extends Module {

    private BlockData data;
    private int bestStack, original;
    private final ModuleManager mm = ClientManager.getInstance().getModuleManager();
    private final NumberProperty<?> angleDeg = new NumberProperty<>("Max Angle Change", 45.0, 1.0, 45.0, 1.0);

    public AutoClutch(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(angleDeg);
    }

    @EventListener
    public void onHandleInput(HandleInputEvent event) {
        if (mm.scaffold.isState()) return;
        placeBlock();
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (event.isPre()) return;
        if (mm.scaffold.isState()) return;

        bestStack = InventoryUtils.getBestBlockStack(mc, InventoryUtils.ONLY_HOT_BAR_BEGIN, InventoryUtils.END);

        if (bestStack >= InventoryUtils.ONLY_HOT_BAR_BEGIN) {
            final BlockPos blockUnder = getBlockUnder();
            data = findBlockData(blockUnder);
        }

        if (data == null) return;

        if (data.hitVec == null) return;

        if (!willBeOverVoidNextTick()) return;

        float[] rotations = RotationUtils.getRotations(new float[]{event.getYaw(), event.getPitch()}, RotationUtils.getHitOrigin(mc.thePlayer), data.hitVec, angleDeg.getValue().floatValue());

        ClientManager.getInstance().getRotationHandler().submit(this, new RotationHandler.RotationRequest(rotations[0], rotations[1], RotationHandler.RotationPriority.ZERO));
    }

    private void placeBlock() {
        if (!mc.theWorld.isAirBlock(getBlockUnder()) || bestStack < InventoryUtils.ONLY_HOT_BAR_BEGIN || !willBeOverVoidNextTick() || data == null) {
            return;
        }

        mc.thePlayer.inventory.currentItem = bestStack - InventoryUtils.ONLY_HOT_BAR_BEGIN;

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), data.pos, data.face, data.hitVec)) {
            mc.thePlayer.swingItem();
        }
    }

    private boolean willBeOverVoidNextTick() {
        // Predict next position using current motion and gravity for one tick
        double nextX = mc.thePlayer.posX + mc.thePlayer.motionX;
        double nextZ = mc.thePlayer.posZ + mc.thePlayer.motionZ;

        // Simplified gravity: motionY will be decreased by 0.08 each tick (client-side)
        double nextMotionY = mc.thePlayer.motionY - 0.08;

        double nextY = mc.thePlayer.posY + nextMotionY;

        // If predicted Y is below 0.5, treat as over void (falling out of world)
        if (nextY < 0.5) return true;

        // The block position under the predicted player position
        BlockPos predictedUnder = new BlockPos(nextX, nextY - 1.0, nextZ);

        // If the chunk containing the predicted position isn't loaded, be conservative and don't attempt clutch
        if (!mc.theWorld.isBlockLoaded(predictedUnder))
            return true; // if chunk not loaded, assume void (helps clutch in unloaded-edge)

        // If there's a non-air block immediately under predicted pos, we're not over the void
        if (!mc.theWorld.isAirBlock(predictedUnder)) return false;

        // If predictedUnder is air, check downward until bedrock/0 to see if any block exists below
        for (int y = predictedUnder.getY() - 1; y >= 0; y--) {
            BlockPos check = new BlockPos(predictedUnder.getX(), y, predictedUnder.getZ());
            if (!mc.theWorld.isBlockLoaded(check)) {
                // If chunk below isn't loaded, assume void (safer)
                return true;
            }
            if (!mc.theWorld.isAirBlock(check)) {
                // Found a block below -> not over void
                return false;
            }
        }

        // No blocks found below -> over void
        return true;
    }

    private BlockPos getBlockUnder() {
        return new BlockPos(mc.thePlayer.posX, mc.thePlayer.posY - 1, mc.thePlayer.posZ);
    }

    private BlockData findBlockData(final BlockPos below) {
        final float reach = mc.playerController.getBlockReachDistance();
        final int maxI = (int) reach;
        double bestDistSq = reach * reach;

        final World world = mc.theWorld;

        final int baseY = below.getY();
        final int baseX = below.getX();
        final int baseZ = below.getZ();

        BlockData best = null;

        final EnumFacing[] facings = {
                EnumFacing.NORTH, EnumFacing.SOUTH,
                EnumFacing.WEST, EnumFacing.EAST,
                EnumFacing.UP
        };

        final EnumFacing[] horizontals = {
                EnumFacing.NORTH, EnumFacing.SOUTH,
                EnumFacing.WEST, EnumFacing.EAST
        };

        final BlockPos.MutableBlockPos player = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        final BlockPos.MutableBlockPos test = new BlockPos.MutableBlockPos();

        for (int yOff = -1; yOff <= 0; yOff++) {
            final int playerY = baseY + yOff;
            player.set(baseX, playerY, baseZ);

            for (int i = 1; i <= maxI; i++) {
                for (EnumFacing facing : facings) {

                    final EnumFacing opposite = facing.getOpposite();
                    pos.set(
                            baseX + opposite.getFrontOffsetX() * i,
                            playerY + opposite.getFrontOffsetY() * i,
                            baseZ + opposite.getFrontOffsetZ() * i
                    );

                    if (pos.getY() < baseY - 1) continue;

                    BlockPos finalPos = pos;
                    EnumFacing finalFace = facing;

                    if (world.isAirBlock(pos)) {
                        boolean found = false;

                        for (EnumFacing side : horizontals) {
                            if (side == facing) continue;

                            final int sx = side.getFrontOffsetX();
                            final int sz = side.getFrontOffsetZ();

                            for (int j = 1; j <= maxI; j++) {
                                test.set(
                                        pos.getX() + sx * j,
                                        pos.getY(),
                                        pos.getZ() + sz * j
                                );

                                if (!world.isAirBlock(test)) {
                                    finalPos = test;
                                    finalFace = side.getOpposite();
                                    found = true;
                                    break;
                                }
                            }
                            if (found) break;
                        }

                        if (!found) continue;
                    }

                    if (finalPos.getY() > playerY) continue;

                    final double dx = player.getX() - finalPos.getX();
                    final double dy = player.getY() - finalPos.getY();
                    final double dz = player.getZ() - finalPos.getZ();
                    final double distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq <= bestDistSq) {
                        bestDistSq = distSq;
                        best = new BlockData(new BlockPos(finalPos.getX(), finalPos.getY(), finalPos.getZ()), finalFace);
                    }
                }
            }
        }

        return best;
    }

    @Override
    public void onEnable() {
        original = mc.thePlayer.inventory.currentItem;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        mc.thePlayer.inventory.currentItem = original;
        super.onDisable();
    }

    private static class BlockData {
        private final BlockPos pos;
        private final EnumFacing face;
        private final Vec3 hitVec;

        public BlockData(BlockPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
            this.hitVec = calculateHitVec();
        }

        private Vec3 calculateHitVec() {
            double x = pos.getX(), y = pos.getY(), z = pos.getZ();
            double xOff = 0.5;
            double yOff = 0.5;

            switch (face) {
                case NORTH:
                    return new Vec3(x + xOff, y + yOff, z);
                case WEST:
                    return new Vec3(x, y + yOff, z + xOff);
                case SOUTH:
                    return new Vec3(x + xOff, y + yOff, z + 1.0);
                case EAST:
                    return new Vec3(x + 1.0, y + yOff, z + xOff);
                case UP:
                    return new Vec3(x + xOff, y + 1.0, z + xOff);
                case DOWN:
                    return new Vec3(x + xOff, y, z + xOff);
                default:
                    throw new IllegalArgumentException();
            }
        }
    }
}