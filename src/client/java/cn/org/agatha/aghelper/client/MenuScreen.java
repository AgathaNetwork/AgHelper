package cn.org.agatha.aghelper.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.util.Identifier;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.FabricLoader;

import java.util.Properties;

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

            // 绘制一个正方形
            int logoX = width/2 - 24;
            int logoY = height/2 - 89;

            drawContext.fill(logoX, logoY, logoX + 48, logoY + 48, 0xFFDDFFFE);
            drawContext.fill(logoX + 6, logoY + 6, logoX + 42, logoY + 42, 0xFF0055FF);
            drawContext.fill(logoX, logoY + 12, logoX + 18, logoY + 24, 0xFF339CFF);
            drawContext.fill(logoX + 24, logoY, logoX + 36, logoY + 18, 0xFF339CFF);
            drawContext.fill(logoX + 12, logoY + 30, logoX + 24, logoY + 48, 0xFF339CFF);
            drawContext.fill(logoX + 30, logoY + 24, logoX + 48, logoY + 36, 0xFF339CFF);

            // 新增：右下角显示版本号
            String version = "©Agatha v" + getModVersion();
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


    public static String getModVersion() {
        // 动态获取本Mod版本号
        ModContainer modContainer = FabricLoader.getInstance().getModContainer("aghelper").orElse(null);
        if(modContainer != null){
            return modContainer.getMetadata().getVersion().getFriendlyString();
        }
        // 错误返回
        return "unknown";
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