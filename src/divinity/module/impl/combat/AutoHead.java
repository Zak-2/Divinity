package divinity.module.impl.combat;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.HandleInputEvent;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.NumberProperty;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;

public class AutoHead extends Module {

    private final NumberProperty<?> delayProperty = new NumberProperty<>("Delay", 500.0, 50.0, 1000.0, 50.0);
    private final NumberProperty<?> healthProperty = new NumberProperty<>("Health", 12.0, 1.0, 20.0, 0.5);
    public boolean shouldSwap = false;
    private int stage = 0;
    private int targetSlot = -1;
    private int originalSlot = -1;
    private int ticksSinceLastEat;

    public AutoHead(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(delayProperty, healthProperty);
    }

    @EventListener
    public void onEvent(HandleInputEvent event) {
        if (!shouldSwap) return;

        switch (stage) {
            case 0:
                mc.thePlayer.inventory.currentItem = targetSlot;
                mc.rightClickMouse();
                stage = 1;
                break;
            case 1:
                mc.thePlayer.inventory.currentItem = originalSlot;
                ticksSinceLastEat = 0;
                shouldSwap = false;
                stage = 0;
                break;
        }
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPre()) {
            ticksSinceLastEat++;

            if (ticksSinceLastEat <= delayProperty.getValue().doubleValue() / 50) return;

            if (mc.thePlayer.getHealth() < healthProperty.getValue().floatValue() && (mc.thePlayer.getAbsorptionAmount() <= 0 || !mc.thePlayer.isPotionActive(Potion.regeneration))) {
                int foundSlot = findAutoHeadItemSlot();

                if (foundSlot != -1 && !shouldSwap) {
                    originalSlot = mc.thePlayer.inventory.currentItem;
                    targetSlot = foundSlot;
                    shouldSwap = true;
                }
            }
        }
    }

    private int findAutoHeadItemSlot() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (isValidAutoHeadItem(stack)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidAutoHeadItem(ItemStack stack) {
        if (stack == null) return false;
        final int itemID = Item.getIdFromItem(stack.getItem());
        return itemID == 282 ||
                itemID == Item.getIdFromItem(Items.skull) ||
                itemID == Item.getIdFromItem(Items.baked_potato) ||
                itemID == Item.getIdFromItem(Items.magma_cream) ||
                itemID == Item.getIdFromItem(Items.mutton);
    }


    @Override
    public void onEnable() {
        ticksSinceLastEat = 0;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        shouldSwap = false;
        stage = 0;
        super.onDisable();
    }
}
