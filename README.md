# Watch Market Wear OS App

Minimal Wear OS app for the private Watch Market FastAPI server.

This project contains only a watch app. There is no phone companion module, no trading/order/wallet flow, and no provider API keys.

## Open In Android Studio

1. Install Android Studio with JDK 17 and the Android SDK.
2. Open the `watch/` directory.
3. Let Android Studio sync Gradle.
4. Install SDK Platform 36 if prompted.

## Debug Server Config

Debug builds read the server URL and bearer token from Gradle properties or environment variables:

```bash
WATCH_MARKET_DEBUG_BASE_URL=https://watch-market.example.com/
WATCH_MARKET_DEBUG_BEARER_TOKEN=replace-locally-only
```

Do not commit real tokens. Prefer putting local values in `~/.gradle/gradle.properties`:

```properties
WATCH_MARKET_DEBUG_BASE_URL=https://watch-market.example.com/
WATCH_MARKET_DEBUG_BEARER_TOKEN=replace-locally-only
```

The app is intended to call only the FastAPI server. Binance, KIS, or other provider keys must never be added to the watch app.

## API Client

The watch app uses Retrofit to call only the FastAPI server:

```text
GET /v1/assets
GET /v1/quotes?ids=BTC,ETH
GET /v1/candles?id=BTC&tf=5m&limit=120
```

`FastApiMarketRepository` adds the Bearer token through an OkHttp interceptor and returns `MarketApiResult` values for success, unauthorized, network failure, HTTP failure, and invalid responses.

## Build Debug APK

```bash
cd watch
./gradlew :app:assembleDebug
```

The APK is written to:

```text
watch/app/build/outputs/apk/debug/app-debug.apk
```

## Install On Wear OS Watch

Enable developer options and ADB debugging on the watch, then connect over USB or wireless debugging.

```bash
adb devices
adb -s <watch-serial> install -r app/build/outputs/apk/debug/app-debug.apk
adb -s <watch-serial> shell monkey -p com.sg.watchmarket 1
```

If you have only one device attached, you can omit `-s <watch-serial>`.

## Add The Watch Tile

The debug APK includes a Wear OS Tile named `Market glance`. On the watch:

1. Long-press the watch face.
2. Edit tiles/widgets.
3. Add `Market glance`.
4. Open the app once to load quotes; the tile then shows the last successful BTC and ETH quote cache.

The tile opens the main app when tapped. It does not call Binance, KIS, or any provider directly.

For the full real-device debug workflow, including Cloudflare HTTPS setup checks, token configuration, wireless ADB, and logcat commands, see [../docs/wear-debugging.md](../docs/wear-debugging.md).
