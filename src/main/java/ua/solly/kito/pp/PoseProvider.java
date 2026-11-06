package ua.solly.kito.pp;

import net.minecraft.client.player.AbstractClientPlayer;

public interface PoseProvider {
    void apply(AbstractClientPlayer player, float partial, PlayerPose pose);
}
