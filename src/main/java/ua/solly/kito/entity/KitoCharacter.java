package ua.solly.kito.entity;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;
import ua.solly.kito.entity.goal.KitoFleeGoal;
import ua.solly.kito.entity.goal.KitoFollowFriendGoal;
import ua.solly.kito.entity.goal.KitoPoseGoal;
import ua.solly.kito.entity.goal.KitoProtectFriendGoal;
import ua.solly.kito.entity.goal.KitoRetaliateGoal;
import ua.solly.kito.entity.goal.KitoStayNearHomeGoal;
import ua.solly.kito.entity.games.KitoGames;
import ua.solly.kito.entity.interact.kiss.KitoKiss;
import ua.solly.kito.entity.interact.sex.KitoPose;
import ua.solly.kito.entity.interact.sex.KitoPoseManager;
import ua.solly.kito.entity.interact.sex.KitoSexScene;
import ua.solly.kito.entity.interact.sex.SexAction;
import ua.solly.kito.entity.interact.tactility.EntityMover;
import ua.solly.kito.entity.interact.tactility.TactilityLib;
import ua.solly.kito.entity.interact.tactility.TactilityScene;
import ua.solly.kito.entity.inventory.KitoInventory;
import ua.solly.kito.entity.relation.Disposition;
import ua.solly.kito.entity.relation.Hormone;
import ua.solly.kito.entity.relation.KitoHome;
import ua.solly.kito.entity.relation.KitoHormones;
import ua.solly.kito.entity.relation.KitoJealousy;
import ua.solly.kito.entity.relation.KitoMood;
import ua.solly.kito.entity.relation.KitoRelation;
import ua.solly.kito.entity.relation.KitoTrust;
import ua.solly.kito.entity.relation.MoodEvent;
import ua.solly.kito.entity.relation.Taste;
import ua.solly.kito.entity.relation.TrustAction;
import ua.solly.kito.kaf.GeckoLibAnimations;
import ua.solly.kito.kaf.KitoAnimations;
import ua.solly.kito.kaf.KitoAsm;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public abstract class KitoCharacter extends PathfinderMob implements GeoEntity {
    public static final float FOLLOW_TRUST = KitoTrust.FOLLOW;
    public static final float HEAD_YAW_LIMIT = 50.0F;
    public static final float HEAD_PITCH_LIMIT = 30.0F;
    private static final int MENU_HOLD_TICKS = 30;
    private static final KitoRelation EMPTY_RELATION = new KitoRelation();
    private static final int COMPANY_SECONDS = 150;
    private static final double COMPANY_RADIUS = 5.0D;
    private static final double HORMONE_FOCUS_RADIUS = 16.0D;
    private static final int FIGHT_MEMORY_TICKS = 200;
    private static final double TRUST_SYNC_RADIUS_SQR = 16.0D * 16.0D;
    private static final int WEAPON_PUT_AWAY_TICKS = 30;
    private static final int FOLLOW_LOST_SECONDS = 30;
    private static final float MISS_TRUST_THRESHOLD = 50.0F;
    private static final double MISS_RADIUS = 16.0D;
    private static final double SAFE_RADIUS = 8.0D;
    private static final double RUN_FOLLOW_DISTANCE_SQR = 10.0D * 10.0D;
    private static final float RUN_HP_THRESHOLD = 0.25F;
    private static final double RUN_SPEED_MODIFIER = 1.5D;
    private static final double FOLLOW_LOST_DISTANCE_SQR = 50.0D * 50.0D;
    private static final EntityDataAccessor<Integer> TOUCH_PART = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Vector3f> TOUCH_POSE = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Boolean> IN_SCENE = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<String> SEX_ANIM = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> SEX_STATE = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KISSING = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Optional<UUID>> KISS_PLAYER = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.OPTIONAL_UUID);
    private static final EntityDataAccessor<Optional<UUID>> FOLLOW = SynchedEntityData.defineId(KitoCharacter.class, EntityDataSerializers.OPTIONAL_UUID);

    private final KitoHome home = new KitoHome();
    private final KitoPose sexPose = new KitoPose();
    private final Map<UUID, KitoRelation> relations = new HashMap<>();
    private final Map<UUID, Integer> companyTimers = new HashMap<>();
    private final Map<UUID, TactilityLib.Memory> touchMemory = new HashMap<>();
    private final EntityMover.Pose pose = new EntityMover.Pose();
    private final KitoHormones hormones;
    private final KitoMood mood = new KitoMood();
    private final KitoInventory inventory = new KitoInventory(this);
    private final float[] headTrack = new float[]{0.0F, 0.0F, -1.0F};
    private UUID tactileWith;
    private UUID menuUser;
    private long menuUntil;
    private ItemStack heldWeapon = ItemStack.EMPTY;
    private int calmTicks;
    private int followLost;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected KitoCharacter(EntityType<? extends KitoCharacter> type, Level level) {
        super(type, level);
        this.lookControl = new KitoLookControl(this);
        this.hormones = gender().hasHormones() ? new KitoHormones(getRandom()) : null;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public abstract Gender gender();

    public abstract Taste taste();

    public abstract KitoAsm asm();

    public KitoHome home() {
        return home;
    }

    public KitoPose sexPose() {
        return sexPose;
    }

    public EntityMover.Pose pose() {
        return pose;
    }

    public float[] headTrack() {
        return headTrack;
    }

    public KitoMood mood() {
        return mood;
    }

    public UUID tactileWith() {
        return tactileWith;
    }

    public void setTactileWith(UUID player) {
        this.tactileWith = player;
        entityData.set(IN_SCENE, player != null);
    }

    public boolean isInScene() {
        return entityData.get(IN_SCENE);
    }

    public boolean isTactileScene() {
        return isInScene() && entityData.get(SEX_ANIM).isEmpty();
    }

    public UUID followUuid() {
        return entityData.get(FOLLOW).orElse(null);
    }

    public boolean isFollowedBy(Player player) {
        UUID id = followUuid();
        return id != null && id.equals(player.getUUID());
    }

    public boolean hasAnyFollower() {
        return followUuid() != null;
    }

    public boolean isHoldingPlayer(Player player) {
        UUID id = holdUuid();
        return id != null && id.equals(player.getUUID());
    }

    public Player followPlayer() {
        UUID id = followUuid();
        return id == null ? null : level().getPlayerByUUID(id);
    }

    public void stopFollowing() {
        entityData.set(FOLLOW, Optional.empty());
        getNavigation().stop();
    }

    public boolean isRunning() {
        return entityData.get(RUNNING);
    }

    public void setGameRunning(boolean running) {
        entityData.set(RUNNING, running);
        getNavigation().setSpeedModifier(running ? 1.8D : 1.0D);
    }

    public boolean isKissing() {
        return entityData.get(KISSING);
    }

    public boolean isKissingFor(UUID player) {
        return isKissing() && entityData.get(KISS_PLAYER).filter(player::equals).isPresent();
    }

    public void setKissing(UUID player, boolean kissing) {
        entityData.set(KISS_PLAYER, kissing && player != null ? Optional.of(player) : Optional.empty());
        entityData.set(KISSING, kissing);
    }

    public boolean isThrusting() {
        return entityData.get(SEX_STATE) == 2;
    }

    public String sexAnimId() {
        return entityData.get(SEX_ANIM);
    }

    public int sexState() {
        return entityData.get(SEX_STATE);
    }

    public void resetSexAnimationState() {
        entityData.set(SEX_ANIM, "");
        entityData.set(SEX_STATE, 0);
    }

    private void syncSexAnim() {
        String id = "";
        int state = 0;
        if (sexPose.isActive() && sexPose.action() != null) {
            id = sexPose.action().id();
            if (KitoSexScene.isThrusting(this)) {
                state = 2;
            } else if (sexPose.isEngaged()) {
                state = 1;
            }
        }
        entityData.set(SEX_ANIM, id);
        entityData.set(SEX_STATE, state);
    }

    public KitoInventory inventory() {
        return inventory;
    }

    public void openInventory(ServerPlayer player) {
        player.openMenu(new SimpleMenuProvider(
                (id, playerInventory, p) -> new ChestMenu(MenuType.GENERIC_9x4, id, playerInventory, inventory, KitoInventory.ROWS),
                getDisplayName()
        ));
    }

    public void commandFollow(ServerPlayer player) {
        if (isFollowedBy(player)) {
            stopFollowing();
            TactilityLib.say(this, player, "kito.menu.follow_stop");
            return;
        }
        UUID current = followUuid();
        if (current != null && !current.equals(player.getUUID())) {
            TactilityLib.say(this, player, "kito.menu.deny.follow_taken");
            moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        KitoRelation relation = peekRelation(player);
        if (relation.trust() < FOLLOW_TRUST) {
            TactilityLib.say(this, player, "kito.menu.deny.follow");
            moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        if (relation.isHurt()) {
            TactilityLib.say(this, player, "kito.menu.deny.follow_hurt");
            moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        if (dispositionTo(player) == Disposition.AGGRESSIVE) {
            TactilityLib.say(this, player, "tactility.kito.deny.angry");
            moodEvent(MoodEvent.DENIED_ACTION);
            return;
        }
        entityData.set(FOLLOW, Optional.of(player.getUUID()));
        followLost = 0;
        TactilityLib.say(this, player, "kito.menu.follow_ok");
    }

    public void moodEvent(MoodEvent event) {
        if (level().isClientSide()) {
            return;
        }
        if (mood.fire(event, level().getGameTime()) && hormones != null) {
            hormones.onMood(event);
        }
    }

    public void moodEvent(MoodEvent event, float multiplier) {
        if (level().isClientSide()) {
            return;
        }
        if (mood.fire(event, level().getGameTime(), multiplier) && hormones != null) {
            hormones.onMood(event);
        }
    }

    public void setMenuUser(Player player, boolean open) {
        if (open) {
            menuUser = player.getUUID();
            menuUntil = level().getGameTime() + MENU_HOLD_TICKS;
        } else if (player.getUUID().equals(menuUser)) {
            menuUser = null;
        }
    }

    public boolean isHeld() {
        return tactileWith != null || isMenuHeld();
    }

    public UUID holdUuid() {
        return tactileWith != null ? tactileWith : menuUser;
    }

    public TactilityLib.Memory touchMemory(Player player) {
        return touchMemory.computeIfAbsent(player.getUUID(), id -> new TactilityLib.Memory());
    }

    public int touchPart() {
        return entityData.get(TOUCH_PART);
    }

    public Vector3f touchPose() {
        return entityData.get(TOUCH_POSE);
    }

    public void setTouch(int part, Vector3f value) {
        if (entityData.get(TOUCH_PART) != part) {
            entityData.set(TOUCH_PART, part);
        }
        if (entityData.get(TOUCH_POSE).distanceSquared(value) > 0.0004F) {
            entityData.set(TOUCH_POSE, new Vector3f(value));
        }
    }

    public KitoRelation relation(Player player) {
        return relations.computeIfAbsent(player.getUUID(), id -> new KitoRelation());
    }

    public KitoRelation peekRelation(Player player) {
        KitoRelation relation = relations.get(player.getUUID());
        return relation == null ? EMPTY_RELATION : relation;
    }

    public boolean hasHormones() {
        return hormones != null;
    }

    public KitoHormones hormones() {
        return hormones;
    }

    public float arousalFactor() {
        return hormones == null ? 1.0F : hormones.arousalGainFactor();
    }

    public void onIntimateTouch(float arousalGain) {
        if (hormones != null && !level().isClientSide()) {
            hormones.onIntimateTouch(arousalGain);
        }
    }

    public float hormone() {
        return hormones == null ? KitoHormones.NEUTRAL : hormones.level();
    }

    public Disposition dispositionTo(Player player) {
        return Disposition.of(peekRelation(player), hormones == null ? 0.0F : hormones.centered());
    }

    public boolean shouldRetaliateAgainst(Player player) {
        return !(dispositionTo(player) == Disposition.FRIENDLY && peekRelation(player).harmScore() < 2);
    }

    public boolean shouldKeepFighting(Player player) {
        if (dispositionTo(player) == Disposition.AGGRESSIVE) {
            return true;
        }
        return getLastHurtByMob() == player && tickCount - getLastHurtByMobTimestamp() < FIGHT_MEMORY_TICKS;
    }

    @Override
    public void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TOUCH_PART, -1);
        builder.define(TOUCH_POSE, new Vector3f());
        builder.define(IN_SCENE, false);
        builder.define(RUNNING, false);
        builder.define(KISSING, false);
        builder.define(KISS_PLAYER, Optional.empty());
        builder.define(SEX_ANIM, "");
        builder.define(SEX_STATE, 0);
        builder.define(FOLLOW, Optional.empty());
    }

    @Override
    public int getMaxHeadYRot() {
        return (int) HEAD_YAW_LIMIT;
    }

    @Override
    public boolean isPushable() {
        return !isHeld() && super.isPushable();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            pose.tick(touchPart(), touchPose());
        }
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new KitoPoseGoal(this));
        goalSelector.addGoal(2, new KitoFleeGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true));
        goalSelector.addGoal(4, new KitoProtectFriendGoal(this));
        goalSelector.addGoal(5, new KitoFollowFriendGoal(this, 1.0D, 5.0F, 3.0F));
        goalSelector.addGoal(6, new KitoStayNearHomeGoal(this));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8D));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new KitoRetaliateGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                10,
                true,
                false,
                target -> target instanceof Player player && dispositionTo(player) == Disposition.AGGRESSIVE
        ));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        syncSexAnim();
        updateArmor();
        KitoKiss.tick(this);
        if (KitoKiss.isActive(this)) {
            return;
        }
        if (KitoGames.isPlaying(this)) {
            return;
        }
        if (sexPose.isActive()) {
            KitoPoseManager.tick(this);
            return;
        }
        if (isHeld()) {
            EntityMover.hold(this);
        }
        updateRunningState();
        updateWeapon();
        if (tickCount % 10 == 0) {
            syncTrust();
        }
        if (tickCount % 20 == 0) {
            serverSecondTick();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        KitoAsm asm = asm();
        RawAnimation idle = asm.loop(KitoAnimations.IDLE);
        RawAnimation walk = asm.loop(KitoAnimations.WALK);
        RawAnimation run = asm.loop(KitoAnimations.RUN);
        RawAnimation kiss = asm.loop("kiss");
        Map<SexAction, RawAnimation> sexPoses = new EnumMap<>(SexAction.class);
        for (SexAction action : SexAction.values()) {
            sexPoses.put(action, RawAnimation.begin().thenLoop(action.poseAnim()));
        }

        AnimationController<KitoCharacter> main = GeckoLibAnimations.controller(
                this,
                GeckoLibAnimations.MOVEMENT_CONTROLLER,
                GeckoLibAnimations.DEFAULT_TRANSITION,
                state -> {
                    if (entityData.get(KISSING)) {
                        return state.setAndContinue(kiss);
                    }
                    SexAction sexAction = SexAction.byId(entityData.get(SEX_ANIM));
                    int sexState = entityData.get(SEX_STATE);
                    if (sexAction != null && sexState > 0) {
                        return state.setAndContinue(sexPoses.get(sexAction));
                    }
                    if (!state.isMoving()) {
                        return state.setAndContinue(idle);
                    }
                    return state.setAndContinue(isRunning() ? run : walk);
                }
        );

        for (String base : swingBases()) {
            for (String suffix : new String[]{"", "Walk", "Run"}) {
                String name = base + suffix;
                main.triggerableAnim(name, asm.play(name));
            }
        }
        main.triggerableAnim("kiss", kiss);
        AnimationController<KitoCharacter> bwjbAction = GeckoLibAnimations.triggerable(
                this,
                GeckoLibAnimations.ACTION_CONTROLLER,
                2,
                Map.of("bwjb_thrust", RawAnimation.begin().thenPlay(SexAction.BWJB.loopAnim()))
        );

        GeckoLibAnimations.register(registrar, main, bwjbAction);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        boolean hit = super.doHurtTarget(target);
        if (!level().isClientSide()) {
            triggerSwing();
            if (hit && !heldWeapon.isEmpty() && heldWeapon.isDamageableItem()) {
                heldWeapon.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
            }
        }
        return hit;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !level().isClientSide()) {
            menuUser = null;
            KitoKiss.stop(this);
            if (sexPose.isActive()) {
                KitoPoseManager.stop(this);
            }
            if (tactileWith != null) {
                TactilityScene.abort(this, TactilityScene.End.HURT);
            }
            moodEvent(MoodEvent.TOOK_DAMAGE);
            onHurtBy(source);
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!level().isClientSide()) {
            KitoKiss.stop(this);
            unequipWeapon();
            unequipArmor();
        }
        super.die(source);
        if (!level().isClientSide()) {
            inventory.dropAll(level(), blockPosition());
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean client = level().isClientSide();

        if (player.isCreative() && stack.is(Items.STICK)) {
            if (!client) {
                debugInfo(player);
            }
            return InteractionResult.sidedSuccess(client);
        }

        return super.mobInteract(player, hand);
    }

    public void giveHeldItem(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        TrustAction action;
        if (stack.isEmpty()) {
            action = TrustAction.PET;
        } else {
            action = TrustAction.forInteraction(stack, player, this);
            if (action == null) {
                action = taste().classifyGift(stack);
            }
        }
        if (action == null) {
            TactilityLib.say(this, player, "kito.menu.gift.refuse");
            return;
        }
        if (react(player, action)) {
            applyInteractionEffect(player, stack, action);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        ListTag list = new ListTag();
        relations.forEach((id, relation) -> {
            CompoundTag entry = relation.save();
            entry.putUUID("player", id);
            list.add(entry);
        });
        tag.put("KitoRelations", list);
        tag.putString("KitoGender", gender().id());
        tag.put("KitoHome", home.save());
        if (hormones != null) {
            tag.put("KitoHormones", hormones.save());
        }
        tag.put("KitoMood", mood.save());
        tag.put("KitoInventory", inventory.write(level().registryAccess()));
        UUID follow = followUuid();
        if (follow != null) {
            tag.putUUID("KitoFollow", follow);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        relations.clear();
        ListTag list = tag.getList("KitoRelations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            relations.put(entry.getUUID("player"), KitoRelation.load(entry));
        }
        if (hormones != null && tag.contains("KitoHormones")) {
            hormones.load(tag.getCompound("KitoHormones"));
        }
        if (tag.contains("KitoMood")) {
            mood.load(tag.getCompound("KitoMood"));
        }
        if (tag.contains("KitoHome")) {
            home.load(tag.getCompound("KitoHome"));
        }
        inventory.read(tag.getList("KitoInventory", Tag.TAG_COMPOUND), level().registryAccess());
        heldWeapon = ItemStack.EMPTY;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        unequipArmor();
        entityData.set(FOLLOW, tag.hasUUID("KitoFollow") ? Optional.of(tag.getUUID("KitoFollow")) : Optional.empty());
    }

    public boolean react(Player player, TrustAction action) {
        if (level().isClientSide() || !action.allowedFor(gender())) {
            return false;
        }
        float weight = taste().weight(action);
        if (hormones != null && action.isGood()) {
            weight *= hormones.trustGainFactor();
        }
        boolean applied = relation(player).apply(action, level().getGameTime(), weight);
        if (applied && player instanceof ServerPlayer serverPlayer) {
            TactilityLib.sendTrust(serverPlayer, this);
        }
        if (applied && hormones != null) {
            hormones.onAction(action);
        }
        if (applied) {
            if (action.isGift()) {
                moodEvent(MoodEvent.GIFT_RECEIVED);
            } else if (action == TrustAction.PET) {
                moodEvent(MoodEvent.PET);
            } else if (action == TrustAction.COMPANY) {
                moodEvent(MoodEvent.COMPANY);
            } else if (action == TrustAction.EXTINGUISH) {
                moodEvent(MoodEvent.EXTINGUISHED);
            } else if (action.isHarm()) {
                moodEvent(MoodEvent.TOOK_DAMAGE);
            }
            KitoJealousy.notifyAttention(this, player);
        }
        if (applied && level() instanceof ServerLevel server) {
            if (action.isGood()) {
                ua.solly.kito.particle.KitoHearts.burst(this, 4, 0.8D);
            } else {
                server.sendParticles(
                        ParticleTypes.ANGRY_VILLAGER,
                        getX(), getEyeY() + 0.3D, getZ(),
                        3, 0.3D, 0.2D, 0.3D, 0.0D
                );
            }
        }
        return applied;
    }

    protected String[] swingBases() {
        return new String[]{"lmb", "rmb"};
    }

    public static AttributeSupplier.Builder baseAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.FOLLOW_RANGE, 24.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    private void syncTrust() {
        if (!(level() instanceof ServerLevel server)) {
            return;
        }
        for (ServerPlayer player : server.players()) {
            if (player.distanceToSqr(this) <= TRUST_SYNC_RADIUS_SQR) {
                TactilityLib.sendTrust(player, this);
            }
        }
    }

    private void serverSecondTick() {
        long now = level().getGameTime();
        for (Map.Entry<UUID, KitoRelation> entry : relations.entrySet()) {
            KitoRelation relation = entry.getValue();
            relation.tick(now);
            TactilityLib.tickArousal(relation, touchMemory.computeIfAbsent(entry.getKey(), id -> new TactilityLib.Memory()), now);
        }

        Player focus = level().getNearestPlayer(this, HORMONE_FOCUS_RADIUS);
        KitoRelation focusRelation = focus == null ? null : peekRelation(focus);
        mood.tick(getRandom(), focusRelation, hormones == null ? 0.0F : hormones.moodShift());
        tickMoodEvents(focus, focusRelation, now);

        if (hormones != null) {
            hormones.tick(getRandom(), focusRelation, mood, level().getDayTime(), level().getGameTime());
        }

        tickCompany();
        tickFollow();

        if (getLeashHolder() instanceof Player holder) {
            react(holder, TrustAction.LEASH);
            moodEvent(MoodEvent.LEASHED);
        }

        if (getTarget() instanceof Player target && !shouldKeepFighting(target)) {
            setTarget(null);
        }
    }

    private void tickMoodEvents(Player focus, KitoRelation focusRelation, long now) {
        float hpPct = getHealth() / getMaxHealth();
        if (hpPct < 0.25F) {
            moodEvent(MoodEvent.LOW_HEALTH);
        }
        if (focus != null && focusRelation != null) {
            if (focusRelation.trust() >= MISS_TRUST_THRESHOLD && distanceToSqr(focus) > MISS_RADIUS * MISS_RADIUS) {
                moodEvent(MoodEvent.PLAYER_ABSENT);
            }
            if (isFollowedBy(focus) && focusRelation.trust() >= MISS_TRUST_THRESHOLD) {
                moodEvent(MoodEvent.FOLLOWING_FRIEND);
            }
        } else {
            moodEvent(MoodEvent.BORED);
        }
        if (getTarget() == null && hpPct > 0.5F
                && level().getEntitiesOfClass(Mob.class, getBoundingBox().inflate(SAFE_RADIUS),
                mob -> mob instanceof net.minecraft.world.entity.monster.Enemy).isEmpty()) {
            moodEvent(MoodEvent.SAFE);
        }
        long dayTime = level().getDayTime() % 24000L;
        if (dayTime < 1000L) {
            moodEvent(MoodEvent.MORNING);
        } else if (dayTime > 13000L && dayTime < 23000L) {
            moodEvent(MoodEvent.NIGHT);
        }
        if (level().isRaining() && level().canSeeSky(blockPosition())) {
            if (level().isThundering()) {
                moodEvent(MoodEvent.THUNDER);
            } else {
                moodEvent(MoodEvent.RAIN);
            }
        } else if (level().getBiome(blockPosition()).value().getBaseTemperature() < 0.15F
                && level().canSeeSky(blockPosition())) {
            moodEvent(MoodEvent.SNOW);
        } else if (level().isDay() && level().canSeeSky(blockPosition())) {
            moodEvent(MoodEvent.SUNNY);
        }
    }

    private void tickFollow() {
        UUID id = followUuid();
        if (id == null) {
            followLost = 0;
            return;
        }
        Player follower = level().getPlayerByUUID(id);
        if (follower == null) {
            if (++followLost >= FOLLOW_LOST_SECONDS) {
                stopFollowing();
                followLost = 0;
            }
            return;
        }
        followLost = 0;
        if (!follower.isAlive() || dispositionTo(follower) == Disposition.AGGRESSIVE) {
            stopFollowing();
            return;
        }
        if (distanceToSqr(follower) > FOLLOW_LOST_DISTANCE_SQR) {
            stopFollowing();
        }
    }

    private void tickCompany() {
        Set<UUID> present = new HashSet<>();
        for (Player player : level().getEntitiesOfClass(Player.class, getBoundingBox().inflate(COMPANY_RADIUS))) {
            if (player.isSpectator() || !player.isAlive()) {
                continue;
            }
            UUID id = player.getUUID();
            present.add(id);
            int seconds = companyTimers.merge(id, 1, Integer::sum);
            if (seconds >= COMPANY_SECONDS) {
                companyTimers.put(id, 0);
                if (peekRelation(player).harmScore() == 0) {
                    react(player, TrustAction.COMPANY);
                }
            }
        }
        companyTimers.keySet().retainAll(present);
    }

    private boolean isAimedAt(Player player) {
        if (player.distanceToSqr(this) < 2.25D) {
            return true;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(6.0D));
        return getBoundingBox().inflate(0.3D).clip(eye, end).isPresent();
    }

    private void onHurtBy(DamageSource source) {
        if (!(source.getEntity() instanceof Player player)) {
            return;
        }
        if (source.getDirectEntity() == player && !isAimedAt(player)) {
            return;
        }
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            react(player, TrustAction.EXPLOSION);
        } else if (source.getDirectEntity() instanceof Projectile) {
            react(player, TrustAction.SHOOT);
        } else {
            react(player, TrustAction.HIT);
        }
        if (getHealth() <= getMaxHealth() * 0.25F) {
            react(player, TrustAction.HIT_CRITICAL);
        }
    }

    private void applyInteractionEffect(Player player, ItemStack stack, TrustAction action) {
        switch (action) {
            case GIFT_FLOWER, GIFT_JEWEL, GIFT_DECOR, GIFT_MUSIC_DISC,
                 GIFT_TOOL, GIFT_GEAR, GIFT_MATERIAL, GIFT_SUPPLY, GIFT_ROTTEN -> consume(player, stack);
            case GIFT_SWEET -> {
                heal(6.0F);
                consume(player, stack);
            }
            case GIFT_MEAT -> {
                heal(5.0F);
                consume(player, stack);
            }
            case EXTINGUISH -> {
                clearFire();
                moodEvent(MoodEvent.EXTINGUISHED);
            }
            case IGNITE -> {
                igniteForSeconds(4.0F);
                if (stack.is(Items.FIRE_CHARGE)) {
                    consume(player, stack);
                }
            }
            default -> {
            }
        }
    }

    private static void consume(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private void debugInfo(Player player) {
        KitoRelation relation = peekRelation(player);
        StringBuilder hormone = new StringBuilder();
        if (hormones == null) {
            hormone.append("off");
        } else {
            for (Hormone entry : Hormone.values()) {
                hormone.append(entry.id(), 0, 3).append('=').append(String.format(Locale.ROOT, "%.0f", hormones.get(entry))).append(' ');
            }
        }
        player.displayClientMessage(Component.literal(String.format(
                Locale.ROOT,
                "%s | trust=%.1f disgust=%.1f arousal=%.1f mood=%.1f harm=%d | hormone=%s | running=%s | %s",
                gender().id(),
                relation.trust(),
                relation.disgust(),
                relation.arousal(),
                mood.level(),
                relation.harmScore(),
                hormone.toString().trim(),
                isRunning(),
                dispositionTo(player)
        )), false);
    }

    private void triggerSwing() {
        String[] bases = swingBases();
        String base = bases[getRandom().nextInt(bases.length)];
        boolean moving = getDeltaMovement().horizontalDistanceSqr() > 0.0025D;
        String suffix = moving ? (isRunning() ? "Run" : "Walk") : "";
        GeckoLibAnimations.trigger(this, GeckoLibAnimations.MOVEMENT_CONTROLLER, base + suffix);
    }

    private void updateWeapon() {
        if (!heldWeapon.isEmpty() && !inventory.holds(heldWeapon)) {
            unequipWeapon();
        }
        LivingEntity target = getTarget();
        boolean fighting = target != null && target.isAlive();
        if (fighting) {
            calmTicks = 0;
            if (heldWeapon.isEmpty() && tickCount % 10 == 0) {
                ItemStack best = inventory.bestWeapon();
                if (!best.isEmpty()) {
                    heldWeapon = best;
                    setItemSlot(EquipmentSlot.MAINHAND, best);
                }
            }
        } else if (!heldWeapon.isEmpty() && ++calmTicks >= WEAPON_PUT_AWAY_TICKS) {
            unequipWeapon();
        }
    }

    private void updateArmor() {
        if (tickCount % 10 != 0) {
            return;
        }
        if (sexPose.isActive()) {
            unequipArmor();
            return;
        }
        for (EquipmentSlot slot : KitoInventory.ARMOR_SLOTS) {
            ItemStack worn = getItemBySlot(slot);
            if (!worn.isEmpty() && !inventory.holds(worn)) {
                setItemSlot(slot, ItemStack.EMPTY);
                worn = ItemStack.EMPTY;
            }
            ItemStack best = inventory.bestArmor(slot);
            if (best != worn) {
                setItemSlot(slot, best);
            }
        }
    }

    private void unequipArmor() {
        for (EquipmentSlot slot : KitoInventory.ARMOR_SLOTS) {
            if (!getItemBySlot(slot).isEmpty()) {
                setItemSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void hurtArmor(DamageSource source, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        int damage = (int) Math.max(1.0F, amount / 4.0F);
        for (EquipmentSlot slot : KitoInventory.ARMOR_SLOTS) {
            ItemStack stack = getItemBySlot(slot);
            if (stack.isEmpty() || !(stack.getItem() instanceof ArmorItem)) {
                continue;
            }
            if (source.is(DamageTypeTags.IS_FIRE) && stack.has(DataComponents.FIRE_RESISTANT)) {
                continue;
            }
            stack.hurtAndBreak(damage, this, slot);
        }
    }

    private void unequipWeapon() {
        heldWeapon = ItemStack.EMPTY;
        calmTicks = 0;
        setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    private void updateRunningState() {
        boolean running = false;
        if (getHealth() <= getMaxHealth() * RUN_HP_THRESHOLD && getLastHurtByMob() != null) {
            running = true;
        } else {
            Player follower = followPlayer();
            if (follower != null && follower.isAlive()
                    && distanceToSqr(follower) > RUN_FOLLOW_DISTANCE_SQR) {
                running = true;
            }
        }
        if (entityData.get(RUNNING) != running) {
            entityData.set(RUNNING, running);
        }
        getNavigation().setSpeedModifier(running ? RUN_SPEED_MODIFIER : 1.0D);
    }

    private Player menuPlayer() {
        if (menuUser == null) {
            return null;
        }
        if (level().getGameTime() > menuUntil) {
            menuUser = null;
            return null;
        }
        Player player = level().getPlayerByUUID(menuUser);
        if (player == null || !player.isAlive() || player.isSpectator()) {
            menuUser = null;
            return null;
        }
        return player;
    }

    private boolean isMenuHeld() {
        if (tactileWith != null || getTarget() != null) {
            return false;
        }
        Player player = menuPlayer();
        return player != null && dispositionTo(player) != Disposition.AGGRESSIVE;
    }
}
