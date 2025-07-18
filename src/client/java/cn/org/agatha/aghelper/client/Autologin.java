package cn.org.agatha.aghelper.client;

import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.nio.file.Path;
import java.io.FileReader;
import java.io.FileWriter;

public class Autologin extends Screen {
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Path.of("config/aghelper.json");

    // 当前设置的自动登录信息
    private String currentUsername;
    private String currentPassword;

    // 用户输入的新密码
    private TextFieldWidget newPasswordField;

    protected Autologin() {
        super(Text.of("自动登录"));
        loadConfig(); // 加载配置文件中的自动登录信息
    }

    @Override
    protected void init() {
        super.init();

        // 获取当前玩家的用户名
        MinecraftClient client = MinecraftClient.getInstance();
        String playerName = client.getSession().getUsername();

        // 创建返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());

        // 创建密码输入框，并在初始化时固定坐标和宽度
        newPasswordField = new TextFieldWidget(textRenderer, width / 2 - 15, height / 2 + 30, 150, 20, Text.of("输入该账号密码"));
        newPasswordField.setMaxLength(32); // 设置最大长度为32
        addDrawableChild(newPasswordField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // 绘制标题
        context.drawCenteredTextWithShadow(textRenderer, "自动登录设置", width / 2, height / 2 - 80, 0xFFFFFF);

        // 绘制当前设置的自动登录信息（表格形式）
        int tableX = width / 2 - 150; // 表格左上角X坐标
        int tableY = height / 2 - 60; // 表格左上角Y坐标
        int tableWidth = 300;        // 表格宽度
        int tableHeight = 40;        // 修改高度为单行内容
        int tableHeight_Input = 100;

        // 绘制表格边框
        context.fill(tableX, tableY, tableX + tableWidth, tableY + tableHeight, 0xFFAAAAAA); // 背景填充
        context.drawBorder(tableX, tableY, tableWidth, tableHeight, 0xFF000000);             // 边框绘制

        // 左侧标签和右侧信息（仅显示用户名）
        String[] labels = {"已保存信息"};
        String[] values = {(currentUsername != null && currentUsername != "") ? currentUsername : "未设置"};
        for (int i = 0; i < labels.length; i++) {
            int textWidth = textRenderer.getWidth(labels[i]);
            context.drawText(textRenderer, labels[i], tableX + 20, tableY + 15 + i * 20, 0xFF000000, false);
            textWidth = textRenderer.getWidth(values[i]);
            context.drawText(textRenderer, values[i], tableX + tableWidth - textWidth - 20, tableY + 15 + i * 20, 0xFF000000, false);
        }

        // 绘制需要设置的自动登录信息（表格形式）
        tableY += tableHeight + 15; // 新表格起始Y坐标
        context.fill(tableX, tableY, tableX + tableWidth, tableY + tableHeight_Input, 0xFFAAAAAA); // 背景填充
        context.drawBorder(tableX, tableY, tableWidth, tableHeight_Input, 0xFF000000);             // 边框绘制

        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        String newPassword = newPasswordField.getText();

        labels = new String[]{"用户名", "密码"};
        values = new String[]{playerName}; // 密码部分不再显示文本
        for (int i = 0; i < labels.length; i++) {
            int textWidth = textRenderer.getWidth(labels[i]);
            context.drawText(textRenderer, labels[i], tableX + 20, tableY + 20 + i * 20, 0xFF000000, false);

            if (i == 0) { // 用户名
                textWidth = textRenderer.getWidth(values[i]);
                context.drawText(textRenderer, values[i], tableX + tableWidth - textWidth - 20, tableY + 20 + i * 20, 0xFF000000, false);
            }
        }

        // 渲染密码输入框
        newPasswordField.render(context, mouseX, mouseY, delta);

        // 添加保存按钮
        ButtonWidget saveButton = new ButtonWidget.Builder(Text.of("保存"), button -> {
            AghelperClient.updateAutologinConfig(playerName, newPassword);
        }).dimensions(width / 2 - 50, tableY + tableHeight + 25, 100, 20).build();
        saveButton.render(context, mouseX, mouseY, delta);
    }

    private void loadConfig() {
        try {
            ConfigData config = GSON.fromJson(new FileReader(CONFIG_PATH.toFile()), ConfigData.class);
            currentUsername = config.username();
            currentPassword = config.password();
        } catch (Exception e) {
            e.printStackTrace();
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

    private record ConfigData(String username, String password) {}
}