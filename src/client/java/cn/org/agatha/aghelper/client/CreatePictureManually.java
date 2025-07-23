package cn.org.agatha.aghelper.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import static org.lwjgl.system.MemoryUtil.memPointerBuffer;

public class CreatePictureManually extends Screen {
    private TextFieldWidget filePathField;

    public CreatePictureManually() {
        super(Text.literal("手动上传照片"));
    }

    public String path;
    @Override
    protected void init() {
        // 保留返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> {
                    assert client != null;
                    client.setScreen(new MenuScreen());
                })
                .dimensions(10, 10, 40, 20)
                .build());

        // 文件路径显示框
        this.filePathField = new TextFieldWidget(
                this.textRenderer,
                this.width / 2 - 150,
                this.height / 2 - 10,
                300,
                20,
                Text.literal("文件路径")
        );
        this.filePathField.setEditable(false);
        this.addDrawableChild(this.filePathField);

        // 初始化拖放监听
        initDragDropHandler();
    }

    private void initDragDropHandler() {
        MinecraftClient client = MinecraftClient.getInstance();
        long windowHandle = client.getWindow().getHandle();
        
        if (windowHandle != 0) {
            GLFW.glfwSetDropCallback(windowHandle, (window, count, paths) -> {
                if (count > 0) {
                    PointerBuffer pathBuffer = memPointerBuffer(paths, count);
                    long pathAddress = pathBuffer.get();
                    int length = 0;
                    while (MemoryUtil.memGetByte(pathAddress + length) != 0) {
                        length++;
                    }
                    path = MemoryUtil.memUTF8(pathAddress, length);
                    client.execute(() -> {
                        this.filePathField.setText(path);
                        // 自动执行确认逻辑并关闭窗口
                        if (!path.isEmpty()) {
                            // 这里可以添加文件处理逻辑
                        }
                    });
                }
            });
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        // 添加拖放提示文本
        String tip = "将文件拖动到窗口中";
        int textWidth = this.textRenderer.getWidth(tip);
        context.drawText(this.textRenderer, tip, this.width/2 - textWidth/2, this.height/2 - 20, 0xFFFFFF, false);
    }

    public void renderBackground(DrawContext context) {
        assert client != null;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        context.fillGradient(0, 0, width, height/2, 0xFF202020, 0xFF101010);
        context.fillGradient(0, height/2, width, height, 0xFF101010, 0xFF202020);
    }
}