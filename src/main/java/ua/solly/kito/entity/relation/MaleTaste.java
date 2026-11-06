package ua.solly.kito.entity.relation;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public final class MaleTaste implements Taste {
    public static final MaleTaste INSTANCE = new MaleTaste();

    private static final Set<Item> MEAT = Set.of(
            Items.COOKED_BEEF,
            Items.COOKED_PORKCHOP,
            Items.COOKED_MUTTON,
            Items.COOKED_CHICKEN,
            Items.COOKED_RABBIT,
            Items.COOKED_COD,
            Items.COOKED_SALMON,
            Items.BREAD,
            Items.RABBIT_STEW
    );

    private static final Set<Item> MATERIALS = Set.of(
            Items.IRON_INGOT,
            Items.COPPER_INGOT,
            Items.COAL,
            Items.CHARCOAL,
            Items.REDSTONE,
            Items.FLINT,
            Items.LEATHER,
            Items.NETHERITE_INGOT
    );

    private static final Set<Item> SUPPLIES = Set.of(
            Items.TORCH,
            Items.LANTERN,
            Items.ENDER_PEARL,
            Items.GOLDEN_APPLE,
            Items.ENCHANTED_GOLDEN_APPLE,
            Items.COMPASS,
            Items.CLOCK,
            Items.SPYGLASS,
            Items.ARROW,
            Items.BUCKET
    );

    private MaleTaste() {
    }

    @Override
    public TrustAction classifyGift(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (Taste.isGear(stack)) {
            return TrustAction.GIFT_GEAR;
        }
        if (Taste.isTool(stack)) {
            return TrustAction.GIFT_TOOL;
        }
        Item item = stack.getItem();
        if (MEAT.contains(item)) {
            return TrustAction.GIFT_MEAT;
        }
        if (MATERIALS.contains(item)) {
            return TrustAction.GIFT_MATERIAL;
        }
        if (SUPPLIES.contains(item)) {
            return TrustAction.GIFT_SUPPLY;
        }
        if (stack.is(ItemTags.FLOWERS) || Taste.isDecor(stack)) {
            return TrustAction.GIFT_DISLIKED;
        }
        return null;
    }

    @Override
    public float weight(TrustAction action) {
        return switch (action) {
            case DEFEND_KILL, DEFEND_HIT, CLEAR_HOSTILES -> 1.5F;
            case PET -> 0.5F;
            case COMPANY -> 0.75F;
            default -> 1.0F;
        };
    }
}
