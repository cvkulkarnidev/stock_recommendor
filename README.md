# Stock Intelligence — Android v0.1

A **local-first Android stock recommendation research app** for Indian equities, prioritizing swing trades. Manual refresh, source-linked verified buy/sell calls, analyst-day deduplication, provisional evidence confidence, and OHLC price-outcome tracking. **No deployed server, account, brokerage integration, or paid API required.**

> Research and journaling only — not personalized investment advice or automatic trading. An evidence score is not a probability of profit.

## Get the APK

1. Open [Actions](https://github.com/cvkulkarnidev/stock_recommendor/actions).
2. Open a successful **Android APK** workflow run (or select **Run workflow**).
3. Download **StockIntelligence-debug-apk** under Artifacts and extract the ZIP.
4. Install `app-debug.apk` on Android 8+ (allow installs from your file manager if prompted).

The workflow builds with JDK 17, Android SDK 35, Gradle 8.9 and Android Gradle Plugin 8.6.1. To build locally use Android Studio, or `gradle :app:assembleDebug` with Gradle 8.9 and Android SDK installed.

## What v0.1 actually does

- **Discover**: explicit/manual Google News RSS query for company/name + Indian stock recommendations. Results are only *news leads*, not automatically BUY/SELL. They include original RSS publication text or normalized UTC timestamp, publisher, source link, and collection time. Because sources can change access policies and free feeds are not guaranteed, failures are shown openly.
- **Review**: read the source in a browser, then manually enter the original analyst, institution, publication **date** (precision limited to a day for manually verified records), original link, direction, entry, target, and stop-loss. The app validates basic field constraints and rejects duplicates of the same analyst/institution, symbol, direction and date.
- **Signals**: groups manually reviewed recommendations by stock; counts one opinion per analyst/institution per day (including opposing opinions), highlights agreement/conflict, and runs deeper **manual** stock-specific RSS discovery on demand.
- **Provisional confidence**: independent analyst-day agreement (30 points), freshness within 14 days (15), complete trade levels (15), up to four supporting sources (15). The maximum is **75 out of 100** until validated historic reliability and price-level consensus are available. No price accuracy or forecast probability is claimed. Exact scores are deterministic for a given date and saved records.
- **Performance**: import a user-supplied CSV of daily OHLC bars; track unfilled entries, targets, stop-losses, open trades, and ambiguous same-day target/stop cases. Conservative rules ignore publication-day bars, do not assume entry-day exit ordering, and treat dual target/stop hits on one bar as ambiguous. Returns exclude brokerage, taxes, and slippage.
- **Privacy**: uses on-device Android SQLite; no cloud backend. An internet connection is needed for manual RSS discovery; RSS queries go to the feed provider and opening links goes to the browser.

## Daily price CSV format

```csv
symbol,date,open,high,low,close
EXAMPLE,2026-10-01,100,103,98,101
EXAMPLE,2026-10-02,101,106,100,105
```

Import with **Performance → Import daily prices CSV**. CSV is a 6-column unquoted format, dates are `YYYY-MM-DD`, prices must be positive, and all OHLC values must be consistent. Sample data under `sample-data/` is synthetic and **never automatically included** in the installed app.

## Security, source compliance and limitations

- Uses only public RSS to discover articles; it does **not** scrape paywalled pages, bypass CAPTCHA, automatically copy article bodies, or imply commercial reuse rights.
- RSS provider may prohibit certain uses. Respect each source's terms. Data is stored locally for personal research.
- Sources are **not independently verified** beyond user review in this release; the word “verified” means **user-recorded and source-linked**, not audited by the application.
- There are no live stock price quotes or brokerage APIs. No notifications or auto-refresh.
- No historical analyst win-rate score yet. An analyst from one institution should not automatically be considered independent of another at that institution; group with judgment.
- Source headlines can be stale, inaccurate, promotional, or duplicate syndications. You must check the underlying dated research before entering a trade.
- For technical conservatism, daily-bar entry-day target/stop crossings are not evaluated; add timestamped intraday bars in a later release if precision is necessary.
- Manual recommendation publication times are currently **date-granular**. The RSS record keeps its feed timestamp separately.

## Planned v0.2 improvements

Directly permitted independent brokerage feeds, source URL canonicalization, full publication timestamps for verified calls, explicit evidence provenance grouping, calibrated historic reliability, more precise trade-fill rules, broker-independent current prices if an authorized free provider exists, and stronger UI/automated tests.
