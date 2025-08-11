package cn.org.agatha.aghelper.client;

import cn.org.agatha.aghelper.client.utils.CreatePicture;
import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class AghelperClient implements ClientModInitializer {

    private static KeyBinding menuKeyBinding;
    private static KeyBinding autologinKeyBinding;
    private static KeyBinding createPictureKeyBinding;
    private static final String MOD_ID = "aghelper";
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Path.of("config/aghelper.json");

    @Override
    public void onInitializeClient() {
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
