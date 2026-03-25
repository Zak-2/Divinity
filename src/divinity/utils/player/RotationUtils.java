package divinity.utils.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.*;

import java.util.Arrays;
import java.util.function.BiFunction;

public final class RotationUtils {

    private static final double RAD_TO_DEG = 180.0 / Math.PI;
    private static final double DEG_TO_RAD = Math.PI / 180.0;

    private RotationUtils() {
    }

    public static Vec3 toVecCenter(BlockPos pos) {
        return new Vec3(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
    }

    public static boolean isPointedEntity(final Minecraft mc) {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit != null;
    }

    public static boolean isPointedBlock(final Minecraft mc) {
        return mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK;
    }

    public static MovingObjectPosition calculateIntercept(final AxisAlignedBB boundingBox,
                                                          final Vec3 src,
                                                          final float yaw,
                                                          final float pitch,
                                                          final double reach) {
        return boundingBox.calculateIntercept(src, getDstVec(src, yaw, pitch, reach));
    }


    public static Vec3 getAttackHitVec(final Minecraft mc,
                                       final Vec3 src,
                                       final AxisAlignedBB boundingBox,
                                       final Vec3 desiredHitVec,
                                       final boolean ignoreBlocks,
                                       final int maxRayTraces) {
        // Validate that closest hit vec is legit
        if (validateHitVec(mc, src, desiredHitVec, ignoreBlocks))
            return desiredHitVec;

        // If not find a better hit vec
        double closestDist = Double.MAX_VALUE;
        Vec3 bone = null;

        final double xWidth = boundingBox.maxX - boundingBox.minX;
        final double zWidth = boundingBox.maxZ - boundingBox.minZ;
        final double height = boundingBox.maxY - boundingBox.minY;

        int passes = 0;

        for (double x = 0.0; x < 1.0; x += 0.2) {
            for (double y = 0.0; y < 1.0; y += 0.2) {
                for (double z = 0.0; z < 1.0; z += 0.2) {
                    if (maxRayTraces != -1 && passes > maxRayTraces) return null;

                    final Vec3 hitVec = new Vec3(boundingBox.minX + xWidth * x,
                            boundingBox.minY + height * y,
                            boundingBox.minZ + zWidth * z);

                    final double dist;
                    if (validateHitVec(mc, src, hitVec, ignoreBlocks) &&
                            (dist = src.distanceTo(hitVec)) < closestDist) {

                        closestDist = dist;
                        bone = hitVec;
                    }

                    passes++;
                }
            }
        }

        return bone;
    }

    public static boolean validateHitVec(final Minecraft mc,
                                         final Vec3 src,
                                         final Vec3 dst,
                                         final boolean ignoreBlocks,
                                         final double penetrationDist) {
        final Vec3 blockHitVec = rayTraceHitVec(mc, src, dst);

        if (blockHitVec == null) return true;

        final double distance = src.distanceTo(dst);

        return ignoreBlocks && distance < penetrationDist;
    }

    public static boolean validateHitVec(final Minecraft mc,
                                         final Vec3 src,
                                         final Vec3 dst,
                                         final boolean ignoreBlocks) {

        return validateHitVec(mc, src, dst, ignoreBlocks, 2.8);
    }

    public static Vec3 getHitOrigin(final Entity entity) {
        return new Vec3(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
    }

    public static Vec3 getClosestPoint(final Vec3 start,
                                       final AxisAlignedBB boundingBox) {
        final double closestX = start.xCoord >= boundingBox.maxX ? boundingBox.maxX :
                start.xCoord <= boundingBox.minX ? boundingBox.minX :
                        boundingBox.minX + (start.xCoord - boundingBox.minX);

        final double closestY = start.yCoord >= boundingBox.maxY ? boundingBox.maxY :
                start.yCoord <= boundingBox.minY ? boundingBox.minY :
                        boundingBox.minY + (start.yCoord - boundingBox.minY);

        final double closestZ = start.zCoord >= boundingBox.maxZ ? boundingBox.maxZ :
                start.zCoord <= boundingBox.minZ ? boundingBox.minZ :
                        boundingBox.minZ + (start.zCoord - boundingBox.minZ);

        return new Vec3(closestX, closestY, closestZ);
    }

    public static Vec3 getCenterPointOnBB(final AxisAlignedBB hitBox,
                                          final double progressToTop) {
        final double xWidth = hitBox.maxX - hitBox.minX;
        final double zWidth = hitBox.maxZ - hitBox.minZ;
        final double height = hitBox.maxY - hitBox.minY;
        return new Vec3(hitBox.minX + xWidth / 2.0, hitBox.minY + height * progressToTop, hitBox.minZ + zWidth / 2.0);
    }

    public static AxisAlignedBB getHittableBoundingBox(final Entity entity,
                                                       final double boundingBoxScale) {
        return entity.getEntityBoundingBox().expand(boundingBoxScale, boundingBoxScale, boundingBoxScale);
    }

    public static AxisAlignedBB getHittableBoundingBox(final Entity entity) {
        return getHittableBoundingBox(entity, entity.getCollisionBorderSize());
    }

    public static Vec3 getPointedVec(final float yaw,
                                     final float pitch) {
        final double theta = -Math.cos(-pitch * DEG_TO_RAD);

        return new Vec3(Math.sin(-yaw * DEG_TO_RAD - Math.PI) * theta, Math.sin(-pitch * DEG_TO_RAD), Math.cos(-yaw * DEG_TO_RAD - Math.PI) * theta);
    }

    public static Vec3 getDstVec(final Vec3 src,
                                 final float yaw,
                                 final float pitch,
                                 final double reach) {
        final Vec3 rotationVec = getPointedVec(yaw, pitch);
        return src.addVector(rotationVec.xCoord * reach,
                rotationVec.yCoord * reach,
                rotationVec.zCoord * reach);
    }

    public static Vec3 rayTraceHitVec(final Minecraft mc,
                                      final Vec3 src,
                                      final Vec3 dst) {
        final MovingObjectPosition rayTraceResult = mc.theWorld.rayTraceBlocks(src, dst,
                false,
                false,
                false);

        return rayTraceResult != null ? rayTraceResult.hitVec : null;
    }

    public static boolean mouseOverEntity(final Minecraft mc, final EntityLivingBase target) {
        return mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mc.objectMouseOver.entityHit != null && mc.objectMouseOver.entityHit == target;
    }

    public static MovingObjectPosition rayTraceBlocks(final Minecraft mc, final Vec3 src, final double reach, final float yaw, final float pitch) {
        return mc.theWorld.rayTraceBlocks(src, getDstVec(src, yaw, pitch, reach), false, false, true);
    }

    public static MovingObjectPosition rayTraceBlocks(final Minecraft mc, final float yaw, final float pitch) {
        return rayTraceBlocks(mc, getHitOrigin(mc.thePlayer), mc.playerController.getBlockReachDistance(), yaw, pitch);
    }

    public static float[] getRotations(final Vec3 start, final Vec3 dst) {
        final double xDif = dst.xCoord - start.xCoord;

        final double yDif = dst.yCoord - start.yCoord;

        final double zDif = dst.zCoord - start.zCoord;

        final double distXZ = Math.sqrt(xDif * xDif + zDif * zDif);

        float yaw = (float) (Math.atan2(zDif, xDif) * RAD_TO_DEG) - 90.0F;

        yaw = MathHelper.wrapAngleTo180_float(yaw);

        float pitch = (float) (-Math.atan2(yDif, distXZ) * RAD_TO_DEG);

        pitch = MathHelper.clamp_float(pitch, -90.0F, 90.0F);

        return new float[]{yaw, pitch};
    }

    public static float[] getRotations(final float[] lastRotations, final Vec3 start, final Vec3 dst, final float maxChange) {
        final float[] rotations = getRotations(start, dst);
        applySmoothing(lastRotations, 1.0F, rotations, maxChange);
        return rotations;
    }

    public static void applySmoothing(final float[] lastRotations, final float smoothing,
                                      final float[] dstRotation, final float maxChange) {
        if (smoothing > 0.0F) {
            final float yawChange = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(dstRotation[0] - lastRotations[0]), -maxChange, maxChange);
            final float pitchChange = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(dstRotation[1] - lastRotations[1]), -maxChange, maxChange);

            final float factor = Math.max(0.01F, 1.0F / smoothing);

            dstRotation[0] = lastRotations[0] + yawChange * factor;
            dstRotation[1] = lastRotations[1] + pitchChange * factor;

            dstRotation[0] = MathHelper.wrapAngleTo180_float(dstRotation[0]);
            dstRotation[1] = MathHelper.clamp_float(dstRotation[1], -90.0F, 90.0F);
        }
    }

