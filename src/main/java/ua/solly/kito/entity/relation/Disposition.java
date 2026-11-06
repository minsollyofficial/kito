package ua.solly.kito.entity.relation;

public enum Disposition {
    AGGRESSIVE,
    NEUTRAL,
    FRIENDLY;

    public static final float AGGRESSIVE_BELOW = -30.0F;
    public static final float FRIENDLY_ABOVE = 35.0F;

    public static float score(KitoRelation relation, float hormone) {
        return relation.trust() * 0.6F - relation.disgust() * 0.8F - relation.harmScore() * 6.0F + hormone * 0.35F;
    }

    public static Disposition of(KitoRelation relation, float hormone) {
        float score = score(relation, hormone);
        if (score <= AGGRESSIVE_BELOW) {
            return AGGRESSIVE;
        }
        if (score >= FRIENDLY_ABOVE) {
            return FRIENDLY;
        }
        return NEUTRAL;
    }
}
