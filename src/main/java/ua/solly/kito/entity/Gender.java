package ua.solly.kito.entity;

public enum Gender {
    MALE("male", false),
    FEMALE("female", true);

    private final String id;
    private final boolean hormonal;

    Gender(String id, boolean hormonal) {
        this.id = id;
        this.hormonal = hormonal;
    }

    public String id() {
        return id;
    }

    public boolean hasHormones() {
        return hormonal;
    }

    public String translationKey() {
        return "gender.kito." + id;
    }

    public static Gender byId(String id, Gender fallback) {
        for (Gender gender : values()) {
            if (gender.id.equals(id)) {
                return gender;
            }
        }
        return fallback;
    }
}
