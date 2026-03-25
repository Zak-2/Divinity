package divinity.module.impl.world.hypixel;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.world.LoadWorldEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.notifications.NotificationManager;
import divinity.utils.server.HypixelUtils;
import lombok.Getter;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public class MurderMystery extends Module {

    private static final String MURDERER_ITEM_INDICATOR = "Knife";

    @Getter
    private EntityPlayer detectedMurderer;
    private boolean murdererFoundThisGame;

    public MurderMystery(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        resetDetectionState();
    }

    @EventListener
    public void onUpdate(final UpdateEvent ignoredEvent) {
        if (ignoredEvent.isPost()) return;

        if (murdererFoundThisGame || mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.ticksExisted % 2 != 0 ||
                !HypixelUtils.getHypixelGameMode().equals("MurderMystery")) {
            return;
        }

        for (final EntityPlayer player : mc.theWorld.playerEntities) {
            if (player == null || player == mc.thePlayer) {
                continue;
            }

            final ItemStack heldItem = player.getHeldItem();
            if (heldItem != null && isMurdererItem(heldItem)) {
                handleMurdererDetection(player);
                return;
            }
        }
    }

    private boolean isMurdererItem(final ItemStack itemStack) {
        return itemStack.hasDisplayName() && itemStack.getDisplayName().contains(MURDERER_ITEM_INDICATOR);
    }

    private void handleMurdererDetection(final EntityPlayer murderer) {
        this.detectedMurderer = murderer;
        this.murdererFoundThisGame = true;

        final NotificationManager notificationManager = ClientManager.getInstance().getNotificationManager();
        if (notificationManager != null) {
            String murdererName = murderer.getGameProfile() != null ? murderer.getGameProfile().getName() : "Unknown Player";
            notificationManager.addNotification(
                    "Murderer Detected!",
                    murdererName + " is the Murderer.",
                    6000
            );
        }
    }

    @EventListener
    public void onEvent(final LoadWorldEvent ignoredEvent) {
        resetDetectionState();
    }

    @Override
    public void onDisable() {
        resetDetectionState();
        super.onDisable();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        resetDetectionState();
    }

    private void resetDetectionState() {
        this.murdererFoundThisGame = false;
        this.detectedMurderer = null;
    }
}