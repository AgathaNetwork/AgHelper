package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class KeybindSettingScreen extends Screen {
    private boolean isCapturing = false;
    private String promptText = "请选择设置对象";
    private String targetName;
    public KeybindSettingScreen() {
        super(Text.of("快捷键设置"));
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(
                Text.of("打开主菜单界面"),
                button -> {
                    isCapturing = true;
                    targetName = "menuShortcutKey";
                }
        ).dimensions(width/2-75, height/2 - 50, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.of("执行快速登录"),
                button -> {
                    isCapturing = true;
                    targetName = "autologinKey";
                }
        ).dimensions(width/2-75, height/2 - 20, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
                Text.of("创建照片"),
                button -> {
                    isCapturing = true;
                    targetName = "createPictureKey";
                }
        ).dimensions(width/2-75, height/2 + 10, 150, 20).build());
    }


    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.getKeycode();
        int scanCode = keyInput.scancode();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE){
            isCapturing = false;
        }
        if (isCapturing && keyCode != GLFW.GLFW_KEY_UNKNOWN && keyCode != GLFW.GLFW_KEY_ESCAPE) {
            isCapturing = false;
            // 更新提示文字
            promptText = "已设置键号：" + keyCode;

            // 更新按键绑定
            AghelperClient.updateKeyBinding(keyInput, targetName);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        
        if (isCapturing) {
            context.drawCenteredTextWithShadow(
                textRenderer, 
                "按下目标按键",
                width/2, 
                height/2 - 80,
                0xFFFFFF
            );
        }
        else{
            context.drawCenteredTextWithShadow(
                textRenderer,
                promptText,
                width/2,
                height/2 - 80,
                0xFFFFFF
            );
        }
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