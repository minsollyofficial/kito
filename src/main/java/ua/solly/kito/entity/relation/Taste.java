package ua.solly.kito.entity.relation;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;

public interface Taste {
    TrustAction classifyGift(ItemStack stack);

    float weight(TrustAction action);

    static boolean isTool(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof DiggerItem || item instanceof ShearsItem || item instanceof FishingRodItem;
    }

    static boolean isGear(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem
                || item instanceof ArmorItem
                || item instanceof ShieldItem
                || item instanceof BowItem
                || item instanceof CrossbowItem
                || item instanceof TridentItem;
    }

    static boolean isDecor(ItemStack stack) {
        return stack.getItem() instanceof DyeItem
                || stack.is(ItemTags.CANDLES)
                || stack.is(ItemTags.BANNERS)
                || stack.is(Items.PAINTING)
                || stack.is(Items.FLOWER_POT);
    }
}
