package divinity.module.impl.movement;

import divinity.event.base.EventListener;
import divinity.event.impl.input.MoveInputOptionsEvent;
import divinity.event.impl.minecraft.UpdateEvent;
import divinity.event.impl.packet.PacketEvent;
import divinity.module.Category;
import divinity.module.Module;
import divinity.module.property.impl.BooleanProperty;
import divinity.module.property.impl.NumberProperty;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C02PacketUseEntity;

import java.util.concurrent.ThreadLocalRandom;

public class SprintReset extends Module {

    private int cooldownRemaining = 0;
    private int resetTicksRemaining = 0;

    private final BooleanProperty onlyOnGround = new BooleanProperty("Ground Only", true);
    private final BooleanProperty onlyWhenSprinting = new BooleanProperty("Only When Sprinting", true);

    // Replace single value props with min/max pairs for randomness
    private final NumberProperty<?> holdMin = new NumberProperty<>("Hold Min", 1, 0, 20, 1);
    private final NumberProperty<?> holdMax = new NumberProperty<>("Hold Max", 4, 0, 20, 1);
    private final NumberProperty<?> waitMin = new NumberProperty<>("Wait Min", 4, 0, 20, 1);
    private final NumberProperty<?> waitMax = new NumberProperty<>("Wait Max", 12, 0, 20, 1);

    // distribution options
    private final BooleanProperty useGaussian = new BooleanProperty("Gaussian", false);
    private final NumberProperty<?> gaussianSigma = new NumberProperty<>("Sigma", 1.25, 0.1, 10.0, 0.1);

    // occasional outliers to break regularity (percent chance)
    private final NumberProperty<?> outlierChance = new NumberProperty<>("Outlier %", 2, 0, 50, 1);
    private final NumberProperty<?> outlierMultiplier = new NumberProperty<>("Outlier Mult", 2, 1, 6, 1);

    public SprintReset(String name, String[] aliases, Category category) {
        super(name, aliases, category);
        addProperty(onlyOnGround, onlyWhenSprinting,
                holdMin, holdMax, waitMin, waitMax,
                useGaussian, gaussianSigma,
                outlierChance, outlierMultiplier);
    }

    @EventListener
    public void onPacket(PacketEvent event) {
        Packet<?> packet = event.getPacket();

        if (!(packet instanceof C02PacketUseEntity)) return;

        C02PacketUseEntity p = (C02PacketUseEntity) packet;

        if (p.getAction() != C02PacketUseEntity.Action.ATTACK) return;

        if (onlyOnGround.getValue() && !mc.thePlayer.onGround) return;
        if (onlyWhenSprinting.getValue() && !mc.thePlayer.isSprinting()) return;
        if (cooldownRemaining > 0) return;

        // sample randomized values
        resetTicksRemaining = sampleHoldTicks();
        cooldownRemaining = sampleCooldownTicks();
    }

    @EventListener
    public void onMoveInput(MoveInputOptionsEvent event) {
        if (resetTicksRemaining > 0) event.setForward(0F);
    }

    @EventListener
    public void onUpdate(UpdateEvent event) {
        if (event.isPost()) return;
        if (cooldownRemaining > 0) cooldownRemaining--;
        if (resetTicksRemaining > 0) resetTicksRemaining--;
    }

    @Override
    public void onEnable() {
        super.onEnable();
        cooldownRemaining = 0;
        resetTicksRemaining = 0;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        cooldownRemaining = 0;
        resetTicksRemaining = 0;
    }

    // -------------------------
    // Random sampling helpers
    // -------------------------
    private int sampleHoldTicks() {
        int min = Math.max(0, holdMin.getValue().intValue());
        int max = Math.max(0, holdMax.getValue().intValue());
        return sampleIntInRange(min, max);
    }

    private int sampleCooldownTicks() {
        int min = Math.max(0, waitMin.getValue().intValue());
        int max = Math.max(0, waitMax.getValue().intValue());
        return sampleIntInRange(min, max);
    }

    private int sampleIntInRange(int a, int b) {
        // Ensure min <= max
        int min = Math.min(a, b);
        int max = Math.max(a, b);

        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        // choose distribution: uniform or gaussian (normal)
        int value;
        if (useGaussian.getValue()) {
            // gaussian around mean
            double mean = (min + max) / 2.0;
            double sigma = Math.max(0.1, gaussianSigma.getValue().doubleValue());
            double sample = rnd.nextGaussian() * sigma + mean;
            // round to nearest int and clamp
            value = (int) Math.round(sample);
        } else {
            // uniform integer in [min, max]
            if (max == min) value = min;
            else value = rnd.nextInt(min, max + 1);
        }

        // clamp to [min,max]
        if (value < min) value = min;
        if (value > max) value = max;

        // occasional outlier multiplier
        int chance = outlierChance.getValue().intValue();
        if (chance > 0 && rnd.nextInt(100) < chance) {
            int mult = Math.max(1, outlierMultiplier.getValue().intValue());
            value = Math.min(max * mult, value * mult);
        }

        // final safety clamp (avoid huge numbers)
        value = Math.max(0, Math.min(value, 1000));

        return value;
    }
}
