package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ScrollableWidget;
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

public class Supplies extends Screen {
    public Supplies() {
        super(Text.of("资源管理"));
    }
    private ScrollableWidget scrollableWidget;
    private static final Gson GSON = new Gson();
    public int entryCount = 0;
    public boolean loaded = false;
    public List<ListData> centerListData = new ArrayList<>();
    public List<ListData> producerListData = new ArrayList<>();
    public List<ListData> storageListData = new ArrayList<>();
    public String LIST_API = "https://api-openid.agatha.org.cn/supply/getSupplyList";

    public int pickedId = -1;
    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());
        // 列表组件
        // 创建滚动容器，位置和大小可以根据需要调整
        this.scrollableWidget = new ScrollableWidget(
                20,  // x位置
                40,  // y位置
                100,                   // 宽度
                height - 60,           // 高度
                Text.literal("")
        ) {
            @Override
            protected void appendClickableNarrations(NarrationMessageBuilder builder) {

            }

            @Override
            protected void renderContents(DrawContext context, int mouseX, int mouseY, float delta) {
                // 在这里渲染您的内容
                renderScrollableContent(context, mouseX, mouseY, delta);
            }

            @Override
            protected int getContentsHeight() {
                // 返回内容的总高度，如果超过容器高度就会显示滚动条
                return calculateContentHeight();
            }

            @Override
            protected double getDeltaYPerScroll() {
                return 0;
            }
        };

        this.addDrawableChild(scrollableWidget);

        // 异步执行，发送 HTTP GET
        new Thread(() -> {

            if (loaded == false){

                try {
                    URL url = new URL(LIST_API);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.connect();
                    int responseCode = connection.getResponseCode();
                    if (responseCode == 200) {
                        loaded = true;
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
                    }

                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
    private void renderScrollableContent(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染您的滚动内容
        TextRenderer textRenderer = this.textRenderer;

        int offset = 62;
        context.drawText(textRenderer,
                Text.literal("储存中心").formatted(Formatting.AQUA),
                30, offset - 12, 0xFF000000, false);
        for (int i = 0; i < centerListData.size(); i++) {
            context.drawText(textRenderer,
                    Text.literal(centerListData.get(i).content),
                    40, i * 12 + offset, 0xFFFFFF, false);
        }
        offset += 12 * centerListData.size() + 20;
        context.drawText(textRenderer,
                Text.literal("生产").formatted(Formatting.AQUA),
                30, offset - 12, 0xFF000000, false);
        for (int i = 0; i < producerListData.size(); i++){
            context.drawText(textRenderer,
                    Text.literal(producerListData.get(i).content),
                    40, i * 12 + offset, 0xFFFFFF, false);
        }
        offset += 12 * producerListData.size() + 20;
        context.drawText(textRenderer,
                Text.literal("库存").formatted(Formatting.AQUA),
                30, offset - 12, 0xFF000000, false);
        for (int i = 0; i < storageListData.size(); i++){
            context.drawText(textRenderer,
                    Text.literal(storageListData.get(i).content),
                    40, i * 12 + offset, 0xFFFFFF, false);
        }

    }

    private int calculateContentHeight() {
        // 计算内容总高度
        return entryCount * 12 + 80;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        // 处理鼠标滚轮事件
        if (this.scrollableWidget.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

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