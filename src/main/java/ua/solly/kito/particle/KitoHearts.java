package ua.solly.kito.particle;

import net.minecraft.server.level.ServerLevel;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.init.KitoModParticles;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class KitoHearts {
    public static final int LIFETIME = 80;
    public static final int INTERVAL = 25;
    public static final int MAX_ALIVE = 8;

    private static final Map<UUID, State> STATES = new HashMap<>();

    private static final class State {
        long next;
        final ArrayDeque<Long> expiries = new ArrayDeque<>();
    }

    private KitoHearts() {
    }

    public static void spawn(KitoCharacter kito) {
        if (!(kito.level() instanceof ServerLevel level)) {
            return;
        }
        long now = level.getGameTime();

        Iterator<Map.Entry<UUID, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            State other = it.next().getValue();
            while (!other.expiries.isEmpty() && other.expiries.peekFirst() <= now) {
                other.expiries.pollFirst();
            }
            if (other.expiries.isEmpty() && other.next <= now) {
                it.remove();
            }
        }

        State state = STATES.computeIfAbsent(kito.getUUID(), id -> new State());
        if (now < state.next || state.expiries.size() >= MAX_ALIVE) {
            return;
        }
        state.next = now + INTERVAL;
        state.expiries.addLast(now + LIFETIME);

        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.35D + level.random.nextDouble() * 0.2D;
        double x = kito.getX() + Math.cos(angle) * radius;
        double y = kito.getY() + kito.getBbHeight() * (0.88D + level.random.nextDouble() * 0.12D);
        double z = kito.getZ() + Math.sin(angle) * radius;
        level.sendParticles(KitoModParticles.HEARTS.get(), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    public static void burst(KitoCharacter kito, int count, double heightFraction) {
        if (!(kito.level() instanceof ServerLevel level)) {
            return;
        }
        for (int i = 0; i < count; i++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double radius = 0.25D + level.random.nextDouble() * 0.35D;
            double x = kito.getX() + Math.cos(angle) * radius;
            double y = kito.getY() + kito.getBbHeight() * Math.min(1.05D, heightFraction + level.random.nextDouble() * 0.12D);
            double z = kito.getZ() + Math.sin(angle) * radius;
            level.sendParticles(KitoModParticles.HEARTS.get(), x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
