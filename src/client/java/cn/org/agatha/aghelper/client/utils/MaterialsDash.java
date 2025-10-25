package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.AghelperClient;
import cn.org.agatha.aghelper.client.MenuScreen;
import cn.org.agatha.aghelper.client.utils.OccupiedItemsHUD;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MaterialsDash extends Screen {
    private static class MaterialDetailItem {
        String name;
        int count;
        String occupied;
        int done;
        long donetime;
        String doneby;

        MaterialDetailItem(String name, int count, String occupied, int done, long donetime, String doneby) {
            this.name = name;
            this.count = count;
            this.occupied = occupied;
            this.done = done;
            this.donetime = donetime;
            this.doneby = doneby;
        }
    }

    // 添加按钮列表用于存储每个物品的按钮
    private List<ButtonWidget> itemButtons = new ArrayList<>();
    
    private List<MaterialDetailItem> materials = new ArrayList<>();
    private boolean loading = false;
    private String errorMessage = null;
    private int currentPage = 0;
    private int itemsPerPage = 0;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private static final int ITEM_HEIGHT = 30;
    private static final int ITEM_SPACING = 5;
    private static final int TOP_MARGIN = 80;
    private static final int BOTTOM_MARGIN = 30;

    public MaterialsDash() {
        super(Text.of("材料列表查看"));
    }
    
    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());

        addDrawableChild(ButtonWidget.builder(Text.of("选择材料列表"), button -> client.setScreen(new MaterialsList()))
                .dimensions(60, 10, 80, 20)
                .build());

        //如果选择了材料列表，那么显示刷新按钮
        if (AghelperClient.selectedMaterialId != -1) {
            addDrawableChild(ButtonWidget.builder(Text.of("刷新"), button -> {
                loadMaterialDetails();
                    })
                .dimensions(150, 10, 40, 20)
                .build());
        }

        // 计算每页可以显示的条目数量
        int availableHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        itemsPerPage = Math.max(1, availableHeight / (ITEM_HEIGHT + ITEM_SPACING));
        
        // 添加翻页按钮
        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonY = height - 30;
        
        prevButton = ButtonWidget.builder(Text.of("上一页"), button -> {
            if (currentPage > 0) {
                currentPage--;
                updateButtons();
            }
        }).dimensions(width / 2 - buttonWidth - 5, buttonY, buttonWidth, buttonHeight).build();
        
        nextButton = ButtonWidget.builder(Text.of("下一页"), button -> {
            int totalPages = (int) Math.ceil((double) materials.size() / itemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateButtons();
            }
        }).dimensions(width / 2 + 5, buttonY, buttonWidth, buttonHeight).build();
        
        addDrawableChild(prevButton);
        addDrawableChild(nextButton);
        
        // 初始化按钮状态
        updateButtons();
        
        // 如果已选择材料列表，则自动加载详情
        if (AghelperClient.selectedMaterialId != -1) {
            loadMaterialDetails();
        }
    }

    private void clearItemButtons() {
        for (ButtonWidget button : itemButtons) {
            remove(button);
        }
        itemButtons.clear();
    }
    
    private void addItemButtons() {
        clearItemButtons();
        
        int startY = TOP_MARGIN;
        
        // 计算当前页的条目范围
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materials.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            MaterialDetailItem material = materials.get(i);
            int itemIndex = i - startIndex; // 当前页中的索引
            int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);

            ButtonWidget itemButton = ButtonWidget.builder(
                            Text.of(material.done == 1 ? "取消完成" : "完成"),
                            button -> onItemButtonClick(material, button))
                    .dimensions(width - 85, itemY + 5, 60, 20)
                    .build();

            addDrawableChild(itemButton);
            itemButtons.add(itemButton);

            // 为每个物品创建按钮（仅对未完成的条目）
            if (material.done != 1) {
                // 如果物品未完成且未被占用，则添加"领取"按钮
                if (material.occupied == null || material.occupied.isEmpty()) {
                    ButtonWidget occupyButton = ButtonWidget.builder(
                        Text.of("领取"),
                        button -> onOccupyButtonClick(material, button))
                        .dimensions(width - 150, itemY + 5, 60, 20)
                        .build();
                    
                    addDrawableChild(occupyButton);
                    itemButtons.add(occupyButton);
                } 
                // 如果物品被当前用户占用，则添加"取消领取"按钮
                else if (material.occupied != null && !material.occupied.isEmpty() && 
                         material.occupied.equals(MinecraftClient.getInstance().getSession().getUsername())) {
                    ButtonWidget unoccupyButton = ButtonWidget.builder(
                        Text.of("取消领取"),
                        button -> onUnoccupyButtonClick(material, button))
                        .dimensions(width - 150, itemY + 5, 60, 20)
                        .build();
                    
                    addDrawableChild(unoccupyButton);
                    itemButtons.add(unoccupyButton);
                }
            }
        }
    }
    
    private void onItemButtonClick(MaterialDetailItem material, ButtonWidget button) {
        if (material.done == 1) {
            // 取消完成
            setItemUndone(material, button);
        } else {
            // 设置为完成
            setItemDone(material, button);
        }
    }
    
    private void onOccupyButtonClick(MaterialDetailItem material, ButtonWidget button) {
        // 领取任务
        setItemOccupied(material, button);
    }
    
    private void onUnoccupyButtonClick(MaterialDetailItem material, ButtonWidget button) {
        // 取消领取任务
        setItemUnoccupied(material, button);
    }
    
    private void setItemDone(MaterialDetailItem material, ButtonWidget button) {
        button.active = false;
        button.setMessage(Text.of("处理中..."));
        
        Thread apiThread = new Thread(() -> {
            try {
                String playerName = MinecraftClient.getInstance().getSession().getUsername();
                // 对URL中的参数进行UTF-8编码
                String encodedName = java.net.URLEncoder.encode(material.name, "UTF-8");
                URL url = new URL("https://api-materials.agatha.org.cn/mod/setDone?id=" + AghelperClient.selectedMaterialId + "&name=" + encodedName + "&doneby=" + playerName);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                    
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    if (response.has("doneby")) {
                        String doneby = response.get("doneby").getAsString();
                        
                        // 在主线程中更新UI
                        client.execute(() -> {
                            material.done = 1;
                            material.doneby = doneby;
                            button.setMessage(Text.of("取消完成"));
                            button.active = true;
                        });
                    }
                } else {
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                client.execute(() -> {
                    button.setMessage(Text.of("重试"));
                    button.active = true;
                    // 可以添加错误提示
                });
            }
        });
        
        apiThread.start();
    }
    
    private void setItemUndone(MaterialDetailItem material, ButtonWidget button) {
        button.active = false;
        button.setMessage(Text.of("处理中..."));
        
        Thread apiThread = new Thread(() -> {
            try {
                // 对URL中的参数进行UTF-8编码
                String encodedName = java.net.URLEncoder.encode(material.name, "UTF-8");
                URL url = new URL("https://api-materials.agatha.org.cn/mod/setUnDone?id=" + AghelperClient.selectedMaterialId + "&name=" + encodedName);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // 在主线程中更新UI
                    client.execute(() -> {
                        material.done = 0;
                        material.doneby = "";
                        button.setMessage(Text.of("完成"));
                        button.active = true;
                    });
                } else {
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                client.execute(() -> {
                    button.setMessage(Text.of("重试"));
                    button.active = true;
                    // 可以添加错误提示
                });
            }
        });
        
        apiThread.start();
    }
    
    private void setItemOccupied(MaterialDetailItem material, ButtonWidget button) {
        button.active = false;
        button.setMessage(Text.of("处理中..."));
        
        Thread apiThread = new Thread(() -> {
            try {
                String playerName = MinecraftClient.getInstance().getSession().getUsername();
                // 对URL中的参数进行UTF-8编码
                String encodedName = java.net.URLEncoder.encode(material.name, "UTF-8");
                URL url = new URL("https://api-materials.agatha.org.cn/mod/setOccupied?id=" + AghelperClient.selectedMaterialId + "&name=" + encodedName + "&user=" + playerName);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    InputStream inputStream = connection.getInputStream();
                    InputStreamReader reader = new InputStreamReader(inputStream, "UTF-8");
                    
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    if (response.has("user")) {
                        String user = response.get("user").getAsString();
                        
                        // 在主线程中更新UI
                        client.execute(() -> {
                            material.occupied = user;
                            button.setMessage(Text.of("取消领取"));
                            button.active = true;
                            // 重新添加按钮以更新按钮状态
                            updateButtons();
                            
                            // 显示HUD通知
                            OccupiedItemsHUD.getInstance().addOccupiedItem(material.name + " *" + material.count);
                        });
                    }
                } else {
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                client.execute(() -> {
                    button.setMessage(Text.of("重试"));
                    button.active = true;
                    // 可以添加错误提示
                });
            }
        });
        
        apiThread.start();
    }
    
    private void setItemUnoccupied(MaterialDetailItem material, ButtonWidget button) {
        button.active = false;
        button.setMessage(Text.of("处理中..."));
        
        Thread apiThread = new Thread(() -> {
            try {
                String playerName = MinecraftClient.getInstance().getSession().getUsername();
                // 对URL中的参数进行UTF-8编码
                String encodedName = java.net.URLEncoder.encode(material.name, "UTF-8");
                URL url = new URL("https://api-materials.agatha.org.cn/mod/setUnOccupied?id=" + AghelperClient.selectedMaterialId + "&name=" + encodedName + "&user=" + playerName);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    // 在主线程中更新UI
                    client.execute(() -> {
                        material.occupied = "";
                        button.setMessage(Text.of("领取"));
                        button.active = true;
                        // 重新添加按钮以更新按钮状态
                        updateButtons();
                        
                        // 从HUD中移除通知
                        OccupiedItemsHUD.getInstance().removeOccupiedItem(material.name + " *" + material.count);
                    });
                } else {
                    throw new IOException("HTTP Error: " + responseCode);
                }
            } catch (Exception e) {
                client.execute(() -> {
                    button.setMessage(Text.of("重试"));
                    button.active = true;
                    // 可以添加错误提示
                });
            }
        });
        
        apiThread.start();
    }
    
    private void loadMaterialDetails() {
        if (AghelperClient.selectedMaterialId == -1) {
            return;
        }
        
        loading = true;
        errorMessage = null;
        materials.clear();
        currentPage = 0;
        clearItemButtons();
        updateButtons();
        
        // 设置HUD的材料列表ID，以便HUD可以自动更新
        OccupiedItemsHUD.getInstance().setMaterialId(AghelperClient.selectedMaterialId);
        
        // 在后台线程中加载数据
        Thread loaderThread = new Thread(() -> {
            try {
                URL url = new URL("https://api-materials.agatha.org.cn/mod/detail?id=" + AghelperClient.selectedMaterialId);
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
                            
                            // 将未完成的条目排在前面，已完成的条目排在后面
                            List<MaterialDetailItem> sortedMaterials = new ArrayList<>();
                            List<MaterialDetailItem> unfinishedMaterials = new ArrayList<>();
                            List<MaterialDetailItem> finishedMaterials = new ArrayList<>();
                            
                            for (MaterialDetailItem item : loadedMaterials) {
                                if (item.done != 1) {
                                    unfinishedMaterials.add(item);
                                } else {
                                    finishedMaterials.add(item);
                                }
                            }
                            
                            // 先添加未完成的条目，再添加已完成的条目
                            sortedMaterials.addAll(unfinishedMaterials);
                            sortedMaterials.addAll(finishedMaterials);
                            
                            // 在主线程中更新UI
                            client.execute(() -> {
                                this.materials = sortedMaterials;
                                this.loading = false;
                                this.errorMessage = null;
                                updateButtons();
                                addItemButtons(); // 添加物品按钮
                            });
                        } else {
                            throw new IOException("Invalid response format: missing json field");
                        }
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
                    clearItemButtons();
                });
            }
        });
        
        loaderThread.start();
    }

    private void updateButtons() {
        int totalPages = (int) Math.ceil((double) materials.size() / itemsPerPage);
        prevButton.active = currentPage > 0;
        nextButton.active = currentPage < totalPages - 1 && !loading;
        
        // 更新物品按钮
        addItemButtons();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);

        super.render(context, mouseX, mouseY, delta);
        
        // 显示材料列表选择状态
        String statusText;
        if (AghelperClient.selectedMaterialId == -1) {
            statusText = "当前未选择材料列表";
        } else {
            statusText = "当前选择的材料列表: " + AghelperClient.selectedMaterialName;
        }
        
        // 在页面顶部居中显示状态
        int textWidth = textRenderer.getWidth(statusText);
        context.drawText(textRenderer, statusText, (width - textWidth) / 2, 40, 0xFFFFFF, true);
        
        // 显示材料详情
        if (loading) {
            // 显示加载中
            context.drawTextWithShadow(textRenderer, "加载中...", 
                (width - textRenderer.getWidth("加载中...")) / 2, height / 2, 0xFFFFFF);
        } else if (errorMessage != null) {
            // 显示错误信息
            context.drawTextWithShadow(textRenderer, errorMessage, 
                (width - textRenderer.getWidth(errorMessage)) / 2, height / 2, 0xFF5555);
        } else if (!materials.isEmpty()) {
            // 渲染材料列表
            renderMaterialList(context, mouseX, mouseY);
            
            // 显示页码信息
            int totalPages = (int) Math.ceil((double) materials.size() / itemsPerPage);
            if (totalPages > 0) {
                String pageText = String.format("第 %d/%d 页", currentPage + 1, totalPages);
                context.drawTextWithShadow(textRenderer, pageText, 
                    width / 2 - textRenderer.getWidth(pageText) / 2, height - 50, 0xFFFFFF);
            }
        } else if (AghelperClient.selectedMaterialId != -1) {
            // 显示无数据提示信息
            context.drawTextWithShadow(textRenderer, "该材料列表暂无数据", 
                (width - textRenderer.getWidth("该材料列表暂无数据")) / 2, height / 2, 0xFFFFFF);
        }

    }

    private void renderMaterialList(DrawContext context, int mouseX, int mouseY) {
        int startY = TOP_MARGIN;
        int itemWidth = width - 20;
        
        // 计算当前页的条目范围
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, materials.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            MaterialDetailItem material = materials.get(i);
            int itemIndex = i - startIndex; // 当前页中的索引
            int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);
            
            // 绘制列表项背景
            int backgroundColor = isMouseOverItem(mouseX, mouseY, itemY) ? 
                0x80AAAAAA : 0x80222222;
            context.fill(20, itemY, itemWidth, itemY + ITEM_HEIGHT, backgroundColor);
            
            // 绘制边框
            context.drawBorder(20, itemY, itemWidth - 20, ITEM_HEIGHT, 0xFFFFFFFF);
            
            // 根据完成状态设置文本颜色
            int textColor = material.done == 1 ? 0x888888 : 0xFFFFFF; // 已完成的条目用灰色显示
            
            // 绘制文本
            String displayText = String.format("%s *%d", material.name, material.count);
            // 如果项目已完成，添加删除线效果
            if (material.done == 1) {
                displayText = "§m" + displayText; // Minecraft中的删除线格式代码
            }
            context.drawTextWithShadow(textRenderer, displayText, 25, itemY + 10, textColor);
            
            // 显示占用状态信息（从右侧开始计算位置）
            if (material.done != 1 && material.occupied != null && !material.occupied.isEmpty()) {
                String occupiedByText = "被 " + material.occupied + " 领取";
                int occupiedTextWidth = textRenderer.getWidth(occupiedByText);
                // 从右侧按钮的左侧开始计算位置，确保不会重叠
                int occupiedTextX = width - 150 - occupiedTextWidth - 5;
                context.drawTextWithShadow(textRenderer, occupiedByText, occupiedTextX, itemY + 10, 0xFFFF00); // 黄色显示
            }
        }
    }

    private boolean isMouseOverItem(int mouseX, int mouseY, int itemY) {
        return mouseX >= 20 && mouseX <= width - 20 && 
               mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
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
    public void tick() {
        super.tick();
        // 页面大小可能已改变，重新计算每页条目数
        int availableHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        int newItemsPerPage = Math.max(1, availableHeight / (ITEM_HEIGHT + ITEM_SPACING));
        
        if (newItemsPerPage != itemsPerPage) {
            itemsPerPage = newItemsPerPage;
            updateButtons();
            addItemButtons(); // 重新添加物品按钮
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 使用鼠标滚轮进行翻页
        if (verticalAmount > 0) {
            // 向上滚动，前往上一页
            if (currentPage > 0) {
                currentPage--;
                updateButtons();
            }
        } else if (verticalAmount < 0) {
            // 向下滚动，前往下一页
            int totalPages = (int) Math.ceil((double) materials.size() / itemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                updateButtons();
            }
        }
        
        return true;
    }
}