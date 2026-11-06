package ua.solly.kito.kaf;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Map;

public final class GeckoLibAnimations {
    public static final int DEFAULT_TRANSITION = 5;
    public static final String MOVEMENT_CONTROLLER = "movement";
    public static final String ACTION_CONTROLLER = "action";

    private GeckoLibAnimations() {
    }

    public static <T extends GeoAnimatable> AnimationController<T> controller(
            T animatable,
            String name,
            int transitionTicks,
            AnimationController.AnimationStateHandler<T> handler
    ) {
        return new AnimationController<>(animatable, name, transitionTicks, handler);
    }

    public static <T extends GeoAnimatable> AnimationController<T> controller(
            T animatable,
            String name,
            AnimationController.AnimationStateHandler<T> handler
    ) {
        return controller(animatable, name, DEFAULT_TRANSITION, handler);
    }

    public static <T extends GeoAnimatable> AnimationController<T> looping(T animatable, String controllerName, String animation) {
        RawAnimation raw = KitoAnimations.loop(animation);
        return controller(animatable, controllerName, state -> state.setAndContinue(raw));
    }

    public static <T extends GeoAnimatable> AnimationController<T> looping(T animatable, String animation) {
        return looping(animatable, MOVEMENT_CONTROLLER, animation);
    }

    public static <T extends GeoAnimatable> AnimationController<T> looping(T animatable, KitoAsm asm, String logicalAnimation) {
        return looping(animatable, MOVEMENT_CONTROLLER, asm.anim(logicalAnimation));
    }

    public static <T extends GeoAnimatable> AnimationController<T> movement(T animatable, String idle, String move) {
        return movement(animatable, MOVEMENT_CONTROLLER, DEFAULT_TRANSITION, idle, move);
    }

    public static <T extends GeoAnimatable> AnimationController<T> movement(T animatable, KitoAsm asm) {
        return movement(animatable, asm.anim(KitoAnimations.IDLE), asm.anim(KitoAnimations.WALK));
    }

    public static <T extends GeoAnimatable> AnimationController<T> movement(
            T animatable,
            String controllerName,
            int transitionTicks,
            String idle,
            String move
    ) {
        RawAnimation idleAnim = KitoAnimations.loop(idle);
        RawAnimation moveAnim = KitoAnimations.loop(move);
        return controller(animatable, controllerName, transitionTicks,
                state -> state.setAndContinue(state.isMoving() ? moveAnim : idleAnim));
    }

    public static <T extends GeoAnimatable> AnimationController<T> triggerable(
            T animatable,
            String controllerName,
            Map<String, RawAnimation> animations
    ) {
        return triggerable(animatable, controllerName, DEFAULT_TRANSITION, animations);
    }

    public static <T extends GeoAnimatable> AnimationController<T> triggerable(
            T animatable,
            String controllerName,
            int transitionTicks,
            Map<String, RawAnimation> animations
    ) {
        AnimationController<T> controller = controller(animatable, controllerName, transitionTicks, state -> PlayState.STOP);
        animations.forEach(controller::triggerableAnim);
        return controller;
    }

    public static <T extends GeoAnimatable> AnimationController<T> triggerable(
            T animatable,
            String controllerName,
            String triggerName,
            RawAnimation animation
    ) {
        return triggerable(animatable, controllerName, Map.of(triggerName, animation));
    }

    public static <T extends GeoAnimatable> AnimationController<T> triggerable(
            T animatable,
            KitoAsm asm,
            String controllerName,
            String triggerName,
            String logicalAnimation
    ) {
        return triggerable(animatable, controllerName, triggerName, asm.play(logicalAnimation));
    }

    public static <T extends GeoAnimatable> PlayState set(AnimationState<T> state, RawAnimation animation) {
        return state.setAndContinue(animation);
    }

    public static void register(AnimatableManager.ControllerRegistrar registrar, AnimationController<?>... controllers) {
        registrar.add(controllers);
    }

    public static void trigger(GeoEntity entity, String controllerName, String triggerName) {
        entity.triggerAnim(controllerName, triggerName);
    }

    public static void trigger(GeoBlockEntity blockEntity, String controllerName, String triggerName) {
        blockEntity.triggerAnim(controllerName, triggerName);
    }

    public static void trigger(GeoItem item, Entity holder, ItemStack stack, String controllerName, String triggerName) {
        if (holder.level() instanceof ServerLevel serverLevel) {
            item.triggerAnim(holder, GeoItem.getOrAssignId(stack, serverLevel), controllerName, triggerName);
        }
    }
}
