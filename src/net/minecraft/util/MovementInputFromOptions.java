package net.minecraft.util;

import divinity.ClientManager;
import divinity.event.impl.input.MoveInputOptionsEvent;
import divinity.event.impl.player.MovementCorrectionEvent;
import divinity.gui.click.Clickable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.input.Keyboard;

public class MovementInputFromOptions extends MovementInput {
    private final GameSettings gameSettings;

    public MovementInputFromOptions(GameSettings gameSettingsIn) {
        this.gameSettings = gameSettingsIn;
    }

    public void updatePlayerMoveState() {
        // reset
        moveStrafe = 0f;
        moveForward = 0f;

        boolean isClickable = Minecraft.getMinecraft().currentScreen instanceof Clickable;

        float forward = ((isClickable ? Keyboard.isKeyDown(gameSettings.keyBindForward.getKeyCode()) : gameSettings.keyBindForward.isKeyDown()) ? 1f : 0f)
                - ((isClickable ? Keyboard.isKeyDown(gameSettings.keyBindBack.getKeyCode()) : gameSettings.keyBindBack.isKeyDown()) ? 1f : 0f);

        float strafe = ((isClickable ? Keyboard.isKeyDown(gameSettings.keyBindLeft.getKeyCode()) : gameSettings.keyBindLeft.isKeyDown()) ? 1f : 0f)
                - ((isClickable ? Keyboard.isKeyDown(gameSettings.keyBindRight.getKeyCode()) : gameSettings.keyBindRight.isKeyDown()) ? 1f : 0f);

        MoveInputOptionsEvent event = new MoveInputOptionsEvent(
                forward,
                strafe,
                gameSettings.keyBindJump.isKeyDown(),
                gameSettings.keyBindSneak.isKeyDown(),
                true,
                true
        );
        ClientManager.getInstance().getEventDispatcher().post(event);

        moveForward = event.getForward();
        moveStrafe = event.getStrafe();

        if (event.isFixMovement()) {
            MovementCorrectionEvent movEvent = new MovementCorrectionEvent(Minecraft.getMinecraft().thePlayer.rotationYaw);
            ClientManager.getInstance().getEventDispatcher().post(movEvent);
            float yaw = MathHelper.wrapAngleTo180_float(ClientManager.getInstance().getRotationHandler().lastYaw - movEvent.getYaw());
            float sin = MathHelper.sin(yaw * (float) Math.PI / 180f);
            float cos = MathHelper.cos(yaw * (float) Math.PI / 180f);

            float fwd = moveForward, str = moveStrafe;
            moveForward = fwd * cos - str * sin;
            moveStrafe = str * cos + fwd * sin;

            if (event.isRoundInput()) {
                moveForward = Math.round(moveForward);
                moveStrafe = Math.round(moveStrafe);
            }
        }

        jump = event.isJumping();
        sneak = event.isSneaking();

        if (sneak) {
            moveStrafe *= 0.3F;
            moveForward *= 0.3F;
        }
    }
}
