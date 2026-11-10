# Hyrule Watch

A native Android stock and price tracker for **The Legend of Zelda 40th Anniversary** Nintendo Switch 2 console, Pro Controller, and Pro Controller with display stand. Built for US shoppers, with an editable ZIP code (**80227** by default). Each person can set their own US ZIP in Settings; coverage is currently limited to US retailer sites.

## Install

Download `hyrule-watch-1.0.1-preview.apk` from the repository's Releases page on your Android phone and allow installation from that download source when Android prompts. Requires Android 8.0 or newer. This is a debug-signed preview, not a Play Store release; use a managed release signing key before distributing production updates.

1. Open the app and tap **Check stock**.
2. Open **Settings** to change the ZIP, enable background checks, and enable notifications. Allow Android notification permission when prompted.
3. Tap **Open retailer** to verify availability and buy. Select your own ZIP/store on that retailer's website for delivery and pickup.
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

These are product listings, not a claim that inventory is available. This app cannot enumerate every unit, retailer, marketplace seller, or local store. It checks the saved URLs; additional product URLs can be added from the five supported retailers (up to 30 listings).

## How checks and alerts work

- HTTPS requests read product-scoped **schema.org JSON-LD** listing prices and stock reports. The product name must match Zelda, 40th anniversary, Switch 2, and the selected hardware variant. Related product recommendations are excluded.
- **A generic `InStock` or preorder flag never establishes orderability.** GameStop, Best Buy, Target and Walmart positive listing flags display **Availability unverified** and do not trigger stock/target alerts. They can describe global inventory or a fulfillment option that is unavailable to your ZIP.
- For Nintendo US only, a positive online stock report additionally requires a boolean `isSalableQty: true` in its public page state, bound to the exact URL SKU, the main page's SKU/name, and the corresponding product record. A matching `false` overrides an optimistic listing flag with **Not orderable online**. Missing, malformed, wrong-SKU, and string-valued flags cannot establish orderability. Disagreement between sources never produces a positive stock report.
- Even **Online stock reported** means Nintendo's page reports orderability, not that checkout, delivery to your address, or pickup is confirmed. Every card explicitly labels delivery/pickup for the selected ZIP as **unverified**. Reported prices are listing prices; taxes, shipping, seller, eligibility, and checkout prices need retailer confirmation.
- The saved ZIP is editable for every user. It is displayed for manual checks, not sent as an inventory query. The GameStop public store lookup tested for 80227 redirected to a product page rather than returning verifiable ZIP/product inventory. This release therefore does not claim automated local inventory coverage. Select your ZIP and fulfillment method on each retailer's site.
- Requests disable local HTTP caching and request revalidation. Retailer pages and their embedded data can still be stale. Each card shows when it was checked. HTTP errors, bot challenges, missing/conflicting data and timeouts require a browser check; there are no CAPTCHA bypasses or private retailer APIs.
- Optional Android JobScheduler checks run roughly every 15 minutes with network access. Android battery/network policies may delay them. Force stopping the app stops jobs until it is opened again. No always-on server is required.
- Optional notifications fire for a supported Nintendo online-orderability transition or when a supported orderable listing crosses a target USD price. Generic positive listing flags never trigger notifications. Alerts explicitly say delivery/pickup for your ZIP is unverified. Unknown/unverified checks do not reset the last orderability report. Notifications must be enabled both in the app and in Android settings.
- On upgrade from 1.0.0, old stock estimates are marked unverified, old stock notifications are dismissed, and listing URLs, price targets, ZIP preferences and price history are retained. Historical stock estimates are labeled legacy/unverified; their original values remain in exported JSON as `legacyStock`.
- Up to 40 recent observations per listing are saved on-device. **Settings → Export history** saves JSON through Android's document picker. Uninstalling clears app data; export before uninstalling. Export is an archive, not an import/restore feature.

## Build and validate

Use Android Studio with JDK 17+ and Android SDK 35, or:

```sh
export ANDROID_HOME=/path/to/android-sdk
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions runs the same checks and uploads a debug APK artifact. Runtime code uses Android platform APIs with no third-party runtime dependencies. Gradle wrapper 8.11.1 and Android Gradle Plugin 8.9.2 are pinned.

Tests cover product/variant matching, malformed pages, conflicting offers, foreign currencies, graph traversal, related products, trusted HTTPS URLs, Nintendo product/SKU orderability checks, stale positive listing flags, GameStop false-stock notifications, and upgrade migration with history preservation.

Validation for 1.0.1 on November 10, 2026: APK assembly passed, all 17 unit tests passed, and Android lint reported no issues. A captured current Nintendo console page resolves to **Not orderable online**; the GameStop console page resolves to **Availability unverified** despite its `InStock` listing flag. These are observations from fetched pages, not a guarantee about current retailer stock. A device/emulator UI smoke test remains outstanding because of this environment's emulator disk-space requirement. Install the preview on your phone before relying on notifications.

## Sources and privacy

Product names and initial URLs were researched on November 10, 2026. Primary product sources:

- [Nintendo console](https://www.nintendo.com/us/store/products/nintendo-switch-2-the-legend-of-zelda-40th-anniversary-edition-121642/)
- [Nintendo Pro Controller](https://www.nintendo.com/us/store/products/nintendo-switch-2-pro-controller-the-legend-of-zelda-40th-anniversary-edition-127074/)
- [Nintendo Pro Controller with display stand](https://www.nintendo.com/us/store/products/nintendo-switch-2-pro-controller-display-stand-the-legend-of-zelda-40th-anniversary-edition-127076/)

No account, analytics, location permission, advertising SDK, purchase automation, or backend. Retailer requests disclose your IP address and the requested product URL to those retailers; opening a retailer uses your chosen browser and its cookies. ZIP and history stay on-device unless you export them. Device backup is disabled.

Independent fan project, not affiliated with or endorsed by Nintendo. Product trademarks belong to their respective owners. Hardware artwork in the app is a simple programmatic illustration rather than official product photography.
