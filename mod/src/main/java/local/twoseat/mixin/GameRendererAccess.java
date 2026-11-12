package local.twoseat.mixin;

import java.util.Map;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameRenderer.class)
public interface GameRendererAccess {
    @Accessor("shaders") Map<String, ShaderInstance> twoseat$shaders();
    @Accessor("blitShader") ShaderInstance twoseat$blitShader();
    @Accessor("blitShader") void twoseat$blitShader(ShaderInstance shader);
}
