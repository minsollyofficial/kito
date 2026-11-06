package ua.solly.kito.entity.interact.sex;

import net.minecraft.network.chat.Component;

public enum SexAction {
    DOGGY("doggy", true, 0.10D),
    WALL_LEAN("walllean", false, -0.15D),
    BWJB("bwjb", false, 0.0D);

    private final String id;
    private final boolean needsBed;
    private final double standOffset;

    SexAction(String id, boolean needsBed, double standOffset) {
        this.id = id;
        this.needsBed = needsBed;
        this.standOffset = standOffset;
    }

    public static SexAction byId(String id) {
        for (SexAction action : values()) {
            if (action.id.equals(id)) {
                return action;
            }
        }
        return null;
    }

    public String id() {
        return id;
    }

    public boolean needsBed() {
        return needsBed;
    }

    public double standOffset() {
        return standOffset;
    }

    public String poseAnim() {
        return "animation.kito." + id + "_pose";
    }

    public String loopAnim() {
        return "animation.kito." + id;
    }

    public Component label() {
        return Component.translatable("kito.pose." + id);
    }
}
