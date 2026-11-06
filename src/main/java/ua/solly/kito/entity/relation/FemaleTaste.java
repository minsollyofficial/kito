package ua.solly.kito.entity.relation;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Set;

public final class FemaleTaste implements Taste {
    public static final FemaleTaste INSTANCE = new FemaleTaste();

    private static final Set<Item> SWEETS = Set.of(
            Items.CAKE,
            Items.COOKIE,
            Items.PUMPKIN_PIE,
            Items.HONEY_BOTTLE,
            Items.SWEET_BERRIES,
            Items.GLOW_BERRIES
    );

    private static final Set<Item> JEWELS = Set.of(
            Items.EMERALD,
            Items.DIAMOND,
            Items.AMETHYST_SHARD,
            Items.LAPIS_LAZULI,
            Items.GOLD_INGOT,
            Items.NAUTILUS_SHELL
    );

    private FemaleTaste() {
    }

    @Override
    public TrustAction classifyGift(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        if (stack.is(ItemTags.FLOWERS)) {
            return TrustAction.GIFT_FLOWER;
        }
        if (stack.has(DataComponents.JUKEBOX_PLAYABLE)) {
            return TrustAction.GIFT_MUSIC_DISC;
        }
        if (SWEETS.contains(stack.getItem())) {
            return TrustAction.GIFT_SWEET;
        }
        if (JEWELS.contains(stack.getItem())) {
            return TrustAction.GIFT_JEWEL;
        }
        if (Taste.isDecor(stack)) {
            return TrustAction.GIFT_DECOR;
        }
        if (Taste.isTool(stack) || Taste.isGear(stack)) {
            return TrustAction.GIFT_DISLIKED;
        }
        return null;
    }

    @Override
    public float weight(TrustAction action) {
        return switch (action) {
            case PET, COMPANY -> 1.5F;
            case DEFEND_KILL, DEFEND_HIT, CLEAR_HOSTILES -> 0.8F;
            default -> 1.0F;
        };
    }
}
