package ua.solly.kito.pp;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ua.solly.kito.KitoMod;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EventBusSubscriber(modid = KitoMod.MODID, value = Dist.CLIENT)
public final class PlayerPoser {
    private static final float HAND_LENGTH = 0.7F;
    private static final float MIN_STRETCH = 0.92F;
    private static final float MAX_STRETCH = 1.12F;
    private static final float WEIGHT_RATE = 0.28F;
    private static final float TARGET_RATE = 0.5F;
    private static final float HIP = 12.0F;
    private static final float FIRST_PERSON_THICKNESS = 1.0F;
    private static final float FIRST_PERSON_SIDE = 0.16F;
    private static final float FIRST_PERSON_DROP = -0.14F;

    private static final List<PoseProvider> PROVIDERS = new ArrayList<>();
    private static final Map<Integer, State> STATES = new HashMap<>();
    private static Field modelField;
    private static Vec3 shiftRight = Vec3.ZERO;
    private static Vec3 shiftLeft = Vec3.ZERO;

    static {
        PROVIDERS.add(new TouchPoser());
        PROVIDERS.add(new SexPoser());
        PROVIDERS.add(new KissPoser());
    }

    private PlayerPoser() {
    }

    private static final class State {
        private final PlayerPose pose = new PlayerPose();
        private final float[] weight = new float[2];
        private final double[][] target = new double[2][3];
        private float lastNow = -1.0F;
        private boolean active;
    }

    public static void register(PoseProvider provider) {
        PROVIDERS.add(provider);
    }

    private static State frame(AbstractClientPlayer player, float partial) {
        if (STATES.size() > 32) {
            STATES.clear();
        }
        State state = STATES.computeIfAbsent(player.getId(), id -> new State());
        float now = player.tickCount + partial;
        if (Math.abs(now - state.lastNow) < 1.0E-4F) {
            return state;
        }
        float dt = state.lastNow < 0.0F || now < state.lastNow ? 1.0F : Mth.clamp(now - state.lastNow, 0.0F, 4.0F);
        state.lastNow = now;
        state.pose.reset();
        for (PoseProvider provider : PROVIDERS) {
            provider.apply(player, partial, state.pose);
        }
        smooth(state, 0, state.pose.right, dt);
        smooth(state, 1, state.pose.left, dt);
        state.active = state.pose.active || state.weight[0] > 0.01F || state.weight[1] > 0.01F;
        return state;
    }

    private static void smooth(State state, int slot, PlayerPose.Aim aim, float dt) {
        float wanted = aim.on ? 1.0F : 0.0F;
        float w = state.weight[slot];
        if (aim.on && w < 0.01F) {
            state.target[slot][0] = aim.x;
            state.target[slot][1] = aim.y;
            state.target[slot][2] = aim.z;
        }
        float kw = 1.0F - (float) Math.pow(1.0F - WEIGHT_RATE, dt);
        state.weight[slot] = w + (wanted - w) * kw;
        if (state.weight[slot] < 0.005F && !aim.on) {
            state.weight[slot] = 0.0F;
        }
        if (aim.on) {
            float kt = 1.0F - (float) Math.pow(1.0F - TARGET_RATE, dt);
            state.target[slot][0] += (aim.x - state.target[slot][0]) * kt;
            state.target[slot][1] += (aim.y - state.target[slot][1]) * kt;
            state.target[slot][2] += (aim.z - state.target[slot][2]) * kt;
        }
    }

    private static void leanPart(ModelPart part, float theta) {
        float c = Mth.cos(theta);
        float s = Mth.sin(theta);
        float y = part.y - HIP;
        float z = part.z;
        part.y = y * c - z * s + HIP;
        part.z = y * s + z * c;
        part.xRot += theta;
    }

