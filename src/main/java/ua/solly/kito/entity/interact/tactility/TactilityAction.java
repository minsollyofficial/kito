package ua.solly.kito.entity.interact.tactility;

import net.minecraft.network.chat.Component;

public enum TactilityAction {
    CROWN("crown", TactilityLib.BASE_TRUST, 0.0F, true, 0.97F),
    HEAD("head", TactilityLib.BASE_TRUST, 0.0F, true, 0.86F),
    CHEEK_LEFT("cheek_left", TactilityLib.BASE_TRUST, 0.0F, true, 0.84F),
    CHEEK_RIGHT("cheek_right", TactilityLib.BASE_TRUST, 0.0F, true, 0.84F),
    ARM_LEFT("arm_left", TactilityLib.BASE_TRUST, 0.0F, true, 0.62F),
    ARM_RIGHT("arm_right", TactilityLib.BASE_TRUST, 0.0F, true, 0.62F),
    BACK("back", TactilityLib.BASE_TRUST, 0.0F, true, 0.68F),
	BELLY("belly", TactilityLib.BASE_TRUST, 0.0F, true, 0.58F),
	LEG_LEFT("leg_left", TactilityLib.BASE_TRUST, 0.0F, true, 0.30F),
	LEG_RIGHT("leg_right", TactilityLib.BASE_TRUST, 0.0F, true, 0.30F),
    CHEST_LEFT("chest_left", TactilityLib.FULL_TRUST, 1.6F, false, 0.71F),
    CHEST_RIGHT("chest_right", TactilityLib.FULL_TRUST, 1.6F, false, 0.71F),
    GROIN("groin", TactilityLib.FULL_TRUST, 2.4F, false, 0.46F);

    private static final TactilityAction[] VALUES = values();

    private final String id;
    private final float requiredTrust;
    private final float arousal;
    private final boolean petting;
    private final float height;

    TactilityAction(String id, float requiredTrust, float arousal, boolean petting, float height) {
        this.id = id;
        this.requiredTrust = requiredTrust;
        this.arousal = arousal;
        this.petting = petting;
        this.height = height;
    }

    public String id() {
        return id;
    }

    public float requiredTrust() {
        return requiredTrust;
    }

    public float arousal() {
        return arousal;
    }

    public boolean petting() {
        return petting;
    }

    public boolean intimate() {
        return arousal > 0.0F;
    }

    public float height() {
        return height;
    }

    public int tier() {
        return requiredTrust >= TactilityLib.FULL_TRUST ? 2 : 1;
    }

    public boolean allowedAt(float trust) {
        return trust >= requiredTrust;
    }

    public Component label() {
        return Component.translatable("tactility.kito.part." + id);
    }

    public static TactilityAction byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }
}
