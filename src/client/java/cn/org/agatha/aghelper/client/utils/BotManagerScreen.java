package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import cn.org.agatha.aghelper.client.network.BotNetworkClient;
import cn.org.agatha.aghelper.client.network.BotNetworkClient.BotInfo;
import cn.org.agatha.aghelper.client.network.BotNetworkClient.PacketHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class BotManagerScreen extends Screen implements PacketHandler {

    private static final int ITEM_HEIGHT = 20;
    private static final int ITEM_SPACING = 1;
    private static final int TOP_MARGIN = 35;
    private static final int BOTTOM_MARGIN = 110;
    private static final int LIST_X = 20;

    private List<BotInfo> bots = new ArrayList<>();
    private List<ButtonWidget> botEntryWidgets = new ArrayList<>();
    private int selectedIndex = -1;
    private int currentPage = 0;
    private int itemsPerPage = 0;
    private String statusText = "";
    private int statusColor = 0xFFFFFFFF;

    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private TextFieldWidget nameField;

    public BotManagerScreen() {
        super(Text.literal("Bot管理"));
    }

    @Override
    protected void init() {
        BotNetworkClient.setHandler(this);

        // back button — matches MaterialsDash/Supplies style (no arrow)
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), btn -> close())
                .dimensions(10, 10, 40, 20).build());

        // calculate items per page
        int availableHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        itemsPerPage = Math.max(1, availableHeight / (ITEM_HEIGHT + ITEM_SPACING));

        // pagination buttons
        int pageBtnWidth = 60;
        int pageBtnY = height - 25;
        prevButton = ButtonWidget.builder(Text.of("上一页"), btn -> {
            if (currentPage > 0) {
                currentPage--;
                refreshEntries();
            }
        }).dimensions(width / 2 - pageBtnWidth - 5, pageBtnY, pageBtnWidth, 20).build();

        nextButton = ButtonWidget.builder(Text.of("下一页"), btn -> {
            int totalPages = bots.isEmpty() ? 1 : (int) Math.ceil((double) bots.size() / itemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshEntries();
            }
        }).dimensions(width / 2 + 5, pageBtnY, pageBtnWidth, 20).build();

        addDrawableChild(prevButton);
        addDrawableChild(nextButton);

        // name input
        int fieldY = height - 55;
        nameField = new TextFieldWidget(client.textRenderer, 20, fieldY, 140, 18, Text.empty());
        nameField.setMaxLength(16);
        nameField.setPlaceholder(Text.literal("Bot自定义名"));
        addDrawableChild(nameField);

        // action buttons
        int btnY = height - 75;
        addDrawableChild(ButtonWidget.builder(Text.literal("创建"), btn -> createBot())
                .dimensions(20, btnY, 50, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("删除"), btn -> deleteBot())
                .dimensions(75, btnY, 50, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("重命名"), btn -> renameBot())
                .dimensions(130, btnY, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("传送去"), btn -> tpToBot())
                .dimensions(195, btnY, 55, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("拉过来"), btn -> tpHere())
                .dimensions(255, btnY, 55, 18).build());

        refreshEntries();
        BotNetworkClient.requestList();
    }

    @Override
    public void close() {
        BotNetworkClient.clearHandler();
        if (client != null) {
            client.setScreen(new MenuScreen());
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        TextRenderer tr = client.textRenderer;

        // title
        context.drawText(tr, Text.literal("Bot管理 - 你创建的假人"), width / 2 - 80, 10, 0xFFFFFF, true);

        // empty state
        if (bots.isEmpty()) {
            context.drawText(tr, Text.literal("(暂无Bot，输入名称后点创建)"), LIST_X + 5, TOP_MARGIN + 4, 0xFFAAAAAA, true);
        }

        // page info
        if (!bots.isEmpty()) {
            int totalPages = (int) Math.ceil((double) bots.size() / itemsPerPage);
            String pageText = "第 " + (currentPage + 1) + "/" + totalPages + " 页";
            context.drawTextWithShadow(tr, pageText, (width - tr.getWidth(pageText)) / 2, height - 100, 0xFFFFFFFF);
        }

        // selected info
        if (selectedIndex >= 0 && selectedIndex < bots.size()) {
            BotInfo sel = bots.get(selectedIndex);
            String info = "选中: " + sel.name();
            context.drawText(tr, Text.literal(info), 20, height - 80, 0xFF00FF00, true);
        }

        // status
        if (!statusText.isEmpty()) {
            context.drawText(tr, Text.literal(statusText), 20, height - 35, statusColor, true);
        }
    }

    // ── PacketHandler ──

    @Override
    public void onBotList(List<BotInfo> list) {
        bots = list;
        if (selectedIndex >= bots.size()) {
            selectedIndex = bots.isEmpty() ? -1 : 0;
        }
        // clamp page
        int totalPages = bots.isEmpty() ? 1 : (int) Math.ceil((double) bots.size() / itemsPerPage);
        if (currentPage >= totalPages) {
            currentPage = Math.max(0, totalPages - 1);
        }
        refreshEntries();
    }

    @Override
    public void onResult(boolean success, String message) {
        statusText = message;
        statusColor = success ? 0xFF00FF00 : 0xFFFF5555;
        if (success) {
            BotNetworkClient.requestList();
        }
    }

    // ── entries ──

    private void refreshEntries() {
        clearBotEntries();

        int totalPages = bots.isEmpty() ? 1 : (int) Math.ceil((double) bots.size() / itemsPerPage);
        prevButton.active = currentPage > 0;
        nextButton.active = currentPage < totalPages - 1;

        int startIdx = currentPage * itemsPerPage;
        int endIdx = Math.min(startIdx + itemsPerPage, bots.size());
        int listWidth = width - LIST_X - 20;

        for (int i = startIdx; i < endIdx; i++) {
            final int idx = i;
            BotInfo bot = bots.get(i);
            int itemY = TOP_MARGIN + (i - startIdx) * (ITEM_HEIGHT + ITEM_SPACING);
            String label = bot.name() + "  [" + dimLabel(bot.dim()) + "]  " + (int) bot.x() + "," + (int) bot.y() + "," + (int) bot.z();
            ButtonWidget entry = ButtonWidget.builder(Text.literal(label), btn -> {
                selectedIndex = idx;
                refreshEntries();
            }).dimensions(LIST_X, itemY, listWidth, ITEM_HEIGHT).build();
            addDrawableChild(entry);
            botEntryWidgets.add(entry);
        }
    }

    private void clearBotEntries() {
        for (ButtonWidget w : botEntryWidgets) {
            remove(w);
        }
        botEntryWidgets.clear();
    }

    // ── actions ──

    private void createBot() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            statusText = "请输入Bot名称";
            statusColor = 0xFFFF5555;
            return;
        }
        BotNetworkClient.createBot(name);
        nameField.setText("");
    }

    private void deleteBot() {
        if (selectedIndex < 0 || selectedIndex >= bots.size()) {
            statusText = "请先选择一个Bot";
            statusColor = 0xFFFF5555;
            return;
        }
        BotNetworkClient.removeBot(bots.get(selectedIndex).name());
    }

    private void renameBot() {
        if (selectedIndex < 0 || selectedIndex >= bots.size()) {
            statusText = "请先选择一个Bot";
            statusColor = 0xFFFF5555;
            return;
        }
        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            statusText = "请输入新名称";
            statusColor = 0xFFFF5555;
            return;
        }
        BotNetworkClient.renameBot(bots.get(selectedIndex).name(), newName);
        nameField.setText("");
    }

    private void tpToBot() {
        if (selectedIndex < 0 || selectedIndex >= bots.size()) {
            statusText = "请先选择一个Bot";
            statusColor = 0xFFFF5555;
            return;
        }
        BotNetworkClient.teleportToBot(bots.get(selectedIndex).name());
    }

    private void tpHere() {
        if (selectedIndex < 0 || selectedIndex >= bots.size()) {
            statusText = "请先选择一个Bot";
            statusColor = 0xFFFF5555;
            return;
        }
        BotNetworkClient.teleportBotHere(bots.get(selectedIndex).name());
    }

    // ── adaptive height & scroll ──

    @Override
    public void tick() {
        super.tick();
        int availableHeight = height - TOP_MARGIN - BOTTOM_MARGIN;
        int newItemsPerPage = Math.max(1, availableHeight / (ITEM_HEIGHT + ITEM_SPACING));
        if (newItemsPerPage != itemsPerPage) {
            itemsPerPage = newItemsPerPage;
            int totalPages = bots.isEmpty() ? 1 : (int) Math.ceil((double) bots.size() / itemsPerPage);
            if (currentPage >= totalPages) {
                currentPage = Math.max(0, totalPages - 1);
            }
            refreshEntries();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount > 0) {
            if (currentPage > 0) {
                currentPage--;
                refreshEntries();
            }
        } else if (verticalAmount < 0) {
            int totalPages = bots.isEmpty() ? 1 : (int) Math.ceil((double) bots.size() / itemsPerPage);
            if (currentPage < totalPages - 1) {
                currentPage++;
                refreshEntries();
            }
        }
        return true;
    }

    private static String dimLabel(String dim) {
        return switch (dim) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            default -> dim;
        };
    }

    public void renderBackground(DrawContext context) {
        int w = client.getWindow().getScaledWidth();
        int h = client.getWindow().getScaledHeight();
        context.fillGradient(0, 0, w, h / 2, 0xFF202020, 0xFF101010);
        context.fillGradient(0, h / 2, w, h, 0xFF101010, 0xFF202020);
    }
}
