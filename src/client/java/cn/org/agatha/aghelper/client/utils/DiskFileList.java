package cn.org.agatha.aghelper.client.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DiskFileList extends Screen {
    private final String userId;
    private final String username;

    public DiskFileList(String userId, String username) {
        super(Text.of("网盘文件"));
        this.userId = userId;
        this.username = username != null ? username : userId;
    }

    private static final Gson GSON = new Gson();
    private static final String DISK_API_PREFIX = "http://127.0.0.1:28188/pc/disk/user/";

    private boolean loaded = false;
    private boolean loadError = false;
    private String errorMessage = "";
    private List<FileData> files = new ArrayList<>();

    // Pagination
    private int currentPage = 0;
    private int itemsPerPage = 10;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private static final int ITEM_HEIGHT = 20;
    private static final int ITEM_SPACING = 5;
    private static final int TOP_MARGIN = 60;
    private static final int BOTTOM_MARGIN = 50;

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> {
            assert client != null;
            client.setScreen(new DiskBrowser());
        }).dimensions(10, 10, 40, 20).build());

        int availableHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        itemsPerPage = Math.max(1, availableHeight / (ITEM_HEIGHT + ITEM_SPACING));

        int buttonWidth = 60;
        int buttonHeight = 20;
        int buttonY = height - 30;

        prevButton = ButtonWidget.builder(Text.of("上一页"), button -> {
            if (currentPage > 0) currentPage--;
        }).dimensions(width / 2 - buttonWidth - 5, buttonY, buttonWidth, buttonHeight).build();

        nextButton = ButtonWidget.builder(Text.of("下一页"), button -> {
            int totalPages = getTotalPages();
            if (currentPage < totalPages - 1) currentPage++;
        }).dimensions(width / 2 + 5, buttonY, buttonWidth, buttonHeight).build();

        addDrawableChild(prevButton);
        addDrawableChild(nextButton);

        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(userId, StandardCharsets.UTF_8.name());
                URL url = new URL(DISK_API_PREFIX + encoded);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
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
                    List<FileData> result = GSON.fromJson(json, new TypeToken<List<FileData>>(){}.getType());
                    if (result == null) result = new ArrayList<>();
                    files = result;
                    loaded = true;
                } else if (responseCode == 401) {
                    loadError = true;
                    errorMessage = "未登录，请先在 MinechatPC 中登录";
                } else {
                    loadError = true;
                    errorMessage = "HTTP " + responseCode;
                }
            } catch (Exception e) {
                loadError = true;
                errorMessage = e.getClass().getSimpleName() + ": " + e.getMessage();
            }
        }).start();
    }

    private int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) files.size() / itemsPerPage));
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "-";
        String[] units = {"B", "KB", "MB", "GB"};
        double v = bytes;
        int i = 0;
        while (v >= 1024 && i < units.length - 1) {
            v /= 1024;
            i++;
        }
        if (i == 0) return String.format("%.0f %s", v, units[i]);
        return String.format("%.1f %s", v, units[i]);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        String title = username + " 的网盘文件";
        context.drawText(textRenderer, title,
                (width - textRenderer.getWidth(title)) / 2, 35, 0xFFFFFFFF, false);

        if (loadError) {
            context.drawText(textRenderer, "加载失败: " + errorMessage,
                    (width - textRenderer.getWidth("加载失败: " + errorMessage)) / 2, height / 2, 0xFFFF5555, false);
        } else if (!loaded) {
            context.drawText(textRenderer, "加载中...",
                    (width - textRenderer.getWidth("加载中...")) / 2, height / 2, 0xFFFFFFFF, false);
        } else if (files.isEmpty()) {
            context.drawText(textRenderer, "该玩家暂无文件",
                    (width - textRenderer.getWidth("该玩家暂无文件")) / 2, height / 2, 0xFFAAAAAA, false);
        } else {
            int totalPages = getTotalPages();
            String pageText = String.format("第 %d/%d 页  共 %d 个文件", currentPage + 1, totalPages, files.size());
            context.drawText(textRenderer, pageText,
                    width / 2 - textRenderer.getWidth(pageText) / 2, height - 45, 0xFFFFFFFF, false);

            renderFileList(context, mouseX, mouseY);
        }
    }

    private void renderFileList(DrawContext context, int mouseX, int mouseY) {
        int startY = TOP_MARGIN;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, files.size());

        // Header
        context.drawText(textRenderer, "文件名", 65, startY - 12, 0xFF888888, false);
        String sizeHeader = "大小";
        context.drawText(textRenderer, sizeHeader, width - 70 - textRenderer.getWidth(sizeHeader), startY - 12, 0xFF888888, false);

        for (int i = startIndex; i < endIndex; i++) {
            FileData file = files.get(i);
            int itemIndex = i - startIndex;
            int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);

            boolean hovered = mouseX >= 60 && mouseX <= width - 60 &&
                    mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            int bgColor = hovered ? 0x80AAAAAA : 0x80222222;
            context.fill(60, itemY, width - 60, itemY + ITEM_HEIGHT, bgColor);
            drawBorder(context, 60, itemY, width - 120, ITEM_HEIGHT, 0xFFFFFFFF);

            // File name (truncated if too long)
            String name = file.name != null ? file.name : "未知文件";
            int maxNameWidth = width - 200;
            String displayName = name;
            if (textRenderer.getWidth(displayName) > maxNameWidth) {
                while (displayName.length() > 1 && textRenderer.getWidth(displayName + "...") > maxNameWidth) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName = displayName + "...";
            }
            context.drawText(textRenderer, displayName, 70, itemY + 6, 0xFFFFFFFF, false);

            // File size
            String sizeText = formatBytes(file.size);
            int sizeWidth = textRenderer.getWidth(sizeText);
            context.drawText(textRenderer, sizeText, width - 70 - sizeWidth, itemY + 6, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            if (currentPage > 0) currentPage--;
        } else if (verticalAmount < 0) {
            int totalPages = getTotalPages();
            if (currentPage < totalPages - 1) currentPage++;
        }
        return true;
    }

    public void drawBorder(DrawContext context, int x, int y, int width, int height, int color) {
        int borderWidth = 1;
        context.fill(x, y, x + width, y + borderWidth, color);
        context.fill(x, y + height - borderWidth, x + width, y + height, color);
        context.fill(x, y, x + borderWidth, y + height, color);
        context.fill(x + width - borderWidth, y, x + width, y + height, color);
    }

    public void renderBackground(DrawContext context) {
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        context.fillGradient(0, 0, w, h / 2, 0xFF202020, 0xFF101010);
        context.fillGradient(0, h / 2, w, h, 0xFF101010, 0xFF202020);
    }

    @Override
    public void tick() {
        super.tick();
        int availableHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        int newItemsPerPage = Math.max(1, availableHeight / (ITEM_HEIGHT + ITEM_SPACING));
        if (newItemsPerPage != itemsPerPage) {
            itemsPerPage = newItemsPerPage;
        }
    }

    private static class FileData {
        String id;
        String owner_id;
        String owner_username;
        String name;
        long size;
        String created_at;
        String updated_at;
    }
}
