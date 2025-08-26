package cn.org.agatha.aghelper.client;

import cn.org.agatha.aghelper.client.elements.MenuRectWidget;
import cn.org.agatha.aghelper.client.utils.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
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

        // 判断是不是主服或测试服
        String thisServerIp = MinecraftClient.getInstance().getCurrentServerEntry().address;
        if (thisServerIp.equalsIgnoreCase("agatha.org.cn")) {
            // 当前服务器是主服
            MenuRectWidget switchServerButton = new MenuRectWidget(
                    width/2+5, height/2+30, 70, 20,
                    Text.literal("去测试服"),
                    new ItemStack(Items.ELYTRA),
                    0xFF696969, // 绿色背景
                    () -> {
                        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("quit 正在退出主服");
                        // 首先退出当前服务器
                        MinecraftClient.getInstance().disconnect();
                        // 然后连接到测试服
                        ServerInfo serverInfo = new ServerInfo("Agatha纯净生存测试服", "doris.agatha.org.cn", ServerInfo.ServerType.OTHER);
                        // 连接到服务器
                        ConnectScreen.connect(null, client, ServerAddress.parse("doris.agatha.org.cn"), serverInfo,  false, null);

                    }
            );
            addDrawableChild(switchServerButton);
        }
        else if (thisServerIp.equalsIgnoreCase("doris.agatha.org.cn")){
            // 当前服务器是主服
            MenuRectWidget switchServerButton = new MenuRectWidget(
                    width/2+5, height/2+30, 70, 20,
                    Text.literal("回主服"),
                    new ItemStack(Items.ELYTRA),
                    0xFF696969, // 绿色背景
                    () -> {
                        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("quit 正在退出测试服");
                        // 首先退出当前服务器
                        MinecraftClient.getInstance().disconnect();
                        // 然后连接到测试服
                        ServerInfo serverInfo = new ServerInfo("Agatha纯净生存", "agatha.org.cn", ServerInfo.ServerType.OTHER);
                        // 连接到服务器
                        ConnectScreen.connect(null, client, ServerAddress.parse("agatha.org.cn"), serverInfo,  false, null);

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
                version,
                drawContext.getScaledWindowWidth() - textWidth - 10,
                drawContext.getScaledWindowHeight() - 20,
                0xFFFFFF,
                true
            );
        });

        // 异步执行版本检查
        new Thread(() -> {
            // 发送HTTP GET请求，获取URL地址，检查版本号
            // https://mc.agatha.org.cn/helper/latest
            try {
                URL url = new URL("https://mc.agatha.org.cn/helper/latest");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();
                    String latestVersion = response.toString();
                    if (!latestVersion.equals(getModVersion())) {
                        assert client != null;
                        client.inGameHud.getChatHud().addMessage(Text.literal("检测到新版本，请前往 https://mc.agatha.org.cn/ 下载新版本。为保证接口版本一致，当前版本菜单功能已禁用，您可以继续使用其他功能。").formatted(Formatting.RED));
                        client.setScreen(null);
                    }
                }

            }
            catch (Exception e) {
            }

        }).start();
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
        int width = client.getWindow().getScaledWidth();
        int height = client.getWindow().getScaledHeight();

        // 顶部渐变
        context.fillGradient(0, 0, width, height/2, 0xFF202020, 0xFF101010);
        // 底部渐变
        context.fillGradient(0, height/2, width, height, 0xFF101010, 0xFF202020);
    }

}