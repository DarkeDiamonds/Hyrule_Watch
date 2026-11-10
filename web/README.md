# Hyrule Watch web

A dependency-free browser interface and Cloudflare Worker for US retailer listing checks. Default area is 100 miles around US ZIP 80227, editable per browser. The area is a saved preference; a geographic inventory filter cannot be applied without a store-level feed. No account, database, analytics, private inventory API or purchased domain.

Run `npm run build` and `npm test`. Build produces `dist/server/index.js` and deployment metadata. Source modules live in `worker/`; the small build script inlines the local modules. Sites helper scripts were unavailable in this environment, so source is synchronized with ordinary Git and the artifact is packaged with tar.

Checks use only seven fixed retailer product URLs. No general listing or online-orderability flag establishes local availability. The API and UI always label local stock unverified until there is a verified store-level inventory source. Old positive browser estimates are normalized on load, including historical display; prices and targets are preserved. Responses are shared for up to 60 seconds. Listing prices may be stale and exclude taxes/shipping. Browser refresh runs only while the page is open and visible. No closed-tab notifications are promised.

Settings, targets and up to 200 history records stay in each browser. Export saves JSON. Clearing browser storage removes them. Requests to the shared checker contain the fixed product ID, ZIP, and radius for context; ZIP and radius are not sent to retailer listing pages. The response explicitly declares `radiusApplied: false` and `localSearchSupported: false`. Retailer links open in the visitor's browser, where they must set their ZIP, seller and fulfillment option.

Browser visual QA was unavailable: managed Sites browser/preview tools were not provided. Parser, Worker routes and client syntax have runnable Node tests. Hosting is subject to the account's limits; no additional paid service or custom domain was configured.
