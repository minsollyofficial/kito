package ua.solly.kito.entity.kito;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import ua.solly.kito.entity.Female;
import ua.solly.kito.entity.KitoCharacter;
import ua.solly.kito.kaf.KitoAsm;

public class KitoEntity extends Female {
    public static final KitoAsm ASM = KitoAsm.of("kito")
            .headTracking(true)
            .headLimits(KitoCharacter.HEAD_YAW_LIMIT, KitoCharacter.HEAD_PITCH_LIMIT);

    public KitoEntity(EntityType<? extends KitoEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KitoCharacter.baseAttributes();
    }

    @Override
    public KitoAsm asm() {
        return ASM;
    }
}
