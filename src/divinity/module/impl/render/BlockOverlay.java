package divinity.module.impl.render;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.render.RenderWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.ColorProperty;
import divinity.module.property.impl.ModeProperty;
import divinity.utils.RenderUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import org.lwjgl.opengl.GL11;

import java.awt.*;

public class BlockOverlay extends Module {
    private final ModeProperty modeProperty = new ModeProperty("Mode", "Outline", "Outline", "Fill");

    private final ColorProperty colorProperty = new ColorProperty("Color", ClientManager.getInstance().getMainColor());

    public BlockOverlay(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(modeProperty, colorProperty);
    }

    @EventListener
    public void onEvent(RenderWorldEvent event) {
        if (modeProperty.isMode("Fill")) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos pos = mc.objectMouseOver.getBlockPos();
                Block block = mc.theWorld.getBlockState(pos).getBlock();
                String s = block.getLocalizedName();
                double x = (double) pos.getX() - RenderManager.renderPosX;
                double y = (double) pos.getY() - RenderManager.renderPosY;
                double z = (double) pos.getZ() - RenderManager.renderPosZ;
                GL11.glPushMatrix();
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDepthMask(false);


                RenderUtils.glColor(new Color(colorProperty.getValue().getRed(), colorProperty.getValue().getGreen(), colorProperty.getValue().getBlue(), colorProperty.getValue().getAlpha()).getRGB());
                double minX = block instanceof BlockStairs || Block.getIdFromBlock(block) == 134 ? 0.0 : block.getBlockBoundsMinX();
                double minY = block instanceof BlockStairs || Block.getIdFromBlock(block) == 134 ? 0.0 : block.getBlockBoundsMinY();
                double minZ = block instanceof BlockStairs || Block.getIdFromBlock(block) == 134 ? 0.0 : block.getBlockBoundsMinZ();
                RenderUtils.drawBoundingBox(new AxisAlignedBB(x + minX, y + minY, z + minZ, x + block.getBlockBoundsMaxX(), y + block.getBlockBoundsMaxY(), z + block.getBlockBoundsMaxZ()));
                RenderUtils.glColor(new Color(colorProperty.getValue().getRed(), colorProperty.getValue().getGreen(), colorProperty.getValue().getBlue(), colorProperty.getValue().getAlpha()).getRGB());
                GL11.glLineWidth(0.5f);
                GL11.glDisable(2848);
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDepthMask(true);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
            }
            GL11.glColor4f(1f, 1f, 1f, 1f);
        } else if (modeProperty.isMode("Outline")) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos pos = mc.objectMouseOver.getBlockPos();
                Block block = mc.theWorld.getBlockState(pos).getBlock();
                double x = (double) pos.getX() - RenderManager.renderPosX;
                double y = (double) pos.getY() - RenderManager.renderPosY;
                double z = (double) pos.getZ() - RenderManager.renderPosZ;

                GL11.glPushMatrix();
                GL11.glEnable(3042);
                GL11.glBlendFunc(770, 771);
                GL11.glDisable(3553);
                GL11.glEnable(2848);
                GL11.glDisable(2929);
                GL11.glDepthMask(false);

                GL11.glLineWidth(2.0f);

                RenderUtils.glColor(new Color(colorProperty.getValue().getRed(),
                        colorProperty.getValue().getGreen(),
                        colorProperty.getValue().getBlue(),
                        colorProperty.getValue().getAlpha()).getRGB());

                double minX = block instanceof BlockStairs || Block.getIdFromBlock(block) == 134 ? 0.0 : block.getBlockBoundsMinX();
                double minY = block instanceof BlockStairs || Block.getIdFromBlock(block) == 134 ? 0.0 : block.getBlockBoundsMinY();
                double minZ = block instanceof BlockStairs || Block.getIdFromBlock(block) == 134 ? 0.0 : block.getBlockBoundsMinZ();

                RenderUtils.drawOutlineBoundingBox(new AxisAlignedBB(x + minX, y + minY, z + minZ,
                        x + block.getBlockBoundsMaxX(),
                        y + block.getBlockBoundsMaxY(),
                        z + block.getBlockBoundsMaxZ()));

                GL11.glLineWidth(0.5f);
                GL11.glDisable(2848);
                GL11.glEnable(3553);
                GL11.glEnable(2929);
                GL11.glDepthMask(true);
                GL11.glDisable(3042);
                GL11.glPopMatrix();
            }
            GL11.glColor4f(1f, 1f, 1f, 1f);
        }
    }
}
