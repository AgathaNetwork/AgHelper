package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import com.google.gson.Gson;
import net.minecraft.util.Formatting;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class Supplies extends Screen {
    public Supplies() {
        super(Text.of("资源管理"));
    }
    
    private static final Gson GSON = new Gson();
    public int entryCount = 0;
    public boolean loaded = false;
    public List<ListData> centerListData = new ArrayList<>();
    public List<ListData> producerListData = new ArrayList<>();
    public List<ListData> storageListData = new ArrayList<>();
    public String LIST_API = "https://api-openid.agatha.org.cn/supply/getSupplyList";
    public String DETAIL_API = "https://api-openid.agatha.org.cn/supply/getSupplyDetail?id=";

    public int hasInfo = 0;
    public List<String> nameList = new ArrayList<>();
    public int facilityId = -1;
    public String facilityPosition = "";
    public String facilityName = "";
    public String facilityMaintainer = "";
    public String facilityDescription = "";

    // 分页相关
    private int currentPage = 0;
    private int itemsPerPage = 10;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private static final int ITEM_HEIGHT = 20;
    private static final int ITEM_SPACING = 5;
    private static final int TOP_MARGIN = 60;
    private static final int BOTTOM_MARGIN = 50;
    
    // 弹窗相关
    private boolean showDetailPopup = false;
    
    // 搜索框
    private TextFieldWidget idInput;
    
    // 搜索过滤后的列表
    private List<ListData> filteredList = new ArrayList<>();

    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());

        // 添加一个输入框
        idInput = new TextFieldWidget(textRenderer, 60, 10, width - 100, 20, null);
        idInput.setPlaceholder(Text.of("输入ID或名称进行搜索"));
        idInput.setChangedListener(this::onSearchTextChanged);
        addDrawableChild(idInput);
        
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
            }
        }).dimensions(width / 2 - buttonWidth - 5, buttonY, buttonWidth, buttonHeight).build();
        
        nextButton = ButtonWidget.builder(Text.of("下一页"), button -> {
            int totalPages = getTotalPages();
            if (currentPage < totalPages - 1) {
                currentPage++;
            }
        }).dimensions(width / 2 + 5, buttonY, buttonWidth, buttonHeight).build();
        
        addDrawableChild(prevButton);
        addDrawableChild(nextButton);
        
        // 异步执行，发送 HTTP GET
        new Thread(() -> {
            if (!loaded) {
                try {
                    URL url = new URL(LIST_API);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = in.readLine()) != null) {
                            response.append(line);
                        }
                        in.close();
                        String json = response.toString();
                        // 处理JSON数据
                        ResponseData responseData = GSON.fromJson(json, ResponseData.class);
                        // 获取条目数
                        entryCount = responseData.data.length;
                        ListData[] listData = responseData.data;
                        for (ListData data : listData) {
                            if (data.type.equalsIgnoreCase("center")){
                                // 添加到center
                                centerListData.add(data);
                            }
                            else if (data.type.equalsIgnoreCase("producer")){
                                // 添加到producer
                                producerListData.add(data);
                            }
                            else if (data.type.equalsIgnoreCase("storage")){
                                // storage
                                storageListData.add(data);
                            }
                        }
                        loaded = true;
                        
                        // 初始化过滤列表为所有设施
                        filteredList.addAll(getAllFacilities());
                        
                        // 在主线程更新UI
                        client.execute(() -> {
                            updateButtons();
                        });
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    
    private void onSearchTextChanged(String text) {
        // 根据输入文本过滤列表
        filterFacilities(text);
        
        // 重置到第一页
        currentPage = 0;
        
        // 判断是否是正整数
        if (isPositiveInteger(text)){
            // 输入了ID，按ID进行查询
            queryData(Integer.parseInt(text));
        }
        else if (text.isEmpty()){
            hasInfo = 0;
            showDetailPopup = false;
        }
        else{
            // 模糊搜索
            int selectedId = -1;
            List<String> nameList = new ArrayList<>();
            for (ListData data : centerListData) {
                if (data.content.contains(text)) {
                    nameList.add(data.content);
                    selectedId = Integer.parseInt(data.id);
                }
            }
            for (ListData data : producerListData){
                if (data.content.contains(text)) {
                    nameList.add(data.content);
                    selectedId = Integer.parseInt(data.id);
                }
            }
            for (ListData data : storageListData){
                if (data.content.contains(text)) {
                    nameList.add(data.content);
                    selectedId = Integer.parseInt(data.id);
                }
            }
            this.nameList = nameList;
            if (nameList.size() > 1) {
                // 有多个候选选项
                this.hasInfo = -1;
            }
            else if (nameList.isEmpty()) {
                // 没有候选选项
                this.hasInfo = 0;
            }
            else {
                this.hasInfo = 2;
                queryData(selectedId);
            }
        }
    }
    
    private void filterFacilities(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            // 如果搜索文本为空，则显示所有设施
            filteredList = new ArrayList<>(getAllFacilities());
        } else {
            // 根据搜索文本过滤设施列表
            filteredList = getAllFacilities().stream()
                    .filter(facility -> facility.content.contains(searchText) || facility.id.contains(searchText))
                    .collect(Collectors.toList());
        }
    }
    
    private void updateButtons() {
        // 重新计算翻页按钮位置
        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonY = height - 30;
        
        prevButton.setX(width / 2 - buttonWidth - 5);
        prevButton.setY(buttonY);
        
        nextButton.setX(width / 2 + 5);
        nextButton.setY(buttonY);
        
        // 更新搜索框大小
        idInput.setWidth(width - 100);

    }
    
    private void clearFacilityButtons() {
        // 这里应该移除之前添加的设施按钮
        // 但由于Minecraft的GUI系统限制，我们无法直接做到这一点
        // 所以我们将在render方法中处理按钮的显示/隐藏
    }
    
    private List<ListData> getAllFacilities() {
        List<ListData> all = new ArrayList<>();
        all.addAll(centerListData);
        all.addAll(producerListData);
        all.addAll(storageListData);
        return all;
    }
    
    private int getTotalPages() {
        int totalItems = filteredList.size();
        return (int) Math.ceil((double) totalItems / itemsPerPage);
    }
    
    private void showFacilityDetails(ListData facility) {
        queryData(Integer.parseInt(facility.id));
        showDetailPopup = true;
    }

    private static class ResponseData {
        ListData[] data;
    }
    
    private static class ListData {
        String id;
        String content;
        String type;
        String status;

    }
    
    private static class DetailData {
        String maintainer;
        String world;
        String x;
        String y;
        String z;
        String efficiency;
        String content;
        String type;
        String confirmation;
        String message;
        String status;
    }
    
    // 判断字符串是否是正整数的方法
    public static boolean isPositiveInteger(String s) {
        // 检查字符串非空且只包含数字，并且不以0开头（多位数），并且大于0
        return s != null && !s.isEmpty() && s.matches("\\d+") && (s.length() == 1 || !s.startsWith("0"));
    }
    
    public void queryData(int id){
        this.facilityId = id;
        new Thread(() -> {
            try {
                URL url = new URL(DETAIL_API + id);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    String json = response.toString();
                    // 处理JSON数据
                    DetailData detailData = GSON.fromJson(json, DetailData.class);

                    if (detailData.status.equals("1")){
                        this.facilityDescription = detailData.message;
                        this.facilityName = detailData.content;
                        this.facilityMaintainer = detailData.maintainer;
                        String worldName = "未知世界";
                        if (detailData.world.equals("world")){
                            worldName = "主世界";
                        }
                        else if (detailData.world.equals("world_the_end")){
                            worldName = "末地";
                        }
                        else if (detailData.world.equals("world_nether")){
                            worldName = "下界";
                        }
                        this.facilityPosition = worldName + " " + detailData.x + ", " + detailData.y + ", " + detailData.z;
                        this.hasInfo = 1;
                    }
                    else{
                        this.hasInfo = 0;
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        
        // 渲染标题
        context.drawText(textRenderer, "资源设施列表", (width - textRenderer.getWidth("资源设施列表")) / 2, 35, 0xFFFFFFFF, false);
        
        if (loaded) {
            // 渲染分页信息
            int totalPages = getTotalPages();
            if (totalPages > 0) {
                String pageText = String.format("第 %d/%d 页", currentPage + 1, totalPages);
                context.drawText(textRenderer, pageText, 
                    width / 2 - textRenderer.getWidth(pageText) / 2, height - 45, 0xFFFFFFFF, false);
            }
            
            // 渲染设施列表
            renderFacilityList(context, mouseX, mouseY);
        } else {
            context.drawText(textRenderer, "加载中...", (width - textRenderer.getWidth("加载中...")) / 2, height / 2, 0xFFFFFFFF, false);
        }
        
        // 渲染弹窗
        if (showDetailPopup && hasInfo == 1) {
            renderDetailPopup(context);
        }
    }
    
    private void renderDetailPopup(DrawContext context) {
        int popupWidth = 300;
        int popupHeight = 200;
        int popupX = (width - popupWidth) / 2;
        int popupY = (height - popupHeight) / 2;
        
        // 绘制半透明背景
        context.fill(0, 0, width, height, 0x80000000);
        
        // 绘制弹窗背景
        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF202020);
        drawBorder(context, popupX, popupY, popupWidth, popupHeight, 0xFFFFFFFF);
        
        // 绘制详细信息
        int textX = popupX + 10;
        int textY = popupY + 10;
        
        context.drawText(textRenderer, "ID：" + facilityId, textX, textY, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "名称：" + facilityName, textX, textY + 20, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "维护者：" + facilityMaintainer, textX, textY + 32, 0xFFFFFFFF, false);
        context.drawText(textRenderer, "坐标：" + facilityPosition, textX, textY + 44, 0xFFFFFFFF, false);
        
        // 备注信息
        int maxWidth = popupWidth - 60;
        List<OrderedText> wrappedTexts = textRenderer.wrapLines(Text.literal(facilityDescription), maxWidth);
        for (int i = 0; i < wrappedTexts.size(); i++) {
            context.drawText(textRenderer,
                    i == 0 ? "备注：" : "      ",
                    textX, textY + 56 + i * 12, 0xFFFFFFFF, false);
            context.drawText(textRenderer,
                    wrappedTexts.get(i),
                    textX + 40, textY + 56 + i * 12, 0xFFFFFFFF, false);
        }
        
        // 绘制传送按钮 (仅当资源可用时显示)
        int teleportButtonX = popupX + popupWidth / 2 - 60;
        int teleportButtonY = popupY + popupHeight - 30;
        int closeButtonX = popupX + popupWidth / 2 + 10;
        int closeButtonY = popupY + popupHeight - 30;
        
        // 绘制传送按钮
        context.fill(teleportButtonX, teleportButtonY, teleportButtonX + 50, teleportButtonY + 20, 0xFF404040);
        drawBorder(context, teleportButtonX, teleportButtonY, 50, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "传送", teleportButtonX + 25, teleportButtonY + 5, 0xFFFFFFFF);
        
        // 绘制关闭按钮
        context.fill(closeButtonX, closeButtonY, closeButtonX + 50, closeButtonY + 20, 0xFF404040);
        drawBorder(context, closeButtonX, closeButtonY, 50, 20, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, "关闭", closeButtonX + 25, closeButtonY + 5, 0xFFFFFFFF);
    }
    
    private void renderFacilityList(DrawContext context, int mouseX, int mouseY) {
        int startY = TOP_MARGIN;
        
        // 计算当前页的条目范围
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredList.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            ListData facility = filteredList.get(i);
            int itemIndex = i - startIndex;
            int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);
            
            // 绘制背景
            int backgroundColor = isMouseOverItem(mouseX, mouseY, itemY) ? 
                0x80AAAAAA : 0x80222222;
            context.fill(60, itemY, width - 60, itemY + ITEM_HEIGHT, backgroundColor);
            
            // 绘制边框 (修正宽度计算，应该与背景一致)
            drawBorder(context, 60, itemY, width - 120, ITEM_HEIGHT, 0xFFFFFFFF);
            
            // 绘制文本
            context.drawText(textRenderer, facility.content, 65, itemY + 6, 0xFFFFFFFF, false);
            
            // 绘制状态
            int statusX = width - 80;
            if (Objects.equals(facility.status, "1")){
                context.drawText(textRenderer, "✓", statusX, itemY + 6, 0xFF00FF00, false);
            }
            else{
                context.drawText(textRenderer, "✗", statusX, itemY + 6, 0xFFFF0000, false);
            }
        }
    }
    
    private boolean isMouseOverItem(int mouseX, int mouseY, int itemY) {
        return mouseX >= 60 && mouseX <= width - 60 && 
               mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
    }
    
    public void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        // 绘制边框（通过绘制四条边）
        int borderWidth = 1;

        // 上边
        context.fill(x, y, x + width, y + borderWidth, color);
        // 下边
        context.fill(x, y + height - borderWidth, x + width, y + height, color);
        // 左边
        context.fill(x, y, x + borderWidth, y + height, color);
        // 右边
        context.fill(x + width - borderWidth, y, x + width, y + height, color);
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
        }
    }
    
    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        // 如果弹窗显示中，只处理弹窗内的点击事件
        if (showDetailPopup && hasInfo == 1) {
            int popupWidth = 300;
            int popupHeight = 200;
            int popupX = (width - popupWidth) / 2;
            int popupY = (height - popupHeight) / 2;
            
            // 检查是否点击了传送按钮
            int teleportButtonX = popupX + popupWidth / 2 - 60;
            int teleportButtonY = popupY + popupHeight - 30;
            if (mouseX >= teleportButtonX && mouseX <= teleportButtonX + 50 &&
                mouseY >= teleportButtonY && mouseY <= teleportButtonY + 20) {
                assert MinecraftClient.getInstance().player != null;
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand("supply " + facilityId);
                assert client != null;
                client.setScreen(null);
                return true;
            }
            
            // 检查是否点击了关闭按钮
            int closeButtonX = popupX + popupWidth / 2 + 10;
            int closeButtonY = popupY + popupHeight - 30;
            if (mouseX >= closeButtonX && mouseX <= closeButtonX + 50 &&
                mouseY >= closeButtonY && mouseY <= closeButtonY + 20) {
                showDetailPopup = false;
                return true;
            }
            
            // 检查是否点击了弹窗区域（但不是按钮）
            if (mouseX >= popupX && mouseX <= popupX + popupWidth &&
                mouseY >= popupY && mouseY <= popupY + popupHeight) {
                return true; // 消费点击事件，不传递给后面的内容
            }
            
            // 点击弹窗外部区域也关闭弹窗
            showDetailPopup = false;
            return true;
        }
        
        // 处理设施列表项的点击
        if (loaded) {
            handleFacilityListClick(mouseX, mouseY);
        }
        
        return super.mouseClicked(click, doubled);
    }
    
    private void handleFacilityListClick(double mouseX, double mouseY) {
        int startY = TOP_MARGIN;
        
        // 计算当前页的条目范围
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredList.size());
        
        for (int i = startIndex; i < endIndex; i++) {
            ListData facility = filteredList.get(i);
            int itemIndex = i - startIndex;
            int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);
            
            // 检查是否点击了这个设施项（确保与渲染区域一致）
            if (mouseX >= 60 && mouseX <= width - 60 && 
                mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT) {
                showFacilityDetails(facility);
                break;
            }
        }
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 使用鼠标滚轮进行翻页
        if (verticalAmount > 0) {
            // 向上滚动，前往上一页
            if (currentPage > 0) {
                currentPage--;
            }
        } else if (verticalAmount < 0) {
            // 向下滚动，前往下一页
            int totalPages = getTotalPages();
            if (currentPage < totalPages - 1) {
                currentPage++;
            }
        }
        
        return true;
    }
}