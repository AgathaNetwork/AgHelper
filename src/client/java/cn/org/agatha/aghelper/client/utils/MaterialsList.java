package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
import cn.org.agatha.aghelper.client.MenuScreen;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MaterialsList extends Screen {
    private static class MaterialItem {
        int id;
        String uploader;
        String name;
        String description;
        int done;
        long uploadTime;

        MaterialItem(int id, String uploader, String name, String description, int done, long uploadTime) {
            this.id = id;
            this.uploader = uploader;
            this.name = name;
            this.description = description;
            this.done = done;
            this.uploadTime = uploadTime;
        }
    }

    private List<MaterialItem> materials = new ArrayList<>();
    private boolean loading = true;
    private String errorMessage = null;

    public MaterialsList() {
        super(Text.of("材料列表选择"));
    }
    
    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MaterialsDash()))
                .dimensions(10, 10, 40, 20)
                .build());

        // 异步加载材料列表
        loadMaterials();
    }

    private void loadMaterials() {
        // 在后台线程中加载数据
        Thread loaderThread = new Thread(() -> {
            try {
                URL url = new URL("https://api-materials.agatha.org.cn/mod/list");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    if (response.has("data")) {
                        JsonArray dataArray = response.getAsJsonArray("data");
                        List<MaterialItem> loadedMaterials = new ArrayList<>();
                        
                        for (int i = 0; i < dataArray.size(); i++) {
                            JsonObject item = dataArray.get(i).getAsJsonObject();
                            MaterialItem material = new MaterialItem(
                                item.get("id").getAsInt(),
                                item.get("uploader").getAsString(),
                                item.get("name").getAsString(),
                                item.get("description").getAsString(),
                                item.get("done").getAsInt(),
                                item.get("upload_time").getAsLong()
                            );
                            loadedMaterials.add(material);
                        }
                        
                        // 在主线程中更新UI
                        client.execute(() -> {
                            this.materials = loadedMaterials;
                            this.loading = false;
                            this.errorMessage = null;
                        });
                    } else {
                        throw new IOException("Invalid response format");
                    }
                } else {
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                client.execute(() -> {
                    this.loading = false;
                    this.errorMessage = "加载失败: " + e.getMessage();
                });
            }
        });
        
        loaderThread.start();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        // 渲染标题
        context.drawText(textRenderer, "材料列表选择", width / 2, 30, 0xFFFFFF, true);
        
        if (loading) {
            // 显示加载中
            context.drawText(textRenderer, "加载中...", width / 2, height / 2, 0xFFFFFF, true);
        } else if (errorMessage != null) {
            // 显示错误信息
            context.drawText(textRenderer, errorMessage, width / 2, height / 2, 0xFF5555, true);
        } else {
            // 渲染材料列表
            int startY = 60;
            int itemHeight = 30;
            int itemWidth = width - 40;
            
            for (int i = 0; i < materials.size(); i++) {
                MaterialItem material = materials.get(i);
                int itemY = startY + i * (itemHeight + 5);
                
                // 绘制列表项背景
                int backgroundColor = (mouseX >= 20 && mouseX <= 20 + itemWidth && 
                                     mouseY >= itemY && mouseY <= itemY + itemHeight) ? 0x80AAAAAA : 0x80222222;
                context.fill(20, itemY, 20 + itemWidth, itemY + itemHeight, backgroundColor);
                
                // 绘制边框
                context.drawBorder(20, itemY, itemWidth, itemHeight, 0xFFFFFFFF);
                
                // 绘制文本
                String displayText = String.format("%s (%s)", material.name, material.uploader);
                context.drawTextWithShadow(textRenderer, displayText, 25, itemY + 10, 0xFFFFFF);
            }
        }

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!loading && errorMessage == null) {
            int startY = 60;
            int itemHeight = 30;
            int itemWidth = width - 40;
            
            for (int i = 0; i < materials.size(); i++) {
                int itemY = startY + i * (itemHeight + 5);
                
                // 检查鼠标是否在某个列表项上
                if (mouseX >= 20 && mouseX <= 20 + itemWidth && 
                    mouseY >= itemY && mouseY <= itemY + itemHeight) {
                    
                    MaterialItem selectedMaterial = materials.get(i);
                    AghelperClient.selectedMaterialId = selectedMaterial.id;
                    client.setScreen(new MaterialsDash());
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
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