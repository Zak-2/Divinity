package divinity.module.impl.world;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.input.MoveInputOptionsEvent;
import divinity.event.impl.minecraft.HandleInputEvent;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.player.MovementCorrectionEvent;
import divinity.event.impl.render.RenderGuiEvent;
import divinity.event.impl.render.RenderItemStackEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.ModuleManager;
import divinity.module.property.impl.MultiSelectProperty;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.font.Fonts;
import divinity.utils.math.MathUtils;
import divinity.utils.player.LocalPlayerUtils;
import divinity.utils.player.RotationUtils;
import divinity.utils.player.inventory.InventoryUtils;
import divinity.utils.player.rotation.RotationHandler;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.lwjgl.input.Keyboard;

import java.util.concurrent.ThreadLocalRandom;

public class Scaffold extends Module {

    private BlockData data;
    private double startPosY;
    private float[] rotations;
    private int bestStack, original;
    private final ModuleManager manager = ClientManager.getInstance().getModuleManager();
    private final MultiSelectProperty addonsProperty = new MultiSelectProperty("Addons", new String[]{"KeepY", "Auto Jump", "Center"}, new String[]{"KeepY", "Auto Jump", "Center"});
    private final MultiSelectProperty visualsProperty = new MultiSelectProperty("Visuals", new String[]{"Spoof Item", "No Swing", "Block Counter"}, new String[]{"Spoof Item", "No Swing", "Block Counter"});
    private final NumberProperty<?> maxAngleChange = new NumberProperty<>("Max Angle Change", 37.0F, 1.0F, 45.0F, 0.5F);

    public Scaffold(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(addonsProperty, visualsProperty, maxAngleChange);
    }

    @EventListener
    public void onEvent(RenderItemStackEvent event) {
        if (visualsProperty.isSelected("Spoof Item")) {
            mc.getItemRenderer().itemToRender = InventoryUtils.hotbar().inv.getStackInSlot(original);
            event.stack = mc.getItemRenderer().itemToRender;
        }
    }

    @EventListener
    public void onEvent(RenderGuiEvent event) {
        if (visualsProperty.isSelected("Block Counter"))
            Fonts.INTER_MEDIUM.get(14).drawCenteredStringWithShadow(
                    InventoryUtils.getHotBarBlockCount() + " §fblocks remaining",
                    (float) event.getWidth() / 2f + 3,
                    (float) event.getHeight() / 2f + 13f,
                    ClientManager.getInstance().getMainColor().getRGB()
            );
    }

    @EventListener
    public void onEvent(MovementCorrectionEvent event) {
        if (addonsProperty.isSelected("Center")) {
            final float rad = (float) Math.toRadians(mc.thePlayer.rotationYaw);
            final Vec3 rotVec = new Vec3(-Math.sin(rad), 0, Math.cos(rad));
            final Vec3 eyes = RotationUtils.getHitOrigin(mc.thePlayer);
            final float[] rotTo = RotationUtils.getRotations(eyes, RotationUtils.toVecCenter(new BlockPos(eyes.add(rotVec))));
            event.setYaw(rotTo[0]);
        }
    }

    @EventListener
    public void onEvent(HandleInputEvent event) {
        if (addonsProperty.isSelected("Auto Jump")) {
            if (mc.thePlayer.offGroundTicks >= 6) placeBlock();
        } else placeBlock();
    }

    @EventListener
    public void onEvent(MoveInputOptionsEvent event) {
        // Nigger Rigged Way To Prevent Moving When Targeting Fireball
        if (manager.antiFireball.currentTarget != null) {
            event.setForward(0);
            event.setStrafe(0);
            event.setJumping(false);
        }

        if (!LocalPlayerUtils.isMoving() || Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) return;

        event.setJumping(shouldAutoJump());
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPre()) return;

        bestStack = InventoryUtils.getBestBlockStack(mc, InventoryUtils.ONLY_HOT_BAR_BEGIN, InventoryUtils.END);

        if (bestStack >= InventoryUtils.ONLY_HOT_BAR_BEGIN) {
            final BlockPos blockUnder = getBlockUnder();
            data = findBlockData(blockUnder);
        }

        if (data == null) return;

        if (data.hitVec == null) return;

        if (manager.antiFireball.currentTarget != null) return;

        rotations = RotationUtils.getRotations(new float[]{event.getYaw(), event.getPitch()}, RotationUtils.getHitOrigin(mc.thePlayer), data.hitVec, maxAngleChange.getValue().floatValue());

        if (shouldAutoJump()) rotations[0] = (float) (mc.thePlayer.rotationYaw + ThreadLocalRandom.current().nextGaussian() / 1000.0F);

        ClientManager.getInstance().getRotationHandler().submit(this, new RotationHandler.RotationRequest(rotations[0], rotations[1], RotationHandler.RotationPriority.ZERO));
    }

    private boolean shouldAutoJump() {
        return addonsProperty.isSelected("Auto Jump") && mc.thePlayer.onGroundTicks >= 2 && !Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode());
    }

    private BlockData findBlockData(final BlockPos below) {
        final float reach = 4.F;
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

        // y offset loop (-1, 0)
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

                    // inline distanceSq
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

    private void placeBlock() {
        if (!mc.theWorld.isAirBlock(getBlockUnder()) || bestStack < InventoryUtils.ONLY_HOT_BAR_BEGIN) {
            return;
        }

        mc.thePlayer.inventory.currentItem = bestStack - InventoryUtils.ONLY_HOT_BAR_BEGIN;

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.getCurrentEquippedItem(), data.pos, data.face, data.hitVec)) {
            if (visualsProperty.isSelected("No Swing")) {
                mc.thePlayer.sendQueue.addToSendQueue(new C0APacketAnimation());
            } else {
                mc.thePlayer.swingItem();
            }
        }
    }

    private BlockPos getBlockUnder() {
        double px = mc.thePlayer.posX;
        double pz = mc.thePlayer.posZ;
        double py = mc.thePlayer.posY;

        if (addonsProperty.isSelected("KeepY") && !Keyboard.isKeyDown(mc.gameSettings.keyBindJump.getKeyCode())) {
            py = Math.min(startPosY, mc.thePlayer.posY) - 1;
            return new BlockPos(px, py, pz);
        } else {
            startPosY = mc.thePlayer.posY;
            return new BlockPos(px, py - 1, pz);
        }
    }

    @Override
    public void onEnable() {
        startPosY = mc.thePlayer.posY;
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