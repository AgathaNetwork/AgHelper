package cn.org.agatha.aghelper.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.message.MapMessage.MapFormat.JSON;

public class CreatePicture_2 extends Screen {
    protected String shaderConfig;
    protected String position;
    protected String filePath;
    protected String name;
    protected String remark;
    protected String username;

    // 添加预设字典
    protected Map<Integer, String> worldOptions = new HashMap<>();
    protected Map<Integer, String> categoryOptions = new HashMap<>();
    
    // 保存选中值的变量
    protected Integer selectedWorld = 1;
    protected Integer selectedCategory = 1;


    public int errorFieldX, errorFieldY;

    private List<String> onlinePlayers = new ArrayList<>();
    private Set<String> selectedPlayers = new HashSet<>();
    private List<CheckboxWidget> playerCheckboxes = new ArrayList<>();

    protected CreatePicture_2(String shaderConfig, String position, String filePath, String name, String remark, String username) {
        super(Text.literal("创建照片"));
        this.shaderConfig = shaderConfig;
        this.position = position;
        this.filePath = filePath;
        this.name = name;
        this.remark = remark;
        this.username = username;
        // 初始化预设字典
        initOptions();
    }

    public TextWidget errorField;
    public TextFieldWidget passwordField;
    public ButtonWidget confirmButton;
    public void updateConfirmStatus(){
        boolean isValid = true;
        if (passwordField.getText() == null || passwordField.getText().isEmpty()) {
            isValid = false;
        }
        if (selectedPlayers.isEmpty()) { // 增加玩家选择校验
            isValid = false;
        }
        confirmButton.active = isValid;
    }
    private void initOptions() {
        // 初始化世界选项（示例数据）
        worldOptions.put(1, "主世界");
        worldOptions.put(2, "下界");
        worldOptions.put(3, "末地");
        
        // 初始化类别选项（示例数据）
        categoryOptions.put(1, "生存");
        categoryOptions.put(2, "建筑");
        categoryOptions.put(3, "生电");
        categoryOptions.put(4, "赤石");
        categoryOptions.put(5, "合照");
        categoryOptions.put(7, "活动");
        categoryOptions.put(8, "其他");

        selectedWorld = 1;
        selectedCategory = 1;
    }

    @Override
    protected void init() {
        // 计算屏幕尺寸和布局参数
        int screenWidth = client.getWindow().getScaledWidth();
        int formWidth = 300;
        int centerX = (screenWidth - formWidth) / 2;
        int startY = 20;
        int spacing = 25;
        int currentY = startY;

        // 创建世界选择下拉框
        CyclingButtonWidget<Integer> worldButton = CyclingButtonWidget.builder((Integer value) ->
                        Text.literal("" + worldOptions.get(value)))
                .values(worldOptions.keySet().toArray(new Integer[0]))
                .initially(selectedWorld)
                .build(centerX, currentY, formWidth, 20, Text.literal("世界"), (button, value) -> {
                    selectedWorld = value;
                });
        this.addDrawableChild(worldButton);
        currentY += spacing;

        // 创建类别选择下拉框
        CyclingButtonWidget<Integer> categoryButton = CyclingButtonWidget.builder((Integer value) ->
                        Text.literal("" + categoryOptions.get(value)))
                .values(categoryOptions.keySet().toArray(new Integer[0]))
                .initially(selectedCategory)
                .build(centerX, currentY, formWidth, 20, Text.literal("类别"), (button, value) -> {
                    selectedCategory = value;
                });
        this.addDrawableChild(categoryButton);
        currentY += spacing;

        onlinePlayers.clear();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null){
            Collection<PlayerListEntry> playerList = client.getNetworkHandler().getPlayerList();
            for (PlayerListEntry entry : playerList){
                String playerName = URLEncoder.encode(entry.getProfile().getName(), StandardCharsets.UTF_8);
                onlinePlayers.add(playerName);
            }
        }

        playerCheckboxes.clear();
        
        for (int i = 0; i < onlinePlayers.size(); i++) {
            String playerName = onlinePlayers.get(i);
            boolean isSelected = selectedPlayers.contains(playerName);

            int thisX = centerX;
            if(i % 2 == 1) thisX = centerX + formWidth/2;
            CheckboxWidget checkbox = CheckboxWidget.builder(Text.literal(playerName), this.textRenderer)
                    .pos(thisX, currentY)
                    .checked(isSelected)
                    .callback((checkboxWidget, checked) -> {
                        if (checked){
                            selectedPlayers.add(playerName);
                        } else {
                            selectedPlayers.remove(playerName);
                        }
                        updateConfirmStatus(); // 增加状态更新
                    })
                    .build();
            this.addDrawableChild(checkbox);
            playerCheckboxes.add(checkbox);
            if(i % 2 == 1) currentY += spacing;
        }
        currentY += spacing;

        // 密码输入框
        passwordField = new TextFieldWidget(textRenderer, centerX, currentY, formWidth, 20, Text.of("密码"));
        passwordField.setPlaceholder(Text.literal("请输入密码"));
        this.addDrawableChild(passwordField);
        currentY += spacing * 2;


        // 添加报错显示框，文字默认为红色
        errorField = new TextWidget(centerX, currentY, formWidth, 20, Text.literal("").formatted(Formatting.RED), textRenderer);
        errorFieldX = centerX;
        errorFieldY = currentY;

        currentY += spacing;
        // 添加确认按钮
        confirmButton = ButtonWidget.builder(Text.literal("确认"), button -> {
            // 处理确认逻辑
            onConfirm(passwordField.getText());
        }).dimensions(screenWidth/2 - 40, currentY, 80, 20).build();
        confirmButton.active = false;

        // 监听密码输入框的变化
        passwordField.setChangedListener(password -> {
            updateConfirmStatus();
        });

