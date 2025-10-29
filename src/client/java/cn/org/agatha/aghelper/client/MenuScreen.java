package cn.org.agatha.aghelper.client;

import cn.org.agatha.aghelper.client.elements.MenuRectWidget;
import cn.org.agatha.aghelper.client.utils.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.util.Formatting;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.FabricLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Objects;

public class MenuScreen extends Screen {
    public MenuScreen() {
        super(Text.of("主菜单"));
    }

    @Override
    protected void init() {

        ItemStack diamondStack = new ItemStack(Items.DIAMOND);
        ItemStack bookStack = new ItemStack(Items.WRITTEN_BOOK);
        ItemStack bookShelfStack = new ItemStack(Items.BOOKSHELF);
        ItemStack compassStack = new ItemStack(Items.COMPASS);
        ItemStack targetStack = new ItemStack(Items.TARGET);
        ItemStack playerHeadStack = new ItemStack(Items.PLAYER_HEAD);
        ItemStack paperStack = new ItemStack(Items.PAPER);

        MenuRectWidget KeybindSettingButton = new MenuRectWidget(
                width/2-75, height/2-30, 70, 20,
                Text.literal("快捷键设置"),
                targetStack,
                0xFF808080, // 绿色背景
                () -> {
                    assert this.client != null;
                    this.client.setScreen(new KeybindSettingScreen());
                }
        );
        addDrawableChild(KeybindSettingButton);

        MenuRectWidget ConnectionDiagnoseButton = new MenuRectWidget(
                width/2+5, height/2-30, 70, 20,
                Text.literal("连接诊断"),
                compassStack,
                0xFF696969, // 绿色背景
                () -> {
                    assert this.client != null;
                    this.client.setScreen(new ConnectionDiagnose());
                }
        );
        addDrawableChild(ConnectionDiagnoseButton);

        MenuRectWidget AutologinButton = new MenuRectWidget(
                width/2-75, height/2, 70, 20,
                Text.literal("快速登录"),
                bookStack,
                0xFF696969, // 绿色背景
                () -> {
                    assert this.client != null;
                    this.client.setScreen(new Autologin());
                }
        );
        addDrawableChild(AutologinButton);

        MenuRectWidget ShareInventoryButton = new MenuRectWidget(
                width/2+5, height/2, 70, 20,
                Text.literal("背包查看"),
                diamondStack,
                0xFF808080, // 绿色背景
                () -> {
                    assert this.client != null;
                    this.client.setScreen(new ShareInventory());
                }
        );
        addDrawableChild(ShareInventoryButton);

        MenuRectWidget SuppliesButton = new MenuRectWidget(
                width/2-75, height/2+30, 70, 20,
                Text.literal("资源管理"),
                bookShelfStack,
                0xFF808080, // 绿色背景
                () -> {
                    assert this.client != null;
                    this.client.setScreen(new Supplies());
                }
        );
        addDrawableChild(SuppliesButton);

        MenuRectWidget OnlineStatisticsButton = new MenuRectWidget(
                width/2+5, height/2+30, 70, 20,
                Text.literal("在线统计"),
                playerHeadStack,
                0xFF696969, // 绿色背景
                () -> {
                    assert this.client != null;
                    // 客户端执行/stat
                    Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("stat");
                }
        );
        addDrawableChild(OnlineStatisticsButton);

        MenuRectWidget MaterialsButton = new MenuRectWidget(
                width/2-75, height/2+60, 70, 20,
                Text.literal("材料列表"),
                paperStack,
                0xFF696969, // 绿色背景
                () -> {
                    assert this.client != null;
                    this.client.setScreen(new MaterialsDash());
                }
        );
        addDrawableChild(MaterialsButton);

        // 判断是不是主服或测试服
        String thisServerIp = Objects.requireNonNull(MinecraftClient.getInstance().getCurrentServerEntry()).address;
        if (thisServerIp.equalsIgnoreCase("agatha.org.cn")) {
            // 当前服务器是主服
            MenuRectWidget switchServerButton = new MenuRectWidget(
                    width/2+5, height/2+60, 70, 20,
                    Text.literal("去测试服"),
                    new ItemStack(Items.ELYTRA),
                    0xFF808080, // 绿色背景
                    () -> {
                        Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("safequit DMS_QUITTING_MAIN_SERVER");
                    }
            );
            addDrawableChild(switchServerButton);
        }
        else if (thisServerIp.equalsIgnoreCase("doris.agatha.org.cn")){
            // 当前服务器是主服
            MenuRectWidget switchServerButton = new MenuRectWidget(
                    width/2+5, height/2+60, 70, 20,
                    Text.literal("回主服"),
                    new ItemStack(Items.ELYTRA),
                    0xFF808080, // 绿色背景
                    () -> {
                        Objects.requireNonNull(MinecraftClient.getInstance().getNetworkHandler()).sendChatCommand("safequit DMS_QUITTING_DORIS");
                    }
            );
            addDrawableChild(switchServerButton);
        }

        ScreenEvents.afterRender(this).register((_screen, drawContext, mouseX, mouseY, tickDelta) -> {

            // 绘制一个正方形
            int logoX = width/2 - 24;
            int logoY = height/2 - 89;

            drawContext.fill(logoX, logoY, logoX + 48, logoY + 48, 0xFFDDFFFE);
            drawContext.fill(logoX + 6, logoY + 6, logoX + 42, logoY + 42, 0xFF0055FF);
            drawContext.fill(logoX, logoY + 12, logoX + 18, logoY + 24, 0xFF339CFF);
            drawContext.fill(logoX + 24, logoY, logoX + 36, logoY + 18, 0xFF339CFF);
            drawContext.fill(logoX + 12, logoY + 30, logoX + 24, logoY + 48, 0xFF339CFF);
            drawContext.fill(logoX + 30, logoY + 24, logoX + 48, logoY + 36, 0xFF339CFF);

            // 新增：右下角显示版本号
            String version = "©Agatha v" + getModVersion();
            int textWidth = MinecraftClient.getInstance().textRenderer.getWidth(version);

            drawContext.drawText(
                    MinecraftClient.getInstance().textRenderer,
                Text.of(version),
                drawContext.getScaledWindowWidth() - textWidth - 10,
                drawContext.getScaledWindowHeight() - 20,
                0xFFFFFFFF,
                true
            );
        });
    }


    public static String getModVersion() {
        // 动态获取本Mod版本号
        ModContainer modContainer = FabricLoader.getInstance().getModContainer("aghelper").orElse(null);
        if(modContainer != null){
            return modContainer.getMetadata().getVersion().getFriendlyString();
        }
        // 错误返回
        return "unknown";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
    }

    public void renderBackground(DrawContext context) {
        // 绘制深灰色渐变背景
        assert client != null;
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // 顶部渐变
        context.fillGradient(0, 0, width, height/2, 0xFF202020, 0xFF101010);
        // 底部渐变
        context.fillGradient(0, height/2, width, height, 0xFF101010, 0xFF202020);
    }

}