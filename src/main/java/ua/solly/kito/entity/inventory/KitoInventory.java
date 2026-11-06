package ua.solly.kito.entity.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;
import ua.solly.kito.entity.KitoCharacter;

public final class KitoInventory extends SimpleContainer {
    public static final int ROWS = 4;
    public static final int SIZE = 36;
    public static final int HOTBAR_START = 27;
    public static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };

    private final KitoCharacter owner;

    public KitoInventory(KitoCharacter owner) {
        super(SIZE);
        this.owner = owner;
    }

    @Override
    public boolean stillValid(Player player) {
        return owner.isAlive() && !owner.isRemoved() && player.distanceToSqr(owner) <= 64.0D;
    }

    public boolean holds(ItemStack reference) {
        if (reference.isEmpty()) {
            return false;
        }
        for (int i = 0; i < getContainerSize(); i++) {
            if (getItem(i) == reference) {
                return true;
            }
        }
        return false;
    }

    public static double weaponDamage(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (entry.attribute().equals(Attributes.ATTACK_DAMAGE)
                    && entry.slot().test(EquipmentSlot.MAINHAND)
                    && entry.modifier().operation() == AttributeModifier.Operation.ADD_VALUE) {
                sum += entry.modifier().amount();
            }
        }
        return sum;
    }

    public ItemStack bestWeapon() {
        ItemStack best = ItemStack.EMPTY;
        double bestDamage = 0.0D;
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            double damage = weaponDamage(stack);
            if (damage > bestDamage) {
                bestDamage = damage;
                best = stack;
            }
        }
        return best;
    }

    public static double armorScore(ItemStack stack, EquipmentSlot slot) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem armorItem) || armorItem.getEquipmentSlot() != slot) {
            return -1.0D;
        }
        double armor = 0.0D;
        double toughness = 0.0D;
        for (ItemAttributeModifiers.Entry entry : stack.getAttributeModifiers().modifiers()) {
            if (!entry.slot().test(slot) || entry.modifier().operation() != AttributeModifier.Operation.ADD_VALUE) {
                continue;
            }
            if (entry.attribute().equals(Attributes.ARMOR)) {
                armor += entry.modifier().amount();
            } else if (entry.attribute().equals(Attributes.ARMOR_TOUGHNESS)) {
                toughness += entry.modifier().amount();
            }
        }
        double condition = stack.isDamageableItem() ? 1.0D - (double) stack.getDamageValue() / (double) stack.getMaxDamage() : 1.0D;
        return armor + toughness * 0.5D + condition * 0.01D;
    }

    public ItemStack bestArmor(EquipmentSlot slot) {
        ItemStack best = ItemStack.EMPTY;
        double bestScore = -1.0D;
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            double score = armorScore(stack, slot);
            if (score > bestScore) {
                bestScore = score;
                best = stack;
            }
        }
        return best;
    }

    public ListTag write(HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < getContainerSize(); i++) {
            ItemStack stack = getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putByte("Slot", (byte) i);
            entry.put("Item", stack.save(provider));
            list.add(entry);
        }
        return list;
    }

    public void read(ListTag list, HolderLookup.Provider provider) {
        clearContent();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            final int slot = entry.getByte("Slot") & 255;
            if (slot >= SIZE || !entry.contains("Item")) {
                continue;
            }
            ItemStack.parse(provider, entry.get("Item")).ifPresent(stack -> setItem(slot, stack));
        }
    }

    public void dropAll(Level level, BlockPos pos) {
        Containers.dropContents(level, pos, this);
        clearContent();
    }
}
