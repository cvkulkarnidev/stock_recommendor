package com.localstocks.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

/**
 * Deliberate query expansion for one NSE equity. No paid search APIs.
 * Search providers may limit indexing/rate/coverage; these are discovery
 * requests, not a guarantee of every available recommendation.
 */
public final class DeepQueries {
    private DeepQueries(){}
    public static final class Query {
        public final String channel, provider, text;
        public Query(String c,String p,String t){channel=c;provider=p;text=t;}
    }
    public static List<Query> build(String symbol,String company){
        if(symbol==null||!symbol.toUpperCase(Locale.ROOT).matches("[A-Z0-9.&-]{2,30}"))
            throw new IllegalArgumentException("Invalid stock symbol");
        String s=symbol.toUpperCase(Locale.ROOT),name=company==null||company.trim().isEmpty()?s:company.trim();
        name=name.replaceAll("[\"()<>]"," ").replaceAll("\\s+"," ").trim();
        if(name.length()>65)name=name.substring(0,65);
        String quoted="\""+name+"\"";
        String ticker="\""+s+"\"";
        List<Query> result=new ArrayList<>();
        LinkedHashSet<String> used=new LinkedHashSet<>();
        add(result,used,"BROKERAGES","GOOGLE",quoted+" stock buy sell entry target stop loss when:30d");
        add(result,used,"BROKERAGES","GOOGLE",quoted+" brokerages recommend buy accumulate sell target when:30d");
        add(result,used,"BROKERAGES","GOOGLE",quoted+" technical swing trading price levels stop loss when:30d");
        add(result,used,"BROKERAGES","GOOGLE",quoted+" site:icicidirect.com research stock trading ideas when:30d");
        add(result,used,"BROKERAGES","GOOGLE",quoted+" site:motilaloswal.com research target when:30d");
        add(result,used,"BROKERAGES","GOOGLE",quoted+" site:sharekhan.com target buy sell when:30d");
        add(result,used,"BROKERAGES","GOOGLE",quoted+" site:anandrathi.com buy sell target when:30d");
        add(result,used,"BROKERAGES","GOOGLE",quoted+" site:hdfcsec.com research target when:30d");
        add(result,used,"NEWS","GOOGLE",quoted+" site:livemint.com stock recommendations when:30d");
        add(result,used,"NEWS","GOOGLE",quoted+" site:moneycontrol.com brokerage buy sell target when:30d");
        add(result,used,"NEWS","GOOGLE",quoted+" site:business-standard.com research report when:30d");
        add(result,used,"NEWS","GOOGLE",quoted+" site:economictimes.indiatimes.com stock buy sell when:30d");
        add(result,used,"NEWS","GOOGLE",quoted+" site:financialexpress.com buy target analysts when:30d");
        add(result,used,"ANALYSTS","GOOGLE",quoted+" \"Sumeet Bagadia\" buy target stop loss when:30d");
        add(result,used,"ANALYSTS","GOOGLE",quoted+" \"Ganesh Dongre\" buy target stop loss when:30d");
        add(result,used,"ANALYSTS","GOOGLE",quoted+" \"Vaishali Parekh\" buy target stop loss when:30d");
        add(result,used,"ANALYSTS","GOOGLE",quoted+" \"Shiju Koothupalakkal\" buy target when:30d");
        add(result,used,"ANALYSTS","GOOGLE",quoted+" \"Raja Venkatraman\" technical buy sell when:30d");
        add(result,used,"REDDIT","BING",quoted+" site:reddit.com/r/IndianStockMarket buy sell");
        add(result,used,"REDDIT","BING",ticker+" site:reddit.com/r/IndianStreetBets target");
        add(result,used,"REDDIT","BING",quoted+" site:reddit.com stock discussion technical");
        add(result,used,"X_INDEX","BING",quoted+" site:x.com stock target buy sell");
        add(result,used,"X_INDEX","BING",ticker+" site:twitter.com NSE target stop loss");
        add(result,used,"ANALYSTS","BING",quoted+" swing trade recommendation independent analyst");
        add(result,used,"BROKERAGES","BING",quoted+" \"buy\" \"target\" \"stop loss\" India");
        return result;
    }
    private static void add(List<Query> out,LinkedHashSet<String> seen,String channel,String provider,String text){
        String key=provider+":"+text.toLowerCase(Locale.ROOT);
        if(seen.add(key))out.add(new Query(channel,provider,text));
    }
}
