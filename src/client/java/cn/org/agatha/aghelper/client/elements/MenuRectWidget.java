package cn.org.agatha.aghelper.client.elements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.AbstractInput;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public class MenuRectWidget extends PressableWidget {
    private int fillColor;
    private Text displayText;
    private ItemStack itemStack; // 可选的物品图标
    private Runnable clickHandler;
    private boolean showItem;

    // 构造函数 - 仅文字
    public MenuRectWidget(int x, int y, int width, int height, Text text, int color, Runnable onPress) {
        super(x, y, width, height, text);
        this.displayText = text;
        this.fillColor = color;
        this.clickHandler = onPress;
        this.showItem = false;
    }

    // 构造函数 - 文字 + 物品图标
    public MenuRectWidget(int x, int y, int width, int height, Text text, ItemStack item, int color, Runnable onPress) {
        super(x, y, width, height, text);
        this.displayText = text;
        this.itemStack = item;
        this.fillColor = color;
        this.clickHandler = onPress;
        this.showItem = true;
    }
    public void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        // 绘制边框（通过绘制四条边）
        int borderWidth = 2;

        // 上边
        context.fill(x, y, x + width, y + borderWidth, color);
        // 下边
        context.fill(x, y + height - borderWidth, x + width, y + height, color);
        // 左边
        context.fill(x, y, x + borderWidth, y + height, color);
        // 右边
        context.fill(x + width - borderWidth, y, x + width, y + height, color);
    }

    @Override
    public void onPress(AbstractInput input) {
        if (clickHandler != null) {
            clickHandler.run();
        }
    }

    @Override
    protected void renderWidget(DrawContext drawContext, int mouseX, int mouseY, float delta) {
        // 绘制矩形背景
        drawContext.fill(this.getX(), this.getY(),
                this.getX() + this.width, this.getY() + this.height,
                this.fillColor);

        // 绘制边框（可选）
        if (this.isHovered()) {
            drawBorder(drawContext, this.getX(), this.getY(), this.width, this.height, 0xFFFFFFFF);
        }

        // 渲染内容
        renderContent(drawContext);
    }

    private void renderContent(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        int contentX = this.getX() + 4; // 左边距
        int contentY = this.getY() + (this.height - 8) / 2; // 垂直居中文字

        if (showItem && itemStack != null && !itemStack.isEmpty()) {
            // 绘制物品图标
            drawContext.drawItem(itemStack, contentX, this.getY() + (this.height - 16) / 2);
            contentX += 20; // 为物品图标留出空间
        }

        // 绘制文字
        if (displayText != null) {
            drawContext.drawText(client.textRenderer, displayText, contentX, contentY, 0xFFFFFF, false);
        }
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder narrationMessageBuilder) {
        this.appendDefaultNarrations(narrationMessageBuilder);
    }

    // Setter方法
    public void setFillColor(int color) {
        this.fillColor = color;
    }

    public void setDisplayText(Text text) {
        this.displayText = text;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
        this.showItem = itemStack != null && !itemStack.isEmpty();
    }

    public void setClickHandler(Runnable clickHandler) {
        this.clickHandler = clickHandler;
    }

    // Getter方法
    public int getFillColor() {
        return fillColor;
    }

    public Text getDisplayText() {
        return displayText;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}