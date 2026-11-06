package ua.solly.kito.entity.interact.menu;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import ua.solly.kito.entity.Gender;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.sex.KitoPoseManager;
import ua.solly.kito.entity.relation.KitoTrust;

public enum MenuAction {
    SEX("sex", "kito.menu.deny.sex_trust"),
    TOUCH("touch", "tactility.kito.deny.trust"),
    BWJB("bwjb", "kito.menu.deny.sex_trust"),
    KISS("kiss", "kito.menu.deny.kiss_trust"),
    GIFT("gift", null),
    FOLLOW("follow", "kito.menu.deny.follow"),
    HOME("home", "kito.menu.deny.home_trust"),
    HIDE_AND_SEEK("hide_and_seek", "kito.games.deny.trust"),
    TAG("tag", "kito.games.deny.trust"),
    INVENTORY("inventory", "kito.menu.deny.inventory");

    private static final MenuAction[] VALUES = values();
    private final String id;
    private final String denyKey;

    MenuAction(String id, String denyKey) {
        this.id = id;
        this.denyKey = denyKey;
    }

    public String denyKey() {
        return denyKey;
    }

    public boolean isIntimate() {
        return this == SEX || this == TOUCH || this == BWJB || this == KISS;
    }

    public float requiredTrust(KitoCharacter kito, Player viewer) {
        return switch (this) {
            case SEX -> KitoPoseManager.hasContraceptive(viewer) ? KitoTrust.SEX : KitoTrust.SEX_UNPROTECTED;
            case TOUCH -> KitoTrust.TOUCH;
            case BWJB -> KitoTrust.PERSONAL_PLEASURE;
            case KISS -> KitoTrust.KISS;
            case GIFT -> 0.0F;
            case FOLLOW -> kito.isFollowedBy(viewer) ? 0.0F : KitoTrust.FOLLOW;
            case HOME -> kito.home().isOwnedBy(viewer.getUUID()) ? 0.0F : KitoTrust.HOME;
            case HIDE_AND_SEEK, TAG -> KitoTrust.GAMES;
            case INVENTORY -> KitoTrust.INVENTORY;
        };
    }

    public Component label(KitoCharacter kito, Player viewer) {
        if (this == FOLLOW && kito.isFollowedBy(viewer)) {
            return Component.translatable("kito.menu.stop_follow");
        }
        if (this == HOME && kito.home().isOwnedBy(viewer.getUUID())) {
            return Component.translatable("kito.menu.home_clear");
        }
        return Component.translatable("kito.menu." + id);
    }

    public boolean availableFor(KitoCharacter kito) {
        if (isIntimate()) {
            return kito.gender() == Gender.FEMALE;
        }
        return true;
    }

    public static MenuAction byOrdinal(int ordinal) {
        return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : null;
    }
}
