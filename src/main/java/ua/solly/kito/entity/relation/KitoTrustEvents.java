package ua.solly.kito.entity.relation;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import ua.solly.kito.KitoMod;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.entity.relation.MoodEvent;

import java.util.List;

@EventBusSubscriber(modid = KitoMod.MODID)
public final class KitoTrustEvents {
    private KitoTrustEvents() {
    }

    private static List<KitoCharacter> nearby(Level level, BlockPos center, double radius) {
        return level.getEntitiesOfClass(KitoCharacter.class, new AABB(center).inflate(radius));
    }

    private static void notifyNearby(Level level, BlockPos center, double radius, Player player, TrustAction action) {
        for (KitoCharacter kito : nearby(level, center, radius)) {
            kito.react(player, action);
        }
    }

    private static boolean isWorkstation(BlockState state) {
        return state.is(Blocks.CRAFTING_TABLE)
                || state.is(Blocks.FURNACE)
                || state.is(Blocks.BLAST_FURNACE)
                || state.is(Blocks.ANVIL)
                || state.is(Blocks.SMITHING_TABLE)
                || state.is(Blocks.GRINDSTONE)
                || state.is(Blocks.STONECUTTER)
                || state.is(Blocks.CHEST)
                || state.is(Blocks.BARREL);
    }

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        for (KitoCharacter kito : nearby(level, victim.blockPosition(), 16.0D)) {
            if (kito == victim) {
                continue;
            }
            double distance = kito.distanceTo(victim);
            if (victim instanceof KitoCharacter) {
                kito.react(player, TrustAction.KILL_OTHER_KITO);
                kito.moodEvent(MoodEvent.WITNESSED_DEATH);
            } else if (victim instanceof Mob mob && mob.getTarget() == kito) {
                kito.react(player, TrustAction.DEFEND_KILL);
                kito.moodEvent(MoodEvent.DEFENDED);
            } else if (victim instanceof Enemy && distance <= 10.0D) {
                kito.react(player, TrustAction.CLEAR_HOSTILES);
                kito.moodEvent(MoodEvent.KILLED_HOSTILE);
            } else if (victim instanceof Animal && distance <= 8.0D) {
                kito.react(player, TrustAction.KILL_PASSIVE_NEAR);
                kito.moodEvent(MoodEvent.WITNESSED_DEATH);
            }
        }
    }

    @SubscribeEvent
    public static void onDamaged(LivingDamageEvent.Post event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof Player player)) {
            return;
        }
        if (!(victim instanceof Mob mob)) {
            return;
        }
        for (KitoCharacter kito : nearby(level, victim.blockPosition(), 16.0D)) {
            if (kito != victim && mob.getTarget() == kito) {
                kito.react(player, TrustAction.DEFEND_HIT);
            }
        }
    }

    @SubscribeEvent
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        BlockState state = event.getPlacedBlock();
        BlockPos pos = event.getPos();
        if (state.is(Blocks.TNT)) {
            notifyNearby(level, pos, 8.0D, player, TrustAction.TNT_NEAR);
        } else if (state.is(Blocks.COBWEB)) {
            notifyNearby(level, pos, 3.0D, player, TrustAction.TRAP_WEB);
        } else if (state.is(BlockTags.BEDS)) {
            notifyNearby(level, pos, 8.0D, player, TrustAction.PLACE_BED);
        } else if (state.is(BlockTags.FLOWERS)) {
            notifyNearby(level, pos, 6.0D, player, TrustAction.PLANT_FLOWER);
        } else if (isWorkstation(state)) {
            notifyNearby(level, pos, 8.0D, player, TrustAction.PLACE_WORKSTATION);
        } else if (state.getLightEmission() >= 10) {
            for (KitoCharacter kito : nearby(level, pos, 6.0D)) {
                if (level.getMaxLocalRawBrightness(kito.blockPosition()) < 8) {
                    kito.react(player, TrustAction.LIGHT_UP);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level) || level.isClientSide()) {
            return;
        }
        if (event.getState().is(BlockTags.BEDS)) {
            notifyNearby(level, event.getPos(), 8.0D, event.getPlayer(), TrustAction.BREAK_BED_NEAR);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        BlockPos pos = event.getPos();
        if (stack.is(Items.LAVA_BUCKET)) {
            notifyNearby(level, pos, 5.0D, player, TrustAction.LAVA_NEAR);
        } else if (stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            notifyNearby(level, pos, 4.0D, player, TrustAction.FIRE_NEAR);
        }
    }
}
