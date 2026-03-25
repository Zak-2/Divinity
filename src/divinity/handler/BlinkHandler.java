package divinity.handler;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.packet.PacketEvent;
import divinity.event.impl.world.LoadWorldEvent;
import divinity.module.Module;
import divinity.utils.RenderUtils;
import divinity.utils.font.Fonts;
import net.minecraft.client.Minecraft;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;

import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlinkHandler {

    private enum Side {
        CLIENT,
        SERVER
    }

    private final Minecraft mc = Minecraft.getMinecraft();
    private final EnumMap<Side, BlinkState> states = new EnumMap<>(Side.class);

    public BlinkHandler() {
        states.put(Side.CLIENT, new BlinkState(EnumPacketDirection.SERVERBOUND));
        states.put(Side.SERVER, new BlinkState(EnumPacketDirection.CLIENTBOUND));
    }

    public void setClientActive(Module module, boolean active, int durationTicks) {
        states.get(Side.CLIENT).setActive(module, active, durationTicks);
    }

    public void setServerActive(Module module, boolean active, int durationTicks) {
        states.get(Side.SERVER).setActive(module, active, durationTicks);
    }

    public boolean isClientActive() {
        return states.get(Side.CLIENT).isActive();
    }

    public boolean isServerActive() {
        return states.get(Side.SERVER).isActive();
    }

    @EventListener
    public void onEvent(LoadWorldEvent event) {
        if (isClientActive() || isServerActive()) {
            states.get(Side.CLIENT).releaseAll(mc);
            states.get(Side.SERVER).releaseAll(mc);
            states.get(Side.CLIENT).clear();
            states.get(Side.SERVER).clear();
        }
    }

    @EventListener
    public void onEvent(UpdateEvent event) {
        if (event.isPost()) return;
        states.get(Side.CLIENT).tickAndMaybeRelease(mc);
        states.get(Side.SERVER).tickAndMaybeRelease(mc);
    }

    @EventListener
    public void onEvent(PacketEvent event) {
        Packet<?> pkt = event.getPacket();
        if (pkt == null) return;

        if (event.isSending() && states.get(Side.CLIENT).isActive()) {
            states.get(Side.CLIENT).enqueue(pkt);
            event.setCancelled(true);
        } else if (event.isReceiving() && states.get(Side.SERVER).isActive()) {
            if (!isPlayingPacket(pkt)) return;
            states.get(Side.SERVER).enqueue(pkt);
            event.setCancelled(true);
        }
    }

    private boolean isPlayingPacket(Packet<?> packet) {
        try {
            return EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    public void renderBlinkProgress(int x, int y) {
        int clientQueued = states.get(Side.CLIENT).getQueuedCount();
        int serverQueued = states.get(Side.SERVER).getQueuedCount();
        int clientTicks = states.get(Side.CLIENT).getTotalTicksRemaining();
        int serverTicks = states.get(Side.SERVER).getTotalTicksRemaining();

        if (clientQueued == 0 && serverQueued == 0 && !isClientActive() && !isServerActive()) return;

        final int barWidth = 160;
        final int barHeight = 10;
        final int padding = 8;
        final int spacing = 8;
        final int widgetWidth = barWidth + padding * 2;
        final int widgetHeight = padding * 2 + barHeight * 2 + spacing;

        final float cap = 20f;
        final float clientProgress = Math.min(1.0f, (float) clientQueued / cap);
        final float serverProgress = Math.min(1.0f, (float) serverQueued / cap);

        RenderUtils.glDrawFilledQuad(x, y, widgetWidth, widgetHeight, 0xCC101419);
        RenderUtils.drawRectOutline(x + .5f, y + .5f, x + widgetWidth - .5f, y + widgetHeight - .5f, 2.0f, 0xFF000000);

        int innerX = x + padding;
        int innerY = y + padding;
        RenderUtils.glDrawFilledQuad(innerX, innerY, barWidth, barHeight, 0x33555555);
        RenderUtils.drawGradientRectWH(innerX, innerY, clientProgress * barWidth, barHeight, true, 0xAA66FF66, 0xAAFF0000);
        Fonts.INTER_MEDIUM.get(14).drawStringWithShadow(String.format("Client: %d pkt (%d ticks)", clientQueued, clientTicks), innerX + 2, innerY + (barHeight / 2f) - 1, 0xFFFFFFFF);

        innerY += barHeight + spacing;
        RenderUtils.glDrawFilledQuad(innerX, innerY, barWidth, barHeight, 0x33555555);
        RenderUtils.drawGradientRectWH(innerX, innerY, serverProgress * barWidth, barHeight, true, 0xAA66FF66, 0xAAFF0000);
        Fonts.INTER_MEDIUM.get(14).drawStringWithShadow(String.format("Server: %d pkt (%d ticks)", serverQueued, serverTicks), innerX + 2, innerY + (barHeight / 2f) - 1, 0xFFFFFFFF);
    }

    private static class BlinkState {
        private final ConcurrentHashMap<Module, Integer> moduleTicks = new ConcurrentHashMap<>();
        private final ConcurrentLinkedQueue<Packet<?>> queue = new ConcurrentLinkedQueue<>();
        private final EnumPacketDirection packetDirection;

        BlinkState(EnumPacketDirection dir) {
            this.packetDirection = dir;
        }

        void setActive(Module module, boolean active, int durationTicks) {
            if (module == null) return;
            if (active) {
                if (durationTicks <= 0) {
                    moduleTicks.put(module, -1);
                } else {
                    moduleTicks.put(module, durationTicks);
                }
            } else {
                moduleTicks.remove(module);
            }
        }

        boolean isActive() {
            return !moduleTicks.isEmpty() || moduleTicks.values().stream().anyMatch(v -> v != null && v > 0);
        }

        void enqueue(Packet<?> pkt) {
            queue.add(pkt);
        }

        int getQueuedCount() {
            return queue.size();
        }

        int getTotalTicksRemaining() {
            int sum = 0;
            for (Integer v : moduleTicks.values()) {
                if (v != null && v > 0) sum += v;
                else if (v != null && v == -1) sum += 0;
            }
            return sum;
        }

        void tickAndMaybeRelease(Minecraft mc) {
            if (moduleTicks.isEmpty()) return;

            for (Map.Entry<Module, Integer> e : moduleTicks.entrySet()) {
                Integer val = e.getValue();

                if (val == null) continue;

                if (val == -1) continue;

                int next = val - 1;

                if (next <= 0) {
                    moduleTicks.remove(e.getKey());
                } else {
                    moduleTicks.put(e.getKey(), next);
                }
            }

            if (!isActive()) releaseAll(mc);
        }

        void releaseAll(Minecraft mc) {
            if (queue.isEmpty()) return;

            NetHandlerPlayClient nh = mc.getNetHandler();

            if (nh == null) {
                queue.clear();
                return;
            }

            NetworkManager nm = nh.getNetworkManager();

            if (nm == null) {
                queue.clear();
                return;
            }

            while (!queue.isEmpty()) {
                Packet<?> p = queue.poll();
                if (p == null) continue;

                if (packetDirection == EnumPacketDirection.SERVERBOUND) {
                    nh.addToSendQueue(p);
                } else {
                    nm.wrapper$channelRead0(null, p);
                }
            }
        }

        void clear() {
            moduleTicks.clear();
            queue.clear();
        }
    }
}