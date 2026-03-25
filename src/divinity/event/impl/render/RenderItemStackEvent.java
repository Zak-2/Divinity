package divinity.event.impl.render;

import divinity.event.base.Event;
import net.minecraft.item.ItemStack;

public class RenderItemStackEvent extends Event {
    public ItemStack stack;

    public RenderItemStackEvent(ItemStack stack) {
        this.stack = stack;
    }
}
