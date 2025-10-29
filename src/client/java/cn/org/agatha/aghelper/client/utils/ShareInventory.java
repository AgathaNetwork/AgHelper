package cn.org.agatha.aghelper.client.utils;

import cn.org.agatha.aghelper.client.MenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ShareInventory extends Screen {
    public ShareInventory() {
        super(Text.of("背包查看"));
    }
    public int textY = 40;
    public String selectedPlayer;
    @Override
    protected void init() {
        // 添加一个返回按钮
        addDrawableChild(ButtonWidget.builder(Text.of("返回"), button -> client.setScreen(new MenuScreen()))
                .dimensions(10, 10, 40, 20)
                .build());
        // 居中说明文字

        // 居中说明文字

        List<String> onlinePlayers = getOnlinePlayers();

        if (!onlinePlayers.isEmpty()) {
            selectedPlayer = onlinePlayers.get(0);
            CyclingButtonWidget<String> playerButton = CyclingButtonWidget.builder((String value) ->
                            Text.literal( "" + value))
                    .values(onlinePlayers)
                    .initially(onlinePlayers.get(0))
                    .build(width/2 - 100, textY + 20, 200, 20, Text.of("选择玩家"),
                            (button, value) -> {
                                // 处理选择玩家的逻辑
                                selectedPlayer = value;
                            });
            this.addDrawableChild(playerButton);
            // 确认按钮
            addDrawableChild(ButtonWidget.builder(Text.of("确认"), button -> {
                        // 执行指令
                        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("share " + selectedPlayer);
                        client.setScreen(null);
                    })
                    .dimensions(width/2 - 100, textY + 50, 200, 20)
                    .build());
        } else {
            addDrawableChild(ButtonWidget.builder(Text.of("无其他玩家在线"), button -> {})
                    .dimensions(width/2 - 100, textY + 20, 200, 20)
                    .build());
        }
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        if (client.getNetworkHandler() != null) {
            Collection<PlayerListEntry> playerList =
                    client.getNetworkHandler().getPlayerList();
            String currentPlayerName = client.player.getName().getString();

            for (net.minecraft.client.network.PlayerListEntry entry : playerList) {
                String playerName = entry.getProfile().name();
                if (!playerName.equals(currentPlayerName)) {
                    players.add(playerName);
                }
            }
        }
        return players;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
            textRenderer,
            "对方玩家同意后，才可查看背包。",
            width/2,
            textY,
            0xFFFFFF
        );
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