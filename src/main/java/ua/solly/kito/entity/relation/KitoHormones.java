package ua.solly.kito.entity.relation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.EnumMap;

public final class KitoHormones {
    public static final float LIMIT = 100.0F;
    public static final float NEUTRAL = 50.0F;
    public static final long CYCLE_TICKS = 24000L * 8L;
    public static final float STRESS_LIMIT = 80.0F;

    private final EnumMap<Hormone, Float> levels = new EnumMap<>(Hormone.class);
    private float cycleOffset;

    public KitoHormones(RandomSource random) {
        for (Hormone hormone : Hormone.values()) {
            levels.put(hormone, hormone.baseline());
        }
        cycleOffset = random.nextFloat();
    }

    public float get(Hormone hormone) {
        return levels.get(hormone);
    }

    public void set(Hormone hormone, float value) {
        levels.put(hormone, Mth.clamp(value, 0.0F, LIMIT));
    }

    public void add(Hormone hormone, float delta) {
        set(hormone, get(hormone) + delta);
    }

    public float level() {
        return NEUTRAL + centered();
    }

    public float centered() {
        float temper = (get(Hormone.SEROTONIN) - 55.0F) * 0.30F
                + (get(Hormone.OXYTOCIN) - 40.0F) * 0.22F
                + (get(Hormone.DOPAMINE) - 40.0F) * 0.10F
                - (get(Hormone.CORTISOL) - 25.0F) * 0.45F
                - (get(Hormone.ADRENALINE) - 15.0F) * 0.12F;
        return Mth.clamp(temper, -50.0F, 50.0F);
    }

    public float moodShift() {
        float shift = (get(Hormone.SEROTONIN) - 55.0F) * 0.30F
                + (get(Hormone.DOPAMINE) - 40.0F) * 0.18F
                + (get(Hormone.OXYTOCIN) - 40.0F) * 0.15F
                - (get(Hormone.CORTISOL) - 25.0F) * 0.35F;
        return Mth.clamp(shift, -25.0F, 25.0F);
    }

    public float trustGainFactor() {
        float factor = 1.0F
                + (get(Hormone.OXYTOCIN) - 40.0F) / 220.0F
                - (get(Hormone.CORTISOL) - 25.0F) / 260.0F;
        return Mth.clamp(factor, 0.6F, 1.4F);
    }

    public float arousalGainFactor() {
        float factor = 1.0F
                + (get(Hormone.ESTROGEN) - 50.0F) / 120.0F
                + (get(Hormone.OXYTOCIN) - 40.0F) / 250.0F
                + (get(Hormone.DOPAMINE) - 40.0F) / 300.0F
                - (get(Hormone.CORTISOL) - 25.0F) / 140.0F;
        return Mth.clamp(factor, 0.5F, 1.7F);
    }

    public boolean isStressed() {
        return get(Hormone.CORTISOL) >= STRESS_LIMIT;
    }

    public void tick(RandomSource random, KitoRelation focus, KitoMood mood, long dayTime, long gameTime) {
        float safety = focus == null ? 0.0F : Mth.clamp(focus.trust() / 100.0F, 0.0F, 1.0F);
        float repulsion = focus == null ? 0.0F : Mth.clamp(focus.disgust() / 100.0F, 0.0F, 1.0F);
        float moodCentered = mood == null ? 0.0F : mood.centered() / 50.0F;

        for (Hormone hormone : Hormone.values()) {
            if (hormone == Hormone.ESTROGEN) {
                continue;
            }
            float target = hormone.baseline();
            switch (hormone) {
                case OXYTOCIN -> target += safety * 25.0F - repulsion * 20.0F;
                case DOPAMINE -> target += moodCentered * 15.0F;
                case SEROTONIN -> target += moodCentered * 20.0F + daylight(dayTime) * 8.0F - repulsion * 10.0F;
                case CORTISOL -> target += repulsion * 30.0F - safety * 8.0F - moodCentered * 10.0F;
                default -> {
                }
            }
            float current = get(hormone);
            float pulled = current + (target - current) * hormone.pull();
            float noise = (random.nextFloat() - 0.5F) * 0.8F;
            set(hormone, pulled + noise);
        }

        float phase = cyclePhase(gameTime);
        set(Hormone.ESTROGEN, NEUTRAL + (float) Math.sin(phase * Math.PI * 2.0D) * 35.0F);

        if (get(Hormone.CORTISOL) > 60.0F) {
            add(Hormone.SEROTONIN, -0.15F);
        }
        if (get(Hormone.OXYTOCIN) > 70.0F) {
            add(Hormone.CORTISOL, -0.2F);
        }
    }

