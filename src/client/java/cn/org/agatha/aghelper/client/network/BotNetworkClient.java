package cn.org.agatha.aghelper.client.network;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class BotNetworkClient {

    public static final Identifier CHANNEL = Identifier.of("aghelper", "bot");

    // C2S
    private static final byte C2S_LIST      = 0x01;
    private static final byte C2S_CREATE    = 0x02;
    private static final byte C2S_REMOVE    = 0x03;
    private static final byte C2S_RENAME    = 0x04;
    private static final byte C2S_TP_TO_BOT = 0x05;
    private static final byte C2S_TP_HERE   = 0x06;

    // S2C
    private static final byte S2C_LIST      = 0x10;
    private static final byte S2C_RESULT    = 0x11;

    // cached bot list for particle rendering
    private static List<BotInfo> cachedBots = Collections.emptyList();
    private static final Random RANDOM = new Random();

    // ── payload ──

    public record BotPayload(PacketByteBuf data) implements CustomPayload {
        private static final Id<BotPayload> ID = new Id<>(CHANNEL);

        public static final PacketCodec<RegistryByteBuf, BotPayload> CODEC = PacketCodec.of(
                (BotPayload payload, RegistryByteBuf buf) -> {
                    var src = payload.data;
                    buf.writeBytes(src, src.readerIndex(), src.readableBytes());
                },
                (RegistryByteBuf buf) -> {
                    PacketByteBuf copy = PacketByteBufs.create();
                    copy.writeBytes(buf, buf.readableBytes());
                    return new BotPayload(copy);
                }
        );

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    // ── callback interface ──

    public interface PacketHandler {
        void onBotList(List<BotInfo> bots);
        void onResult(boolean success, String message);
    }

    private static PacketHandler handler;

    public static void setHandler(PacketHandler h) {
        handler = h;
    }

    public static void clearHandler() {
        handler = null;
    }

    // ── register ──

    public static void register() {
        // register payload type codecs (required by Fabric API 0.136.0+)
        PayloadTypeRegistry.playS2C().register(BotPayload.ID, BotPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(BotPayload.ID, BotPayload.CODEC);

        // network receiver
        ClientPlayNetworking.registerGlobalReceiver(BotPayload.ID, (payload, context) -> {
            PacketByteBuf buf = payload.data();
            byte packetId = buf.readByte();

            if (packetId == S2C_LIST) {
                int count = buf.readInt();
                List<BotInfo> bots = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    bots.add(new BotInfo(
                            buf.readString(),
                            buf.readString(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readDouble(),
                            buf.readString()
                    ));
                }
                context.client().execute(() -> {
                    cachedBots = bots;
                    if (handler != null) handler.onBotList(bots);
                });
            } else if (packetId == S2C_RESULT) {
                boolean success = buf.readBoolean();
                String message = buf.readString();
                context.client().execute(() -> {
                    if (handler != null) handler.onResult(success, message);
                });
            }
        });

        // particle tick — render bot position markers in world
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;
            String playerDim = client.world.getRegistryKey().getValue().toString();

            for (BotInfo bot : cachedBots) {
                if (!playerDim.equals(bot.dim())) continue;

                double dx = bot.x() - client.player.getX();
                double dz = bot.z() - client.player.getZ();
                if (dx * dx + dz * dz > 64 * 64) continue;

                for (int i = 0; i < 2; i++) {
                    double px = bot.x() + (RANDOM.nextDouble() - 0.5) * 0.4;
                    double py = bot.y() + 1.0 + RANDOM.nextDouble() * 1.5;
                    double pz = bot.z() + (RANDOM.nextDouble() - 0.5) * 0.4;
                    client.particleManager.addParticle(
                            ParticleTypes.END_ROD, px, py, pz, 0.0, 0.02, 0.0
                    );
                }
            }
        });
    }

    // ── send methods ──

    public static void requestList() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(C2S_LIST);
        ClientPlayNetworking.send(new BotPayload(buf));
    }

    public static void createBot(String customName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(C2S_CREATE);
        buf.writeString(customName);
        ClientPlayNetworking.send(new BotPayload(buf));
    }

    public static void removeBot(String fullName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(C2S_REMOVE);
        buf.writeString(fullName);
        ClientPlayNetworking.send(new BotPayload(buf));
    }

    public static void renameBot(String oldFullName, String newCustomName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(C2S_RENAME);
        buf.writeString(oldFullName);
        buf.writeString(newCustomName);
        ClientPlayNetworking.send(new BotPayload(buf));
    }

    public static void teleportToBot(String fullName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(C2S_TP_TO_BOT);
        buf.writeString(fullName);
        ClientPlayNetworking.send(new BotPayload(buf));
    }

    public static void teleportBotHere(String fullName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(C2S_TP_HERE);
        buf.writeString(fullName);
        ClientPlayNetworking.send(new BotPayload(buf));
    }

    // ── BotInfo ──

    public record BotInfo(String name, String dim, double x, double y, double z, String owner) {}
}
