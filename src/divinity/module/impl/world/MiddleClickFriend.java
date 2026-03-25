package divinity.module.impl.world;

import divinity.ClientManager;
import divinity.event.base.EventListener;
import divinity.event.impl.input.MouseClickEvent;
import divinity.module.Category;
import divinity.module.Module;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;

public class MiddleClickFriend extends Module {
    public MiddleClickFriend(String name, String[] aliases, Category category) {
        super(name, aliases, category);
    }

    @EventListener
    public void onClick(MouseClickEvent e) {
        if (e.isPressed()) {
            if (e.getMouseButton() == 2) {
                MovingObjectPosition object = Minecraft.getMinecraft().objectMouseOver;
                if (object != null && object.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                    Entity entity = object.entityHit;
                    if (entity instanceof EntityPlayer) {
                        if (ClientManager.getInstance().getFriendManager().isFriend(entity.getName())) {
                            ClientManager.getInstance().getFriendManager().remove(entity.getName());
                            ClientManager.getInstance().getNotificationManager().addNotification("Removed Friend", entity.getName() + " is removed from friends", 5000);
                        } else {
                            ClientManager.getInstance().getFriendManager().addFriend(entity.getName());
                            ClientManager.getInstance().getNotificationManager().addNotification("Added Friend", entity.getName() + " is now a friend", 5000);
                        }
                    }
                }
            }
        }
    }
}