    public void onMood(MoodEvent event) {
        switch (event) {
            case LOW_HEALTH -> {
                add(Hormone.CORTISOL, 4.0F);
                add(Hormone.ADRENALINE, 6.0F);
            }
            case TOOK_DAMAGE -> {
                add(Hormone.CORTISOL, 7.0F);
                add(Hormone.ADRENALINE, 18.0F);
                add(Hormone.OXYTOCIN, -4.0F);
            }
            case GIFT_RECEIVED -> {
                add(Hormone.DOPAMINE, 12.0F);
                add(Hormone.OXYTOCIN, 3.0F);
            }
            case PLAYER_ABSENT -> {
                add(Hormone.OXYTOCIN, -1.0F);
                add(Hormone.CORTISOL, 0.6F);
            }
            case SAFE -> {
                add(Hormone.CORTISOL, -1.5F);
                add(Hormone.SEROTONIN, 0.4F);
            }
            case FOLLOWING_FRIEND -> add(Hormone.OXYTOCIN, 0.8F);
            case DENIED_ACTION -> {
                add(Hormone.CORTISOL, 2.0F);
                add(Hormone.DOPAMINE, -3.0F);
            }
            case PET -> {
                add(Hormone.OXYTOCIN, 9.0F);
                add(Hormone.DOPAMINE, 3.0F);
                add(Hormone.CORTISOL, -3.0F);
            }
            case INTIMATE -> {
                add(Hormone.OXYTOCIN, 8.0F);
                add(Hormone.DOPAMINE, 8.0F);
                add(Hormone.CORTISOL, -4.0F);
            }
            case COMPANY -> {
                add(Hormone.OXYTOCIN, 2.5F);
                add(Hormone.CORTISOL, -1.0F);
            }
            case HUNGRY -> {
                add(Hormone.CORTISOL, 2.0F);
                add(Hormone.SEROTONIN, -1.0F);
            }
            case DEFENDED -> add(Hormone.ADRENALINE, 6.0F);
            case KILLED_HOSTILE -> {
                add(Hormone.DOPAMINE, 5.0F);
                add(Hormone.ADRENALINE, 6.0F);
            }
            case RAIN -> add(Hormone.SEROTONIN, -0.5F);
            case THUNDER -> {
                add(Hormone.CORTISOL, 2.5F);
                add(Hormone.ADRENALINE, 4.0F);
            }
            case SNOW -> add(Hormone.DOPAMINE, 1.5F);
            case SUNNY -> add(Hormone.SEROTONIN, 0.6F);
            case NIGHT -> add(Hormone.SEROTONIN, -0.3F);
            case MORNING -> {
                add(Hormone.SEROTONIN, 1.0F);
                add(Hormone.CORTISOL, 1.5F);
            }
            case SLEPT -> {
                add(Hormone.CORTISOL, -12.0F);
                add(Hormone.SEROTONIN, 6.0F);
            }
            case BORED -> add(Hormone.DOPAMINE, -1.5F);
            case PLAYED -> {
                add(Hormone.DOPAMINE, 10.0F);
                add(Hormone.OXYTOCIN, 4.0F);
                add(Hormone.ADRENALINE, 8.0F);
            }
            case GOT_FLOWER -> {
                add(Hormone.DOPAMINE, 8.0F);
                add(Hormone.OXYTOCIN, 3.0F);
            }
            case TALKED -> add(Hormone.OXYTOCIN, 2.0F);
            case IGNORED -> {
                add(Hormone.OXYTOCIN, -2.0F);
                add(Hormone.DOPAMINE, -2.0F);
            }
            case LEASHED -> {
                add(Hormone.CORTISOL, 2.0F);
                add(Hormone.OXYTOCIN, -1.5F);
            }
            case EXTINGUISHED -> {
                add(Hormone.CORTISOL, -6.0F);
                add(Hormone.OXYTOCIN, 5.0F);
            }
            case SAVED_FROM_FIRE -> {
                add(Hormone.OXYTOCIN, 10.0F);
                add(Hormone.CORTISOL, -8.0F);
            }
            case WITNESSED_DEATH -> {
                add(Hormone.CORTISOL, 8.0F);
                add(Hormone.ADRENALINE, 10.0F);
            }
            case WITNESSED_KILL -> add(Hormone.ADRENALINE, 4.0F);
            case FORGIVEN -> {
                add(Hormone.OXYTOCIN, 8.0F);
                add(Hormone.CORTISOL, -8.0F);
            }
            case APOLOGY -> {
                add(Hormone.OXYTOCIN, 4.0F);
                add(Hormone.CORTISOL, -3.0F);
            }
            case JEALOUS -> {
                add(Hormone.CORTISOL, 3.0F);
                add(Hormone.OXYTOCIN, -2.0F);
            }
        }
    }

    public void onAction(TrustAction action) {
        switch (action.type()) {
            case GIFT -> {
                add(Hormone.DOPAMINE, 6.0F);
                add(Hormone.OXYTOCIN, 4.0F);
            }
            case KINDNESS -> add(Hormone.OXYTOCIN, 3.0F);
            case ABUSE -> {
                add(Hormone.CORTISOL, 12.0F);
                add(Hormone.ADRENALINE, 20.0F);
                add(Hormone.OXYTOCIN, -8.0F);
            }
            case MISDEED -> {
                add(Hormone.CORTISOL, 6.0F);
                add(Hormone.ADRENALINE, 6.0F);
                add(Hormone.OXYTOCIN, -3.0F);
            }
        }
    }

    public void onIntimateTouch(float arousalGain) {
        add(Hormone.OXYTOCIN, 1.5F + arousalGain);
        add(Hormone.DOPAMINE, 1.0F + arousalGain);
        add(Hormone.ADRENALINE, arousalGain * 0.8F);
        add(Hormone.CORTISOL, -0.5F);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        for (Hormone hormone : Hormone.values()) {
            tag.putFloat(hormone.id(), get(hormone));
        }
        tag.putFloat("cycle", cycleOffset);
        return tag;
    }

    public void load(CompoundTag tag) {
        for (Hormone hormone : Hormone.values()) {
            if (tag.contains(hormone.id())) {
                set(hormone, tag.getFloat(hormone.id()));
            }
        }
        if (tag.contains("cycle")) {
            cycleOffset = tag.getFloat("cycle");
        }
    }

    private float cyclePhase(long gameTime) {
        float raw = (float) (gameTime % CYCLE_TICKS) / (float) CYCLE_TICKS + cycleOffset;
        return raw - (float) Math.floor(raw);
    }

    private static float daylight(long dayTime) {
        float t = (float) (dayTime % 24000L) / 24000.0F;
        return (float) Math.sin((t - 0.25F) * Math.PI * 2.0D);
    }
}
