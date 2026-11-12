package id.bayzu.livehud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.CompletionStage;

public final class BayzuLiveHudClient implements ClientModInitializer {
    private static final Deque<String> CHAT = new ArrayDeque<>();
    private static volatile String status = "Bridge offline";
    private static volatile String viewer = "Viewer: --";
    private static volatile String gift = "Gift: --";
    private static volatile boolean enabled = true;

    @Override
    public void onInitializeClient() {
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath("bayzu_livehud", "floating_overlay"),
            (graphics, deltaTracker) -> {
                Minecraft mc = Minecraft.getInstance();
                if (!enabled || mc.player == null || mc.options.hideGui) return;

                int x = 10;
                int y = 10;
                int width = 188;
                graphics.fill(x, y, x + width, y + 92, 0xB8101020);
                graphics.fill(x, y, x + width, y + 2, 0xFFB56CFF);
                graphics.drawString(mc.font, Component.literal("BAYZU  •  TIKTOK LIVE"), x + 7, y + 7, 0xFFFFFFFF, false);
                graphics.drawString(mc.font, Component.literal(status), x + 7, y + 20, 0xFFB9A4D8, false);
                graphics.drawString(mc.font, Component.literal(viewer), x + 7, y + 33, 0xFFFFD36E, false);
                graphics.drawString(mc.font, Component.literal(gift), x + 7, y + 46, 0xFFFF8CCF, false);
                graphics.fill(x + 7, y + 59, x + width - 7, y + 60, 0x665F5577);

                int lineY = y + 65;
                synchronized (CHAT) {
                    for (String line : CHAT) {
                        graphics.drawString(mc.font, Component.literal(line), x + 7, lineY, 0xFFE9E4F2, false);
                        lineY += 10;
                    }
                }
            }
        );
        connectBridge();
    }

    private static void connectBridge() {
        HttpClient.newHttpClient().newWebSocketBuilder()
            .buildAsync(URI.create("ws://127.0.0.1:8765"), new WebSocket.Listener() {
                @Override
                public void onOpen(WebSocket webSocket) {
                    status = "Bridge connected";
                    webSocket.request(1);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    try {
                        JsonObject event = JsonParser.parseString(data.toString()).getAsJsonObject();
                        String type = event.has("type") ? event.get("type").getAsString() : "unknown";
                        switch (type) {
                            case "status" -> status = safe(event, "text", "Connected");
                            case "viewer" -> viewer = "Viewer: " + safe(event, "count", "--");
                            case "chat" -> {
                                String user = safe(event, "user", "viewer");
                                String comment = safe(event, "comment", "");
                                String line = (user + ": " + comment);
                                if (line.length() > 27) line = line.substring(0, 27) + "…";
                                synchronized (CHAT) {
                                    CHAT.addFirst(line);
                                    while (CHAT.size() > 3) CHAT.removeLast();
                                }
                            }
                            case "gift" -> gift = safe(event, "user", "viewer") + " • " +
                                safe(event, "gift", "gift") + " x" + safe(event, "repeat", "1");
                        }
                    } catch (Exception ignored) { }
                    webSocket.request(1);
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    status = "Bridge error / offline";
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                    status = "Bridge disconnected";
                    return null;
                }
            });
    }

    private static String safe(JsonObject obj, String key, String fallback) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : fallback;
    }
}
