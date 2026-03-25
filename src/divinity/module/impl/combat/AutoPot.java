package divinity.module.impl.combat;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.HandleInputEvent;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.player.inventory.InventoryUtils;
import divinity.utils.player.rotation.RequireRotationPriority;
import divinity.utils.player.rotation.RotationHandler;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;

import java.util.List;

public class AutoPot extends Module {

    private final NumberProperty<Double> delayProperty = new NumberProperty<>("Delay", 500.0, 50.0, 1000.0, 50.0);
    private final NumberProperty<Double> healthProperty = new NumberProperty<>("Health Threshold", 12.0, 1.0, 20.0, 0.5);
    private int stage = 0;
    private int targetSlot = -1;
    private int originalSlot = -1;
    private boolean shouldSwap = false;
    private int ticksSinceLastThrow = 0;
    private boolean awaitingRotation = false;

    public AutoPot(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(delayProperty, healthProperty);
    }

    @EventListener
    @RequireRotationPriority
    public void onEvent(HandleInputEvent event) {
        if (!shouldSwap) return;

        switch (stage) {
            case 0:
                originalSlot = mc.thePlayer.inventory.currentItem;
                mc.thePlayer.inventory.currentItem = targetSlot;
                mc.rightClickMouse();
                stage = 1;
                break;
            case 1:
                mc.thePlayer.inventory.currentItem = originalSlot;
                resetState();
                break;
        }
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPre()) return;

        ticksSinceLastThrow++;

        int delayTicks = (int) (delayProperty.getValue() / 50.0);

        if (ticksSinceLastThrow < delayTicks) return;

        if (awaitingRotation) {
            shouldSwap = true;
            awaitingRotation = false;
            return;
        }

        int slot = findBestPotionSlot();

        if (slot != -1) {
            targetSlot = slot;
            awaitingRotation = true;
            ClientManager.getInstance().getRotationHandler().submit(this, new RotationHandler.RotationRequest(mc.thePlayer.rotationYaw, 87.0F, RotationHandler.RotationPriority.FIVE, 2));
        }
    }

    private int findBestPotionSlot() {
        int bestSlot = -1;
        int bestAmp = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);

            if (stack == null || !(stack.getItem() instanceof ItemPotion)) continue;

            if (!ItemPotion.isSplash(stack.getMetadata())) continue;

            if (!InventoryUtils.isBuffPotion(stack)) continue;

            ItemPotion pot = (ItemPotion) stack.getItem();

            List<PotionEffect> effects = pot.getEffects(stack);

            if (effects.isEmpty()) continue;

            boolean skip = false;
            int maxAmp = 0;

            for (PotionEffect eff : effects) {
                Potion p = Potion.potionTypes[eff.getPotionID()];

                if (p == Potion.jump) {
                    skip = true;
                    break;
                }

                if (mc.thePlayer.isPotionActive(p)) {
                    skip = true;
                    break;
                }

                if ((p == Potion.heal || p == Potion.regeneration) && mc.thePlayer.getHealth() >= healthProperty.getValue()) {
                    skip = true;
                    break;
                }

                maxAmp = Math.max(maxAmp, eff.getAmplifier());
            }

            if (skip) continue;

            if (maxAmp > bestAmp) {
                bestAmp = maxAmp;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    @Override
    public void onEnable() {
        super.onEnable();
    }

    @Override
    public void onDisable() {
        resetState();
        super.onDisable();
    }

    private void resetState() {
        stage = 0;
        shouldSwap = false;
        awaitingRotation = false;
        targetSlot = -1;
        originalSlot = -1;
        ticksSinceLastThrow = 0;
    }
}
