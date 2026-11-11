package com.localstocks.intelligence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Candidate discovery domains, NOT an audited/approved source list. This
 * directory is queried through public search indexes only. Its size never
 * implies that all domains were reachable or returned recommendations.
 */
public final class SourceDirectory {
    private SourceDirectory(){}
    public static final class Site {
        public final String host, category;
        public Site(String host,String category){this.host=host;this.category=category;}
    }
    private static final Site[] SITES={
        new Site("icicidirect.com","BROKERAGES"),
        new Site("motilaloswal.com","BROKERAGES"),
        new Site("axisdirect.in","BROKERAGES"),
        new Site("choiceindia.com","BROKERAGES"),
        new Site("hdfcsec.com","BROKERAGES"),
        new Site("kotaksecurities.com","BROKERAGES"),
        new Site("anandrathi.com","BROKERAGES"),
        new Site("sharekhan.com","BROKERAGES"),
        new Site("sbisecurities.in","BROKERAGES"),
        new Site("nirmalbang.com","BROKERAGES"),
        new Site("geojit.com","BROKERAGES"),
        new Site("5paisa.com","BROKERAGES"),
        new Site("angelone.in","BROKERAGES"),
        new Site("zerodha.com","BROKERAGES"),
        new Site("groww.in","BROKERAGES"),
        new Site("upstox.com","BROKERAGES"),
        new Site("dhan.co","BROKERAGES"),
        new Site("fyers.in","BROKERAGES"),
        new Site("samco.in","BROKERAGES"),
        new Site("aliceblueonline.com","BROKERAGES"),
        new Site("bonanzaonline.com","BROKERAGES"),
        new Site("religareonline.com","BROKERAGES"),
        new Site("venturasecurities.com","BROKERAGES"),
        new Site("marwadionline.com","BROKERAGES"),
        new Site("hemsecurities.com","BROKERAGES"),
        new Site("smcindiaonline.com","BROKERAGES"),
        new Site("adityabirlacapital.com","BROKERAGES"),
        new Site("iiflcapital.com","BROKERAGES"),
        new Site("indmoney.com","BROKERAGES"),
        new Site("jmfl.com","BROKERAGES"),
        new Site("nuvama.com","BROKERAGES"),
        new Site("emkayglobal.com","BROKERAGES"),
        new Site("bobcaps.in","BROKERAGES"),
        new Site("elaracapital.com","BROKERAGES"),
        new Site("yesinvest.in","BROKERAGES"),
        new Site("philipcapital.in","BROKERAGES"),
        new Site("stockholding.com","BROKERAGES"),
        new Site("primedatabase.com","BROKERAGES"),
        new Site("investec.com","BROKERAGES"),
        new Site("clsa.com","BROKERAGES"),
        new Site("jefferies.com","BROKERAGES"),
        new Site("nomura.com","BROKERAGES"),
        new Site("morganstanley.com","BROKERAGES"),
        new Site("goldmansachs.com","BROKERAGES"),
        new Site("hsbc.com","BROKERAGES"),
        new Site("ubs.com","BROKERAGES"),
        new Site("citi.com","BROKERAGES"),
        new Site("macquarie.com","BROKERAGES"),
        new Site("bernstein.com","BROKERAGES"),
        new Site("livemint.com","NEWS"),
        new Site("moneycontrol.com","NEWS"),
        new Site("business-standard.com","NEWS"),
        new Site("economictimes.indiatimes.com","NEWS"),
        new Site("financialexpress.com","NEWS"),
        new Site("cnbctv18.com","NEWS"),
        new Site("ndtvprofit.com","NEWS"),
        new Site("thehindubusinessline.com","NEWS"),
        new Site("thehindu.com","NEWS"),
        new Site("timesofindia.indiatimes.com","NEWS"),
        new Site("hindustantimes.com","NEWS"),
        new Site("reuters.com","NEWS"),
        new Site("bloomberg.com","NEWS"),
        new Site("investing.com","NEWS"),
        new Site("indiatoday.in","NEWS"),
        new Site("businesstoday.in","NEWS"),
        new Site("zeebiz.com","NEWS"),
        new Site("etnownews.com","NEWS"),
        new Site("goodreturns.in","NEWS"),
        new Site("tradebrains.in","NEWS"),
        new Site("equitymaster.com","NEWS"),
        new Site("capitalmarket.com","NEWS"),
        new Site("marketwatch.com","NEWS"),
        new Site("forbes.com","NEWS"),
        new Site("fortuneindia.com","NEWS"),
        new Site("outlookbusiness.com","NEWS"),
        new Site("businessworld.in","NEWS"),
        new Site("firstpost.com","NEWS"),
        new Site("news18.com","NEWS"),
        new Site("indianexpress.com","NEWS"),
        new Site("deccanherald.com","NEWS"),
        new Site("freepressjournal.in","NEWS"),
        new Site("financialpost.com","NEWS"),
        new Site("mintgenie.livemint.com","NEWS"),
        new Site("theweek.in","NEWS"),
        new Site("trendlyne.com","RESEARCH"),
        new Site("tickertape.in","RESEARCH"),
        new Site("screener.in","RESEARCH"),
        new Site("chartink.com","RESEARCH"),
        new Site("tradingview.com","RESEARCH"),
        new Site("simplywall.st","RESEARCH"),
        new Site("marketsmojo.com","RESEARCH"),
        new Site("moneyworks4me.com","RESEARCH"),
        new Site("stockedge.com","RESEARCH"),
        new Site("researchbytes.com","RESEARCH"),
        new Site("marketsmithindia.com","RESEARCH"),
        new Site("stockaxis.com","RESEARCH"),
        new Site("valuepickr.com","RESEARCH"),
        new Site("tradingqna.com","RESEARCH"),
        new Site("valueresearchonline.com","RESEARCH"),
        new Site("morningstar.in","RESEARCH"),
        new Site("marketscreener.com","RESEARCH"),
        new Site("finology.in","RESEARCH"),
        new Site("finshots.in","RESEARCH"),
        new Site("stockanalysis.com","RESEARCH"),
        new Site("stocktwits.com","RESEARCH"),
        new Site("screener.com","RESEARCH"),
        new Site("stockinvest.us","RESEARCH"),
        new Site("investorgain.com","RESEARCH"),
        new Site("chittorgarh.com","RESEARCH"),
        new Site("ipoji.com","RESEARCH"),
        new Site("ipowatch.in","RESEARCH"),
        new Site("reddit.com","COMMUNITY"),
        new Site("x.com","COMMUNITY"),
        new Site("twitter.com","COMMUNITY"),
        new Site("youtube.com","COMMUNITY"),
        new Site("medium.com","COMMUNITY"),
        new Site("substack.com","COMMUNITY"),
        new Site("quora.com","COMMUNITY"),
        new Site("nseindia.com","OFFICIAL_CONTEXT"),
        new Site("bseindia.com","OFFICIAL_CONTEXT"),
        new Site("sebi.gov.in","OFFICIAL_CONTEXT"),
        new Site("rbi.org.in","OFFICIAL_CONTEXT"),
        new Site("nsdl.co.in","OFFICIAL_CONTEXT"),
        new Site("cdslindia.com","OFFICIAL_CONTEXT"),
        new Site("investor.gov.in","OFFICIAL_CONTEXT")
    };
    public static List<Site> all(){
        return Collections.unmodifiableList(Arrays.asList(SITES));
    }
    public static List<Site> searchable(){
        List<Site> result=new ArrayList<>();
        for(Site s:SITES)if(!s.category.equals("OFFICIAL_CONTEXT")&&!s.category.equals("COMMUNITY"))result.add(s);
        return result;
    }
    public static Set<String> uniqueDomains(){
        Set<String> hosts=new HashSet<>();
        for(Site site:SITES)hosts.add(site.host);
        return hosts;
    }
    public static boolean containsDomain(String url) {
        try {
            String host=new java.net.URL(url).getHost().toLowerCase(Locale.ROOT);
            for(Site site:SITES)if(host.equals(site.host)||host.endsWith("."+site.host))return true;
        } catch(Exception ignored){}
        return false;
    }
}
