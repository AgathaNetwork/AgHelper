package cn.org.agatha.aghelper.client.utils;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ScreenshotSentScreen extends Screen {

    private final String statusMessage;
    private final int statusColor;

    public ScreenshotSentScreen(String statusMessage, int statusColor) {
        super(Text.literal("截图完成"));
        this.statusMessage = statusMessage;
        this.statusColor = statusColor;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.literal("返回游戏"), btn -> close())
                .dimensions(width / 2 - 50, height / 2 + 30, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(textRenderer,
                "截图完成",
                width / 2, height / 2 - 40, 0xFFFFFFFF);

        context.drawCenteredTextWithShadow(textRenderer,
                statusMessage,
                width / 2, height / 2 - 10, statusColor);

        context.drawCenteredTextWithShadow(textRenderer,
                "你现在可以切换到 Minechat 进行操作",
                width / 2, height / 2 + 15, 0xFFAAAAAA);
    }

    @Override
    public void close() {
        if (client != null) {
            client.setScreen(null);
        }
    }

    public void renderBackground(DrawContext context) {
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        context.fillGradient(0, 0, w, h / 2, 0xFF202020, 0xFF101010);
        context.fillGradient(0, h / 2, w, h, 0xFF101010, 0xFF202020);
    }
}
