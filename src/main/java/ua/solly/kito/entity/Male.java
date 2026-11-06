package ua.solly.kito.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import ua.solly.kito.entity.relation.MaleTaste;
import ua.solly.kito.entity.relation.Taste;

public abstract class Male extends KitoCharacter {
    protected Male(EntityType<? extends Male> type, Level level) {
        super(type, level);
    }

    @Override
    public final Gender gender() {
        return Gender.MALE;
    }

    @Override
    public Taste taste() {
        return MaleTaste.INSTANCE;
    }
}
