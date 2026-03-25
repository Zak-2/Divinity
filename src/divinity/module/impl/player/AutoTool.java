package divinity.module.impl.player;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.impl.combat.Aura;
import divinity.module.impl.world.Breaker;
import divinity.module.impl.world.Scaffold;
import divinity.module.property.impl.BooleanProperty;
import divinity.utils.player.RotationUtils;
import divinity.utils.player.inventory.InventoryUtils;
import net.minecraft.block.Block;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.BlockPos;

public class AutoTool extends Module {

    private final BooleanProperty autoWeaponProperty = new BooleanProperty("Auto Weapon", true);
    private final BooleanProperty switchBackProperty = new BooleanProperty("Switch back", true);
    private boolean switched;
    private int previousSlot;

    public AutoTool(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(autoWeaponProperty, switchBackProperty);
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPost()) return;

        Aura aura = ClientManager.getInstance().getModuleManager().aura;
        Breaker breaker = ClientManager.getInstance().getModuleManager().breaker;
        Scaffold scaffold = ClientManager.getInstance().getModuleManager().scaffold;

        if (switchBackProperty.getValue() && switched && previousSlot != -1) {
            mc.thePlayer.inventory.currentItem = previousSlot;
            previousSlot = -1;
            switched = false;
        }

        if (autoWeaponProperty.getValue() && aura.target != null && !scaffold.isState() || (RotationUtils.isPointedEntity(mc) && mc.gameSettings.keyBindAttack.isKeyDown())) {
            selectBestWeapon();
            return;
        }

        if (breaker.currentTarget != null || (RotationUtils.isPointedBlock(mc) && mc.gameSettings.keyBindAttack.isKeyDown())) {
            BlockPos targetPos = breaker.currentTarget != null ? breaker.currentTarget : mc.objectMouseOver.getBlockPos();
            Block block = mc.theWorld.getBlockState(targetPos).getBlock();
            selectBestTool(block);
        }
    }

    private void selectBestWeapon() {
        double bestDamage = 1;
        int bestSlot = -1;
        for (int i = InventoryUtils.ONLY_HOT_BAR_BEGIN; i < InventoryUtils.END; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null && !(stack.getItem() instanceof ItemTool) && !(stack.getItem() instanceof ItemShears)) {
                double dmg = InventoryUtils.getItemDamage(stack);
                if (dmg > bestDamage) {
                    bestDamage = dmg;
                    bestSlot = i;
                }
            }
        }
        if (bestSlot != -1) {
            previousSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = bestSlot - 36;
            switched = true;
        }
    }

    private void selectBestTool(Block block) {
        double bestEfficiency = 1;
        int bestSlot = -1;
        for (int i = InventoryUtils.ONLY_HOT_BAR_BEGIN; i < InventoryUtils.END; i++) {
            ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();
            if (stack != null) {
                if (stack.getItem() instanceof ItemTool || stack.getItem() instanceof ItemShears) {
                    double eff = stack.getItem().getStrVsBlock(stack, block);
                    if (eff > bestEfficiency) {
                        bestEfficiency = eff;
                        bestSlot = i;
                    }
                }
            }
        }
        if (bestSlot != -1) {
            previousSlot = mc.thePlayer.inventory.currentItem;
            mc.thePlayer.inventory.currentItem = bestSlot - 36;
            switched = true;
        }
    }

    @Override
    public void onEnable() {
        switched = false;
        previousSlot = -1;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (switched && previousSlot != -1) {
            mc.thePlayer.inventory.currentItem = previousSlot;
            previousSlot = -1;
            switched = false;
        }
        super.onDisable();
    }
}
