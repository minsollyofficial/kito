package ua.solly.kito.entity.relation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;

import java.util.EnumMap;

public final class KitoRelation {
    public static final float MIN_TRUST = 0.0F;
    public static final float MAX_TRUST = 100.0F;
    public static final float MAX_DISGUST = 100.0F;
    public static final float MAX_AROUSAL = 100.0F;
    public static final float DISGUST_LOCK = 20.0F;
    public static final float FORGIVE_TRUST = 60.0F;
    public static final int MAX_HARM = 5;

    private static final long CYCLE_WINDOW = 3600L;
    private static final long HARM_DECAY_INTERVAL = 1200L;
    private static final long DISGUST_DECAY_INTERVAL = 1200L;
    private static final float CYCLE_DISGUST = 6.0F;
    private static final float CYCLE_GAIN_FACTOR = 0.5F;
    private static final float LOCKED_RECOVERY_FACTOR = 0.8F;
    private static final int STREAK_START = 3;
    private static final float STREAK_DISGUST = 3.0F;
    private static final float STREAK_DISGUST_CAP = 30.0F;

    private final EnumMap<TrustAction, Long> cooldowns = new EnumMap<>(TrustAction.class);

    private float trust;
    private float disgust;
    private float arousal;
    private int harmScore;
    private long lastHarmTick = -1_000_000L;
    private long harmDecayAt;
    private long disgustDecayAt;

    public float trust() {
        return trust;
    }

    public float disgust() {
        return disgust;
    }

    public float arousal() {
        return arousal;
    }

    public int harmScore() {
        return harmScore;
    }

    public boolean isHurt() {
        return harmScore >= STREAK_START && trust < FORGIVE_TRUST;
    }

    public boolean isDisgustLocked() {
        return disgust > DISGUST_LOCK;
    }

    public void setArousal(float value) {
        arousal = Mth.clamp(value, 0.0F, MAX_AROUSAL);
    }

    public void addArousal(float delta) {
        setArousal(arousal + delta);
    }

    public boolean apply(TrustAction action, long now) {
        return apply(action, now, 1.0F);
    }

    public boolean apply(TrustAction action, long now, float weight) {
        Long until = cooldowns.get(action);
        if (until != null && now < until) {
            return false;
        }
        cooldowns.put(action, now + action.cooldown());
        if (action.isGood()) {
            applyGood(action, now, weight);
        } else {
            applyBad(action, now);
        }
        return true;
    }

    private void applyGood(TrustAction action, long now, float weight) {
        float gain = action.amount() * weight;
        if (action.isGift() && harmScore >= 2 && now - lastHarmTick < CYCLE_WINDOW) {
            disgust += CYCLE_DISGUST;
            gain *= CYCLE_GAIN_FACTOR;
        }
        disgust = Mth.clamp(disgust, 0.0F, MAX_DISGUST);
        if (disgust > DISGUST_LOCK) {
            disgust = Math.max(0.0F, disgust - gain * LOCKED_RECOVERY_FACTOR);
        } else {
            trust = Mth.clamp(trust + gain, MIN_TRUST, MAX_TRUST);
        }
    }

    private void applyBad(TrustAction action, long now) {
        trust = Mth.clamp(trust - action.amount(), MIN_TRUST, MAX_TRUST);
        if (action.isHarm()) {
            harmScore = Math.min(MAX_HARM, harmScore + 1);
            lastHarmTick = now;
            harmDecayAt = now + HARM_DECAY_INTERVAL;
            if (harmScore >= STREAK_START) {
                float added = disgust + STREAK_DISGUST * (harmScore - STREAK_START + 1);
                disgust = Math.min(added, Math.max(disgust, STREAK_DISGUST_CAP));
            }
            disgust = Mth.clamp(disgust, 0.0F, MAX_DISGUST);
        }
    }

    public void addDisgust(float delta) {
        disgust = Mth.clamp(disgust + delta, 0.0F, MAX_DISGUST);
    }

    public void tick(long now) {
        if (harmScore > 0 && now >= harmDecayAt) {
            long steps = (now - harmDecayAt) / HARM_DECAY_INTERVAL + 1L;
            harmScore = (int) Math.max(0L, harmScore - steps);
            harmDecayAt = now + HARM_DECAY_INTERVAL;
            if (harmScore == 0) {
                disgustDecayAt = now;
            }
        }
        if (harmScore == 0 && disgust > 0.0F && now >= disgustDecayAt) {
            long steps = (now - disgustDecayAt) / DISGUST_DECAY_INTERVAL + 1L;
            disgust = Math.max(0.0F, disgust - steps);
            disgustDecayAt = now + DISGUST_DECAY_INTERVAL;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("trust", trust);
        tag.putFloat("disgust", disgust);
        tag.putFloat("arousal", arousal);
        tag.putInt("harm", harmScore);
        tag.putLong("lastHarm", lastHarmTick);
        tag.putLong("harmDecayAt", harmDecayAt);
        tag.putLong("disgustDecayAt", disgustDecayAt);
        return tag;
    }

    public static KitoRelation load(CompoundTag tag) {
        KitoRelation relation = new KitoRelation();
        relation.trust = Mth.clamp(tag.getFloat("trust"), MIN_TRUST, MAX_TRUST);
        relation.disgust = Mth.clamp(tag.getFloat("disgust"), 0.0F, MAX_DISGUST);
        relation.arousal = Mth.clamp(tag.getFloat("arousal"), 0.0F, MAX_AROUSAL);
        relation.harmScore = Mth.clamp(tag.getInt("harm"), 0, MAX_HARM);
        relation.lastHarmTick = tag.contains("lastHarm") ? tag.getLong("lastHarm") : -1_000_000L;
        relation.harmDecayAt = tag.getLong("harmDecayAt");
        relation.disgustDecayAt = tag.getLong("disgustDecayAt");
        return relation;
    }
}
