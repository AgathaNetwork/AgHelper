package cn.org.agatha.aghelper.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.util.Identifier;

public class MenuScreen extends Screen {
    protected MenuScreen() {
        super(Text.of("主菜单"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(
            Text.of("快捷键设置"),
            button -> this.client.setScreen(new KeybindSettingScreen())
        ).dimensions(width/2-75, height/2-30, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.of("连接诊断"),
                button -> this.client.setScreen(new ConnectionDiagnose())
        ).dimensions(width/2-75, height/2, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.of("快速登录"),
                button -> this.client.setScreen(new Autologin())
        ).dimensions(width/2-75, height/2+30, 150, 20).build());


        ScreenEvents.afterRender(this).register((_screen, drawContext, mouseX, mouseY, tickDelta) -> {
            // 直接渲染图片
            drawContext.drawTexture(
                    Identifier.of("aghelper", "textures/assets/Ag_0404.png"),
                    width/2 - 25,
                    height/2 - 90,
                    0, 0,
                    50, 50,
                    50, 50
            );
            // 新增：右下角显示版本号
            String version = "Version 1.0.0";
            int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(version);
            drawContext.drawText(
                    MinecraftClient.getInstance().textRenderer,
                version,
                drawContext.getScaledWindowWidth() - textWidth - 10,
                drawContext.getScaledWindowHeight() - 20,
                0xFFFFFF,
                true
            );
        });
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext context) {
        // 绘制深灰色渐变背景
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // 顶部渐变
        context.fillGradient(0, 0, width, height/2, 0xFF202020, 0xFF101010);
        // 底部渐变
        context.fillGradient(0, height/2, width, height, 0xFF101010, 0xFF202020);
    }

}