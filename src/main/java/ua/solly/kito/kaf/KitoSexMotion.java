package ua.solly.kito.kaf;

import net.minecraft.util.Mth;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.GeoModel;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.interact.sex.SexAction;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class KitoSexMotion {
    public static final int THRUST_TICKS = 26;
    public static final float BWJB_LEAN_MAX = 16.0F;
    public static final float BWJB_PUSH = 1.0F;
    private static final float BWJB_RISE = 0.3F;
    private static final float BWJB_HOLD = 0.45F;

    private static final double HIP = 12.0D;
    private static final double TORSO = 10.0D;
    private static final double ARM = 10.25D;
    private static final double ARM_HALF = 2.25D;
    private static final double BED_TOP = 9.0D;
    private static final double CLEAR = 0.02D;
    private static final double DOGGY_LEAN = 60.0D;
    private static final double WALL_LEAN = 20.0D;
    private static final double DOGGY_HEAD_DOWN = 28.0D;
    private static final double WALL_HEAD_DOWN = 8.0D;
    private static final float RHO_THRUST = 4.0F;
    private static final float RHO_BREATH = 0.5F;
    private static final float BREAST_SWING = 9.0F;
    private static final float HEAD_THROW = 5.0F;
    private static final float FADE_TICKS = 8.0F;
    private static final float WOBBLE_TICKS = 90.0F;

    private static final Map<SexAction, double[]> REST = new EnumMap<>(SexAction.class);
    private static final Map<Integer, Track> TRACKS = new HashMap<>();

    private KitoSexMotion() {
    }

    private static final class Track {
        private String action = "";
        private boolean active;
        private boolean thrusting;
        private float enter = -1000.0F;
        private float exit = -1000.0F;
        private float start = -1000.0F;
    }

    public static <T extends GeoAnimatable> void apply(KitoAsm asm, GeoModel<T> model, KitoCharacter kito, float partial) {
        float now = kito.tickCount + partial;
        if (TRACKS.size() > 64) {
            TRACKS.clear();
        }
        Track track = TRACKS.computeIfAbsent(kito.getId(), id -> new Track());
        SexAction action = SexAction.byId(kito.sexAnimId());
        boolean active = action != null && kito.sexState() > 0;

        if (active) {
            if (!track.active || !action.id().equals(track.action)) {
                track.enter = now;
                track.action = action.id();
            }
            track.active = true;
        } else if (track.active) {
            track.active = false;
            track.exit = now;
        }

        float weight;
        SexAction use;
        if (active) {
            weight = smooth((now - track.enter) / FADE_TICKS);
            use = action;
        } else {
            weight = 1.0F - smooth((now - track.exit) / FADE_TICKS);
            use = SexAction.byId(track.action);
        }
        if (use == null || weight <= 0.0F) {
            track.thrusting = false;
            return;
        }

        boolean thrusting = active && kito.isThrusting();
        if (use == SexAction.BWJB) {
            track.thrusting = thrusting;
            return;
        }

        if (thrusting && !track.thrusting) {
            track.start = now;
        }
        track.thrusting = thrusting;
        float elapsed = now - track.start;

        float stroke = stroke(elapsed / THRUST_TICKS);
        float rho = RHO_THRUST * stroke + RHO_BREATH * Mth.sin(now * 0.15F);

        double[] sol = solve(use, rho);
        double waist = sol[0];
        double arm = sol[1];
        double headDown = use == SexAction.WALL_LEAN ? WALL_HEAD_DOWN : DOGGY_HEAD_DOWN;

        blend(asm, model, KitoAsm.ROOT, (float) Math.toRadians(-rho), weight);
        blend(asm, model, KitoAsm.WAIST, (float) Math.toRadians(-waist), weight);
        blend(asm, model, KitoAsm.LEFT_ARM, (float) Math.toRadians(arm), weight);
        blend(asm, model, KitoAsm.RIGHT_ARM, (float) Math.toRadians(arm), weight);

        float rhoRad = rho * Mth.DEG_TO_RAD * weight;
        asm.addRotation(KitoAsm.LEFT_LEG, rhoRad, 0.0F, 0.0F);
        asm.addRotation(KitoAsm.RIGHT_LEG, rhoRad, 0.0F, 0.0F);

        float head = (float) Math.toRadians(rho + waist - headDown + HEAD_THROW * stroke);
        asm.addRotation(KitoAsm.HEAD, head * weight, 0.0F, 0.0F);

        float wobble = 0.0F;
        if (elapsed >= 0.0F && elapsed < WOBBLE_TICKS) {
            wobble = Mth.sin(elapsed * 0.55F) * (float) Math.exp(-elapsed * 0.09F);
        }
        asm.addRotation(KitoAsm.BREASTS, BREAST_SWING * wobble * Mth.DEG_TO_RAD * weight, 0.0F, 0.0F);
    }

    private static <T extends GeoAnimatable> void blend(KitoAsm asm, GeoModel<T> model, String logical, float target, float weight) {
        Optional<GeoBone> bone = asm.bone(model, logical);
        if (bone.isEmpty()) {
            return;
        }
        asm.addRotation(logical, weight * (target - bone.get().getRotX()), 0.0F, 0.0F);
    }

    public static float bwjbPhase(float seconds) {
        if (seconds <= 0.0F || seconds >= 1.0F) {
            return 0.0F;
        }
        if (seconds < BWJB_RISE) {
            return smooth(seconds / BWJB_RISE);
        }
        if (seconds < BWJB_HOLD) {
            return 1.0F;
        }
        return 1.0F - smooth((seconds - BWJB_HOLD) / (1.0F - BWJB_HOLD));
    }

    private static float smooth(float t) {
        float c = Mth.clamp(t, 0.0F, 1.0F);
        return c * c * (3.0F - 2.0F * c);
    }

    private static float stroke(float p) {
        if (p <= 0.0F || p >= 1.0F) {
            return 0.0F;
        }
        float u = 1.0F - (float) Math.pow(1.0F - p, 1.35D);
        return Mth.sin((float) Math.PI * u);
    }

    private static double[] rest(SexAction action) {
        double[] cached = REST.get(action);
        if (cached != null) {
            return cached;
        }
        boolean wall = action == SexAction.WALL_LEAN;
        double edge = (0.5D - action.standOffset()) * 16.0D;
        double lean = wall ? WALL_LEAN : DOGGY_LEAN;
        double r = Math.toRadians(lean);
        double sf = TORSO * Math.sin(r);
        double sh = HIP + TORSO * Math.cos(r);
        double phi = Math.toRadians(wall ? 30.0D : 140.0D);
        double tf = 0.0D;
        double th = 0.0D;
        for (int i = 0; i < 8; i++) {
            if (wall) {
                tf = edge - ARM_HALF * Math.abs(Math.cos(phi)) - CLEAR;
                double dx = tf - sf;
                th = sh + Math.sqrt(Math.max(0.0D, ARM * ARM - dx * dx));
            } else {
                th = BED_TOP + ARM_HALF * Math.abs(Math.sin(phi)) + CLEAR;
                double dh = th - sh;
                tf = sf + Math.sqrt(Math.max(0.0D, ARM * ARM - dh * dh));
            }
            phi = Math.atan2(tf - sf, th - sh);
        }
        double[] result = {lean, tf, th};
        REST.put(action, result);
        return result;
    }

    private static double reach(double[] rest, double rho, double waist) {
        double a = rho + Math.toRadians(waist);
        double sf = HIP * Math.sin(rho) + TORSO * Math.sin(a);
        double sh = HIP * Math.cos(rho) + TORSO * Math.cos(a);
        return Math.hypot(rest[1] - sf, rest[2] - sh) - ARM;
    }

    private static double[] solve(SexAction action, float rhoDeg) {
        double[] rest = rest(action);
        double rho = Math.toRadians(rhoDeg);
        double lo = action == SexAction.WALL_LEAN ? -30.0D : 5.0D;
        double hi = action == SexAction.WALL_LEAN ? rest[0] : 110.0D;
        double waist;
        if (reach(rest, rho, lo) <= 0.0D) {
            waist = lo;
        } else if (reach(rest, rho, hi) >= 0.0D) {
            waist = hi;
        } else {
            for (int i = 0; i < 28; i++) {
                double mid = (lo + hi) * 0.5D;
                if (reach(rest, rho, mid) > 0.0D) {
                    lo = mid;
                } else {
                    hi = mid;
                }
            }
            waist = (lo + hi) * 0.5D;
        }
        double a = rho + Math.toRadians(waist);
        double sf = HIP * Math.sin(rho) + TORSO * Math.sin(a);
        double sh = HIP * Math.cos(rho) + TORSO * Math.cos(a);
        double phi = Math.atan2(rest[1] - sf, rest[2] - sh);
        double arm = Math.toDegrees(a + Math.PI - phi);
        return new double[]{waist, arm};
    }
}
