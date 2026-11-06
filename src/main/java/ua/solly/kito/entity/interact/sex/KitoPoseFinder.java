package ua.solly.kito.entity.interact.sex;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import ua.solly.kito.entity.KitoCharacter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class KitoPoseFinder {
    public static final int SEARCH_RADIUS = 12;
    public static final int BED_SEARCH_RADIUS = 24;
    public static final int SEARCH_DY = 3;
    private static final int MAX_PATH_CHECKS = 10;
    private static final int MAX_ACCEPTED = 3;
    private static final int MAX_PATH_NODES = 64;
    private static final double BED_BONUS = 4.0D;
    private static final double BED_LONG_SIDE_BONUS = 2.0D;
    private static final double PLAYER_WEIGHT = 0.5D;
    private static final double PLAYER_WIDTH = 0.6D;
    private static final double PLAYER_HEIGHT = 1.8D;

    private KitoPoseFinder() {
    }

    public static Result choose(KitoCharacter kito, ServerPlayer player) {
        return choose(kito, player, null);
    }

    public static Result choose(KitoCharacter kito, ServerPlayer player, SexAction forcedAction) {
        if (forcedAction == SexAction.BWJB) {
            float yaw = Mth.wrapDegrees(player.getYRot() + 180.0F);
            Vec3 look = player.getLookAngle();
            double horizontal = Math.sqrt(look.x * look.x + look.z * look.z);
            if (horizontal < 1.0E-4D) {
                return null;
            }
            double x = player.getX() + look.x / horizontal * 1.15D;
            double z = player.getZ() + look.z / horizontal * 1.15D;
            BlockPos stand = BlockPos.containing(x, player.getY(), z);
            if (!isStandable(kito.level(), stand, kito.getBbWidth(), kito.getBbHeight()) || !isReachable(kito, stand)) {
                return null;
            }
            kito.getNavigation().stop();
            return new Result(SexAction.BWJB, stand, yaw, kito.distanceToSqr(x, stand.getY(), z));
        }
        List<Result> results = find(kito, player);
        if (results.isEmpty()) {
            return null;
        }
        List<SexAction> present = new ArrayList<>();
        for (Result result : results) {
            if (!present.contains(result.action())) {
                present.add(result.action());
            }
        }
        SexAction action = present.get(kito.getRandom().nextInt(present.size()));
        if (forcedAction != null && forcedAction != action) {
            for (Result result : results) {
                if (result.action() == forcedAction) {
                    action = forcedAction;
                    break;
                }
            }
            if (action != forcedAction) {
                return null;
            }
        }
        List<Result> pool = new ArrayList<>();
        for (Result result : results) {
            if (result.action() == action) {
                pool.add(result);
            }
        }
        return pool.get(kito.getRandom().nextInt(Math.min(2, pool.size())));
    }

    public static List<Result> find(KitoCharacter kito, ServerPlayer player) {
        Level level = kito.level();
        List<Candidate> candidates = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        collectBeds(level, kito, candidates, seen);
        collectWalls(level, kito, candidates, seen);
        for (Candidate candidate : candidates) {
            double cx = candidate.pos().getX() + 0.5D;
            double cz = candidate.pos().getZ() + 0.5D;
            double travel = Math.sqrt(kito.distanceToSqr(cx, candidate.pos().getY(), cz));
            double toPlayer = Math.sqrt(player.distanceToSqr(cx, candidate.pos().getY(), cz));
            candidate.score = travel + toPlayer * PLAYER_WEIGHT - candidate.bonus;
        }
        candidates.sort(Comparator.comparingDouble(c -> c.score));

        List<Result> accepted = new ArrayList<>();
        int[] checks = new int[SexAction.values().length];
        int[] taken = new int[SexAction.values().length];
        for (Candidate candidate : candidates) {
            int slot = candidate.action().ordinal();
            if (taken[slot] >= MAX_ACCEPTED || checks[slot] >= MAX_PATH_CHECKS) {
                continue;
            }
            checks[slot]++;
            if (!isReachable(kito, candidate.pos())) {
                continue;
            }
            taken[slot]++;
            accepted.add(new Result(candidate.action(), candidate.pos(), candidate.yaw(), candidate.score));
        }
        kito.getNavigation().stop();
        return accepted;
    }

    private static void collectBeds(Level level, KitoCharacter kito, List<Candidate> out, Set<Long> seen) {
        BlockPos origin = kito.blockPosition();
        int r = BED_SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -SEARCH_DY; dy <= SEARCH_DY; dy++) {
                    BlockPos bed = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(bed)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(bed);
                    if (!(state.getBlock() instanceof BedBlock) || !state.is(BlockTags.BEDS)) {
                        continue;
                    }
                    if (!level.getBlockState(bed.above()).getCollisionShape(level, bed.above()).isEmpty()) {
                        continue;
                    }
                    Direction bedAxis = state.getValue(BedBlock.FACING);
                    BedPart part = state.getValue(BedBlock.PART);
                    for (Direction side : Direction.Plane.HORIZONTAL) {
                        BlockPos stand = bed.relative(side);
                        if (level.getBlockState(stand).getBlock() instanceof BedBlock) {
                            continue;
                        }
                        Direction toBed = side.getOpposite();
                        if (!isSpotValid(level, kito, stand, toBed)) {
                            continue;
                        }
                        double bonus = BED_BONUS;
                        if (side.getAxis() != bedAxis.getAxis()) {
                            bonus += BED_LONG_SIDE_BONUS;
                        }
                        if (part == BedPart.HEAD) {
                            bonus -= 0.5D;
                        }
                        add(out, seen, SexAction.DOGGY, stand, toBed.toYRot(), bonus);
                    }
                }
            }
        }
    }

    private static void collectWalls(Level level, KitoCharacter kito, List<Candidate> out, Set<Long> seen) {
        BlockPos origin = kito.blockPosition();
        int r = SEARCH_RADIUS;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -SEARCH_DY; dy <= SEARCH_DY; dy++) {
                    BlockPos pos = origin.offset(dx, dy, dz);
                    if (!level.hasChunkAt(pos)) {
                        continue;
                    }
                    for (Direction toWall : Direction.Plane.HORIZONTAL) {
                        BlockPos wall = pos.relative(toWall);
                        if (!isFlatWall(level, wall, toWall)) {
                            continue;
                        }
                        if (!isSpotValid(level, kito, pos, toWall)) {
                            continue;
                        }
                        add(out, seen, SexAction.WALL_LEAN, pos, toWall.toYRot(), 0.0D);
                    }
                }
            }
        }
    }

    private static void add(List<Candidate> out, Set<Long> seen, SexAction action, BlockPos pos, float yaw, double bonus) {
        long key = pos.asLong() * 31L + action.ordinal();
        if (!seen.add(key)) {
            return;
        }
        out.add(new Candidate(action, pos.immutable(), yaw, bonus));
    }

    private static boolean isFlatWall(Level level, BlockPos wall, Direction toWall) {
        Direction face = toWall.getOpposite();
        for (int i = 0; i < 2; i++) {
            BlockPos p = wall.above(i);
            BlockState state = level.getBlockState(p);
            if (!state.isFaceSturdy(level, p, face)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSpotValid(Level level, KitoCharacter kito, BlockPos stand, Direction toSupport) {
        if (!isStandable(level, stand, kito.getBbWidth(), kito.getBbHeight())) {
            return false;
        }
        BlockPos behind = stand.relative(toSupport.getOpposite());
        return isStandable(level, behind, PLAYER_WIDTH, PLAYER_HEIGHT);
    }

    private static boolean isStandable(Level level, BlockPos pos, double width, double height) {
        BlockPos below = pos.below();
        BlockState floor = level.getBlockState(below);
        if (!floor.isFaceSturdy(level, below, Direction.UP)) {
            return false;
        }
        if (isHazard(floor) || isHazard(level.getBlockState(pos)) || isHazard(level.getBlockState(pos.above()))) {
            return false;
        }
        if (!level.getFluidState(pos).isEmpty() || !level.getFluidState(pos.above()).isEmpty() || !level.getFluidState(below).isEmpty()) {
            return false;
        }
        double half = width * 0.5D;
        AABB box = new AABB(
                pos.getX() + 0.5D - half, pos.getY(), pos.getZ() + 0.5D - half,
                pos.getX() + 0.5D + half, pos.getY() + height, pos.getZ() + 0.5D + half
        );
        return level.noCollision(box);
    }

    private static boolean isHazard(BlockState state) {
        return state.is(BlockTags.FIRE)
                || state.is(BlockTags.CAMPFIRES)
                || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK)
                || state.is(Blocks.SWEET_BERRY_BUSH)
                || state.is(Blocks.WITHER_ROSE)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.COBWEB);
    }

    private static boolean isReachable(KitoCharacter kito, BlockPos pos) {
        Path path = kito.getNavigation().createPath(pos, 0);
        return path != null && path.canReach() && path.getNodeCount() <= MAX_PATH_NODES;
    }

    private static final class Candidate {
        private final SexAction action;
        private final BlockPos pos;
        private final float yaw;
        private final double bonus;
        private double score;

        private Candidate(SexAction action, BlockPos pos, float yaw, double bonus) {
            this.action = action;
            this.pos = pos;
            this.yaw = yaw;
            this.bonus = bonus;
        }

        private SexAction action() {
            return action;
        }

        private BlockPos pos() {
            return pos;
        }

        private float yaw() {
            return yaw;
        }
    }

    public record Result(SexAction action, BlockPos pos, float yaw, double score) {
    }
}
