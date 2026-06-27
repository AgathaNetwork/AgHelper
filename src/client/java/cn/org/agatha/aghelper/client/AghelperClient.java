package cn.org.agatha.aghelper.client;

import cn.org.agatha.aghelper.client.network.BotNetworkClient;
import cn.org.agatha.aghelper.client.utils.OccupiedItemsHUD;
import cn.org.agatha.aghelper.client.utils.ScreenshotSentScreen;
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
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;


public class AghelperClient implements ClientModInitializer {

    public static int selectedMaterialId = -1; // 材料列表ID，默认为-1表示未选择
    public static String selectedMaterialName = ""; // 材料列表名称
    private static KeyBinding menuKeyBinding;
    private static KeyBinding createPictureKeyBinding;
    private static final String MOD_ID = "aghelper";
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Path.of("config/aghelper.json");
    private static final String VERSION_CHECK_URL = "https://mc.agatha.org.cn/helper/latest";
    private static final String DOWNLOAD_URL_TEMPLATE = "https://mc.agatha.org.cn/helper/AgHelper-%s.jar";
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(Identifier.of("aghelper", "aghelper"));

    @Override
    public void onInitializeClient() {
        // 注册Bot网络通信
        BotNetworkClient.register();

        // 注册HUD
        HudRenderCallback.EVENT.register(OccupiedItemsHUD.getInstance());
        
        // 注册鼠标事件监听器
        ScreenEvents.BEFORE_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.beforeMouseClick(screen).register((screen1, mouse) -> {
                // 检查是否点击在HUD区域内
                if (OccupiedItemsHUD.getInstance().isMouseOver(mouse.x(), mouse.y())) {
                    OccupiedItemsHUD.getInstance().startDragging(mouse.x(), mouse.y());
                }
            });
            
            ScreenMouseEvents.afterMouseRelease(screen).register((screen1, mouse, button) -> {
                OccupiedItemsHUD.getInstance().stopDragging();
                return true;
            });
        });


        // 如果配置文件不存在
        if (!CONFIG_PATH.toFile().exists()) {
            saveConfig(new ConfigData(GLFW.GLFW_KEY_UP, "", GLFW.GLFW_KEY_RIGHT, true));
        }
        // 读取配置文件
        ConfigData config = loadConfig();

        // 创建菜单快捷键绑定
        menuKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".menu_key",
                config.menuShortcutKey,
                CATEGORY
        ));

        createPictureKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".create_picture_key",
                config.createPictureKey,
                CATEGORY
        ));

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
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
                    updateMod(latestVersion);
                } else {
                    logMessage("当前已是最新版本");

                    // 读取最新配置，检查是否开启自动进服
                    ConfigData latestConfig = loadConfig();
                    if (latestConfig.autoJoinServer()) {
                        logMessage("自动进服已开启，正在连接…");
                        MinecraftClient.getInstance().execute(() -> {
                            ServerInfo serverInfo = new ServerInfo("Agatha纯净生存", "cd.agatha.org.cn", ServerInfo.ServerType.OTHER);
                            ConnectScreen.connect(MinecraftClient.getInstance().currentScreen, MinecraftClient.getInstance(), ServerAddress.parse("cd.agatha.org.cn"), serverInfo, false, null);
                        });
                    } else {
                        logMessage("自动进服已关闭，跳过自动连接");
                    }
                }
            } catch (Exception e) {
                logMessage("检查更新时出错: " + e.getMessage());
                e.printStackTrace();
            }

        });

        // 注册客户端tick监听
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (menuKeyBinding.wasPressed()) {
                client.setScreen(new MenuScreen());
            }
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (createPictureKeyBinding.wasPressed()) {

                // 获取运行目录
                Path runDirectory = client.runDirectory.toPath();

                Path screenshotDir = runDirectory.resolve("gallery");

                try {
                    Files.createDirectories(screenshotDir);
                } catch (IOException e) {
                    throw new RuntimeException("无法创建截图目录", e);
                }
                String fileName = "ag_" + System.currentTimeMillis() + ".png";
                ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), screenshot -> {
                    try {
                        // 生成文件名

                        Path screenshotPath = screenshotDir.resolve(fileName);

                        // 保存截图
                        screenshot.writeTo(screenshotPath);

                        // screenshot 写入完成，构造绝对路径并调用本地 API
                        String absolutePath = screenshotPath.toAbsolutePath().toString();

                        // 通知玩家：截图已完成，正在尝试拉起 Minechat（调用本地 API）
                        MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("截图已保存，正在尝试调用 Minechat 本地 API...").formatted(Formatting.AQUA)));

                        // 在后台线程中执行 HTTP POST 请求到本地服务
                        new Thread(() -> {
                            HttpURLConnection conn = null;
                            try {
                                URL url = new URL("http://127.0.0.1:28188/pc/gallery/import");
                                conn = (HttpURLConnection) url.openConnection();
                                conn.setRequestMethod("POST");
                                conn.setConnectTimeout(3000);
                                conn.setReadTimeout(5000);
                                conn.setDoOutput(true);
                                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                                // 构造 JSON，确保反斜杠被转义
                                String json = "{\"path\":\"" + absolutePath.replace("\\", "\\\\") + "\"}";

                                try (OutputStream os = conn.getOutputStream()) {
                                    os.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                                }

                                int code = conn.getResponseCode();
                                if (code >= 200 && code < 300) {
                                    MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new ScreenshotSentScreen("已向 Minechat 发送上传请求", 0xFF00FF00)));
                                } else {
                                    MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new ScreenshotSentScreen("本地返回错误: " + code + "，请检查 Minechat 是否已启动", 0xFFFF5555)));
                                }

                            } catch (Exception e) {
                                // 连接失败或其他异常（例如服务未启动/未安装）
                                MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().setScreen(new ScreenshotSentScreen("无法连接到 Minechat，确认 Minechat 是否已启动。错误: " + e.getClass().getSimpleName(), 0xFFFF5555)));
                            } finally {
                                if (conn != null) {
                                    conn.disconnect();
                                }
                            }
                        }).start();

                    } catch (IOException ignored) {
                    }
                });


            }
        });

        // 游戏关闭时保存当前按键绑定到配置文件
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            ConfigData latest = loadConfig();
            int menuCode = getKeyCodeFromBinding(menuKeyBinding);
            int pictureCode = getKeyCodeFromBinding(createPictureKeyBinding);
            if (menuCode >= 0 && pictureCode >= 0) {
                saveConfig(new ConfigData(menuCode, latest.password(), pictureCode, latest.autoJoinServer()));
                logMessage("已保存当前按键配置");
            }
        });

        // 注册HUD渲染回调
        // 已经在上面注册过了，删除重复的注册
    }

    /**
     * 从 KeyBinding 对象提取当前绑定的按键码
     */
    private static int getKeyCodeFromBinding(KeyBinding binding) {
        try {
            String translationKey = binding.getBoundKeyTranslationKey();
            InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
            return key.getCode();
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 记录日志消息
     * @param message 要记录的消息
     */
    private void logMessage(String message) {
        System.out.println("[AgHelper] " + message);
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
     * @param latestVersion 最新版本
     */
    private void updateMod(String latestVersion) {
        try {
            
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
                logMessage("显示更新通知窗口");

                ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
                    if (screen instanceof TitleScreen) {
                        client.execute(() -> {
                            client.setScreen(new UpdateNotificationScreen(currentModFile, latestVersion));
                        });
                    }
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

    /** 获取当前 menuKeyBinding 的实时按键码（可能被系统界面修改过） */
    public static int getCurrentMenuKeyCode() {
        return getKeyCodeFromBinding(menuKeyBinding);
    }

    /** 获取当前 createPictureKeyBinding 的实时按键码 */
    public static int getCurrentPictureKeyCode() {
        return getKeyCodeFromBinding(createPictureKeyBinding);
    }

    public static ConfigData loadConfig() {
        try {
            File configFile = CONFIG_PATH.toFile();
            if (configFile.exists()) {
                String raw = new String(java.nio.file.Files.readAllBytes(configFile.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                ConfigData config = GSON.fromJson(raw, ConfigData.class);
                // 旧配置迁移：没有 autoJoinServer 字段时，默认为 true
                if (!raw.contains("\"autoJoinServer\"")) {
                    config = new ConfigData(config.menuShortcutKey(), config.password(), config.createPictureKey(), true);
                    saveConfig(config);
                }
                return config;
            }
        } catch (Exception e) {
            // fall through to default
        }
        return new ConfigData(GLFW.GLFW_KEY_UP, "", GLFW.GLFW_KEY_RIGHT, true);
    }

    public static void updateKeyBinding(KeyInput key, String keyName) {
        ConfigData config = loadConfig();
        int menuShortcutKey = config.menuShortcutKey;
        int keyCode = key.getKeycode();
        int createPictureKey = config.createPictureKey;
        if(keyName.equals("menuShortcutKey")){
            menuShortcutKey = keyCode;
            menuKeyBinding.setBoundKey(InputUtil.fromKeyCode(key));
            KeyBinding.updateKeysByCode();
        }
        if(keyName.equals("createPictureKey")){
            createPictureKey = keyCode;
            createPictureKeyBinding.setBoundKey(InputUtil.fromKeyCode(key));
            KeyBinding.updateKeysByCode();
        }
        saveConfig(new ConfigData(menuShortcutKey, "", createPictureKey, config.autoJoinServer()));
    }

    public static void setAutoJoinServer(boolean enabled) {
        ConfigData config = loadConfig();
        saveConfig(new ConfigData(config.menuShortcutKey(), config.password(), config.createPictureKey(), enabled));
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

    public record ConfigData(int menuShortcutKey, String password, int createPictureKey, boolean autoJoinServer) {}
}