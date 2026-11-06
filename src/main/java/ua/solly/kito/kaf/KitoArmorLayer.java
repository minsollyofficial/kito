package ua.solly.kito.kaf;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.ItemArmorGeoLayer;
import ua.solly.kito.entity.KitoCharacter;

public class KitoArmorLayer extends ItemArmorGeoLayer<KitoCharacter> {
    private final boolean inner;

    public KitoArmorLayer(GeoRenderer<KitoCharacter> renderer, boolean inner) {
        super(renderer);
        this.inner = inner;
    }

    private EquipmentSlot slotFor(String bone) {
        if (inner) {
            return switch (bone) {
                case "body", "right_leg", "left_leg" -> EquipmentSlot.LEGS;
                default -> null;
            };
        }
        return switch (bone) {
            case "head" -> EquipmentSlot.HEAD;
            case "body", "right_arm", "left_arm" -> EquipmentSlot.CHEST;
            case "right_leg", "left_leg" -> EquipmentSlot.FEET;
            default -> null;
        };
    }

    @Override
    protected ItemStack getArmorItemForBone(GeoBone bone, KitoCharacter animatable) {
        EquipmentSlot slot = slotFor(bone.getName());
        if (slot == null) {
            return null;
        }
        ItemStack stack = animatable.getItemBySlot(slot);
        return stack.isEmpty() ? null : stack;
    }

    @Override
    protected EquipmentSlot getEquipmentSlotForBone(GeoBone bone, ItemStack stack, KitoCharacter animatable) {
        EquipmentSlot slot = slotFor(bone.getName());
        return slot == null ? EquipmentSlot.CHEST : slot;
    }

    @Override
    protected ModelPart getModelPartForBone(GeoBone bone, EquipmentSlot slot, ItemStack stack, KitoCharacter animatable, HumanoidModel<?> baseModel) {
        return switch (bone.getName()) {
            case "head" -> baseModel.head;
            case "right_arm" -> baseModel.rightArm;
            case "left_arm" -> baseModel.leftArm;
            case "right_leg" -> baseModel.rightLeg;
            case "left_leg" -> baseModel.leftLeg;
            default -> baseModel.body;
        };
    }
}
