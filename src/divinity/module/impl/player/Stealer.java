package divinity.module.impl.player;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.packet.PacketEvent;
import divinity.event.impl.player.WindowClickEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.player.inventory.InventoryUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S2DPacketOpenWindow;

public class Stealer extends Module {

    private final NumberProperty<?> delay = new NumberProperty<>("Delay", 100, 0, 500, 50);
    private long lastClickTime;
    private long nextDelay = 100L;

    public Stealer(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(delay);
    }

    @EventListener
    public void onEvent(WindowClickEvent event) {
        this.lastClickTime = System.currentTimeMillis();
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        final Packet<?> packet = event.getPacket();
        if (event.isReceiving()) {
            if (packet instanceof S2DPacketOpenWindow) {
                final S2DPacketOpenWindow open = (S2DPacketOpenWindow) packet;
                if (open.getGuiId().equals("minecraft:container")) this.reset();
            }
        }
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPost()) return;
        final long timeSinceLastClick = System.currentTimeMillis() - this.lastClickTime;
        if (timeSinceLastClick < this.nextDelay) return;

        final GuiScreen current = mc.currentScreen;

        if (current instanceof GuiChest) {
            final GuiChest guiChest = (GuiChest) current;

            final IInventory lowerChestInventory = guiChest.getLowerChestInventory();

            final String chestName = lowerChestInventory.getDisplayName().getUnformattedText();

            if (!chestName.equals(I18n.format("container.chest")) && !chestName.equals(I18n.format("container.chestDouble")))
                return;

            if (!InventoryUtils.hasFreeSlots(mc.thePlayer)) {
                if (timeSinceLastClick > 100L) mc.thePlayer.closeScreen();
                return;
            }

            final int nSlots = lowerChestInventory.getSizeInventory();

            for (int i = 0; i < nSlots; i++) {
                final ItemStack stack = lowerChestInventory.getStackInSlot(i);

                if (InventoryUtils.isValidStack(mc.thePlayer, stack)) {
                    this.nextDelay = this.delay.getValue().longValue();
                    if (timeSinceLastClick < this.nextDelay) return;
                    InventoryUtils.windowClick(mc, mc.thePlayer.openContainer.windowId, i, 0, InventoryUtils.ClickType.SHIFT_CLICK);
                    return;
                }
            }

            if (timeSinceLastClick > 100L) mc.thePlayer.closeScreen();
        }
    }

    private void reset() {
        this.lastClickTime = System.currentTimeMillis();
        this.nextDelay = 100L;
    }

    @Override
    public void onEnable() {
        this.reset();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        super.onDisable();
    }
}
