package divinity.module.impl.world;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.world.LoadWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import net.minecraft.block.BlockBed;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumFacing;

import java.util.HashSet;
import java.util.Set;

public class Defender extends Module {

    private boolean needsDetect = true;
    private final Set<BlockPos> bedSet = new HashSet<>();

    public Defender(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onLoadWorld(LoadWorldEvent event) {
        needsDetect = true;
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (event.isPost()) return;
        if (needsDetect) detectBed();
    }

    private void detectBed() {
        BlockPos player = mc.thePlayer.getPosition();

        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos pos = player.add(dx, dy, dz);
                    if (mc.theWorld.getBlockState(pos).getBlock() instanceof BlockBed) {
                        BlockPos[] halves = getHalves(pos);
                        if (halves.length == 2) {
                            bedSet.add(halves[0]);
                            bedSet.add(halves[1]);
                            mc.thePlayer.addChatMessage(new ChatComponentText("Whitelisted Bed"));
                            needsDetect = false;
                            return;
                        }
                    }
                }
            }
        }
    }

    private BlockPos[] getHalves(BlockPos pos) {
        for (EnumFacing d : EnumFacing.values()) {
            BlockPos n = pos.offset(d);
            if (mc.theWorld.getBlockState(n).getBlock() instanceof BlockBed) {
                return new BlockPos[]{pos, n};
            }
        }
        return new BlockPos[0];
    }

    public boolean isBed(BlockPos pos) {
        return bedSet.contains(pos);
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        needsDetect = true;
        super.onDisable();
    }
}