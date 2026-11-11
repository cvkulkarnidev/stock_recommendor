# Source coverage methodology (v0.6)

This application does **not** claim to scrape 125 websites directly.

- Candidate directory: 125 unique domains, including market context and community sites.
- Research/news/brokerage target subset: over 100 domains. A Deep Search makes grouped public-index discovery queries (three site: predicates per group) in addition to the original source-specific searches.
- These are free index requests, not authenticated platform APIs or unrestricted full-text extraction.
- Search results show stock-matched URL-host and named-publisher counts independently of candidate-directory size.
- Repeated URLs, copies of a publisher's story, unverifiable dates and unaudited Reddit/X index mentions must not be counted as independently successful trades.
- A free provider may omit some sites, reject index queries or throttle. No invented coverage or scores.

### Release gate

Run Java unit tests for query generation, unique domains, buy/sell attribution and target/entry/stop parsing, then compile and upload the APK.

### Future improvements

Direct provider-permitted RSS feeds, SEBI registration lookup, signed analyst attribution, original report extraction where authorized, and outcome backtesting remain distinct enhancements.
