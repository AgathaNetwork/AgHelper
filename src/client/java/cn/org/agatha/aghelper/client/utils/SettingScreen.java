package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class SettingScreen extends Screen {
    private boolean isCapturing = false;
    private String captureTarget = "";
    private String statusText = "";
    private int menuKeyCode;
    private int pictureKeyCode;
    private boolean autoJoin;

    public SettingScreen() {
        super(Text.of("设置"));
    }

    @Override
    protected void init() {
        // 读取当前按键（从 KeyBinding 实时获取，以反映系统界面的修改）
        menuKeyCode = AghelperClient.getCurrentMenuKeyCode();
        pictureKeyCode = AghelperClient.getCurrentPictureKeyCode();
        // 自动进服从配置文件读取
        autoJoin = AghelperClient.loadConfig().autoJoinServer();

        // 返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> {
            assert client != null;
            client.setScreen(new MenuScreen());
        }).dimensions(10, 10, 40, 20).build());

        int btnX = width / 2 + 80;
        int row1Y = 55;
        int row2Y = 82;
        int row3Y = 109;

        // === 设置项 1：打开主菜单 ===
        addDrawableChild(ButtonWidget.builder(Text.of("修改"), button -> {
            isCapturing = true;
            captureTarget = "menuShortcutKey";
            statusText = "";
        }).dimensions(btnX, row1Y, 40, 20).build());

        // === 设置项 2：截图发送 ===
        addDrawableChild(ButtonWidget.builder(Text.of("修改"), button -> {
            isCapturing = true;
            captureTarget = "createPictureKey";
            statusText = "";
        }).dimensions(btnX, row2Y, 40, 20).build());

        // === 设置项 3：自动进服 ===
        addDrawableChild(ButtonWidget.builder(Text.of(autoJoin ? "开启" : "关闭"), button -> {
            autoJoin = !autoJoin;
            AghelperClient.setAutoJoinServer(autoJoin);
            button.setMessage(Text.of(autoJoin ? "开启" : "关闭"));
            statusText = autoJoin ? "自动进服已开启" : "自动进服已关闭";
        }).dimensions(btnX, row3Y, 40, 20).build());
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        int keyCode = keyInput.getKeycode();
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (isCapturing) {
                isCapturing = false;
                captureTarget = "";
                statusText = "已取消";
                return true;
            }
        }
        if (isCapturing && keyCode != GLFW.GLFW_KEY_UNKNOWN) {
            isCapturing = false;
            // 更新配置
            AghelperClient.updateKeyBinding(keyInput, captureTarget);
            // 刷新显示的按键
            AghelperClient.ConfigData config = AghelperClient.loadConfig();
            menuKeyCode = config.menuShortcutKey();
            pictureKeyCode = config.createPictureKey();
            statusText = "已更新";
            captureTarget = "";
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        int centerX = width / 2;
        int row1Y = 55;
        int row2Y = 82;
        int row3Y = 109;
        int labelX = centerX - 100;
        int keyX = centerX + 10;
        int btnX = centerX + 80;

        // 标题
        context.drawCenteredTextWithShadow(textRenderer, "设 置", centerX, 30, 0xFFFFFFFF);

        // --- 设置行 1：打开主菜单 ---
        String key1Name = getKeyName(menuKeyCode);
        context.drawTextWithShadow(textRenderer, "打开主菜单", labelX, row1Y + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "[" + key1Name + "]", keyX, row1Y + 6, 0xFFCCCCCC);

        if (isCapturing && captureTarget.equals("menuShortcutKey")) {
            context.drawTextWithShadow(textRenderer, "按下目标按键…", btnX + 50, row1Y + 6, 0xFFFFFF00);
        }

        // --- 设置行 2：截图发送 ---
        String key2Name = getKeyName(pictureKeyCode);
        context.drawTextWithShadow(textRenderer, "截图发送", labelX, row2Y + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "[" + key2Name + "]", keyX, row2Y + 6, 0xFFCCCCCC);

        if (isCapturing && captureTarget.equals("createPictureKey")) {
            context.drawTextWithShadow(textRenderer, "按下目标按键…", btnX + 50, row2Y + 6, 0xFFFFFF00);
        }

        // --- 设置行 3：自动进服 ---
        context.drawTextWithShadow(textRenderer, "自动进服", labelX, row3Y + 6, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "[" + (autoJoin ? "开启" : "关闭") + "]", keyX, row3Y + 6, 0xFFCCCCCC);

        // 状态提示
        if (!statusText.isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, statusText, centerX, height - 40, 0xFFAAAAAA);
        }
    }

    private String getKeyName(int keyCode) {
        try {
            return InputUtil.Type.KEYSYM.createFromCode(keyCode).getLocalizedText().getString();
        } catch (Exception e) {
            return "键码" + keyCode;
        }
    }

    public void renderBackground(DrawContext context) {
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        context.fillGradient(0, 0, w, h / 2, 0xFF202020, 0xFF101010);
        context.fillGradient(0, h / 2, w, h, 0xFF101010, 0xFF202020);
    }
}
