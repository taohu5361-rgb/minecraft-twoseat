package local.twoseat.mixin;

import io.netty.channel.ChannelHandlerContext;
import local.twoseat.SplitSession;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void twoseat$route(ChannelHandlerContext ctx, Packet<?> packet, CallbackInfo ci) {
        if (SplitSession.enqueue((Connection)(Object)this, packet)) ci.cancel();
    }
}
