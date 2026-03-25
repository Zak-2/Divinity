package divinity.module.impl.combat;

import divinity.event.base.EventListener;
import divinity.event.impl.input.MoveInputOptionsEvent;
import divinity.event.impl.packet.PacketEvent;
import divinity.module.Category;
import divinity.module.Module;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S12PacketEntityVelocity;

public class Velocity extends Module {

    private boolean isFallDamage;

    public Velocity(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        setSuffix("Jump Reset");
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        final Packet<?> packet = event.getPacket();

        if (!event.isReceiving()) return;

        if (packet instanceof S12PacketEntityVelocity) {
            S12PacketEntityVelocity vel = (S12PacketEntityVelocity) packet;

            if (vel.getEntityID() != mc.thePlayer.getEntityId()) return;

            double mx = vel.getMotionX() / 8000.0;
            double mz = vel.getMotionZ() / 8000.0;

            isFallDamage = Math.abs(mx) < 1E-4 && Math.abs(mz) < 1E-4;
        }
    }

    @EventListener
    public void onEvent(MoveInputOptionsEvent event) {
        boolean shouldJump = mc.thePlayer.hurtTime >= 9 && mc.thePlayer.onGround && !isFallDamage;
        if (shouldJump) event.setJumping(true);
    }


    @Override
    public void onDisable() {
        isFallDamage = false;
        super.onDisable();
    }
}