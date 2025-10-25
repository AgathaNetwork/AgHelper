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
    private int materialId = -1;
    private long lastUpdate = 0;
    private static final long UPDATE_INTERVAL = 1000; // 1秒更新间隔

    private OccupiedItemsHUD() {
    }

    public static OccupiedItemsHUD getInstance() {
        return INSTANCE;
    }

    public void setMaterialId(int materialId) {
        this.materialId = materialId;
        this.lastUpdate = 0; // 重置更新时间，确保立即更新
    }

    private void updateOccupiedItems() {
        long currentTime = System.currentTimeMillis();
        if (materialId == -1 || currentTime - lastUpdate < UPDATE_INTERVAL) {
            return;
        }
        
        lastUpdate = currentTime;
        
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
                                // 清空当前列表
                                occupiedItems.clear();
                                
                                for (MaterialDetailItem item : loadedMaterials) {
                                    // 检查物品是否被当前用户领取且未完成
                                    if (item.occupied != null && item.occupied.equals(playerName) && item.done != 1) {
                                        occupiedItems.add(item.name + " *" + item.count);
                                    }
                                }
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
        // 更新已领取的项目
        updateOccupiedItems();
        
        if (!occupiedItems.isEmpty()) {
            MinecraftClient client = MinecraftClient.getInstance();
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // 显示领取的物品
            int yPosition = screenHeight - 40;
            for (int i = 0; i < occupiedItems.size() && i < 5; i++) { // 最多显示5个物品
                String text = occupiedItems.get(i);
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