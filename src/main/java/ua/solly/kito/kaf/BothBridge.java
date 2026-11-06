package ua.solly.kito.kaf;

import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

public final class BothBridge {
    private BothBridge() {
    }

    public static Dist dist() {
        return FMLEnvironment.dist;
    }

    public static boolean isClient() {
        return FMLEnvironment.dist == Dist.CLIENT;
    }

    public static boolean isServer() {
        return FMLEnvironment.dist == Dist.DEDICATED_SERVER;
    }

    public static void client(Supplier<Runnable> task) {
        if (isClient()) {
            task.get().run();
        }
    }

    public static void server(Supplier<Runnable> task) {
        if (isServer()) {
            task.get().run();
        }
    }

    public static void both(Runnable common, Supplier<Runnable> clientOnly) {
        common.run();
        client(clientOnly);
    }

    public static void split(Supplier<Runnable> clientTask, Supplier<Runnable> serverTask) {
        if (isClient()) {
            clientTask.get().run();
        } else {
            serverTask.get().run();
        }
    }

    public static <T> T pick(Supplier<Supplier<T>> clientValue, Supplier<Supplier<T>> serverValue) {
        return isClient() ? clientValue.get().get() : serverValue.get().get();
    }

    public static void onSide(Level level, Runnable clientTask, Runnable serverTask) {
        if (level.isClientSide()) {
            clientTask.run();
        } else {
            serverTask.run();
        }
    }

    public static void onClientLevel(Level level, Runnable task) {
        if (level.isClientSide()) {
            task.run();
        }
    }

    public static void onServerLevel(Level level, Runnable task) {
        if (!level.isClientSide()) {
            task.run();
        }
    }
}
