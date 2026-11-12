# BAYZU TikTok LIVE HUD (Minecraft Java 1.21.11)

Client-side Fabric mod + local Node.js bridge. It displays a compact floating HUD with:
- TikTok LIVE connection status
- viewer count (when the connector emits room-user statistics)
- latest chat messages
- latest gift event

## Compatibility

- Minecraft Java: 1.21.11
- Fabric Loader: 0.18.x
- Fabric API: 0.141.1+1.21.11
- Java: 21
- Bridge: Node.js 20+

## Build the mod

On a computer with JDK 21 and Gradle installed:

```bash
gradle build
```

The JAR will be in `build/libs/`.

Or push this project to GitHub and run the **Build Fabric Mod** workflow. Download the workflow artifact and copy the JAR into the Minecraft `mods` folder. Install Fabric API too.

## Run the TikTok bridge

1. Install Node.js 20+.
2. In `bridge/`, install packages:

```bash
npm install
```

3. Set your TikTok LIVE username (without @):

```bash
export TIKTOK_USERNAME=your_username
npm start
```

The bridge listens only on `127.0.0.1:8765`; it does not expose a public network port.
4. Start Minecraft with the mod. The HUD connects to `ws://127.0.0.1:8765`.

## Android / Termux note

The bridge can run in Termux on the same Android device if Node.js 20+ is available. Start it before Minecraft. The launcher must share the same device loopback network namespace for `127.0.0.1` to work; if not, use a LAN bridge host and update the WebSocket address in `BayzuLiveHudClient.java`.

## Important limitations

- TikTok LIVE Connector is an unofficial community reverse-engineered integration, not an official TikTok API. TikTok may change it or limit event delivery. It only works while the target account is actually live and publicly accessible.
- Viewer statistics depend on the event payload available for that room; the HUD displays `--` if no count arrives.
- This is an initial source scaffold. It has not been compiled or tested in a running Minecraft client in this environment. The JAR is not included until the Gradle build succeeds.
- The current HUD is fixed at the top-left. Drag-to-move controls, config screen, gift icons/animations and reconnect backoff are planned next.
- No TikTok password, session cookie, or message-sending permission is requested.
