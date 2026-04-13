package cn.org.agatha.aghelper.client.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final String DISK_DOWNLOAD_PREFIX = "http://127.0.0.1:28188/pc/disk/";
    private static final int DOWNLOAD_BTN_WIDTH = 40;

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

    // Per-file download state: 0=idle, 1=downloading, 2=completed, 3=failed
    private final Map<String, Integer> downloadState = new HashMap<>();

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

    private boolean isLitematic(FileData file) {
        String name = file.name;
        return name != null && name.toLowerCase().endsWith(".litematic");
    }

    private void downloadLitematic(FileData file) {
        if (downloadState.getOrDefault(file.id, 0) != 0) return;
        downloadState.put(file.id, 1);
        new Thread(() -> {
            try {
                String encoded = URLEncoder.encode(file.id, StandardCharsets.UTF_8.name());
                URL url = new URL(DISK_DOWNLOAD_PREFIX + encoded + "/download");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(30000);
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    Path gameDir = client.runDirectory.toPath();
                    Path schematicDir = gameDir.resolve("schematics");
                    Files.createDirectories(schematicDir);

                    String fileName = file.name;
                    if (fileName == null || fileName.isBlank()) fileName = file.id + ".litematic";
                    // Sanitize: remove path separators
                    fileName = fileName.replace("/", "_").replace("\\", "_");
                    final String savedName = fileName;
                    Path target = schematicDir.resolve(savedName);

                    try (InputStream is = connection.getInputStream()) {
                        Files.copy(is, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    }

                    downloadState.put(file.id, 2);
                    net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                        net.minecraft.client.MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("投影文件已保存到 schematics/" + savedName)
                                .formatted(net.minecraft.util.Formatting.GREEN)
                        )
                    );
                } else {
                    downloadState.put(file.id, 3);
                    net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                        net.minecraft.client.MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                            Text.literal("下载失败: HTTP " + responseCode)
                                .formatted(net.minecraft.util.Formatting.RED)
                        )
                    );
                }
            } catch (Exception e) {
                downloadState.put(file.id, 3);
                String msg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                net.minecraft.client.MinecraftClient.getInstance().execute(() ->
                    net.minecraft.client.MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                        Text.literal("下载失败: " + msg)
                            .formatted(net.minecraft.util.Formatting.RED)
                    )
                );
            }
        }).start();
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

            boolean isLit = isLitematic(file);
            int rightEdge = width - 60;

            boolean hovered = mouseX >= 60 && mouseX <= rightEdge &&
                    mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;
            int bgColor = hovered ? 0x80AAAAAA : 0x80222222;
            context.fill(60, itemY, rightEdge, itemY + ITEM_HEIGHT, bgColor);
            drawBorder(context, 60, itemY, rightEdge - 60, ITEM_HEIGHT, 0xFFFFFFFF);

            // File name (truncated if too long)
            String name = file.name != null ? file.name : "未知文件";
            int nameRightLimit = isLit ? (rightEdge - DOWNLOAD_BTN_WIDTH - 80) : (rightEdge - 80);
            int maxNameWidth = nameRightLimit - 70;
            String displayName = name;
            if (textRenderer.getWidth(displayName) > maxNameWidth) {
                while (displayName.length() > 1 && textRenderer.getWidth(displayName + "...") > maxNameWidth) {
                    displayName = displayName.substring(0, displayName.length() - 1);
                }
                displayName = displayName + "...";
            }
            context.drawText(textRenderer, displayName, 70, itemY + 6, 0xFFFFFFFF, false);

            // Download button for litematic files
            if (isLit) {
                int state = downloadState.getOrDefault(file.id, 0);
                int btnX = rightEdge - DOWNLOAD_BTN_WIDTH - 65;
                boolean btnHovered = state == 0 && mouseX >= btnX && mouseX <= btnX + DOWNLOAD_BTN_WIDTH &&
                        mouseY >= itemY + 2 && mouseY <= itemY + ITEM_HEIGHT - 2;
                int btnColor;
                String btnText;
                if (state == 1) {
                    btnColor = 0xFF333333;
                    btnText = "...";
                } else if (state == 2) {
                    btnColor = 0xFF333333;
                    btnText = "\u5B8C\u6210";
                } else if (state == 3) {
                    btnColor = 0xFF333333;
                    btnText = "\u5931\u8D25";
                } else {
                    btnColor = btnHovered ? 0xFF696969 : 0xFF404040;
                    btnText = "\u4E0B\u8F7D";
                }
                context.fill(btnX, itemY + 2, btnX + DOWNLOAD_BTN_WIDTH, itemY + ITEM_HEIGHT - 2, btnColor);
                drawBorder(context, btnX, itemY + 2, DOWNLOAD_BTN_WIDTH, ITEM_HEIGHT - 4, state == 0 ? 0xFFFFFFFF : 0xFF666666);
                int btnTextColor = state == 0 ? 0xFFFFFFFF : 0xFF888888;
                int btnTextX = btnX + (DOWNLOAD_BTN_WIDTH - textRenderer.getWidth(btnText)) / 2;
                context.drawText(textRenderer, btnText, btnTextX, itemY + 6, btnTextColor, false);
            }

            // File size
            String sizeText = formatBytes(file.size);
            int sizeWidth = textRenderer.getWidth(sizeText);
            context.drawText(textRenderer, sizeText, rightEdge - 10 - sizeWidth, itemY + 6, 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (loaded && !files.isEmpty()) {
            int startY = TOP_MARGIN;
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, files.size());
            int rightEdge = width - 60;

            for (int i = startIndex; i < endIndex; i++) {
                FileData file = files.get(i);
                int itemIndex = i - startIndex;
                int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);

                if (isLitematic(file)) {
                    int btnX = rightEdge - DOWNLOAD_BTN_WIDTH - 65;
                    int state = downloadState.getOrDefault(file.id, 0);
                    if (state == 0 && mouseX >= btnX && mouseX <= btnX + DOWNLOAD_BTN_WIDTH &&
                            mouseY >= itemY + 2 && mouseY <= itemY + ITEM_HEIGHT - 2) {
                        downloadLitematic(file);
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(click, doubled);
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
