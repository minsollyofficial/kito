package ua.solly.kito.entity.relation;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

import java.util.EnumMap;

public final class KitoMood {
    public static final float MIN = 0.0F;
    public static final float MAX = 100.0F;
    public static final float NEUTRAL = 50.0F;

    private final EnumMap<MoodEvent, Long> cooldowns = new EnumMap<>(MoodEvent.class);

    private float level = NEUTRAL;

    public float level() {
        return level;
    }

    public float centered() {
        return level - NEUTRAL;
    }

    public float intensity() {
        return Math.abs(centered()) / NEUTRAL;
    }

    public void set(float value) {
        level = Mth.clamp(value, MIN, MAX);
    }

    public void add(float delta) {
        set(level + delta);
    }

    public boolean fire(MoodEvent event, long now) {
        Long until = cooldowns.get(event);
        if (until != null && now < until) {
            return false;
        }
        cooldowns.put(event, now + event.cooldown());
        add(event.delta());
        return true;
    }

    public boolean fire(MoodEvent event, long now, float multiplier) {
        Long until = cooldowns.get(event);
        if (until != null && now < until) {
            return false;
        }
        cooldowns.put(event, now + event.cooldown());
        add(event.delta() * multiplier);
        return true;
    }

    public void tick(RandomSource random, KitoRelation focus, float hormoneShift) {
        float disgust = focus == null ? 0.0F : focus.disgust();
        float base = Mth.clamp(90.0F - disgust + hormoneShift, MIN, MAX);
        level += (base - level) * 0.01F;

        if (random.nextFloat() < 0.008F) {
            float jump = (random.nextFloat() - 0.5F) * 8.0F * (1.0F + intensity());
            level = Mth.clamp(level + jump, MIN, MAX);
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("mood", level);
        return tag;
    }

    public void load(CompoundTag tag) {
        level = Mth.clamp(tag.getFloat("mood"), MIN, MAX);
    }
}
