# Hindalco recommendation attribution audit — 11 November 2026

**Purpose:** independent quality-control corpus for the Android recommendation parser. These are dated reports/articles, **not automatically imported live trading signals**. The app must retrieve its own dated evidence before displaying recommendations. The underlying signed reports were not available for every item.

| Underlying institution | Direction | Published target | Reporting source / source date | Check |
|---|---|---:|---|---|
| Motilal Oswal | BUY | ₹1,180 | Moneycontrol, 9 Nov (research dated 7 Nov) | named firm, explicit rating/target |
| ICICI Securities | BUY | ₹1,200 (another article's headline ₹1,204) | Moneycontrol, 7 and 9 Nov, respectively | title/body discrepancy: withhold inconsistent target for specific article |
| Elara Securities | BUY reiterated | ₹1,227 (previous ₹1,270) | Business Today, 11 Nov | target reduction **not** a SELL |
| Axis Securities | BUY maintained | ₹1,060 (previous ₹1,155) | Business Today, 11 Nov | target reduction **not** a SELL |
| Nuvama | Upgrade to BUY | ₹1,184 (previous ₹1,108) | Business Today, 11 Nov | change in rating; use latest target |
| UBS | BUY maintained | ₹1,325 | Moneycontrol, 11 Nov | same underlying call repeated across sources |
| Nomura | BUY maintained | ₹1,100 | Moneycontrol, 11 Nov | independent underlying broker |
| CLSA | OUTPERFORM | ₹1,240 | Moneycontrol, 11 Nov | bullish-equivalent, not literally a BUY rating |
| JPMorgan | OVERWEIGHT | ₹1,165 | Moneycontrol, 11 Nov | bullish-equivalent, not literally a BUY rating |
| Kotak / Shrikant Chouhan | BUY | FV ₹1,200 | Business Standard, 11 Nov | CMP ₹960 is a price reference, **not entry** |
| Jefferies | SELL (secondary table, unverified primary report) | ₹780 | Investing.com ratings table, 8 Nov | opposing signal; lower verification level |

**All source data as of 11 Nov 2026**; dates in reporting source are not necessarily the brokerage report dates. No stop-loss was established for the long-term fundamental reports listed here. Never synthesize one.

## Where to cross-check

- Moneycontrol Motilal: https://www.moneycontrol.com/news/business/earnings/buy-hindalco-target-of-rs-1180-motilal-oswal-14075292.html
- Moneycontrol ICICI latest: https://www.moneycontrol.com/news/business/earnings/buy-hindalco-industries-target-of-rs-1204-icici-securities-14075403.html
- Business Today multi-broker: https://www.businesstoday.in/amp/markets/stocks/story/hindalco-shares-brokerages-stick-to-buy-calls-elara-axis-trim-targets-nuvama-upgrades-566773-2026-11-11
- Moneycontrol UBS/Nomura/CLSA/JPMorgan: https://www.moneycontrol.com/news/business/markets/hindalco-shares-slip-0-3-brokerages-bullish-on-realisations-after-q2-results-but-warn-about-near-term-risks-14076753.html/amp
- Business Standard bylined Kotak: https://www.business-standard.com/markets/news/stocks-to-buy-today-hindalco-airtel-among-2-stock-recommendations-by-kotak-s-shrikant-chouhan-126111100077_1.html
- Investing.com ratings table: https://in.investing.com/equities/hindalco-industries-consensus-estimates
- Trendlyne reports index: https://trendlyne.com/research-reports/stock/549/HINDALCO/hindalco-industries-ltd/

## Main problems found in v0.6

1. One portal article was collapsed into at most one BUY or SELL, even if it cited multiple distinct brokerages.
2. The news publisher could be counted as an independent investment recommendation without identifying the actual broker.
3. Sources could quote multiple different target prices without preserving brokerage attribution.
4. Decreased target prices were at risk of being mistaken for bearish SELL signals.
5. Entry, quoted current market price (CMP), long-term fair value (FV), and technical stop-loss were not adequately distinguished.
6. A conflict between headline and text (ICICI's ₹1,204 vs ₹1,200) was not surfaced.

## v0.7 correction and limitations

- Extract individually named broker/analyst views from **public titles and RSS excerpts**.
- Allow multiple underlying firms to be attributed to one source article, but deduplicate **per institution** across republications.
- Separate one institution's target/entry/stop-loss from another's and preserve original source URL and publication.
- Reject cross-stock contamination, display opposing opinions, and withhold contradictory targets.
- Optional **robots-checked, manually initiated** enrichment from two reviewed public article-host domains: Business Today and Business Standard. Unreadable or disallowed content is skipped, not bypassed. Store short relevant passages only.
- Unit tests are **representative known examples**, not proof that extraction is perfect on 125 source domains.
- **Do not mark all external sources as verified.** Direct original signed reports, SEBI registration, dates and current trade validity need independent confirmation.