    public static void applyToModel(PosedPlayerModel m, AbstractClientPlayer player, float partial) {
        float p = Mth.clamp(partial, 0.0F, 1.0F);
        State state = frame(player, p);
        if (!state.active) {
            return;
        }
        PlayerPose pose = state.pose;

        if (pose.lean != 0.0F) {
            leanPart(m.body, pose.lean);
            leanPart(m.head, pose.lean);
            leanPart(m.rightArm, pose.lean);
            leanPart(m.leftArm, pose.lean);
        }
        m.head.xRot += pose.headPitch;
        m.head.yRot += pose.headYaw;
        if (pose.hipPush != 0.0F) {
            float push = pose.hipPush * 16.0F;
            m.head.z -= push;
            m.body.z -= push;
            m.rightArm.z -= push;
            m.leftArm.z -= push;
            m.rightLeg.z -= push;
            m.leftLeg.z -= push;
        }

        Vec3 feet = player.getPosition(p);
        float yaw = Mth.rotLerp(p, player.yBodyRotO, player.yBodyRot);
        aim(m.rightArm, state.weight[0], state.target[0], feet.add(shiftRight), yaw);
        aim(m.leftArm, state.weight[1], state.target[1], feet.add(shiftLeft), yaw);

        m.hat.copyFrom(m.head);
        m.jacket.copyFrom(m.body);
        m.leftPants.copyFrom(m.leftLeg);
        m.rightPants.copyFrom(m.rightLeg);
        m.leftSleeve.copyFrom(m.leftArm);
        m.rightSleeve.copyFrom(m.rightArm);
        m.leftSleeve.yScale = m.leftArm.yScale;
        m.rightSleeve.yScale = m.rightArm.yScale;
    }

    private static void aim(ModelPart arm, float weight, double[] target, Vec3 feet, float yaw) {
        if (weight <= 0.001F) {
            return;
        }
        double rad = Math.toRadians(180.0F - yaw);
        double ex = -arm.x / 16.0D;
        double ey = 1.501D - arm.y / 16.0D;
        double ez = arm.z / 16.0D;
        double px = feet.x + ex * Math.cos(rad) + ez * Math.sin(rad);
        double py = feet.y + ey;
        double pz = feet.z - ex * Math.sin(rad) + ez * Math.cos(rad);

        double dx = target[0] - px;
        double dy = target[1] - py;
        double dz = target[2] - pz;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist < 1.0E-4D) {
            return;
        }
        double ang = Math.toRadians(yaw);
        double x1 = dx * Math.cos(ang) + dz * Math.sin(ang);
        double z1 = -dx * Math.sin(ang) + dz * Math.cos(ang);
        double mx = x1 / dist;
        double my = -dy / dist;
        double mz = -z1 / dist;

        float a = Mth.clamp((float) Math.acos(Mth.clamp(my, -1.0D, 1.0D)), 0.08F, 2.35F);
        float c = Mth.clamp((float) Math.atan2(mx, mz), -1.3F, 1.3F);
        float s = Mth.clamp((float) dist / HAND_LENGTH, MIN_STRETCH, MAX_STRETCH);

