package cn.org.agatha.aghelper.client;

import com.google.gson.Gson;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;

public class AghelperClient implements ClientModInitializer {

    private static KeyBinding menuKeyBinding;
    private static KeyBinding autologinKeyBinding;
    private static final String MOD_ID = "aghelper";
    private static final Gson GSON = new Gson();
    private static final Path CONFIG_PATH = Path.of("config/aghelper.json");

    @Override
    public void onInitializeClient() {
        // 如果配置文件不存在
        if (!CONFIG_PATH.toFile().exists()) {
            saveConfig(new ConfigData(GLFW.GLFW_KEY_UP, "", "", GLFW.GLFW_KEY_ENTER));
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
        // 注册客户端tick监听
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (menuKeyBinding.wasPressed()) {
                client.setScreen(new MenuScreen());
            }
        });
    }

    static ConfigData loadConfig() {
        try {
            return GSON.fromJson(new FileReader(CONFIG_PATH.toFile()), ConfigData.class);
        } catch (Exception e) {
            return new ConfigData(GLFW.GLFW_KEY_UP, "", "", GLFW.GLFW_KEY_ENTER);
        }
    }

    public static void updateKeyBinding(int keyCode, String keyName, int scanCode) {
        ConfigData config = loadConfig();
        int menuShortcutKey = config.menuShortcutKey;
        String password = config.password;
        String username = config.username;
        int autologinKey = config.autologinKey;
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
        saveConfig(new ConfigData(menuShortcutKey, password, username, autologinKey));
    }

    public static void updateAutologinConfig(String username, String password) {
        ConfigData config = loadConfig();
        int menuShortcutKey = config.menuShortcutKey;
        String oldPassword = config.password;
        String oldUsername = config.username;
        int autologinKey = config.autologinKey;
        if(username.equals("")){
            username = oldUsername;
        }
        if(password.equals("")){
            password = oldPassword;
        }
        saveConfig(new ConfigData(menuShortcutKey, password, username, autologinKey));
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

    private record ConfigData(int menuShortcutKey, String password, String username, int autologinKey) {}
}
