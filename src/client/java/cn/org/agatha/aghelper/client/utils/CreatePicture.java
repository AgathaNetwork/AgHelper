package cn.org.agatha.aghelper.client.utils;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TextWidget;
import net.minecraft.text.Text;


public class CreatePicture extends Screen {
    protected double x;
    protected double y;
    protected double z;
    protected String worldName;
    protected String filePath;

    // 添加成员变量存储输入框内容
    private String titleContent = "";
    private String remarkContent = "";
    private String shaderName = "";
    private String shaderConfig = "";
    // 添加确定按钮成员变量
    private ButtonWidget confirmButton;

    public CreatePicture(String filePath, double x, double y, double z, String worldName) {
        super(Text.literal("创建照片"));
        this.filePath = filePath;
        this.x = x;
        this.y = y;
        this.z = z;
        this.worldName = worldName;
    }

    private void updateConfirmButtonState() {
        // 检查标题和备注是否都有内容
        boolean hasTitle = titleContent != null && !titleContent.trim().isEmpty();
        boolean hasRemark = remarkContent != null && !remarkContent.trim().isEmpty();
        confirmButton.active = hasTitle && hasRemark;
    }

    @Override
    protected void init() {
        // 给这个创建照片的页面创建一个表单，打开时就要显示，表单应该允许滚动，填写以下字段：图片名称、备注、X、Y、Z、所在世界、光影名称、光影配置
        // 世界、类别、玩家、文件名、文件大小、高度、宽度。
        // 添加显示参数的标签

        if (worldName.equalsIgnoreCase("overworld"))worldName = "world";
        if (worldName.equalsIgnoreCase("the_nether"))worldName = "world_nether";
        if (worldName.equalsIgnoreCase("the_end"))worldName = "world_the_end";

        Text filePathText = Text.literal("文件路径: " + filePath);
        Text xyzText = Text.literal("坐标: X=" + x + " Y=" + y + " Z=" + z);
        Text worldText = Text.literal("世界: " + worldName);
        
        // 计算屏幕宽度
        int screenWidth = client.getWindow().getScaledWidth();
        
        // 计算文本宽度并创建自适应宽度的文本组件
        int filePathWidth = this.textRenderer.getWidth(filePathText) + 10;
        int xyzWidth = this.textRenderer.getWidth(xyzText) + 10;
        int worldWidth = this.textRenderer.getWidth(worldText) + 10;
        
        // 计算总宽度（取最大值）
        int totalWidth = Math.max(filePathWidth, Math.max(xyzWidth, worldWidth));
        
        // 计算居中X坐标
        int centerX = (screenWidth - totalWidth) / 2;
        
        // 将文本标签添加到界面中（水平居中）
        this.addDrawableChild(new TextWidget(centerX, 10, filePathWidth, 20, filePathText, this.textRenderer));
        this.addDrawableChild(new TextWidget(centerX, 30, xyzWidth, 20, xyzText, this.textRenderer));
        this.addDrawableChild(new TextWidget(centerX, 50, worldWidth, 20, worldText, this.textRenderer));
        
        // 添加输入框（宽度与上方文本一致）
        // 单行输入框
        TextFieldWidget titleInput = new TextFieldWidget(this.textRenderer, centerX, 80, totalWidth, 20, Text.literal(titleContent));
        titleInput.setText(titleContent);
        titleInput.setPlaceholder(Text.literal("输入标题"));
        // 添加文本变化监听器
        titleInput.setChangedListener(text -> {
            titleContent = text;
            updateConfirmButtonState();
        });
        this.addDrawableChild(titleInput);

        TextFieldWidget remarkInput = new TextFieldWidget(this.textRenderer, centerX, 110, totalWidth, 20, Text.literal(remarkContent));
        remarkInput.setText(remarkContent);
        remarkInput.setPlaceholder(Text.literal("输入备注"));
        // 添加文本变化监听器
        remarkInput.setChangedListener(text -> {
            remarkContent = text;
            updateConfirmButtonState();
        });
        this.addDrawableChild(remarkInput);

        // 单行输入框
        TextFieldWidget shaderInput = new TextFieldWidget(this.textRenderer, centerX, 140, totalWidth, 20, Text.literal(shaderName));
        shaderInput.setText(shaderName);
        shaderInput.setPlaceholder(Text.literal("输入光影名（可选）"));
        // 添加文本变化监听器
        shaderInput.setChangedListener(text -> shaderName = text);
        this.addDrawableChild(shaderInput);

        TextFieldWidget shaderConfigInput = new TextFieldWidget(this.textRenderer, centerX, 170, totalWidth, 20, Text.literal(shaderConfig));
        shaderConfigInput.setText(shaderConfig);
        shaderConfigInput.setPlaceholder(Text.literal("输入光影配置（可选）"));
        // 添加文本变化监听器
        shaderConfigInput.setChangedListener(text -> shaderConfig = text);
        this.addDrawableChild(shaderConfigInput);

        // 添加确定按钮
        confirmButton = ButtonWidget.builder(Text.literal("确定"), button -> {
            // {"x":1,"y":2,"z":3,"world":"world","timestamp":1751108743}
            String position = "{\"x\":" + x + ",\"y\":" + y + ",\"z\":" + z + ",\"world\":\"" + worldName + "\",\"timestamp\":" + (int)Math.floor((double) System.currentTimeMillis() /1000) + "}";

            JsonObject shaderConfig = new JsonObject();
            // 如果为空，则默认为“无”
            if(shaderInput.getText().equalsIgnoreCase("") || shaderInput.getText() == null){
                shaderConfig.addProperty("name", "无");
            }
            else{
                shaderConfig.addProperty("name", shaderInput.getText());
            }
            if(shaderConfigInput.getText().equalsIgnoreCase("") || shaderConfigInput.getText() == null){
                shaderConfig.addProperty("config", "无");
            }
            else{
                shaderConfig.addProperty("config", shaderConfigInput.getText());
            }

            // 使用Gson转换为字符串时自动处理转义
            String shaderConfigJson = shaderConfig.toString();

            //String shaderConfig, String position, String filePath, String name, String remark
            client.setScreen(new CreatePicture_2(shaderConfigJson, position, filePath, titleInput.getText(), remarkInput.getText(), client.getSession().getUsername())); // 关闭当前界面
        }).build();
        
        // 计算按钮位置（在输入框下方，保持20px间距）
        confirmButton.setPosition(centerX, 200);
        confirmButton.setWidth(totalWidth);
        this.addDrawableChild(confirmButton);

        // 初始时更新按钮状态
        updateConfirmButtonState();
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