package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
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

        addDrawableChild(ButtonWidget.builder(Text.of("选择材料列表"), button -> client.setScreen(new MaterialsList()))
                .dimensions(60, 10, 80, 20)
                .build());

        // 居中说明文字

    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);
        // 显示材料列表选择状态
        String statusText;
        if (AghelperClient.selectedMaterialId == -1) {
            statusText = "当前未选择材料列表";
        } else {
            statusText = "当前选择的材料列表ID: " + AghelperClient.selectedMaterialId;
        }
        
        // 在页面顶部居中显示状态
        int textWidth = textRenderer.getWidth(statusText);

        context.drawText(textRenderer, statusText, (width - textWidth) / 2 + 25, 15, 0xFFFFFF, true);

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