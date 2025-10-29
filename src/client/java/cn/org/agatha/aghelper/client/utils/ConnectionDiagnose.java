package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets; // 新增导入

public class ConnectionDiagnose extends Screen {
    public ConnectionDiagnose() {
        super(Text.of("连接诊断"));
    }
    // 绘制表格内容
    String L1_label = "数据时间";
    String L1_currentTime = "加载中";
    String L2_label = "游戏类别";
    String L2_type = "加载中";
    String L3_label = "接入点";
    String L3_point = "无数据";
    String L4_point = "";

    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());
        L1_currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日HH:mm:ss"));

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.isInSingleplayer()){
            L2_type = "单机游戏";
        }
        else{
            if (client.getCurrentServerEntry() != null){
                L2_type = "联机游戏";
                if (client.getNetworkHandler() != null && client.getNetworkHandler().getConnection() != null){
                    SocketAddress address = client.getNetworkHandler().getConnection().getAddress();
                    if (address instanceof InetSocketAddress){
                        L3_point = ((InetSocketAddress) address).getAddress().getHostAddress();
                        fetchGeoIpData(L3_point);
                    }
                }
            }
        }
    }

    private void fetchGeoIpData(String ip) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://api-openid.agatha.org.cn/info/geoIp?ip=" + ip);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept-Charset", "UTF-8"); // 设置字符集为UTF-8
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)); // 使用UTF-8字符集
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    L4_point = response.toString();
                } else {
                    L4_point = "无法获取地理位置信息";
                }
            } catch (Exception e) {
                L4_point = "无法获取地理位置信息";
            }
        });
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        // 绘制表格框
        int tableX = width / 2 - 150; // 表格左上角X坐标
        int tableY = height / 2 - 50; // 表格左上角Y坐标
        int tableWidth = 300;        // 表格宽度
        int tableHeight = 100;       // 表格高度

        // 绘制表格边框
        context.fill(tableX, tableY, tableX + tableWidth, tableY + tableHeight, 0xFFAAAAAA); // 背景填充
        drawBorder(context, tableX, tableY, tableWidth, tableHeight, 0xFF000000);             // 边框绘制

        // 左侧标签
        context.drawText(textRenderer, L1_label, tableX + 20, tableY + 20, 0xFF000000, false);
        // 右侧信息
        int textWidth = textRenderer.getWidth(L1_currentTime);
        context.drawText(textRenderer, L1_currentTime, tableX + tableWidth - textWidth - 20, tableY + 20, 0xFF000000, false);

        // 左侧标签
        context.drawText(textRenderer, L2_label, tableX + 20, tableY + 40, 0xFF000000, false);
        // 右侧信息
        textWidth = textRenderer.getWidth(L2_type);
        context.drawText(textRenderer, L2_type, tableX + tableWidth - textWidth - 20, tableY + 40, 0xFF000000, false);

        // 左侧标签
        context.drawText(textRenderer, L3_label, tableX + 20, tableY + 60, 0xFF000000, false);
        // 右侧信息
        textWidth = textRenderer.getWidth(L3_point);
        context.drawText(textRenderer, L3_point, tableX + tableWidth - textWidth - 20, tableY + 60, 0xFF000000, false);

        // 右侧信息
        textWidth = textRenderer.getWidth(L4_point);
        context.drawText(textRenderer, L4_point, tableX + tableWidth - textWidth - 20, tableY + 80, 0xFF000000, false);
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