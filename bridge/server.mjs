import { WebSocketServer } from "ws";
import { TikTokLiveConnection } from "tiktok-live-connector";

const PORT = Number(process.env.PORT || 8765);
const username = (process.env.TIKTOK_USERNAME || "").replace(/^@/, "").trim();

if (!username || username === "your_tiktok_username_without_at") {
  console.error("Set TIKTOK_USERNAME in bridge/.env or environment first.");
  process.exit(1);
}

const clients = new Set();
const wss = new WebSocketServer({ host: "127.0.0.1", port: PORT });

function broadcast(payload) {
  const data = JSON.stringify(payload);
  for (const client of clients) {
    if (client.readyState === 1) client.send(data);
  }
}

wss.on("connection", socket => {
  clients.add(socket);
  socket.send(JSON.stringify({ type: "status", text: "Bridge ready; waiting for TikTok LIVE" }));
  socket.on("close", () => clients.delete(socket));
});

const live = new TikTokLiveConnection(username);
live.on("connected", () => broadcast({ type: "status", text: "TikTok LIVE connected" }));
live.on("disconnected", () => broadcast({ type: "status", text: "TikTok LIVE disconnected" }));
live.on("chat", data => broadcast({
  type: "chat",
  user: data.nickname || data.uniqueId || "viewer",
  comment: data.comment || ""
}));
live.on("gift", data => broadcast({
  type: "gift",
  user: data.nickname || data.uniqueId || "viewer",
  gift: data.giftName || data.gift?.name || "gift",
  repeat: data.repeatCount || 1
}));
live.on("roomUser", data => {
  const count = data.viewerCount ?? data.viewer_count ?? data.total;
  if (count !== undefined) broadcast({ type: "viewer", count: String(count) });
});
live.on("error", error => {
  console.error("TikTok connector error:", error?.message || error);
  broadcast({ type: "status", text: "TikTok connection error" });
});

wss.on("listening", () => {
  console.log(`BAYZU bridge listening on ws://127.0.0.1:${PORT}`);
  console.log(`Connecting to TikTok LIVE @${username} ...`);
});
live.connect().catch(error => {
  console.error("Could not connect to LIVE:", error?.message || error);
  broadcast({ type: "status", text: "TikTok LIVE not found / unavailable" });
});
