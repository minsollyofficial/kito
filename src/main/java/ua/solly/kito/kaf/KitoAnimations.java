package ua.solly.kito.kaf;

import software.bernie.geckolib.animation.RawAnimation;

public final class KitoAnimations {
    public static final String IDLE = "idle";
    public static final String WALK = "walk";
    public static final String RUN = "run";

    private KitoAnimations() {
    }

    public static RawAnimation play(String animation) {
        return RawAnimation.begin().thenPlay(animation);
    }

    public static RawAnimation loop(String animation) {
        return RawAnimation.begin().thenLoop(animation);
    }

    public static RawAnimation hold(String animation) {
        return RawAnimation.begin().thenPlayAndHold(animation);
    }
}
