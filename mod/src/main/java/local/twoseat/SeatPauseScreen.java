package local.twoseat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.network.chat.Component;

/** A menu for one local player; the integrated world continues like multiplayer. */
public final class SeatPauseScreen extends Screen {
    private final boolean guest;
    public SeatPauseScreen(boolean guest) {
        super(Component.translatable(guest ? "twoseat.menu.p2" : "twoseat.menu.p1"));
        this.guest = guest;
    }
    @Override protected void init() {
        int w = Math.min(200, width - 24), x = (width-w)/2, y = Math.max(54, height/2-62);
        var resume = addRenderableWidget(Button.builder(Component.translatable("menu.returnToGame"), b -> onClose()).bounds(x,y,w,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.advancements"), b -> minecraft.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements()))).bounds(x,y+24,w,20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.stats"), b -> minecraft.setScreen(new StatsScreen(this,minecraft.player.getStats()))).bounds(x,y+48,w,20).build());
        addRenderableWidget(Button.builder(Component.translatable("twoseat.menu.options"), b -> minecraft.setScreen(new OptionsScreen(this,minecraft.options))).bounds(x,y+72,w,20).build());
        addRenderableWidget(Button.builder(Component.translatable(guest ? "twoseat.menu.leave" : "twoseat.menu.end"), b -> SplitSession.requestLeave(!guest)).bounds(x,y+104,w,20).build());
        setInitialFocus(resume);
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void onClose() { minecraft.setScreen(null); }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial) {
        renderBackground(g);
        g.drawCenteredString(font,title,width/2,28,0xFFFFFF);
        g.drawCenteredString(font,Component.translatable("twoseat.menu.running"),width/2,height-24,0xBFC8D0);
        super.render(g,mouseX,mouseY,partial);
    }
}
