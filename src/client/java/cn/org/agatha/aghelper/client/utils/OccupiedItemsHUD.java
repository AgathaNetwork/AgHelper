package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.render.RenderTickCounter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OccupiedItemsHUD implements HudRenderCallback {
    private static final OccupiedItemsHUD INSTANCE = new OccupiedItemsHUD();
    private final List<String> occupiedItems = new ArrayList<>();
    private boolean loaded = false;

    private OccupiedItemsHUD() {
    }

    public static OccupiedItemsHUD getInstance() {
        return INSTANCE;
    }

    public void loadOccupiedItemsForMaterialList(int materialId) {
        // 清空当前列表
        occupiedItems.clear();
        
        // 在后台线程中加载数据
        Thread loaderThread = new Thread(() -> {
            try {
                String playerName = MinecraftClient.getInstance().getSession().getUsername();
                URL url = new URL("https://api-materials.agatha.org.cn/mod/detail?id=" + materialId);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                    
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    if (response.has("data")) {
                        JsonObject data = response.getAsJsonObject("data");
                        if (data.has("json")) {
                            String json = data.get("json").getAsString();
                            
                            // 解析JSON数组
                            Type listType = new TypeToken<List<MaterialDetailItem>>(){}.getType();
                            List<MaterialDetailItem> loadedMaterials = new Gson().fromJson(json, listType);
                            
                            // 在主线程中更新UI
                            MinecraftClient.getInstance().execute(() -> {
                                for (MaterialDetailItem item : loadedMaterials) {
                                    // 检查物品是否被当前用户领取
                                    if (item.occupied != null && item.occupied.equals(playerName)) {
                                        addOccupiedItem(item.name + " *" + item.count);
                                    }
                                }
                                loaded = true;
                            });
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        
        loaderThread.start();
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
    
    private static class MaterialDetailItem {
        String name;
        int count;
        String occupied;
        int done;
        long donetime;
        String doneby;
    }
}