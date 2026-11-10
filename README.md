# Hyrule Watch

A native Android stock and price tracker for **The Legend of Zelda 40th Anniversary** Nintendo Switch 2 console, Pro Controller, and Pro Controller with display stand. Built for US shoppers, with a configurable Colorado ZIP reminder (Denver **80202** by default).

## Install

Download `hyrule-watch-1.0.0-preview.apk` from the repository's Releases page on your Android phone and allow installation from that download source when Android prompts. Requires Android 8.0 or newer. This is a debug-signed preview, not a Play Store release; use a managed release signing key before distributing production updates.

1. Open the app and tap **Check stock**.
2. Open **Settings** to change the ZIP, enable background checks, and enable notifications. Allow Android notification permission when prompted.
3. Tap **Open retailer** to verify availability and buy. Select your ZIP/store on that retailer's website for Colorado pickup.
4. Use **Manage** to set a target USD price, pause tracking, view history, or remove a listing. Add more supported retailer product pages with **Add listing**.

## What's tracked

Seven initial listing URLs cover all three hardware variants:

| Retailer | Console | Pro Controller | Pro Controller + stand |
| --- | --- | --- | --- |
| Nintendo US | Yes | Yes | Yes |
| Best Buy | Yes | Add URL | Add URL |
| GameStop | Yes | Yes | Add URL |
| Target | Add URL | Yes | Add URL |
| Walmart | Add URL | Add URL | Add URL |

These are product listings, not a claim that inventory is available. This app cannot enumerate every unit, retailer, marketplace seller, or Colorado store. It checks the saved URLs; additional product URLs can be added from the five supported retailers (up to 30 listings).

## How checks and alerts work

- HTTPS requests read public, product-scoped **schema.org JSON-LD** offers. The product name must match Zelda, 40th anniversary, Switch 2, and the selected hardware variant. Related product recommendations are excluded.
- Stock and USD prices are never seeded or fabricated. Malformed data, bot challenges, missing product data, conflicting offers, timeouts and HTTP failures yield **Unknown / Needs browser check**. Some retailers require JavaScript or block requests, so automatic coverage is partial. No CAPTCHA bypasses or private retailer APIs are used.
- Each card shows when it was checked. Even a successful observation may become stale; verify seller, actual price, shipping, pickup and availability at checkout. Online schema data does **not** verify Colorado pickup inventory. Your ZIP is a reminder, not an inventory query or a browser location setting.
- Optional Android JobScheduler checks run roughly every 15 minutes with network access. Android battery/network policies may delay them. Force stopping the app stops jobs until it is opened again. No always-on server is required.
- Alerts fire when a watched listing first reports in-stock/preorder or returns to that state, or when a buyable offer crosses a target price. Unknown checks do not reset the last confirmed state and cause repeated restock alerts. Notifications must be enabled both in the app and in Android settings.
- Up to 40 recent observations per listing are saved on-device. **Settings → Export history** saves JSON through Android's document picker. Uninstalling clears app data; export before uninstalling. Export is an archive, not an import/restore feature.

## Build and validate

Use Android Studio with JDK 17+ and Android SDK 35, or:

```sh
export ANDROID_HOME=/path/to/android-sdk
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs the same checks and uploads a debug APK artifact. Runtime code uses Android platform APIs with no third-party runtime dependencies. Gradle wrapper 8.11.1 and Android Gradle Plugin 8.9.2 are pinned.

Tests cover product and variant matching, explicit stock states, malformed and challenge pages, conflicting offers, foreign currencies, graph traversal, related products, trusted HTTPS retailer URLs, and restock/price-alert transitions.

Validation on November 10, 2026: APK assembly passed, all 11 unit tests passed, and Android lint reported no issues. The parser also read price and availability from an actual fetched Nintendo US controller page. A device/emulator UI smoke test remains outstanding: the environment lacks hardware acceleration and the emulator requires more data-partition disk space than is available. Install and test the preview on your phone before relying on notifications.

## Sources and privacy

Product names and initial URLs were researched on November 10, 2026. Primary product sources:

- [Nintendo console](https://www.nintendo.com/us/store/products/nintendo-switch-2-the-legend-of-zelda-40th-anniversary-edition-121642/)
- [Nintendo Pro Controller](https://www.nintendo.com/us/store/products/nintendo-switch-2-pro-controller-the-legend-of-zelda-40th-anniversary-edition-127074/)
- [Nintendo Pro Controller with display stand](https://www.nintendo.com/us/store/products/nintendo-switch-2-pro-controller-display-stand-the-legend-of-zelda-40th-anniversary-edition-127076/)

No account, analytics, location permission, advertising SDK, purchase automation, or backend. Retailer requests disclose your IP address and the requested product URL to those retailers; opening a retailer uses your chosen browser and its cookies. ZIP and history stay on-device unless you export them. Device backup is disabled.

Independent fan project, not affiliated with or endorsed by Nintendo. Product trademarks belong to their respective owners. Hardware artwork in the app is a simple programmatic illustration rather than official product photography.