    public static void applyGCD(final float[] rotations, final float[] prevRots) {
        float yawDif = rotations[0] - prevRots[0];

        float pitchDif = rotations[1] - prevRots[1];

        yawDif = wrapDegrees(yawDif);

        pitchDif = wrapDegrees(pitchDif);

        double gcd = getMouseGCD();

        float snappedYaw = (float) (Math.round(yawDif / gcd) * gcd);

        float snappedPitch = (float) (Math.round(pitchDif / gcd) * gcd);

        rotations[0] = prevRots[0] + snappedYaw;

        rotations[1] = prevRots[1] + snappedPitch;
    }

    public static double getMouseGCD() {
        final float sens = Minecraft.getMinecraft().gameSettings.mouseSensitivity * 0.6F + 0.2F;
        final float pow = sens * sens * sens * 8.0F;
        return (double) pow * 0.15;
    }

    private static float wrapDegrees(float angle) {
        angle %= 360.0f;
        if (angle >= 180.0f) angle -= 360.0f;
        if (angle < -180.0f) angle += 360.0f;
        return angle;
    }

    public static float calculateYawFromSrcToDst(final float yaw,
                                                 final double srcX,
                                                 final double srcZ,
                                                 final double dstX,
                                                 final double dstZ) {
        final double xDist = dstX - srcX;
        final double zDist = dstZ - srcZ;
        final float var1 = (float) (StrictMath.atan2(zDist, xDist) * 180.0 / Math.PI) - 90.0F;
        return yaw + MathHelper.wrapAngleTo180_float(var1 - yaw);
    }

