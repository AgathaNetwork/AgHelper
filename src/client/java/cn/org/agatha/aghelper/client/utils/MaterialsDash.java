package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MaterialsDash extends Screen {
    public MaterialsDash() {
        super(Text.of("材料列表查看"));
    }
    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());
        // 居中说明文字

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