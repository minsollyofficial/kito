package ua.solly.kito.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.control.LookControl;

public class KitoLookControl extends LookControl {
    private final KitoCharacter kito;

    public KitoLookControl(KitoCharacter kito) {
        super(kito);
        this.kito = kito;
    }

    @Override
    public void setLookAt(double x, double y, double z, float deltaYaw, float deltaPitch) {
        if (kito.isInScene()) {
            return;
        }
        super.setLookAt(x, y, z, deltaYaw, deltaPitch);
    }

    @Override
    public void tick() {
        if (kito.isInScene()) {
            kito.yHeadRot = Mth.approachDegrees(kito.yHeadRot, kito.yBodyRot, 20.0F);
            kito.setXRot(Mth.approach(kito.getXRot(), 0.0F, 10.0F));
            return;
        }
        super.tick();
    }
}