    public enum RotationsPoint {
        CLOSEST("Closest", RotationUtils::getClosestPoint),
        HEAD("Head", (start, hitBox) -> RotationUtils.getCenterPointOnBB(hitBox, 0.9)),
        CHEST("Chest", (start, hitBox) -> RotationUtils.getCenterPointOnBB(hitBox, 0.7)),
        PELVIS("Pelvis", (start, hitBox) -> RotationUtils.getCenterPointOnBB(hitBox, 0.5)),
        LEGS("Legs", (start, hitBox) -> RotationUtils.getCenterPointOnBB(hitBox, 0.3)),
        FEET("Feet", (start, hitBox) -> RotationUtils.getCenterPointOnBB(hitBox, 0.1));

        private final String name;
        private final BiFunction<Vec3, AxisAlignedBB, Vec3> getHitVecFunc;

        RotationsPoint(final String name, BiFunction<Vec3, AxisAlignedBB, Vec3> getHitVecFunc) {
            this.name = name;
            this.getHitVecFunc = getHitVecFunc;
        }

        public static RotationsPoint fromString(String name) {
            return Arrays.stream(values())
                    .filter(mode -> mode.name.equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }

        public Vec3 getHitVec(final Vec3 start, final AxisAlignedBB hitBox) {
            return this.getHitVecFunc.apply(start, hitBox);
        }

        @Override
        public String toString() {
            return this.name;
        }
    }
}