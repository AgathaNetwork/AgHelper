package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
import net.minecraft.text.Text;
public class Supplies extends Screen {
    public Supplies() {
        super(Text.of("资源管理"));
    }
    private ScrollableWidget scrollableWidget;
    public int entryCount = 20;

    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());
        // 列表组件
        // 创建滚动容器，位置和大小可以根据需要调整
        this.scrollableWidget = new ScrollableWidget(
                20,  // x位置
                40,  // y位置
                200,                   // 宽度
                height - 60,           // 高度
                Text.literal("Scrollable Content")
        ) {
            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {

            }

            @Override
            protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
                // 在这里渲染您的内容
                renderScrollableContent(context, mouseX, mouseY, delta);
            }

            @Override
            protected int getContentsHeight() {
                // 返回内容的总高度，如果超过容器高度就会显示滚动条
                return calculateContentHeight();
            }

            @Override
            protected double getDeltaYPerScroll() {
                return 0;
            }
        };

        this.addDrawableChild(scrollableWidget);
    }
    private void renderScrollableContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染您的滚动内容
        TextRenderer textRenderer = this.textRenderer;

        for (int i = 0; i < entryCount; i++) {
            context.drawText(textRenderer,
                    Text.literal("滚动内容行 " + (i + 1)),
                    30, i * 12 + 50, 0xFFFFFF, false);
        }
    }

    private int calculateContentHeight() {
        // 计算内容总高度
        return entryCount * 12 + 10;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 处理鼠标滚轮事件
        if (this.scrollableWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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