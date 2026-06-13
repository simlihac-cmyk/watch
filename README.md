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
GET /v1/search?q=BTC
POST /v1/watchlist
GET /v1/quotes?ids=BTC,ETH,SOL,XRP
GET /v1/candles?id=BTC&tf=5m&limit=120
GET /v1/indicators?id=BTC&tf=5m
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
4. Open the app once to load quotes; the tile then shows the last successful BTC, ETH, SOL, and XRP price/change cache.

The tile opens the main app when tapped. It shows compact prices plus 24h percentage changes and does not call Binance, KIS, or any provider directly.

## Add Assets On The Watch

Use the `+` button on the watch list to search and add assets. The watch sends the query only to the FastAPI server.

The current MVP is intentionally crypto-only:

- Crypto ids already configured on the server: `BTC`, `ETH`, `SOL`, and `XRP`.

Stock search/add can be enabled later on the server after choosing and configuring a stock data provider. The watch app still never talks directly to Binance, KIS, or other providers.

## Detail Indicators

The detail screen loads candles first, then requests `/v1/indicators` for the selected asset and timeframe. The chart and price remain visible if indicators fail; only the indicator panel shows `Unavailable`.

Current indicators:

- `RSI 14`
- `24h Vol`
- `Now Vol`
- `Avg 7d`
- `Avg 30d`
- `Avg 6m`
- `Avg 1y`

Volume values are displayed in the server-provided `volumeCurrency`, currently `USDT` for crypto.

## Price Alerts

The debug app schedules a local Wear OS price-alert job when opened. It checks the FastAPI server for configured Binance crypto assets and sends a watch notification when an asset's 24h change crosses:

- 5%
- 10%
- 15%

Alerts are based on the absolute 24h change rate, so both sharp gains and drops can notify. Each asset/threshold pair can notify only once per local calendar day; the record resets after midnight on the watch.

The app requests Android notification permission on first launch. If notifications are denied, alerts are not shown.

For the full real-device debug workflow, including Cloudflare HTTPS setup checks, token configuration, wireless ADB, and logcat commands, see [../docs/wear-debugging.md](../docs/wear-debugging.md).
