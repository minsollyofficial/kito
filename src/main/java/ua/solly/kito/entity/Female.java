package ua.solly.kito.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import ua.solly.kito.entity.relation.FemaleTaste;
import ua.solly.kito.entity.relation.Taste;

public abstract class Female extends KitoCharacter {
    protected Female(EntityType<? extends Female> type, Level level) {
        super(type, level);
    }

    @Override
    public final Gender gender() {
        return Gender.FEMALE;
    }

    @Override
    public Taste taste() {
        return FemaleTaste.INSTANCE;
    }
}
