package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
import cn.org.agatha.aghelper.client.MenuScreen;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
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
    private ScrollableWidget scrollableWidget;
    private int contentHeight = 0;
    private static final int ITEM_HEIGHT = 30;
    private static final int ITEM_SPACING = 5;
    private double currentScrollY = 0;

    public MaterialsList() {
        super(Text.of("材料列表选择"));
    }
    
    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MaterialsDash()))
                .dimensions(10, 10, 40, 20)
                .build());

        // 创建滚动容器
        this.scrollableWidget = new ScrollableWidget(
                20,  // x位置
                40,  // y位置
                width - 40,  // 宽度
                height - 60, // 高度
                Text.literal("")) {
            
            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {
                // 不需要实现
            }

            @Override
            protected int getContentsHeight() {
                return contentHeight;
            }

            @Override
            protected double getDeltaYPerScroll() {
                return 15.0; // 每次滚动的距离
            }

            @Override
            protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
                renderMaterialList(context, mouseX, mouseY);
            }
            
            // 重写setScrollY方法来跟踪滚动位置
            @Override
            protected void setScrollY(double scrollY) {
                super.setScrollY(scrollY);
                currentScrollY = scrollY;
            }
        };
        
        this.addDrawableChild(scrollableWidget);

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
                            // 更新内容高度
                            this.contentHeight = Math.max(0, loadedMaterials.size() * (ITEM_HEIGHT + ITEM_SPACING) - ITEM_SPACING);
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

    private void renderMaterialList(DrawContext context, int mouseX, int mouseY) {
        if (loading) {
            // 显示加载中
            context.drawTextWithShadow(textRenderer, "加载中...", 
                (width - textRenderer.getWidth("加载中...")) / 2, 10, 0xFFFFFF);
        } else if (errorMessage != null) {
            // 显示错误信息
            context.drawTextWithShadow(textRenderer, errorMessage, 
                (width - textRenderer.getWidth(errorMessage)) / 2, 10, 0xFF5555);
        } else {
            // 渲染材料列表
            int startY = 50; // 向下移动50 (原来30 + 新增20)
            int itemWidth = width - 35; // 扩展宽度以覆盖完整区域
            
            for (int i = 0; i < materials.size(); i++) {
                MaterialItem material = materials.get(i);
                int itemY = startY + i * (ITEM_HEIGHT + ITEM_SPACING);
                
                // 绘制列表项背景，向右移动30
                int backgroundColor = isMouseOverItem(mouseX, mouseY, itemY) ? 
                    0x80AAAAAA : 0x80222222;
                context.fill(30, itemY, itemWidth, itemY + ITEM_HEIGHT, backgroundColor);
                
                // 绘制边框，向右移动30
                context.drawBorder(30, itemY, itemWidth - 30, ITEM_HEIGHT, 0xFFFFFFFF);
                
                // 绘制文本，向右移动30
                String displayText = String.format("%s (%s)", material.name, material.uploader);
                context.drawTextWithShadow(textRenderer, displayText, 35, itemY + 10, 0xFFFFFF);
            }
        }
    }

    private boolean isMouseOverItem(int mouseX, int mouseY, int itemY) {
        // 考虑滚动偏移量和位置调整
        int adjustedMouseY = (int) (mouseY + currentScrollY);
        return mouseX >= 30 && mouseX <= width - 35 &&
               adjustedMouseY >= itemY && adjustedMouseY <= itemY + ITEM_HEIGHT;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        // 渲染标题
        context.drawText(textRenderer, "材料列表选择", width / 2, 15, 0xFFFFFF, true);

    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!loading && errorMessage == null) {
            // 检查是否点击了滚动区域内的项目，考虑位置调整
            if (mouseX >= 50 && mouseX <= width - 20 && mouseY >= 80 && mouseY <= height - 20) {
                // 让滚动部件先处理点击事件
                if (scrollableWidget.mouseClicked(mouseX, mouseY, button)) {
                    return true;
                }
                
                // 计算点击的项目索引，考虑位置调整
                double adjustedMouseY = mouseY - 90 + currentScrollY; // 调整为新的起始位置
                int index = (int) (adjustedMouseY / (ITEM_HEIGHT + ITEM_SPACING));
                
                if (index >= 0 && index < materials.size()) {
                    MaterialItem selectedMaterial = materials.get(index);
                    AghelperClient.selectedMaterialId = selectedMaterial.id;
                    client.setScreen(new MaterialsDash());
                    return true;
                }
            }
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 让滚动部件处理滚动事件
        if (scrollableWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
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