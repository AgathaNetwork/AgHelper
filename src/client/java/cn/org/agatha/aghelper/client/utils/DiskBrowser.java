package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DiskBrowser extends Screen {
    public DiskBrowser() {
        super(Text.of("网盘文件"));
    }

    private static final Gson GSON = new Gson();
    private static final String USERS_API = "http://127.0.0.1:28188/pc/users";

    private boolean loaded = false;
    private boolean loadError = false;
    private String errorMessage = "";
    private List<UserData> allUsers = new ArrayList<>();
    private List<UserData> filteredUsers = new ArrayList<>();

    // Pagination
    private int currentPage = 0;
    private int itemsPerPage = 10;
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private static final int ITEM_HEIGHT = 20;
    private static final int ITEM_SPACING = 5;
    private static final int TOP_MARGIN = 60;
    private static final int BOTTOM_MARGIN = 50;

    // Search
    private TextFieldWidget searchInput;

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());

        searchInput = new TextFieldWidget(textRenderer, 60, 10, width - 100, 20, null);
        searchInput.setPlaceholder(Text.of("搜索玩家名称"));
        searchInput.setChangedListener(this::onSearchTextChanged);
        addDrawableChild(searchInput);

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
                URL url = new URL(USERS_API);
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
                    List<UserData> users = GSON.fromJson(json, new TypeToken<List<UserData>>(){}.getType());
                    if (users == null) users = new ArrayList<>();
                    allUsers = users;
                    filteredUsers = new ArrayList<>(allUsers);
                    loaded = true;
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

    private void onSearchTextChanged(String text) {
        currentPage = 0;
        if (text == null || text.trim().isEmpty()) {
            filteredUsers = new ArrayList<>(allUsers);
        } else {
            String lower = text.trim().toLowerCase();
            filteredUsers = allUsers.stream()
                    .filter(u -> {
                        String name = u.username != null ? u.username.toLowerCase() : "";
                        String id = u.id != null ? u.id.toLowerCase() : "";
                        return name.contains(lower) || id.contains(lower);
                    })
                    .collect(Collectors.toList());
        }
    }

    private int getTotalPages() {
        int totalItems = filteredUsers.size();
        return Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawText(textRenderer, "网盘文件 - 选择玩家",
                (width - textRenderer.getWidth("网盘文件 - 选择玩家")) / 2, 35, 0xFFFFFFFF, false);

        if (loadError) {
            String msg = "加载失败: " + errorMessage;
            context.drawText(textRenderer, msg,
                    (width - textRenderer.getWidth(msg)) / 2, height / 2, 0xFFFF5555, false);
            String hint = "请确认 MinechatPC 已启动";
            context.drawText(textRenderer, hint,
                    (width - textRenderer.getWidth(hint)) / 2, height / 2 + 14, 0xFFAAAAAA, false);
        } else if (!loaded) {
            context.drawText(textRenderer, "加载中...",
                    (width - textRenderer.getWidth("加载中...")) / 2, height / 2, 0xFFFFFFFF, false);
        } else {
            int totalPages = getTotalPages();
            String pageText = String.format("第 %d/%d 页  共 %d 位玩家", currentPage + 1, totalPages, filteredUsers.size());
            context.drawText(textRenderer, pageText,
                    width / 2 - textRenderer.getWidth(pageText) / 2, height - 45, 0xFFFFFFFF, false);

            renderUserList(context, mouseX, mouseY);
        }
    }

    private void renderUserList(DrawContext context, int mouseX, int mouseY) {
        int startY = TOP_MARGIN;
        int startIndex = currentPage * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, filteredUsers.size());

        for (int i = startIndex; i < endIndex; i++) {
            UserData user = filteredUsers.get(i);
            int itemIndex = i - startIndex;
            int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);

            boolean hovered = mouseX >= 60 && mouseX <= width - 60 &&
                    mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT;

            int bgColor = hovered ? 0x80AAAAAA : 0x80222222;
            context.fill(60, itemY, width - 60, itemY + ITEM_HEIGHT, bgColor);
            drawBorder(context, 60, itemY, width - 120, ITEM_HEIGHT, 0xFFFFFFFF);

            String displayName = user.username != null ? user.username : String.valueOf(user.id);
            context.drawText(textRenderer, displayName, 70, itemY + 6, 0xFFFFFFFF, false);

            String hint = "点击查看网盘 →";
            int hintWidth = textRenderer.getWidth(hint);
            context.drawText(textRenderer, hint, width - 70 - hintWidth, itemY + 6, 0xFF888888, false);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        if (loaded) {
            int startY = TOP_MARGIN;
            int startIndex = currentPage * itemsPerPage;
            int endIndex = Math.min(startIndex + itemsPerPage, filteredUsers.size());

            for (int i = startIndex; i < endIndex; i++) {
                int itemIndex = i - startIndex;
                int itemY = startY + itemIndex * (ITEM_HEIGHT + ITEM_SPACING);

                if (mouseX >= 60 && mouseX <= width - 60 &&
                        mouseY >= itemY && mouseY <= itemY + ITEM_HEIGHT) {
                    UserData user = filteredUsers.get(i);
                    assert client != null;
                    client.setScreen(new DiskFileList(user.id, user.username));
                    return true;
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

    private static class UserData {
        String id;
        String username;
        String minecraftUuid;
        String faceUrl;
    }
}
