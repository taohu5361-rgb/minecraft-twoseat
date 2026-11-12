package local.twoseat;

import com.mojang.blaze3d.pipeline.RenderTarget;
import local.twoseat.mixin.MinecraftAccess;
import local.twoseat.mixin.ParticleAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.network.Connection;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.HitResult;

/** A lightweight world/player view; assets, managers and the Minecraft singleton stay shared. */
final class ClientContext {
    MouseHandler mouse;
    final SeatKeys keys = new SeatKeys();
    final ParticleState particles = new ParticleState();
    ClientLevel level;
    LocalPlayer player;
    MultiPlayerGameMode gameMode;
    LevelRenderer levelRenderer;
    GameRenderer gameRenderer;
    Gui gui;
    ToastComponent toasts;
    Screen screen;
    Entity camera, crosshair;
    HitResult hit;
    Connection pending;
    int useDelay, missTime;

    void capture(Minecraft mc) {
        MinecraftAccess a = (MinecraftAccess)mc;
        mouse = mc.mouseHandler;
        keys.capture(mc);
        particles.capture(mc.particleEngine);
        level = mc.level; player = mc.player; gameMode = mc.gameMode;
        levelRenderer = mc.levelRenderer; gameRenderer = mc.gameRenderer; gui = mc.gui;
        toasts = mc.getToasts();
        camera = a.twoseat$camera(); crosshair = mc.crosshairPickEntity; hit = mc.hitResult;
        screen = mc.screen; pending = a.twoseat$pending();
        useDelay = a.twoseat$useDelay(); missTime = a.twoseat$missTime();
    }

    void apply(Minecraft mc) {
        MinecraftAccess a = (MinecraftAccess)mc;
        a.twoseat$mouse(mouse);
        keys.apply(mc);
        mc.level = level; mc.player = player; mc.gameMode = gameMode;
        ((ParticleAccess)mc.particleEngine).twoseat$level(level);
        particles.apply(mc.particleEngine);
        a.twoseat$levelRenderer(levelRenderer); a.twoseat$gameRenderer(gameRenderer); a.twoseat$gui(gui);
        a.twoseat$toasts(toasts);
        a.twoseat$camera(camera); mc.crosshairPickEntity = crosshair; mc.hitResult = hit;
        mc.screen = screen; a.twoseat$pending(pending);
        a.twoseat$useDelay(useDelay); a.twoseat$missTime(missTime);
    }
}
