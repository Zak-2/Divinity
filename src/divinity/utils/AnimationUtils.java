package divinity.utils;

import divinity.ClientManager;

public final class AnimationUtils {
    private static final float defaultSpeed = 0.1f;

    public static float moveUD(float current, float end, float deltaTime, float minSpeed) {
        return moveUD(current, end, defaultSpeed, deltaTime, minSpeed);
    }

    public static float moveUD(float current, float end, float smoothSpeed, float deltaTime, float minSpeed) {
        float movement = (end - current) * smoothSpeed * deltaTime;

        if (movement > 0) {
            movement = Math.max(minSpeed, movement);
            movement = Math.min(end - current, movement);
        } else if (movement < 0) {
            movement = Math.min(-minSpeed, movement);
            movement = Math.max(end - current, movement);
        }

        return current + movement;
    }

    public static float moveUD(float current, float end, float minSpeed) {
        return moveUD(current, end, defaultSpeed, minSpeed);
    }

    public static float getAnimationState(float animation, float finalState, float speed) {
        final float add = (ClientManager.getInstance().delta * (speed / 1000f));
        if (animation < finalState) {
            if (animation + add < finalState) {
                animation += add;
            } else {
                animation = finalState;
            }
        } else if (animation - add > finalState) {
            animation -= add;
        } else {
            animation = finalState;
        }
        return animation;
    }

    public static float smoothAnimation(float ani, float finalState, float speed, float scale) {
        return getAnimationState(ani, finalState, Math.max(10, (Math.abs(ani - finalState)) * speed) * scale);
    }
}
