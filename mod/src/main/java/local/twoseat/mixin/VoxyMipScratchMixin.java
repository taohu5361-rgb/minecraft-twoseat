package local.twoseat.mixin;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Each Voxy model processor needs its own mutable mip flood-fill workspace. */
@Pseudo
@Mixin(targets = "me.cortex.voxy.client.core.model.MipGen", remap = false)
public abstract class VoxyMipScratchMixin {
    @Shadow @Final private static short[] SCRATCH;
    @Unique private static final ThreadLocal<short[]> twoseat$scratch =
        ThreadLocal.withInitial(() -> new short[SCRATCH.length]);
    @Unique private static final ThreadLocal<ByteArrayFIFOQueue> twoseat$queue =
        ThreadLocal.withInitial(() -> new ByteArrayFIFOQueue(SCRATCH.length));

    @Redirect(method = "solidify", at = @At(value = "FIELD", target = "Lme/cortex/voxy/client/core/model/MipGen;SCRATCH:[S"), remap = false)
    private static short[] twoseat$threadScratch() { return twoseat$scratch.get(); }

    @Redirect(method = "solidify", at = @At(value = "FIELD", target = "Lme/cortex/voxy/client/core/model/MipGen;QUEUE:Lit/unimi/dsi/fastutil/bytes/ByteArrayFIFOQueue;"), remap = false)
    private static ByteArrayFIFOQueue twoseat$threadQueue() { return twoseat$queue.get(); }
}
