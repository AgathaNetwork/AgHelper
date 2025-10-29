package cn.org.agatha.aghelper.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class UpdateNotificationScreen extends Screen {
    private final Path oldModFile;
    private final String newVersion;

    protected UpdateNotificationScreen(Path oldModFile, String newVersion) {
        super(Text.of("模组更新提醒"));
        this.oldModFile = oldModFile;
        this.newVersion = newVersion;
    }

    @Override
    protected void init() {
        super.init();
        
        // 添加打开mods文件夹的按钮
        this.addDrawableChild(ButtonWidget.builder(Text.literal("打开Mods文件夹"), button -> {
            try {
                File modsFolder = oldModFile.getParent().toFile();
                Util.getOperatingSystem().open(modsFolder);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).dimensions(this.width / 2 - 100, this.height / 2 + 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        super.render(context, mouseX, mouseY, delta);

        // 显示标题
        context.drawCenteredTextWithShadow(this.textRenderer, 
            "AgHelper更新完成",
            this.width / 2, 
            this.height / 2 - 60, 
            0xFFFFFFFF);

        // 显示提示信息
        drawTextLines(context, new String[] {
            "检测到新版本 " + newVersion + " 已下载完成",
            "请关闭游戏后手动删除旧版本文件:",
            oldModFile.getFileName().toString(),
            "然后再启动游戏以使用新版本"
        }, this.height / 2 - 40);

        // 显示警告信息
        context.drawCenteredTextWithShadow(this.textRenderer,
            "注意：请在关闭游戏后再删除旧文件，否则可能导致文件被占用无法删除",
            this.width / 2,
            this.height / 2 + 60,
            0xFFFFFF00);
    }

    private void drawTextLines(DrawContext context, String[] lines, int startY) {
        for (int i = 0; i < lines.length; i++) {
            context.drawCenteredTextWithShadow(this.textRenderer, 
                lines[i], 
                this.width / 2, 
                startY + i * 12, 
                0xFFFFFFFF);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false; // 禁止通过ESC键关闭窗口
    }
}