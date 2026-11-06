package ua.solly.kito.entity.relation;

public enum MoodEvent {
    LOW_HEALTH(-3.0F, 20),
    TOOK_DAMAGE(-5.0F, 10),
    GIFT_RECEIVED(4.0F, 60),
    PLAYER_ABSENT(-0.2F, 200),
    SAFE(0.5F, 100),
    FOLLOWING_FRIEND(0.4F, 100),
    DENIED_ACTION(-2.0F, 40),
    PET(1.5F, 40),
    INTIMATE(2.5F, 60),
    COMPANY(0.8F, 120),
    HUNGRY(-1.5F, 80),
    DEFENDED(-0.5F, 60),
    KILLED_HOSTILE(1.0F, 100),
    RAIN(-0.8F, 100),
    THUNDER(-2.0F, 60),
    SNOW(1.2F, 100),
    SUNNY(0.3F, 200),
    NIGHT(-0.6F, 100),
    MORNING(1.0F, 200),
    SLEPT(3.0F, 400),
    BORED(-1.0F, 150),
    PLAYED(1.5F, 120),
    GOT_FLOWER(2.0F, 100),
    TALKED(1.0F, 80),
    IGNORED(-1.5F, 60),
    LEASHED(-0.7F, 60),
    EXTINGUISHED(2.0F, 40),
    SAVED_FROM_FIRE(4.0F, 80),
    WITNESSED_DEATH(-2.5F, 80),
    WITNESSED_KILL(0.5F, 80),
    FORGIVEN(5.0F, 200),
    APOLOGY(2.0F, 100),
    JEALOUS(-1.2F, 80);

    private final float delta;
    private final int cooldown;

    MoodEvent(float delta, int cooldown) {
        this.delta = delta;
        this.cooldown = cooldown;
    }

    public float delta() {
        return delta;
    }

    public int cooldown() {
        return cooldown;
    }
}
