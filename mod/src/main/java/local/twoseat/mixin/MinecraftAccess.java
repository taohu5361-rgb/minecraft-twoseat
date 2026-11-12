package local.twoseat.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.Connection;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftAccess {
    @Mutable @Accessor("mouseHandler") void twoseat$mouse(MouseHandler value);
    @Mutable @Accessor("levelRenderer") void twoseat$levelRenderer(LevelRenderer value);
    @Mutable @Accessor("gameRenderer") void twoseat$gameRenderer(GameRenderer value);
    @Mutable @Accessor("gui") void twoseat$gui(Gui value);
    @Mutable @Accessor("toast") void twoseat$toasts(ToastComponent value);
    @Mutable @Accessor("particleEngine") void twoseat$particles(ParticleEngine value);
    @Accessor("cameraEntity") Entity twoseat$camera();
    @Accessor("cameraEntity") void twoseat$camera(Entity value);
    @Mutable @Accessor("mainRenderTarget") void twoseat$target(RenderTarget value);
    @Accessor("pendingConnection") Connection twoseat$pending();
    @Accessor("pendingConnection") void twoseat$pending(Connection value);
    @Accessor("rightClickDelay") int twoseat$useDelay();
    @Accessor("rightClickDelay") void twoseat$useDelay(int value);
    @Accessor("missTime") int twoseat$missTime();
    @Accessor("missTime") void twoseat$missTime(int value);
    @Invoker("startAttack") boolean twoseat$attack();
    @Invoker("startUseItem") void twoseat$use();
    @Invoker("continueAttack") void twoseat$continueAttack(boolean held);
}
