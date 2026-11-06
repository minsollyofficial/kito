package ua.solly.kito.entity.relation;

public enum Hormone {
    OXYTOCIN("oxytocin", 40.0F, 0.020F),
    DOPAMINE("dopamine", 40.0F, 0.060F),
    SEROTONIN("serotonin", 55.0F, 0.010F),
    CORTISOL("cortisol", 25.0F, 0.020F),
    ADRENALINE("adrenaline", 15.0F, 0.150F),
    ESTROGEN("estrogen", 50.0F, 0.0F);

    private final String id;
    private final float baseline;
    private final float pull;

    Hormone(String id, float baseline, float pull) {
        this.id = id;
        this.baseline = baseline;
        this.pull = pull;
    }

    public String id() {
        return id;
    }

    public float baseline() {
        return baseline;
    }

    public float pull() {
        return pull;
    }

    public static Hormone byId(String id) {
        for (Hormone hormone : values()) {
            if (hormone.id.equals(id)) {
                return hormone;
            }
        }
        return null;
    }
}
