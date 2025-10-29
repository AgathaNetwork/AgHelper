package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
import cn.org.agatha.aghelper.client.MenuScreen;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

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
    private int currentPage = 0;
    private int itemsPerPage = 0;
    private int totalItems = 0;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private static final int ITEM_HEIGHT = 30;
    private static final int ITEM_SPACING = 5;
    private static final int TOP_MARGIN = 50;
    private static final int BOTTOM_MARGIN = 30;

    public MaterialsList() {
        super(Text.of("材料列表选择"));
    }
    
    @Override
    protected void init() {
        // 添加返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MaterialsDash()))
                .dimensions(10, 10, 40, 20)
                .build());

        // 计算每页可以显示的条目数量
        int availableHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        itemsPerPage = Math.max(1, availableHeight / (ITEM_HEIGHT + ITEM_SPACING));
        
        // 添加翻页按钮
        int buttonWidth = 80;
        int buttonHeight = 20;
        int buttonY = height - 25;
        
        prevButton = ButtonWidget.builder(Text.of("上一页"), button -> {
            if (currentPage > 0) {
                currentPage--;
                updateButtons();
            }
        }).dimensions(width / 2 - buttonWidth - 10, buttonY, buttonWidth, buttonHeight).build();
        
        nextButton = ButtonWidget.builder(Text.of("下一页"), button -> {
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateButtons();
            }
        }).dimensions(width / 2 + 10, buttonY, buttonWidth, buttonHeight).build();
        
        addDrawableChild(prevButton);
        addDrawableChild(nextButton);

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
                    InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                    
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    if (response.has("data")) {
                        JsonArray dataArray = response.getAsJsonArray("data");
                        List<MaterialItem> loadedMaterials = new ArrayList<>();
                        
                        for (int i = 0; i < dataArray.size(); i++) {
                            JsonObject item = dataArray.get(i).getAsJsonObject();
                            int done = item.get("done").getAsInt();
                            
                            // 如果done=1，不显示已完成的条目
                            if (done != 1) {
                                MaterialItem material = new MaterialItem(
                                    item.get("id").getAsInt(),
                                    item.get("uploader").getAsString(),
                                    item.get("name").getAsString(),
                                    item.get("description").getAsString(),
                                    done,
                                    item.get("upload_time").getAsLong()
                                );
                                loadedMaterials.add(material);
                            }
                        }
                        
                        // 在主线程中更新UI
                        client.execute(() -> {
                            this.materials = loadedMaterials;
                            this.loading = false;
                            this.errorMessage = null;
                            this.totalItems = loadedMaterials.size();
                            updateButtons();
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
                    updateButtons();
                });
            }
        });
        
        loaderThread.start();
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
    private void updateButtons() {
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
        prevButton.active = currentPage > 0;
        nextButton.active = currentPage < totalPages - 1;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);
        // 渲染标题
        context.drawText(textRenderer, "材料列表选择", width / 2 - textRenderer.getWidth("材料列表选择") / 2, 15, 0xFFFFFFFF, true);
        
        if (loading) {
            // 显示加载中
            context.drawTextWithShadow(textRenderer, "加载中...", 
                (width - textRenderer.getWidth("加载中...")) / 2, height / 2, 0xFFFFFFFF);
        } else if (errorMessage != null) {
            // 显示错误信息
            context.drawTextWithShadow(textRenderer, errorMessage, 
                (width - textRenderer.getWidth(errorMessage)) / 2, height / 2, 0xFFFF5555);
        } else {
            // 渲染材料列表
            renderMaterialList(context, mouseX, mouseY);
            
            // 显示页码信息
            int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);
            if (totalPages > 0) {
                String pageText = String.format("第 %d/%d 页", currentPage + 1, totalPages);
                context.drawTextWithShadow(textRenderer, pageText, 
                    width / 2 - textRenderer.getWidth(pageText) / 2, height - 50, 0xFFFFFFFF);
            }
        }

    }

    private void renderMaterialList(DrawContext context, int mouseX, int mouseY) {
        int startY = TOP_MARGIN;
        int itemWidth = width - 35;
        
        // 计算当前页的条目范围
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materials.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            MaterialItem material = materials.get(i);
            int itemIndex = i - startIndex; // 当前页中的索引
            int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);
            
            // 绘制列表项背景
            int backgroundColor = isMouseOverItem(mouseX, mouseY, itemY) ? 
                0x80AAAAAA : 0x80222222;
            context.fill(30, itemY, itemWidth, itemY + ITEM_HEIGHT, backgroundColor);
            
            // 绘制边框
            drawBorder(context, 30, itemY, itemWidth - 30, ITEM_HEIGHT, 0xFFFFFFFF);
            
            // 绘制文本
            String displayText = String.format("%s (%s)", material.name, material.uploader);
            context.drawTextWithShadow(textRenderer, displayText, 35, itemY + 10, 0xFFFFFFFF);
            
            // 在条目右侧绘制"选择"按钮
            int buttonX = itemWidth - 60;
            int buttonY = itemY + 5;
            int buttonWidth = 50;
            int buttonHeight = 20;
            
            // 检查鼠标是否悬停在按钮上
            boolean isButtonHovered = mouseX >= buttonX && mouseX <= buttonX + buttonWidth && 
                                     mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
            
            // 根据悬停状态设置按钮颜色
            int buttonColor = isButtonHovered ? 0xFF5555FF : 0xFF333333;
            context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonColor);
            drawBorder(context, buttonX, buttonY, buttonWidth, buttonHeight, 0xFFFFFFFF);
            
            // 绘制按钮文本
            String buttonText = "选择";
            int textX = buttonX + (buttonWidth - textRenderer.getWidth(buttonText)) / 2;
            int textY = buttonY + (buttonHeight - textRenderer.fontHeight) / 2;
            context.drawTextWithShadow(textRenderer, buttonText, textX, textY, 0xFFFFFFFF);
        }
    }

    private boolean isMouseOverItem(int mouseX, int mouseY, int itemY) {
        return mouseX >= 30 && mouseX <= width - 35 && 
               mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (!loading && errorMessage == null) {
            int startY = TOP_MARGIN;
            double mouseX = click.x();
            double mouseY = click.y();
            int itemWidth = width - 35;
            
            // 计算当前页的条目范围
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, materials.size());
            
            for (int i = startIndex; i < endIndex; i++) {
                int itemIndex = i - startIndex; // 当前页中的索引
                int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);
                
                // 检查是否点击了"选择"按钮
                int buttonX = itemWidth - 60;
                int buttonY = itemY + 5;
                int buttonWidth = 50;
                int buttonHeight = 20;
                
                if (mouseX >= buttonX && mouseX <= buttonX + buttonWidth && 
                    mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
                    MaterialItem selectedMaterial = materials.get(i);
                    AghelperClient.selectedMaterialId = selectedMaterial.id;
                    AghelperClient.selectedMaterialName = selectedMaterial.name; // 保存材料列表名称
                    client.setScreen(new MaterialsDash());
                    return true;
                }
            }
        }
        
        return super.mouseClicked(click, doubled);
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
    
    @Override
    public void removed() {
        super.removed();
    }
}