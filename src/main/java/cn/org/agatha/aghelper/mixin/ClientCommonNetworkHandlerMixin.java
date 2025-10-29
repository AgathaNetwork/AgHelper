package cn.org.agatha.aghelper.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void onDisconnected(DisconnectionInfo info, CallbackInfo ci) {
        Text reason = info.reason();
        String reasonString = reason.getString();
        if (reasonString.contains("DMS_QUITTING_MAIN_SERVER")){
            // 然后连接到测试服
            ServerInfo serverInfo = new ServerInfo("Agatha纯净生存测试服", "doris.agatha.org.cn", ServerInfo.ServerType.OTHER);
            // 连接到服务器
            ConnectScreen.connect(null, MinecraftClient.getInstance(), ServerAddress.parse("doris.agatha.org.cn"), serverInfo,  false, null);

        }
        else if (reasonString.contains("DMS_QUITTING_DORIS")){
            // 然后连接到主服
            ServerInfo serverInfo = new ServerInfo("Agatha纯净生存主服", "agatha.org.cn", ServerInfo.ServerType.OTHER);
            // 连接到服务器
            ConnectScreen.connect(null, MinecraftClient.getInstance(), ServerAddress.parse("agatha.org.cn"), serverInfo,  false, null);
        }
    }
}