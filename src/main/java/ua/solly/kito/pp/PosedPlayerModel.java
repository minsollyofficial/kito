package ua.solly.kito.pp;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;

public final class PosedPlayerModel extends PlayerModel<AbstractClientPlayer> {
    public PosedPlayerModel(ModelPart root, boolean slim) {
        super(root, slim);
    }

    @Override
    public void setupAnim(AbstractClientPlayer entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        head.resetPose();
        body.resetPose();
        rightArm.resetPose();
        leftArm.resetPose();
        rightLeg.resetPose();
        leftLeg.resetPose();
        hat.resetPose();
        jacket.resetPose();
        rightSleeve.resetPose();
        leftSleeve.resetPose();
        rightPants.resetPose();
        leftPants.resetPose();
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        rightArm.yScale = 1.0F;
        leftArm.yScale = 1.0F;
        rightSleeve.yScale = 1.0F;
        leftSleeve.yScale = 1.0F;
        PlayerPoser.applyToModel(this, entity, ageInTicks - entity.tickCount);
    }
}
