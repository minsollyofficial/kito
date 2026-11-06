package ua.solly.kito.entity.interact.sex;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public final class KitoPose {
    public enum Phase {
        APPROACH,
        SETTLE,
        ENGAGED
    }

    private SexAction action;
    private BlockPos targetPos;
    private float yaw;
    private Phase phase = Phase.APPROACH;
    private boolean sceneActive;
    private boolean intimate;
    private int phaseTicks;
    private int stuck;
    private int failedPaths;
    private double lastDist = -1.0D;

    public boolean isActive() {
        return action != null;
    }

    public SexAction action() {
        return action;
    }

    public BlockPos targetPos() {
        return targetPos;
    }

    public float yaw() {
        return yaw;
    }

    public Phase phase() {
        return phase;
    }

    public boolean isEngaged() {
        return phase == Phase.ENGAGED;
    }

    public boolean isSceneActive() {
        return sceneActive;
    }

    public boolean isIntimate() {
        return intimate;
    }

    public int phaseTicks() {
        return phaseTicks;
    }

    public int stuck() {
        return stuck;
    }

    public int failedPaths() {
        return failedPaths;
    }

    public double lastDist() {
        return lastDist;
    }

    public void set(SexAction action, BlockPos pos, float yaw) {
        this.action = action;
        this.targetPos = pos == null ? null : pos.immutable();
        this.yaw = yaw;
        this.sceneActive = false;
        this.intimate = false;
        setPhase(Phase.APPROACH);
    }

    public void clear() {
        action = null;
        targetPos = null;
        yaw = 0.0F;
        sceneActive = false;
        intimate = false;
        setPhase(Phase.APPROACH);
    }

    public void setPhase(Phase phase) {
        this.phase = phase;
        this.phaseTicks = 0;
        this.stuck = 0;
        this.failedPaths = 0;
        this.lastDist = -1.0D;
    }

    public void tickPhase() {
        phaseTicks++;
    }

    public void addStuck() {
        stuck++;
    }

    public void resetStuck() {
        stuck = 0;
    }

    public void addFailedPath() {
        failedPaths++;
    }

    public void resetFailedPaths() {
        failedPaths = 0;
    }

    public void setLastDist(double lastDist) {
        this.lastDist = lastDist;
    }

    public void setSceneActive(boolean active) {
        this.sceneActive = active;
    }

    public void markIntimate() {
        this.intimate = true;
    }

    public Vec3 standVec() {
        if (targetPos == null || action == null) {
            return null;
        }
        double rad = Math.toRadians(yaw);
        double offset = action.standOffset();
        return new Vec3(
                targetPos.getX() + 0.5D - Math.sin(rad) * offset,
                targetPos.getY(),
                targetPos.getZ() + 0.5D + Math.cos(rad) * offset
        );
    }

    public Vec3 targetVec() {
        return standVec();
    }

    public CompoundTag save() {
        return new CompoundTag();
    }

    public void load(CompoundTag tag) {
        clear();
    }
}
