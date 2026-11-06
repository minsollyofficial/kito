package ua.solly.kito.entity.relation;

public record HormoneProfile(
        float jumpChance,
        float jumpScale,
        float maxJump,
        float decay,
        float trustWeight,
        float disgustWeight,
        float arousalWeight,
        float equilibriumScale
) {
    public static final HormoneProfile FEMALE = new HormoneProfile(0.035F, 10.0F, 60.0F, 0.02F, 1.1F, 1.2F, 0.2F, 25.0F);
}