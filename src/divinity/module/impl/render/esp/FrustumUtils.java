package divinity.module.impl.render.esp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.AxisAlignedBB;

public class FrustumUtils {
    private static final Frustum FRUSTUM = new Frustum();
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static boolean isBoundingBoxInFrustum(AxisAlignedBB aabb) {
        FRUSTUM.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ);
        return FRUSTUM.isBoundingBoxInFrustum(aabb);
    }
}