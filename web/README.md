# Hyrule Watch web

Public website: https://hyrule-watch-dd.thinlight2.chatgpt.site

A dependency-free browser interface and Cloudflare Worker for US retailer listing checks. Default US ZIP 80227 is editable and browser-local. No account, database, analytics, private inventory API or purchased domain.

Run `npm run build` and `npm test`. Build produces `dist/server/index.js` and deployment metadata. Source modules live in `worker/`; the small build script inlines the local modules. Sites helper scripts were unavailable in this environment, so source is synchronized with ordinary Git and the artifact is packaged with tar.

Checks use only seven fixed retailer product URLs. Nintendo positive stock reports require exact-SKU orderability evidence. Generic positive flags from other retailers remain unverified; no ZIP inventory is inferred. Responses are shared for up to 60 seconds. Listing prices may be stale and exclude taxes/shipping. Browser refresh runs only while the page is open and visible. No closed-tab notifications are promised.

Settings, targets and up to 200 history records stay in each browser. Export saves JSON. Clearing browser storage removes them. Requests to the shared checker contain only the fixed product ID; the saved ZIP is not sent upstream. Retailer links open in the visitor's browser, where they must set their ZIP, seller and fulfillment option.

Browser visual QA was unavailable: managed Sites browser/preview tools were not provided. Parser, Worker routes and client syntax have runnable Node tests. Hosting is subject to the account's limits; no additional paid service or custom domain was configured.
