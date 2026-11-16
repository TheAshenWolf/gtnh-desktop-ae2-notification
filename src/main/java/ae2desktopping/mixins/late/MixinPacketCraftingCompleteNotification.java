package ae2desktopping.mixins.late;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ae2desktopping.AE2DesktopPing;
import ae2desktopping.client.CraftCompletions;
import appeng.api.storage.data.IAEStack;
import appeng.core.sync.AppEngPacket;
import appeng.core.sync.network.INetworkInfo;
import appeng.core.sync.packets.PacketCraftingCompleteNotification;

/**
 * The server sends this packet to each online follower when a "Start and follow" craft finishes (and replays it on
 * login for crafts that finished while offline). This may run on the network thread, so it only queues the event.
 */
@Mixin(value = PacketCraftingCompleteNotification.class, remap = false)
public abstract class MixinPacketCraftingCompleteNotification {

    @Shadow
    @Final
    private IAEStack<?> stack;

    @Shadow
    @Final
    private long numsOfOutput;

    @Shadow
    @Final
    private long elapsedTime;

    @Inject(method = "clientPacketData", at = @At("HEAD"))
    private void ae2desktopping$onCraftComplete(INetworkInfo network, AppEngPacket packet, EntityPlayer player,
        CallbackInfo callback) {
        try {
            CraftCompletions.post(stack, numsOfOutput, elapsedTime);
        } catch (RuntimeException exception) {
            AE2DesktopPing.LOG.error("Could not record a finished AE2 craft", exception);
        }
    }
}
