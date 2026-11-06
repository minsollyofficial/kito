package ua.solly.kito.entity.relation;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import ua.solly.kito.entity.Gender;
import ua.solly.kito.entity.KitoCharacter;

import java.util.Set;

public enum TrustAction {
    GIFT_FLOWER(Type.GIFT, 6.0F, 1200, Gender.FEMALE),
    GIFT_SWEET(Type.GIFT, 7.0F, 1200, Gender.FEMALE),
    GIFT_JEWEL(Type.GIFT, 10.0F, 6000, Gender.FEMALE),
    GIFT_DECOR(Type.GIFT, 5.0F, 1200, Gender.FEMALE),
    GIFT_MUSIC_DISC(Type.GIFT, 12.0F, 12000, Gender.FEMALE),

    GIFT_TOOL(Type.GIFT, 8.0F, 2400, Gender.MALE),
    GIFT_GEAR(Type.GIFT, 10.0F, 6000, Gender.MALE),
    GIFT_MEAT(Type.GIFT, 5.0F, 600, Gender.MALE),
    GIFT_MATERIAL(Type.GIFT, 6.0F, 1200, Gender.MALE),
    GIFT_SUPPLY(Type.GIFT, 6.0F, 1200, Gender.MALE),

    PET(Type.KINDNESS, 2.0F, 400),
    EXTINGUISH(Type.KINDNESS, 8.0F, 200),
    DEFEND_KILL(Type.KINDNESS, 8.0F, 100),
    DEFEND_HIT(Type.KINDNESS, 3.0F, 100),
    CLEAR_HOSTILES(Type.KINDNESS, 2.0F, 200),
    COMPANY(Type.KINDNESS, 3.0F, 1200),
    PLAY_GAME(Type.KINDNESS, 6.0F, 2400),
    LIGHT_UP(Type.KINDNESS, 2.0F, 600),
    PLACE_BED(Type.KINDNESS, 5.0F, 6000),
    PLANT_FLOWER(Type.KINDNESS, 2.0F, 300, Gender.FEMALE),
    PLACE_WORKSTATION(Type.KINDNESS, 4.0F, 1200, Gender.MALE),

    HIT(Type.ABUSE, 5.0F, 20),
    SHOOT(Type.ABUSE, 6.0F, 20),
    IGNITE(Type.ABUSE, 15.0F, 100),
    EXPLOSION(Type.ABUSE, 12.0F, 100),

    HIT_CRITICAL(Type.MISDEED, 10.0F, 40),
    LEASH(Type.MISDEED, 3.0F, 200),
    GIFT_ROTTEN(Type.MISDEED, 8.0F, 200),
    GIFT_DISLIKED(Type.MISDEED, 2.0F, 100),
    LAVA_NEAR(Type.MISDEED, 10.0F, 200),
    TNT_NEAR(Type.MISDEED, 8.0F, 200),
    FIRE_NEAR(Type.MISDEED, 6.0F, 200),
    TRAP_WEB(Type.MISDEED, 6.0F, 200),
    BREAK_BED_NEAR(Type.MISDEED, 6.0F, 200),
    KILL_PASSIVE_NEAR(Type.MISDEED, 4.0F, 100),
    KILL_OTHER_KITO(Type.MISDEED, 15.0F, 100);

    public enum Type {
        GIFT,
        KINDNESS,
        ABUSE,
        MISDEED
    }

    private static final Set<Item> ROTTEN = Set.of(Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.POISONOUS_POTATO, Items.PUFFERFISH);

    private final Type type;
    private final float amount;
    private final int cooldown;
    private final Gender only;

    TrustAction(Type type, float amount, int cooldown) {
        this(type, amount, cooldown, null);
    }

    TrustAction(Type type, float amount, int cooldown, Gender only) {
        this.type = type;
        this.amount = amount;
        this.cooldown = cooldown;
        this.only = only;
    }

    public Type type() {
        return type;
    }

    public float amount() {
        return amount;
    }

    public int cooldown() {
        return cooldown;
    }

    public boolean isGood() {
        return type == Type.GIFT || type == Type.KINDNESS;
    }

    public boolean isGift() {
        return type == Type.GIFT;
    }

    public boolean isHarm() {
        return type == Type.ABUSE;
    }

    public boolean allowedFor(Gender gender) {
        return only == null || only == gender;
    }

    public static TrustAction forInteraction(ItemStack stack, Player player, KitoCharacter kito) {
        if (stack.isEmpty()) {
            return player.isShiftKeyDown() ? PET : null;
        }
        if (stack.is(Items.WATER_BUCKET) && kito.isOnFire()) {
            return EXTINGUISH;
        }
        if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            return IGNITE;
        }
        if (ROTTEN.contains(stack.getItem())) {
            return GIFT_ROTTEN;
        }
        return null;
    }
}