        float k = Mth.clamp((float) Math.sin(a) * 6.0F, 0.0F, 1.0F);
        arm.xRot = Mth.lerp(weight, arm.xRot, a);
        arm.yRot = Mth.lerp(weight * k, arm.yRot, c);
        arm.zRot = Mth.lerp(weight, arm.zRot, 0.0F);
        arm.yScale = Mth.lerp(weight, 1.0F, s);
    }

    private static void ensureModel(PlayerRenderer renderer, AbstractClientPlayer player) {
        if (renderer.getModel() instanceof PosedPlayerModel) {
            return;
        }
        try {
            if (modelField == null) {
                for (Field field : LivingEntityRenderer.class.getDeclaredFields()) {
                    if (EntityModel.class.isAssignableFrom(field.getType()) && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                        field.setAccessible(true);
                        modelField = field;
                        break;
                    }
                }
            }
            if (modelField == null) {
                return;
            }
            boolean slim = player.getSkin().model() == PlayerSkin.Model.SLIM;
            ModelPart root = Minecraft.getInstance().getEntityModels().bakeLayer(slim ? ModelLayers.PLAYER_SLIM : ModelLayers.PLAYER);
            modelField.set(renderer, new PosedPlayerModel(root, slim));
        } catch (Exception | LinkageError ignored) {
        }
    }

    @SubscribeEvent
    public static void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (event.getEntity() instanceof AbstractClientPlayer player) {
            ensureModel(event.getRenderer(), player);
        }
    }

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        Minecraft mc = Minecraft.getInstance();
        AbstractClientPlayer player = mc.player;
        if (player == null) {
            return;
        }
        float partial = event.getPartialTick();
        State state = frame(player, partial);
        if (!state.active || (state.weight[0] <= 0.01F && state.weight[1] <= 0.01F)) {
            return;
        }
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer)) {
            return;
        }
        ensureModel(renderer, player);
        if (!(renderer.getModel() instanceof PosedPlayerModel model)) {
            return;
        }
        model.young = false;
        model.riding = false;
        model.crouching = false;
        model.attackTime = 0.0F;
        model.swimAmount = 0.0F;
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        Camera camera = mc.gameRenderer.getMainCamera();
        Quaternionf cameraRotation = new Quaternionf(camera.rotation());
        Vector3f shiftR = cameraRotation.transform(new Vector3f(FIRST_PERSON_SIDE, FIRST_PERSON_DROP, 0.0F));
        Vector3f shiftL = cameraRotation.transform(new Vector3f(-FIRST_PERSON_SIDE, FIRST_PERSON_DROP, 0.0F));
        shiftRight = new Vec3(shiftR.x, shiftR.y, shiftR.z);
        shiftLeft = new Vec3(shiftL.x, shiftL.y, shiftL.z);
        try {
            model.setupAnim(player, 0.0F, 0.0F, player.tickCount + partial, 0.0F, 0.0F);
        } finally {
            shiftRight = Vec3.ZERO;
            shiftLeft = Vec3.ZERO;
        }

        Vec3 cam = camera.getPosition();
        Quaternionf inverse = new Quaternionf(camera.rotation()).conjugate();
        Vec3 feet = player.getPosition(partial);
        float yaw = Mth.rotLerp(partial, player.yBodyRotO, player.yBodyRot);
        Vector3f rel = inverse.transform(new Vector3f((float) (feet.x - cam.x), (float) (feet.y - cam.y), (float) (feet.z - cam.z)));

        PoseStack stack = event.getPoseStack();
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityTranslucent(player.getSkin().texture()));
        int light = event.getPackedLight();
        if (state.weight[0] > 0.01F) {
            drawArm(stack, consumer, light, inverse, rel, yaw, model.rightArm, model.rightSleeve,
                    player.isModelPartShown(PlayerModelPart.RIGHT_SLEEVE), 1.0F);
        }
        if (state.weight[1] > 0.01F) {
            drawArm(stack, consumer, light, inverse, rel, yaw, model.leftArm, model.leftSleeve,
                    player.isModelPartShown(PlayerModelPart.LEFT_SLEEVE), -1.0F);
        }
    }

    private static void drawArm(PoseStack stack, VertexConsumer consumer, int light, Quaternionf inverse, Vector3f rel, float yaw,
                                ModelPart arm, ModelPart sleeve, boolean sleeveShown, float side) {
        float armXs = arm.xScale;
        float armZs = arm.zScale;
        float sleeveXs = sleeve.xScale;
        float sleeveZs = sleeve.zScale;
        arm.xScale = FIRST_PERSON_THICKNESS;
        arm.zScale = FIRST_PERSON_THICKNESS;
        sleeve.xScale = FIRST_PERSON_THICKNESS;
        sleeve.zScale = FIRST_PERSON_THICKNESS;
        stack.pushPose();
        stack.translate(side * FIRST_PERSON_SIDE, FIRST_PERSON_DROP, 0.0F);
        stack.translate(rel.x, rel.y, rel.z);
        stack.mulPose(inverse);
        stack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        stack.scale(-1.0F, -1.0F, 1.0F);
        stack.translate(0.0F, -1.501F, 0.0F);
        arm.render(stack, consumer, light, OverlayTexture.NO_OVERLAY);
        if (sleeveShown) {
            sleeve.render(stack, consumer, light, OverlayTexture.NO_OVERLAY);
        }
        stack.popPose();
        arm.xScale = armXs;
        arm.zScale = armZs;
        sleeve.xScale = sleeveXs;
        sleeve.zScale = sleeveZs;
    }
}
