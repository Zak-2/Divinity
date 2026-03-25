package divinity.module.impl.player;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.packet.PacketEvent;
import divinity.event.impl.player.WindowClickEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.impl.combat.Aura;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.NumberProperty;
import divinity.utils.player.inventory.InventoryUtils;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.*;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.server.S2DPacketOpenWindow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class Manager extends Module {

    private final NumberProperty<?> delayProperty = new NumberProperty<>("Delay", 100, 0, 500, 50);
    private final BooleanProperty spoofOpenProperty = new BooleanProperty("Spoof", true);
    private final BooleanProperty dropItemsProperty = new BooleanProperty("Drop Items", true);
    private final BooleanProperty sortItemsProperty = new BooleanProperty("Sort Items", true);
    private final BooleanProperty autoArmorProperty = new BooleanProperty("Auto Armor", true);
    private final BooleanProperty ignoreItemsWithCustomName = new BooleanProperty("Ignore Custom Name", true);

    private final int[] bestArmorPieces = new int[4];
    private final List<Integer> trash = new ArrayList<>();
    private final int[] bestToolSlots = new int[3];
    private final List<Integer> gappleStackSlots = new ArrayList<>();
    private int bestSwordSlot;
    private int bestBowSlot;
    private boolean serverOpen;
    private boolean clientOpen;
    private int ticksSinceLastClick;
    private boolean nextTickCloseInventory;

    public Manager(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(delayProperty, dropItemsProperty, sortItemsProperty, autoArmorProperty, spoofOpenProperty, ignoreItemsWithCustomName);
    }

    private static boolean isValidStack(final ItemStack stack) {
        if (stack.getItem() instanceof ItemBlock && InventoryUtils.isStackValidToPlace(stack)) return true;
        else if (stack.getItem() instanceof ItemPotion && InventoryUtils.isBuffPotion(stack)) return true;
        else if (stack.getItem() instanceof ItemFood && InventoryUtils.isGoodFood(stack)) return true;
        else return InventoryUtils.isGoodItem(stack.getItem());
    }

    @EventListener
    public void onEvent(WindowClickEvent event) {
        ticksSinceLastClick = 0;
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if (event.isSending()) {
            if (packet instanceof C16PacketClientStatus) {
                final C16PacketClientStatus clientStatus = (C16PacketClientStatus) packet;
                if (clientStatus.getStatus() == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                    clientOpen = true;
                    serverOpen = true;
                    ticksSinceLastClick = 0;
                }
            } else if (packet instanceof C0DPacketCloseWindow) {
                final C0DPacketCloseWindow packetCloseWindow = (C0DPacketCloseWindow) packet;
                if (packetCloseWindow.windowId == mc.thePlayer.inventoryContainer.windowId) {
                    clientOpen = false;
                    serverOpen = false;
                }
            }
        } else { // Receiving
            if (packet instanceof S2DPacketOpenWindow) {
                this.clientOpen = false;
                this.serverOpen = false;
            }
        }
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPost()) return;
        Aura aura = ClientManager.getInstance().getModuleManager().aura;

        ticksSinceLastClick++;

        if (ticksSinceLastClick < Math.floor(delayProperty.getValue().floatValue() / 50)) return;

        if (aura.target != null) {
            if (nextTickCloseInventory) nextTickCloseInventory = false;
            close();
            return;
        }

        if (clientOpen || (mc.currentScreen == null && spoofOpenProperty.getValue())) {
            clear();

            for (int slot = InventoryUtils.INCLUDE_ARMOR_BEGIN; slot < InventoryUtils.END; slot++) {
                final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();

                if (stack != null) {
                    if (ignoreItemsWithCustomName.getValue() && stack.hasDisplayName()) continue;

                    if (stack.getItem() instanceof ItemSword && InventoryUtils.isBestSword(mc.thePlayer, stack))
                        bestSwordSlot = slot;
                    else if (stack.getItem() instanceof ItemTool && InventoryUtils.isBestTool(mc.thePlayer, stack)) {
                        final int toolType = InventoryUtils.getToolType(stack);
                        if (toolType != -1 && slot != bestToolSlots[toolType]) bestToolSlots[toolType] = slot;
                    } else if (stack.getItem() instanceof ItemArmor && InventoryUtils.isBestArmor(mc.thePlayer, stack)) {
                        final ItemArmor armor = (ItemArmor) stack.getItem();
                        final int pieceSlot = bestArmorPieces[armor.armorType];
                        if (pieceSlot == -1 || slot != pieceSlot) bestArmorPieces[armor.armorType] = slot;
                    } else if (stack.getItem() instanceof ItemBow && InventoryUtils.isBestBow(mc.thePlayer, stack)) {
                        if (slot != bestBowSlot) bestBowSlot = slot;
                    } else if (stack.getItem() instanceof ItemAppleGold) gappleStackSlots.add(slot);
                    else if (!trash.contains(slot) && !isValidStack(stack)) trash.add(slot);
                }
            }

            final boolean busy = (!trash.isEmpty() && dropItemsProperty.getValue()) || equipArmor(false) || sortItems(false);

            if (!busy) {
                if (nextTickCloseInventory) {
                    close();
                    nextTickCloseInventory = false;
                } else nextTickCloseInventory = true;
                return;
            } else {
                boolean waitUntilNextTick = !serverOpen;
                open();
                if (nextTickCloseInventory) nextTickCloseInventory = false;
                if (waitUntilNextTick) return;
            }

            if (equipArmor(true)) return;
            if (dropItem(trash)) return;
            sortItems(true);
        }

    }

    private boolean equipArmor(boolean moveItems) {
        if (autoArmorProperty.getValue()) {
            for (int i = 0; i < bestArmorPieces.length; i++) {
                final int piece = bestArmorPieces[i];

                if (piece != -1) {
                    int armorPieceSlot = i + 5;
                    final ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(armorPieceSlot).getStack();
                    if (stack != null) continue;
                    if (moveItems)
                        InventoryUtils.windowClick(mc, piece, 0, InventoryUtils.ClickType.SHIFT_CLICK, spoofOpenProperty.getValue());
                    return true;
                }
            }
        }

        return false;
    }

    private boolean dropItem(final List<Integer> listOfSlots) {
        if (dropItemsProperty.getValue()) {
            if (!listOfSlots.isEmpty()) {
                int slot = listOfSlots.remove(0);
                InventoryUtils.windowClick(mc, slot, 1, InventoryUtils.ClickType.DROP_ITEM, spoofOpenProperty.getValue());
                return true;
            }
        }
        return false;
    }

    private boolean sortItems(final boolean moveItems) {
        if (sortItemsProperty.getValue()) {
            if (bestSwordSlot != -1) {
                if (bestSwordSlot != 36) {
                    if (moveItems) {
                        putItemInSlot(36, bestSwordSlot);
                        bestSwordSlot = 36;
                    }
                    return true;
                }
            }

            if (bestBowSlot != -1) {
                if (bestBowSlot != 38) {
                    if (moveItems) {
                        putItemInSlot(38, bestBowSlot);
                        bestBowSlot = 38;
                    }
                    return true;
                }
            }

            if (!gappleStackSlots.isEmpty()) {
                gappleStackSlots.sort(Comparator.comparingInt(slot -> mc.thePlayer.inventoryContainer.getSlot(slot).getStack().stackSize));

                final int bestGappleSlot = gappleStackSlots.get(0);

                if (bestGappleSlot != 37) {
                    if (moveItems) {
                        putItemInSlot(37, bestGappleSlot);
                        gappleStackSlots.set(0, 37);
                    }
                    return true;
                }
            }

            final int[] toolSlots = {39, 40, 41};

            for (final int toolSlot : bestToolSlots) {
                if (toolSlot != -1) {
                    final int type = InventoryUtils.getToolType(mc.thePlayer.inventoryContainer.getSlot(toolSlot).getStack());
                    if (type != -1) {
                        if (toolSlot != toolSlots[type]) {
                            if (moveItems) putToolsInSlot(type, toolSlots);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private void putItemInSlot(final int slot, final int slotIn) {
        InventoryUtils.windowClick(mc, slotIn, slot - 36, InventoryUtils.ClickType.SWAP_WITH_HOT_BAR_SLOT, spoofOpenProperty.getValue());
    }

    private void putToolsInSlot(final int tool, final int[] toolSlots) {
        final int toolSlot = toolSlots[tool];
        InventoryUtils.windowClick(mc, bestToolSlots[tool], toolSlot - 36, InventoryUtils.ClickType.SWAP_WITH_HOT_BAR_SLOT, spoofOpenProperty.getValue());
        bestToolSlots[tool] = toolSlot;
    }

    private void open() {
        if (!clientOpen && !serverOpen) {
            //mc.thePlayer.sendQueue.addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
            serverOpen = true;
        }
    }

    private void close() {
        if (!clientOpen && serverOpen) {
            //mc.thePlayer.sendQueue.addToSendQueue(new C0DPacketCloseWindow(mc.thePlayer.inventoryContainer.windowId));
            serverOpen = false;
        }
    }

    private void clear() {
        trash.clear();
        bestBowSlot = -1;
        bestSwordSlot = -1;
        gappleStackSlots.clear();
        Arrays.fill(bestArmorPieces, -1);
        Arrays.fill(bestToolSlots, -1);
    }

    @Override
    public void onEnable() {
        ticksSinceLastClick = 0;
        clientOpen = mc.currentScreen instanceof GuiInventory;
        serverOpen = clientOpen;
        super.onEnable();
    }

    @Override
    public void onDisable() {
        close();
        clear();
        super.onDisable();
    }
}
