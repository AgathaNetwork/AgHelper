package cn.org.agatha.aghelper.client;

import cn.org.agatha.aghelper.client.utils.CreatePicture;
import cn.org.agatha.aghelper.client.utils.OccupiedItemsHUD;
import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class AghelperClient implements ClientModInitializer {

    public static int selectedMaterialId = -1; // 材料列表ID，默认为-1表示未选择
    public static String selectedMaterialName = ""; // 材料列表名称
    private static KeyBinding menuKeyBinding;
    private static KeyBinding autologinKeyBinding;
    private static KeyBinding createPictureKeyBinding;
    private static final String MOD_ID = "aghelper";
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Path.of("config/aghelper.json");
    private static final String VERSION_CHECK_URL = "https://mc.agatha.org.cn/helper/latest";
    private static final String DOWNLOAD_URL_TEMPLATE = "https://mc.agatha.org.cn/helper/AgHelper-%s.jar";

    @Override
    public void onInitializeClient() {
        // 注册HUD
        HudRenderCallback.EVENT.register(OccupiedItemsHUD.getInstance());
        
        // 注册鼠标事件监听器
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.beforeMouseClick(screen).register((screen1, mouseX, mouseY, button) -> 
                OccupiedItemsHUD.getInstance().mouseClicked(mouseX, mouseY, button)
            );
            
            ScreenMouseEvents.afterMouseRelease(screen).register((screen1, mouseX, mouseY, button) -> 
                OccupiedItemsHUD.getInstance().mouseReleased(mouseX, mouseY, button)
            );
            
            ScreenMouseEvents.beforeMouseClick(screen).register((screen1, mouseX, mouseY, button) ->
                OccupiedItemsHUD.getInstance().mouseDragged(mouseX, mouseY, button)
            );
        });
        
        // 检查更新
        checkForUpdates();

        // 如果配置文件不存在
        if (!CONFIG_PATH.toFile().exists()) {
            saveConfig(new ConfigData(GLFW.GLFW_KEY_UP, "", "", GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_RIGHT));
        }
        // 读取配置文件
        ConfigData config = loadConfig();

        // 创建菜单快捷键绑定
        menuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".menu_key",
                config.menuShortcutKey(),
                "category." + MOD_ID + ".main"
        ));

        autologinKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".autologin_key",
                config.autologinKey(),
                "category." + MOD_ID + ".main"
        ));

        createPictureKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".create_picture_key",
                config.createPictureKey(),
                "category." + MOD_ID + ".main"
        ));
        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            client.execute(() -> {
                ServerInfo serverInfo = new ServerInfo("Agatha纯净生存", "agatha.org.cn", ServerInfo.ServerType.OTHER);
                // 连接到服务器
                ConnectScreen.connect(client.currentScreen, client, ServerAddress.parse("agatha.org.cn"), serverInfo,  false, null);
            });
        });

        // 注册客户端tick监听
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (menuKeyBinding.wasPressed()) {
                client.setScreen(new MenuScreen());
            }
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (autologinKeyBinding.wasPressed()) {
                // 收集自动登录配置
                performAutologin();
            }
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {

            if (createPictureKeyBinding.wasPressed()) {

                // 判断登录的服务器地址
                if (MinecraftClient.getInstance().getCurrentServerEntry() == null) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("当前世界不可用").formatted(Formatting.RED));
                }
                else{
                    if (!MinecraftClient.getInstance().getCurrentServerEntry().address.equals("agatha.org.cn")){
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("请先将登录入口切换至agatha.org.cn").formatted(Formatting.RED));
                    }else{
                        // 保存图片frameBuffer，客户端截图到文件
                        String filename = "gallery/aghelper_" + System.currentTimeMillis() + ".png";

                        // 确保目录存在
                        File screenshotFile = new File(client.runDirectory, filename);
                        screenshotFile.getParentFile().mkdirs();

                        try {
                            // 获取帧缓冲区
                            Framebuffer framebuffer = client.getFramebuffer();
                            int width = framebuffer.textureWidth;
                            int height = framebuffer.textureHeight;

                            // 创建原生图像
                            NativeImage nativeImage = new NativeImage(width, height, false);

                            // 绑定帧缓冲区并读取像素
                            framebuffer.beginRead();
                            nativeImage.loadFromTextureImage(0, false);
                            framebuffer.endRead();

                            // 翻转图像（OpenGL坐标系与图像坐标系不同）
                            nativeImage.mirrorVertically();

                            // 保存文件
                            nativeImage.writeTo(screenshotFile);
                            nativeImage.close();

                        } catch (IOException e) {

                        }
                        int x = (int) Math.floor(client.player.getX());
                        int y = (int) Math.floor(client.player.getY());
                        int z = (int) Math.floor(client.player.getZ());
                        String worldName = client.player.getWorld().getRegistryKey().getValue().getPath();
                        client.setScreen(new CreatePicture(filename, x, y, z, worldName));
                    }
                }
            }
        });

        // 注册HUD渲染回调
        // 已经在上面注册过了，删除重复的注册
    }

    /**
     * 检查更新并在后台执行更新过程
     */
    private void checkForUpdates() {
        CompletableFuture.runAsync(() -> {
            try {
                // 获取当前版本
                String currentVersion = getCurrentModVersion();
                logMessage("当前版本: " + currentVersion);
                
                // 获取最新版本
                String latestVersion = getLatestVersion();
                logMessage("最新版本: " + latestVersion);
                
                // 如果版本不匹配，执行更新
                if (!currentVersion.equals(latestVersion)) {
                    logMessage("发现新版本，准备更新...");
                    updateMod(currentVersion, latestVersion);
                } else {
                    logMessage("当前已是最新版本");
                }
            } catch (Exception e) {
                logMessage("检查更新时出错: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /**
     * 记录日志消息
     * @param message 要记录的消息
     */
    private void logMessage(String message) {
        System.out.println(message);
    }

    /**
     * 获取当前模组版本
     * @return 当前版本字符串
     */
    private String getCurrentModVersion() {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (modContainer.isPresent()) {
            return modContainer.get().getMetadata().getVersion().getFriendlyString();
        }
        return "unknown";
    }

    /**
     * 从远程服务器获取最新版本号
     * @return 最新版本字符串
     * @throws IOException 网络异常
     */
    private String getLatestVersion() throws IOException {
        URL url = new URL(VERSION_CHECK_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000); // 5秒连接超时
        connection.setReadTimeout(5000);    // 5秒读取超时
        
        int responseCode = connection.getResponseCode();
        logMessage("版本检查响应码: " + responseCode);
        
        try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString().trim();
        }
    }

    /**
     * 更新模组到最新版本
     * @param currentVersion 当前版本
     * @param latestVersion 最新版本
     */
    private void updateMod(String currentVersion, String latestVersion) {
        try {
            MinecraftClient.getInstance().execute(() -> {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(
                    Text.literal("[AgHelper] 发现新版本 " + latestVersion + "，正在后台更新...")
                         .formatted(Formatting.YELLOW));
            });
            
            // 构造下载URL
            String downloadUrl = String.format(DOWNLOAD_URL_TEMPLATE, latestVersion);
            logMessage("下载URL: " + downloadUrl);
            
            // 下载新版本
            Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
            Path newModFile = modsDir.resolve("AgHelper-" + latestVersion + ".jar");
            logMessage("新版本将保存到: " + newModFile.toString());
            
            downloadFile(downloadUrl, newModFile);
            logMessage("新版本下载完成");
            
            // 获取当前版本文件路径并显示更新通知
            Path currentModFile = getCurrentModContainerFile();
            logMessage("当前模组文件: " + (currentModFile != null ? currentModFile.toString() : "未知"));
            
            // 显示更新通知窗口
            if (currentModFile != null) {
                MinecraftClient.getInstance().execute(() -> {
                    MinecraftClient.getInstance().setScreen(new UpdateNotificationScreen(currentModFile, latestVersion));
                });
            }

        } catch (Exception e) {
            logMessage("更新失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 获取当前模组的文件路径
     * @return 当前模组文件的路径
     */
    private Path getCurrentModContainerFile() {
        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer(MOD_ID);
        if (modContainer.isPresent()) {
            logMessage("函数获取当前模组文件: " + modContainer.get().getOrigin().getPaths().getFirst().toString());
            return Path.of(modContainer.get().getOrigin().getPaths().getFirst().toString());
        }
        return null;
    }

    /**
     * 下载文件
     * @param urlString 下载URL
     * @param destination 目标路径
     * @throws IOException 下载异常
     */
    private void downloadFile(String urlString, Path destination) throws IOException {
        URL url = new URL(urlString);
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream());
             FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        }
    }

    public void performAutologin() {
        // 获取配置文件
        ConfigData config = loadConfig();
        // 获取用户名密码
        String username = config.username;
        String password = config.password;
        if (username.equals("") || password.equals("")){
            // 聊天框显示内容，调用sendSystemMessage
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("请先设置快速登录密码").formatted(Formatting.RED));
        }else{
            // 聊天框显示内容，调用sendSystemMessage
            if (!MinecraftClient.getInstance().getSession().getUsername().equals(username)) {
                MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("快速登录设置的游戏ID与实际登录不符").formatted(Formatting.RED));
            }else{
                // 判断登录的服务器地址
                if (MinecraftClient.getInstance().getCurrentServerEntry() == null) {
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("当前世界不可用").formatted(Formatting.RED));
                }else{
                    if (!MinecraftClient.getInstance().getCurrentServerEntry().address.equals("agatha.org.cn")){
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("请先将登录入口切换至agatha.org.cn").formatted(Formatting.RED));
                    }else{
                        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("正在执行快速登录").formatted(Formatting.AQUA));
                        MinecraftClient.getInstance().getNetworkHandler().sendChatCommand("login " + password);
                    }
                }
            }
        }
    }
    static ConfigData loadConfig() {
        try {
            return GSON.fromJson(new FileReader(CONFIG_PATH.toFile()), ConfigData.class);
        } catch (Exception e) {
            return new ConfigData(GLFW.GLFW_KEY_UP, "", "", GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_RIGHT);
        }
    }

    public static void updateKeyBinding(int keyCode, String keyName, int scanCode) {
        ConfigData config = loadConfig();
        int menuShortcutKey = config.menuShortcutKey;
        String password = config.password;
        String username = config.username;
        int autologinKey = config.autologinKey;
        int createPictureKey = config.createPictureKey;
        if(keyName.equals("menuShortcutKey")){
            menuShortcutKey = keyCode;
            menuKeyBinding.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            KeyBinding.updateKeysByCode();
        }
        if(keyName.equals("autologinKey")){
            autologinKey = keyCode;
            autologinKeyBinding.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            KeyBinding.updateKeysByCode();
        }
        if(keyName.equals("createPictureKey")){
            createPictureKey = keyCode;
            createPictureKeyBinding.setBoundKey(InputUtil.fromKeyCode(keyCode, scanCode));
            KeyBinding.updateKeysByCode();
        }
        saveConfig(new ConfigData(menuShortcutKey, password, username, autologinKey, createPictureKey));
    }

    public static void updateAutologinConfig(String username, String password) {
        ConfigData config = loadConfig();
        int menuShortcutKey = config.menuShortcutKey;
        String oldPassword = config.password;
        String oldUsername = config.username;
        int autologinKey = config.autologinKey;
        int createPictureKey = config.createPictureKey;
        if(username.equals("")){
            username = oldUsername;
        }
        if(password.equals("")){
            password = oldPassword;
        }
        saveConfig(new ConfigData(menuShortcutKey, password, username, autologinKey, createPictureKey));
    }
    private static void saveConfig(ConfigData config) {
        try {
            CONFIG_PATH.getParent().toFile().mkdirs();
            FileWriter writer = new FileWriter(CONFIG_PATH.toFile());
            writer.write(GSON.toJson(config));
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private record ConfigData(int menuShortcutKey, String password, String username, int autologinKey, int createPictureKey) {}
}