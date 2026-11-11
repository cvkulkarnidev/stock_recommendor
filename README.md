# Stock Intelligence — Android v0.2

A **local-first, discovery-first Android research app for Indian stocks**. You do not need to enter a stock to begin. Press **Refresh market** and the phone searches public financial-news RSS coverage, extracts stock mentions, classifies explicit directional headlines, performs secondary stock-specific searches, and ranks a monitored NSE universe.

**No hosted server, account, subscription, automatic trading, or cloud database.** This is an evidence discovery tool, not a professional trade recommendation service.

## Install APK from GitHub Actions

1. Open [Android APK Actions](https://github.com/cvkulkarnidev/stock_recommendor/actions).
2. Open a successful run from `main`.
3. Under **Artifacts**, download `StockIntelligence-debug-apk`. Extract and install `app-debug.apk`.
4. If an older debug APK is installed and Android reports **App not installed**, uninstall the older APK first. GitHub-hosted debug builds may use different signing keys, and uninstalling removes on-device app data.
5. Requires Android 8 (API 26) or newer and an internet connection for manual refresh.

## What this build includes

- **Discover on first launch**: press **Refresh market**, no manual stock entry needed. Runs 14 topical/sector public RSS searches and follows up on up to 8 named candidate stocks (22 network queries maximum for the full initial scan).
- **Stock cards**: searchable ranked results with directional BUY / SELL / MIXED / WATCH classification, provisional evidence score, buy/sell source counts, and article count.
- **Detailed research**: all included article headlines, publisher, publication timestamp (when supplied), link, inclusion status, and clear rationale. Tap **Investigate this stock further** for two more targeted manual searches.
- **Confidence**: directional agreement 25 points, count of apparently independent research origins 20, recent coverage 15, headlines with target/entry/stop terminology 10. Maximum is 70/100 because audited historical outcomes are unavailable. Same-origin publishers and identical headline syndications are conservatively grouped. Unclear articles remain WATCH.
- **Watchlist**: saved stock symbols remain on your phone via WebView local storage. Article coverage is in local SQLite.
- **Sources**: overview of discovered publishers, distinct from verified trader performance.
- **Dark mobile dashboard**: polished card UI, filters, search, source detail pages, and navigation.

## Important limitations

This is a real automated **headline-discovery and evidence aggregation app**, not a fully autonomous trading-advice engine. It **does not scrape restricted article bodies**, bypass paywalls, or invent a trade entry, target, stop-loss, win rate, or real-time stock price. It relies on Google News's publicly accessible RSS search; availability and applicable terms are controlled by Google, and news may be incomplete, stale, biased, duplicated, or inaccessible. Only the 70-company initial equity universe is recognized. The headline classifier is a conservative rule-based heuristic, and may misclassify or miss articles. A reported BUY/SELL indicates **explicit directional language found in a published headline**, not a verified institutional call. A score is **not** the probability a trade will succeed.

The previous v0.1 manual-entry journal remains in source code (`Store.java`, `Analytics.java`) but its interface is replaced in v0.2 by the automatic-discovery dashboard. Verified analyst performance, structured trade levels, price-data integrations, robust underlying-publication deduplication, and wide-universe entity extraction remain future work.

## Technical architecture

- Android Java, Android WebView for the bundled **offline HTML/CSS/JS UI**, SQLite for evidence, and Android's built-in `XmlPullParser` for public RSS.
- Zero paid dependencies or remote processing. RSS URL requests originate on the phone only after a manual refresh, and article links open in the external browser.
- Android SDK 35, Gradle 8.9, Android Gradle Plugin 8.6.1, Java 17.
- GitHub Actions builds and uploads a debug APK on main pushes and pull requests.

## Build / verify

Open the project in Android Studio or run `gradle :app:assembleDebug` after installing JDK 17 and Android SDK 35. CI checks that the output APK exists and is nonempty, then uploads the artifact. Build success is **not** equivalent to independently testing live RSS reliability on a physical phone.

For personal research only. **Do not trade based solely on these signals.**
