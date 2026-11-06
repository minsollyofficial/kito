package ua.solly.kito.kaf;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

public class KitoHeldItemLayer<T extends GeoAnimatable> extends BlockAndItemGeoLayer<T> {
    public static final String HAND_BONE = "right_hand_item";

    private static final float OFFSET_X = 0.0F;
    private static final float OFFSET_Y = 0.0F;
    private static final float OFFSET_Z = 0.0F;
    private static final float TILT_X = 0.0F;
    private static final float TILT_Y = 0.0F;
    private static final float TILT_Z = 0.0F;
    private static final float SCALE = 1.0F;

    public KitoHeldItemLayer(GeoRenderer<T> renderer) {
        super(renderer);
    }

    @Override
    protected ItemStack getStackForBone(GeoBone bone, T animatable) {
        if (!HAND_BONE.equals(bone.getName()) || !(animatable instanceof LivingEntity living)) {
            return null;
        }
        ItemStack stack = living.getMainHandItem();
        return stack.isEmpty() ? null : stack;
    }

    @Override
    protected ItemDisplayContext getTransformTypeForStack(GeoBone bone, ItemStack stack, T animatable) {
        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Override
    protected void renderStackForBone(PoseStack poseStack, GeoBone bone, ItemStack stack, T animatable,
                                      MultiBufferSource bufferSource, float partialTick, int packedLight, int packedOverlay) {
        LivingEntity living = animatable instanceof LivingEntity entity ? entity : null;
        poseStack.pushPose();
        poseStack.translate(OFFSET_X, OFFSET_Y, OFFSET_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(TILT_X));
        poseStack.mulPose(Axis.YP.rotationDegrees(TILT_Y));
        poseStack.mulPose(Axis.ZP.rotationDegrees(TILT_Z));
        poseStack.scale(SCALE, SCALE, SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                living,
                stack,
                getTransformTypeForStack(bone, stack, animatable),
                false,
                poseStack,
                bufferSource,
                living == null ? null : living.level(),
                packedLight,
                packedOverlay,
                living == null ? 0 : living.getId()
        );
        poseStack.popPose();
    }
}
