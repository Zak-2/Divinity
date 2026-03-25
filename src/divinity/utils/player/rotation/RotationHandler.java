package divinity.utils.player.rotation;

import divinity.event.base.EventListener;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.module.Module;
import divinity.utils.player.RotationUtils;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.MathHelper;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RotationHandler {

    public float lastYaw;
    public float lastPitch;
    private Module activeModule = null;
    private final Map<Module, RotationRequest> requests = new ConcurrentHashMap<>();

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (event.isPost()) return;

        activeModule = null;

        Optional<Map.Entry<Module, RotationRequest>> winner = requests.entrySet().stream().min(Map.Entry.comparingByValue(Comparator.comparingInt((RotationRequest t) -> t.getPriority().ordinal())));

        if (winner.isPresent()) {
            RotationRequest bestReq = winner.get().getValue();

            float[] rots = processRotations(bestReq);

            if (bestReq.isSilentAim()) {
                event.setYaw(rots[0]);
                event.setPitch(rots[1]);
            } else {
                Minecraft.getMinecraft().thePlayer.rotationYaw = rots[0];
                Minecraft.getMinecraft().thePlayer.rotationPitch = rots[1];
            }

            activeModule = winner.get().getKey();

            lastYaw = rots[0];
            lastPitch = rots[1];

            bestReq.ticksRemaining--;

            if (bestReq.ticksRemaining <= 0) requests.remove(activeModule);
        }

        requests.entrySet().removeIf(e -> e.getValue().ticksRemaining <= 0);
    }

    private float[] processRotations(RotationRequest request) {
        float[] rotations = new float[]{request.yaw, request.pitch};
        float[] lastRotations = {lastYaw, lastPitch};

        RotationUtils.applyGCD(rotations, lastRotations);

        rotations[1] = MathHelper.clamp_float(rotations[1], -90.0f, 90.0f);

        return rotations;
    }

    public void submit(Module module, RotationRequest req) {
        if (module == null || req == null) return;
        requests.put(module, req);
    }

    public void clear(Module module) {
        if (module != null) requests.remove(module);
    }

    public Optional<RotationRequest> getBestRequest() {
        return requests.values().stream().filter(r -> r.ticksRemaining > 0).min(Comparator.comparingInt(r -> r.getPriority().ordinal()));
    }

    public boolean isActiveModule(Module module) {
        return activeModule != null && activeModule.equals(module);
    }

    public enum RotationPriority {
        ZERO, ONE, TWO, THREE, FOUR, FIVE
    }

    @Getter
    public static class RotationRequest {
        private final float yaw, pitch;
        private final RotationPriority priority;
        private int ticksRemaining;
        private final boolean silentAim;

        public RotationRequest(float yaw, float pitch, RotationPriority priority) {
            this(yaw, pitch, priority, 1, true);
        }

        public RotationRequest(float yaw, float pitch, RotationPriority priority, boolean silentAim) {
            this(yaw, pitch, priority, 1, silentAim);
        }

        public RotationRequest(float yaw, float pitch, RotationPriority priority, int ticks) {
            this(yaw, pitch, priority, ticks, true);
        }

        public RotationRequest(float yaw, float pitch, RotationPriority priority, int ticks, boolean silentAim) {
            this.yaw = yaw;
            this.pitch = pitch;
            this.priority = priority;
            this.ticksRemaining = ticks;
            this.silentAim = silentAim;
        }
    }
}