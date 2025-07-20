package cn.org.agatha.aghelper.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;


public class CreatePicture extends Screen {
    protected double x;
    protected double y;
    protected double z;
    protected String worldName;
    protected String filePath;

    protected CreatePicture(String filePath, double x, double y, double z, String worldName) {
        super(Text.literal("创建照片"));
        this.filePath = filePath;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    @Override
    protected void init() {
        // 给这个创建照片的页面创建一个表单，打开时就要显示，表单应该允许滚动，填写以下字段：图片名称、备注、世界、类别、玩家、X、Y、Z、所在世界、光影名称、光影配置、文件名、文件大小、高度、宽度。
        
        // 添加显示参数的标签
        Text filePathText = Text.literal("文件路径: " + filePath);
        Text xyzText = Text.literal("坐标: X=" + x + " Y=" + y + " Z=" + z);
        Text worldText = Text.literal("世界: " + worldName);
        
        // 将文本标签添加到界面中
        this.addDrawableChild(new TextWidget(10, 10, 100, 20, filePathText, this.textRenderer));
        this.addDrawableChild(new TextWidget(10, 30, 100, 20, xyzText, this.textRenderer));
        this.addDrawableChild(new TextWidget(10, 50, 100, 20, worldText, this.textRenderer));
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