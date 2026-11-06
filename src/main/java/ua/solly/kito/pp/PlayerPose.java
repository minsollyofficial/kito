package ua.solly.kito.pp;

public final class PlayerPose {
    public boolean active;
    public float lean;
    public float hipPush;
    public float headPitch;
    public float headYaw;
    public final Aim right = new Aim();
    public final Aim left = new Aim();

    public static final class Aim {
        public boolean on;
        public double x;
        public double y;
        public double z;

        public void set(double tx, double ty, double tz) {
            on = true;
            x = tx;
            y = ty;
            z = tz;
        }
    }

    void reset() {
        active = false;
        lean = 0.0F;
        hipPush = 0.0F;
        headPitch = 0.0F;
        headYaw = 0.0F;
        right.on = false;
        left.on = false;
    }
}
