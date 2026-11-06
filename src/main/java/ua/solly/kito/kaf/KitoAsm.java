package ua.solly.kito.kaf;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.tactility.EntityMover;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class KitoAsm {
    public static final String ROOT = "root";
    public static final String HEAD = "head";
    public static final String BODY = "body";
    public static final String WAIST = "waist";
    public static final String BREASTS = "bearsts";
    public static final String LEFT_BREAST = "left_bearst";
    public static final String RIGHT_BREAST = "right_bearst";
    public static final String LEFT_ARM = "left_arm";
    public static final String RIGHT_ARM = "right_arm";
    public static final String LEFT_LEG = "left_leg";
    public static final String RIGHT_LEG = "right_leg";

    private static final float HEAD_SMOOTH = 0.5F;
    private static final float HEAD_FORWARD_DIP = 3.0F;
    private static final float HEAD_SNAP_GAP = 10.0F;
    private static final float HEAD_MAX_DT = 3.0F;
    private static final float EPS = 1.0E-4F;

    private final String modelName;
    private final Map<String, String> bones = new HashMap<>();
    private final Map<String, String> animations = new HashMap<>();
    private final Map<String, float[]> pending = new HashMap<>();
    private final Map<String, float[]> applied = new HashMap<>();
    private boolean headTracking;
    private float maxYaw = 50.0F;
    private float maxPitch = 30.0F;

    private KitoAsm(String modelName) {
        this.modelName = modelName;
    }

    public static KitoAsm of(String modelName) {
        return new KitoAsm(modelName);
    }

    public String modelName() {
        return modelName;
    }

    public KitoAsm bone(String logical, String actual) {
        bones.put(logical, actual);
        return this;
    }

    public KitoAsm animation(String logical, String actual) {
        animations.put(logical, actual);
        return this;
    }

    public KitoAsm headTracking(boolean enabled) {
        this.headTracking = enabled;
        return this;
    }

    public KitoAsm headLimits(float maxYaw, float maxPitch) {
        this.maxYaw = maxYaw;
        this.maxPitch = maxPitch;
        return this;
    }

    public String boneName(String logical) {
        return bones.getOrDefault(logical, logical);
    }

    public String anim(String logical) {
        return animations.getOrDefault(logical, "animation." + modelName + "." + logical);
    }

    public RawAnimation play(String logical) {
        return KitoAnimations.play(anim(logical));
    }

    public RawAnimation loop(String logical) {
        return KitoAnimations.loop(anim(logical));
    }

    public RawAnimation hold(String logical) {
        return KitoAnimations.hold(anim(logical));
    }

    public <T extends GeoAnimatable> Optional<GeoBone> bone(GeoModel<T> model, String logical) {
        return model.getBone(boneName(logical));
    }

    public <T extends GeoAnimatable> void hide(GeoModel<T> model, String logical, boolean hidden) {
        bone(model, logical).ifPresent(bone -> bone.setHidden(hidden));
    }

    public void addRotation(String logical, float rx, float ry, float rz) {
        float[] sum = pending.computeIfAbsent(logical, key -> new float[3]);
        sum[0] += rx;
        sum[1] += ry;
        sum[2] += rz;
    }

    public <T extends GeoAnimatable> void apply(GeoModel<T> model, AnimationState<T> state) {
        undo(model);
        pending.clear();
        T animatable = state.getAnimatable();
        if (headTracking) {
            applyHead(state);
        }
        if (animatable instanceof KitoCharacter kito) {
            EntityMover.apply(this, model, kito, state.getPartialTick());
            KitoSexMotion.apply(this, model, kito, state.getPartialTick());
        }
        flush(model);
    }

    private static boolean near(float a, float b) {
        return Math.abs(a - b) < EPS;
    }

    private <T extends GeoAnimatable> void undo(GeoModel<T> model) {
        for (Map.Entry<String, float[]> entry : applied.entrySet()) {
            float[] a = entry.getValue();
            bone(model, entry.getKey()).ifPresent(bone -> {
                if (near(bone.getRotX(), a[3])) {
                    bone.setRotX(a[3] - a[0]);
                }
                if (near(bone.getRotY(), a[4])) {
                    bone.setRotY(a[4] - a[1]);
                }
                if (near(bone.getRotZ(), a[5])) {
                    bone.setRotZ(a[5] - a[2]);
                }
            });
        }
        applied.clear();
    }

    private <T extends GeoAnimatable> void flush(GeoModel<T> model) {
        for (Map.Entry<String, float[]> entry : pending.entrySet()) {
            float[] delta = entry.getValue();
            String name = entry.getKey();
            bone(model, name).ifPresent(bone -> {
                float nx = bone.getRotX() + delta[0];
                float ny = bone.getRotY() + delta[1];
                float nz = bone.getRotZ() + delta[2];
                bone.setRotX(nx);
                bone.setRotY(ny);
                bone.setRotZ(nz);
                applied.put(name, new float[]{delta[0], delta[1], delta[2], nx, ny, nz});
            });
        }
        pending.clear();
    }

    private <T extends GeoAnimatable> void applyHead(AnimationState<T> state) {
        T animatable = state.getAnimatable();
        if (animatable instanceof KitoCharacter kito && kito.isTactileScene()) {
            return;
        }
        float partial = state.getPartialTick();
        float yaw;
        float pitch;
        EntityModelData data = state.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data != null) {
            yaw = Mth.wrapDegrees(data.netHeadYaw());
            pitch = data.headPitch();
        } else if (animatable instanceof LivingEntity living) {
            float body = Mth.rotLerp(partial, living.yBodyRotO, living.yBodyRot);
            float head = Mth.rotLerp(partial, living.yHeadRotO, living.yHeadRot);
            yaw = -Mth.wrapDegrees(head - body);
            pitch = -Mth.lerp(partial, living.xRotO, living.getXRot());
        } else {
            return;
        }

        float[] track = null;
        float time = 0.0F;
        if (animatable instanceof KitoCharacter kito) {
            track = kito.headTrack();
            time = kito.tickCount + partial;
        }

        yaw = Mth.clamp(yaw, -maxYaw, maxYaw);
        pitch = Mth.clamp(pitch, -maxPitch, maxPitch);

        if (track != null) {
            if (track[2] < 0.0F || time < track[2] || time - track[2] > HEAD_SNAP_GAP) {
                track[0] = yaw;
                track[1] = pitch;
                track[2] = time;
            } else if (time > track[2]) {
                float dt = Math.min(time - track[2], HEAD_MAX_DT);
                float k = 1.0F - (float) Math.exp(-dt * HEAD_SMOOTH);
                track[0] += (yaw - track[0]) * k;
                track[1] += (pitch - track[1]) * k;
                track[2] = time;
            }
            yaw = track[0];
            pitch = track[1];
        }

        addRotation(HEAD, (pitch + HEAD_FORWARD_DIP) * Mth.DEG_TO_RAD, yaw * Mth.DEG_TO_RAD, 0.0F);
    }
}
