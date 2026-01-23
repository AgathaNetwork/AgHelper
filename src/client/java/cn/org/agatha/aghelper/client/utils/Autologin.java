package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class Autologin extends Screen {
    public Autologin() {
        super(Text.of("快速登录（已移除）"));
    }

    @Override
    protected void init() {
        // 仅保留返回按钮，说明功能已移除
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> {
            assert client != null;
            client.setScreen(new MenuScreen());
        }).dimensions(10, 10, 40, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer, "快速登录功能已被移除。", width/2, height/2 - 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "请使用其它方式登录或管理账号信息。", width/2, height/2 + 10, 0xFFAAAAAA);
    }

    public void renderBackground(DrawContext context) {
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();
        context.fillGradient(0, 0, width, height/2, 0xFF202020, 0xFF101010);
        context.fillGradient(0, height/2, width, height, 0xFF101010, 0xFF202020);
    }
}