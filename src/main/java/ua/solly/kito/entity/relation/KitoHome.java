package ua.solly.kito.entity.relation;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

import java.util.Optional;
import java.util.UUID;

public final class KitoHome {
    private BlockPos pos;
    private UUID owner;

    public boolean has() {
        return pos != null && owner != null;
    }

    public BlockPos pos() {
        return pos;
    }

    public UUID owner() {
        return owner;
    }

    public boolean isOwnedBy(UUID player) {
        return owner != null && owner.equals(player);
    }

    public void set(BlockPos pos, UUID owner) {
        this.pos = pos.immutable();
        this.owner = owner;
    }

    public void clear() {
        pos = null;
        owner = null;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        if (has()) {
            tag.putLong("pos", pos.asLong());
            tag.putUUID("owner", owner);
        }
        return tag;
    }

    public void load(CompoundTag tag) {
        if (tag.contains("pos") && tag.hasUUID("owner")) {
            pos = BlockPos.of(tag.getLong("pos"));
            owner = tag.getUUID("owner");
        } else {
            clear();
        }
    }

    public Optional<BlockPos> asOptional() {
        return Optional.ofNullable(pos);
    }
}
