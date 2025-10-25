package cn.org.agatha.aghelper.client.utils;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.RenderTickCounter;

import java.util.ArrayList;
import java.util.List;

public class OccupiedItemsHUD implements HudRenderCallback {
    private static final OccupiedItemsHUD INSTANCE = new OccupiedItemsHUD();
    private final List<String> occupiedItems = new ArrayList<>();

    private OccupiedItemsHUD() {
    }

    public static OccupiedItemsHUD getInstance() {
        return INSTANCE;
    }

    public void addOccupiedItem(String itemName) {
        // 检查物品是否已经存在于列表中，避免重复
        if (!occupiedItems.contains(itemName)) {
            occupiedItems.add(itemName);
        }
    }
    
    public void removeOccupiedItem(String itemName) {
        occupiedItems.remove(itemName);
    }

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        if (!occupiedItems.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // 显示领取的物品
            int yPosition = screenHeight - 40;
            for (int i = 0; i < occupiedItems.size() && i < 5; i++) { // 最多显示5个物品
                String text = "已领取: " + occupiedItems.get(i);
                int textWidth = client.textRenderer.getWidth(text);
                int xPosition = (screenWidth - textWidth) / 2;
                
                // 绘制带背景的文字
                context.fill(xPosition - 3, yPosition - 1, xPosition + textWidth + 3, yPosition + 9, 
                           0xAA222222); // 半透明黑色背景
                context.drawTextWithShadow(client.textRenderer, text, xPosition, yPosition, 
                                         0xFFFFFF); // 白色文字
                
                yPosition -= 12;
            }
        }
    }
}