        this.addDrawableChild(confirmButton);
        updateConfirmStatus(); // 初始化按钮状态
    }

    private void onConfirm(String password) {
        confirmButton.active = false;
        // 发送HTTP POST请求，x-www-form-urlencoded格式
        String url = "https://api-gallery.agatha.org.cn/gallery/create";
        // playerJSON格式：["MikeWu597",""....]，首先制作字符串数组，然后合成JSON
        String playersJSON = "[\"" + String.join("\",\"", selectedPlayers) + "\"]";
        // metadata格式：{"name":"bname","size":1024,"h":1080,"w":1080}，从PNG文件中解析
        File file = new File(filePath);
        BufferedImage image = null;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int width = image.getWidth();
        int height = image.getHeight();
        int sizeKiB = (int) (file.length() / 1024);
        String filePathTrimmed = filePath.substring(filePath.lastIndexOf("/") + 1);
        String metadata = "{\"name\":\"" + filePathTrimmed + "\",\"size\":" + sizeKiB + ",\"h\":" + height + ",\"w\":" + width +"}";
        Map<String, String> params = new HashMap<>();
        params.put("username", URLEncoder.encode(username, StandardCharsets.UTF_8));
        params.put("password", URLEncoder.encode(password, StandardCharsets.UTF_8));
        params.put("name", URLEncoder.encode(name, StandardCharsets.UTF_8));
        params.put("annotation", URLEncoder.encode(remark, StandardCharsets.UTF_8));
        params.put("world", URLEncoder.encode(selectedWorld.toString()));
        params.put("type", URLEncoder.encode(selectedCategory.toString()));
        params.put("year", String.valueOf(new Date().getYear() + 1900));
        params.put("players", URLEncoder.encode(playersJSON, StandardCharsets.UTF_8));
        params.put("shader", URLEncoder.encode(shaderConfig, StandardCharsets.UTF_8));
        params.put("position", URLEncoder.encode(position, StandardCharsets.UTF_8));
        params.put("metadata", URLEncoder.encode(metadata, StandardCharsets.UTF_8));
        params.put("timestamp", String.valueOf((int) Math.floor(new Date().getTime()/1000)));
        String publishers = params.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("&"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(publishers))
                .build();
        HttpClient clientHTTP = HttpClient.newHttpClient();
        try {
            HttpResponse<String> response = clientHTTP.send(request, HttpResponse.BodyHandlers.ofString());


            //判断状态码
            if (response.statusCode() == 200){

                String responseBody = response.body();
                // 解析JSON
                JsonObject jsonObject;
                try {
                    jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                } catch (JsonSyntaxException e) {
                    throw new RuntimeException("JSON解析失败：" + responseBody, e);
                }
                // 获取id
                if (!jsonObject.has("id")) {
                    throw new RuntimeException("响应中缺少id字段：" + responseBody);
                }
                String id = jsonObject.get("id").getAsString();
                // 发送POST请求，格式：form-data
                String urlUpload = "https://api-gallery.agatha.org.cn/gallery/upload";
                File fileUpload = new File(filePath);
                // form共有4个参数：username password id file ，其中file为文件

                List<byte[]> parts = new ArrayList<>();
                String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();

                // 添加文本字段
                parts.add(("--" + boundary + "\r\n").getBytes());
                parts.add(("Content-Disposition: form-data; name=\"username\"\r\n\r\n").getBytes());
                parts.add((username + "\r\n").getBytes());

                parts.add(("--" + boundary + "\r\n").getBytes());
                parts.add(("Content-Disposition: form-data; name=\"password\"\r\n\r\n").getBytes());
                parts.add((password + "\r\n").getBytes());

                parts.add(("--" + boundary + "\r\n").getBytes());
                parts.add(("Content-Disposition: form-data; name=\"id\"\r\n\r\n").getBytes());
                parts.add((id + "\r\n").getBytes());

                // 添加文件字段
                parts.add(("--" + boundary + "\r\n").getBytes());
                parts.add(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileUpload.getName() + "\"\r\n").getBytes());
                parts.add(("Content-Type: image/png\r\n\r\n").getBytes());
                parts.add(Files.readAllBytes(fileUpload.toPath()));
                parts.add(("\r\n").getBytes());

                // 结束分隔符
                parts.add(("--" + boundary + "--\r\n").getBytes());

                HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofByteArrays(parts);

                HttpClient clientUpload = HttpClient.newHttpClient();
                HttpRequest requestUpload = HttpRequest.newBuilder()
                        .uri(URI.create(urlUpload))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(bodyPublisher)
                        .build();
                HttpResponse<String> responseUpload = clientUpload.send(requestUpload, HttpResponse.BodyHandlers.ofString());
                // 回收响应内容
                JsonObject retJSON = new JsonParser().parse(responseUpload.body()).getAsJsonObject();
                // 判断是否含有File uploaded successfully
                if (retJSON.get("message") != null && retJSON.get("message").getAsString().contains("File uploaded successfully")){
                    MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("图片上传成功").formatted(Formatting.GREEN));
                    client.setScreen(null);
                }
            }
            else if (response.statusCode() == 401){
                //写上“密码不正确”，width为五个字符的宽度
                this.errorField = new TextWidget(errorFieldX, errorFieldY, 50, 20, Text.literal("密码错误").formatted(Formatting.RED), MinecraftClient.getInstance().textRenderer);
                this.addDrawableChild(errorField);
                updateConfirmStatus();
            }
        } catch (IOException e) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("图片上传失败").formatted(Formatting.RED));
            updateConfirmStatus();
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("图片上传失败").formatted(Formatting.RED));
            updateConfirmStatus();
            throw new RuntimeException(e);
        }
        
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