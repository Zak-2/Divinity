package divinity.module.impl.world.lag;

import divinity.event.base.EventListener;
import divinity.event.impl.packet.PacketEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.NumberProperty;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S00PacketKeepAlive;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PingSpoof extends Module {

    private final NumberProperty<?> pingMS = new NumberProperty<>("Latency", 1800L, 10L, 2000L, 10L);
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public PingSpoof(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(pingMS);
    }

    @EventListener
    public void onPacket(PacketEvent event) {
        final Packet<?> packet = event.getPacket();
        if (event.isSending()) return;

        if (packet instanceof S00PacketKeepAlive) {
            executor.schedule(() -> schedulePacketProcess((Packet<NetHandlerPlayClient>) packet), pingMS.getValue().longValue(), TimeUnit.MILLISECONDS);
            event.setCancelled(true);
        }
    }

    private void schedulePacketProcess(Packet<NetHandlerPlayClient> packet) {
        mc.addScheduledTask(() -> NetworkManager.processPacket(mc.getNetHandler(), packet));
    }
}
