package com.localstocks.intelligence;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class DiscoveryEngine extends SQLiteOpenHelper {
    private static final String[][] UNIVERSE = {
      {"RELIANCE","Reliance Industries"},{"HDFCBANK","HDFC Bank"},{"ICICIBANK","ICICI Bank"},{"SBIN","State Bank of India"},
      {"TCS","Tata Consultancy Services"},{"INFY","Infosys"},{"BHARTIARTL","Bharti Airtel"},{"ITC","ITC Ltd"},
      {"LT","Larsen & Toubro"},{"AXISBANK","Axis Bank"},{"KOTAKBANK","Kotak Mahindra Bank"},{"BAJFINANCE","Bajaj Finance"},
      {"HINDUNILVR","Hindustan Unilever"},{"MARUTI","Maruti Suzuki"},{"M&M","Mahindra & Mahindra"},
      {"TATAMOTORS","Tata Motors"},{"TATASTEEL","Tata Steel"},{"JSWSTEEL","JSW Steel"},{"HINDALCO","Hindalco"},
      {"SUNPHARMA","Sun Pharma"},{"DRREDDY","Dr Reddy"},{"CIPLA","Cipla"},{"DIVISLAB","Divis Laboratories"},
      {"ADANIENT","Adani Enterprises"},{"ADANIPORTS","Adani Ports"},{"ADANIGREEN","Adani Green"},{"POWERGRID","Power Grid"},
      {"NTPC","NTPC"},{"ONGC","ONGC"},{"COALINDIA","Coal India"},{"BPCL","BPCL"},{"IOC","Indian Oil"},
      {"TITAN","Titan Company"},{"ASIANPAINT","Asian Paints"},{"ULTRACEMCO","UltraTech Cement"},{"GRASIM","Grasim"},
      {"NESTLEIND","Nestle India"},{"BRITANNIA","Britannia"},{"TRENT","Trent"},{"DMART","Avenue Supermarts"},
      {"WIPRO","Wipro"},{"HCLTECH","HCL Technologies"},{"TECHM","Tech Mahindra"},{"PERSISTENT","Persistent Systems"},
      {"COFORGE","Coforge"},{"DIXON","Dixon Technologies"},{"HAL","Hindustan Aeronautics"},{"BEL","Bharat Electronics"},
      {"BHEL","BHEL"},{"IRCTC","IRCTC"},{"RVNL","Rail Vikas Nigam"},{"IRFC","Indian Railway Finance Corp"},
      {"ZOMATO","Eternal Ltd Zomato"},{"PAYTM","One 97 Communications Paytm"},{"NYKAA","FSN E-Commerce Nykaa"},
      {"JIOFIN","Jio Financial"},{"BAJAJFINSV","Bajaj Finserv"},{"INDIGO","InterGlobe Aviation IndiGo"},
      {"EICHERMOT","Eicher Motors"},{"HEROMOTOCO","Hero MotoCorp"},{"BAJAJ-AUTO","Bajaj Auto"},
      {"APOLLOHOSP","Apollo Hospitals"},{"MAXHEALTH","Max Healthcare"},{"POLYCAB","Polycab India"},
      {"CGPOWER","CG Power"},{"HAVELLS","Havells India"},{"PIDILITIND","Pidilite Industries"},
      {"GODREJCP","Godrej Consumer Products"},{"DABUR","Dabur India"},{"INDUSINDBK","IndusInd Bank"}
    };
    private static final String[] QUERIES = {
      "India stocks to buy today brokerage analysts target price when:30d",
      "India stocks to sell exit reduce analysts recommendation when:30d",
      "NSE swing trade stock buy recommendation target stop loss when:30d",
      "Indian stock market brokerages top picks buy call when:30d",
      "ICICI Direct Motilal Oswal buy sell stock technical call when:30d",
      "Prabhudas Lilladher Choice Broking Anand Rathi buy stocks when:30d",
      "Indian midcap stocks buy recommendations analysts when:30d",
      "Indian largecap stocks brokerage upgrade downgrade when:30d",
      "NSE bank IT pharma stocks buy sell recommendation when:30d",
      "Indian equity market technical stock picks for next week when:30d",
      "Reliance HDFC Bank ICICI Bank SBI stock buy sell brokerage when:30d",
      "Infosys TCS Wipro HCL Tech stock buy sell target when:30d",
      "Tata Motors Maruti Suzuki Mahindra stock recommendation when:30d",
      "HAL BEL Dixon Persistent Systems stock buy analysts when:30d",
      "site:moneycontrol.com brokerages recommend stocks buy sell when:30d",
      "site:livemint.com markets stock recommendations analysts when:30d",
      "site:business-standard.com markets research report buy sell when:30d",
      "Kotak Securities Axis Securities HDFC Securities stocks buy sell when:30d",
      "\"Sumeet Bagadia\" stock pick target stop loss when:30d",
      "\"Ganesh Dongre\" stocks target stop loss when:30d",
      "\"Shiju Koothupalakkal\" stocks target stop loss when:30d",
      "\"Raja Venkatraman\" stock recommendations when:30d",
      "\"Vaishali Parekh\" stocks buy stop loss when:30d"
    };
    public static final class RefreshReport {
        public int articles, inserted, failures, requests;
        public final List<String> errors=new ArrayList<>();
    }
    private static final class Mention {
        final String symbol, name;
        Mention(String s,String n){symbol=s;name=n;}
    }
    public DiscoveryEngine(Context c){super(c,"discovery_v2.db",null,2);}
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE coverage(symbol TEXT NOT NULL, title TEXT NOT NULL, publisher TEXT NOT NULL, url TEXT NOT NULL, published TEXT NOT NULL, captured TEXT NOT NULL, stance TEXT NOT NULL, origin TEXT NOT NULL, channel TEXT NOT NULL DEFAULT 'NEWS', PRIMARY KEY(symbol,url))");
        db.execSQL("CREATE INDEX idx_cov_symbol ON coverage(symbol)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        if(oldVersion<2)db.execSQL("ALTER TABLE coverage ADD COLUMN channel TEXT NOT NULL DEFAULT 'NEWS'");
    }
    private static String normalize(String s){return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();}
    private static boolean containsPhrase(String text,String phrase){return (" "+text+" ").contains(" "+normalize(phrase)+" ");}
    private static List<Mention> findMentions(String title){
        String norm=normalize(title);
        List<Mention> out=new ArrayList<>();
        for(String[] item:UNIVERSE){
            String symbol=item[0], name=item[1];
            // Avoid false positives from short tickers (ITC, LT, M&M, TCS etc.).
            String[] names=name.split(" ");
            boolean nameHit=containsPhrase(norm,name);
            boolean tickerHit=symbol.length()>=4 && !symbol.equals("INDIGO") && containsPhrase(norm,symbol);
            // For named brands some common textual variants.
            if(symbol.equals("RELIANCE")) nameHit|=containsPhrase(norm,"reliance");
            if(symbol.equals("SBIN")) nameHit|=containsPhrase(norm,"sbi shares")||containsPhrase(norm,"sbi stock")||containsPhrase(norm,"state bank of india");
            if(symbol.equals("LT")) nameHit|=containsPhrase(norm,"l&t stock")||containsPhrase(norm,"larsen and toubro");
            if(symbol.equals("HINDUNILVR")) nameHit|=containsPhrase(norm,"hul stock")||containsPhrase(norm,"hindustan unilever");
            if(symbol.equals("BHARTIARTL")) nameHit|=containsPhrase(norm,"airtel shares")||containsPhrase(norm,"bharti airtel");
            if(symbol.equals("BAJFINANCE")) nameHit|=containsPhrase(norm,"bajaj finance");
            if(symbol.equals("JIOFIN")) nameHit|=containsPhrase(norm,"jio financial");
            if(symbol.equals("MARUTI")) nameHit|=containsPhrase(norm,"maruti suzuki");

            if(symbol.equals("TATAMOTORS")) nameHit|=containsPhrase(norm,"tata motors");
            if(symbol.equals("HDFCBANK")) nameHit|=containsPhrase(norm,"hdfc bank");
            if(symbol.equals("ITC")) nameHit|=containsPhrase(norm,"itc shares")||containsPhrase(norm,"itc stock");
            if(symbol.equals("TCS")) nameHit|=containsPhrase(norm,"tcs shares")||containsPhrase(norm,"tcs stock");
            if(symbol.equals("LT")) nameHit|=containsPhrase(norm,"larsen toubro")||containsPhrase(norm,"l t shares");
            if(symbol.equals("M&M")) nameHit|=containsPhrase(norm,"mahindra mahindra");
            if(symbol.equals("ZOMATO")) nameHit|=containsPhrase(norm,"zomato")||containsPhrase(norm,"eternal shares");
            if(symbol.equals("PAYTM")) nameHit|=containsPhrase(norm,"paytm");
            if(symbol.equals("NYKAA")) nameHit|=containsPhrase(norm,"nykaa");
            if(symbol.equals("DMART")) nameHit|=containsPhrase(norm,"dmart");
            if(symbol.equals("INDIGO")) nameHit|=containsPhrase(norm,"indigo airline")||containsPhrase(norm,"interglobe aviation");
            if(nameHit||tickerHit)out.add(new Mention(symbol,name));
        }
        return out;
    }
    private static String stance(String title){
        String t=normalize(title);
        boolean sell=t.matches(".*\\b(sell|selling|exit|reduce|downgrade|avoid|bearish|underperform|book profit|cuts rating)\\b.*");
        boolean buy=t.matches(".*\\b(buy|buying|accumulate|upgrade|bullish|outperform|add rating|top pick|stock picks|stocks to buy)\\b.*");
        if(buy&&sell)return "MIXED";
        if(sell)return "SELL";
        if(buy)return "BUY";
        return "WATCH";
    }
    private static String origin(String title,String publisher) {
        String n=normalize(title);
        String[] firms={"motilal oswal","icici direct","anand rathi","choice broking","prabhudas lilladher","hdfc securities","kotak securities","axis securities","emkay","jm financial","nuvama","jefferies","nomura","goldman sachs","morgan stanley","ubs","bernstein","citi","hsbc","bob capital","sharekhan","geojit"};
        for(String f:firms)if(containsPhrase(n,f))return "analyst:"+f;
        return "publisher:"+normalize(publisher);
    }
    private static String iso(String pub){
        try {return ZonedDateTime.parse(pub,DateTimeFormatter.RFC_1123_DATE_TIME).withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);}
        catch(Exception ignored){return "";}
    }
    private static String now(){return OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);}
    private boolean insert(String symbol,String title,String publisher,String url,String published,String stance,String origin) {
        if(!url.startsWith("https://")||title.isEmpty())return false;
        ContentValues v=new ContentValues();v.put("symbol",symbol);v.put("title",title);v.put("publisher",publisher);
        v.put("url",url);v.put("published",published);v.put("captured",now());
        v.put("stance",stance);v.put("origin",origin);
        return getWritableDatabase().insertWithOnConflict("coverage",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    private boolean insertIndexed(String symbol,SearchChannels.Hit hit){
        if(hit.url==null||!hit.url.startsWith("https://")||hit.title==null)return false;
        ContentValues v=new ContentValues();v.put("symbol",symbol);v.put("title",hit.title);
        v.put("publisher",hit.channel.equals("REDDIT")?"Reddit indexed post":hit.channel.equals("X_INDEX")?"X indexed post":"Independent analyst indexed result");
        v.put("url",hit.url);v.put("published","");
        v.put("captured",now());v.put("stance","WATCH");
        v.put("origin","index:"+hit.channel);v.put("channel",hit.channel);
        return getWritableDatabase().insertWithOnConflict("coverage",null,v,SQLiteDatabase.CONFLICT_IGNORE)!=-1;
    }
    public RefreshReport refresh() {
        RefreshReport report=new RefreshReport();
        for(String q:QUERIES) fetch(q,report);
        SearchChannels.Result index=SearchChannels.search();
        report.requests+=index.queries;report.failures+=index.failures.size();
        for(String error:index.failures)if(report.errors.size()<5)report.errors.add(error);
        for(SearchChannels.Hit hit:index.hits){
            report.articles++;
            List<Mention> mentions=findMentions(hit.title);
            if(mentions.isEmpty())mentions=findMentions(hit.description);
            int added=0;
            for(Mention m:mentions){
                if(added>=3)break;
                if(insertIndexed(m.symbol,hit))report.inserted++;
                added++;
            }
        }
        // Targeted investigations for stocks discovered from real article headlines.
        List<String> candidates=topSymbols(8);
        for(String symbol:candidates){
            String name=symbolName(symbol);
            fetch("\""+name+"\" stock buy sell target analyst NSE when:30d",report);
        }
        getWritableDatabase().delete("coverage","published <> '' AND substr(published,1,10) < ?",new String[]{LocalDate.now(ZoneOffset.UTC).minusDays(60).toString()});
        return report;
    }
    public RefreshReport investigate(String symbol) {
        RefreshReport r=new RefreshReport();
        String name=symbolName(symbol);
        if(!name.isEmpty()) {
            fetch("\""+name+"\" stock recommendation buy sell brokerage when:30d",r);
            fetch("\""+name+"\" target price swing trade analysts when:30d",r);
            fetch("\""+name+"\" \"Sumeet Bagadia\" OR \"Ganesh Dongre\" stock buy sell when:30d",r);
            fetch("\""+name+"\" \"Shiju Koothupalakkal\" OR \"Raja Venkatraman\" when:30d",r);
        }
        return r;
    }
    private void fetch(String query,RefreshReport out) {
        out.requests++;
        HttpURLConnection con=null;
        try {
            String url="https://news.google.com/rss/search?q="+URLEncoder.encode(query,"UTF-8")+"&hl=en-IN&gl=IN&ceid=IN:en";
            con=(HttpURLConnection)new URL(url).openConnection();
            con.setConnectTimeout(9000);con.setReadTimeout(12000);con.setInstanceFollowRedirects(true);
            con.setRequestProperty("User-Agent","StockIntelligence/1.0 (Android personal research RSS)");
            if(con.getResponseCode()!=200)throw new IllegalStateException("Feed HTTP "+con.getResponseCode());
            XmlPullParser parser=Xml.newPullParser();parser.setInput(con.getInputStream(),"UTF-8");
            int event=parser.getEventType();boolean item=false;String title="",link="",pub="",publisher="";
            int count=0;
            while(event!=XmlPullParser.END_DOCUMENT&&count<100){
                if(event==XmlPullParser.START_TAG){
                    String tag=parser.getName();
                    if("item".equalsIgnoreCase(tag)){item=true;title="";link="";pub="";publisher="";}
                    else if(item&&("title".equals(tag)||"link".equals(tag)||"pubDate".equals(tag)||"source".equals(tag))){
                        String value=parser.nextText();
                        if("title".equals(tag))title=value;
                        else if("link".equals(tag))link=value;
                        else if("pubDate".equals(tag))pub=value;
                        else publisher=value;
                    }
                }else if(event==XmlPullParser.END_TAG&&"item".equalsIgnoreCase(parser.getName())){
                    item=false;count++;
                    out.articles++;
                    if(publisher.isEmpty()&&title.contains(" - "))publisher=title.substring(title.lastIndexOf(" - ")+3);
                    String baseTitle=title;
                    if(title.endsWith(" - "+publisher))baseTitle=title.substring(0,title.length()-publisher.length()-3);
                    String position=stance(baseTitle),published=iso(pub),source=origin(baseTitle,publisher);
                    for(Mention m:findMentions(baseTitle))
                        if(insert(m.symbol,baseTitle,publisher,link,published,position,source))out.inserted++;
                }
                event=parser.next();
            }
        }catch(Exception e){
            out.failures++; if(out.errors.size()<5)out.errors.add(e.getClass().getSimpleName()+": "+e.getMessage());
        }finally{if(con!=null)con.disconnect();}
    }
    private List<String> topSymbols(int limit) {
        List<String> out=new ArrayList<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT symbol,COUNT(*) n FROM coverage WHERE stance IN ('BUY','SELL') GROUP BY symbol ORDER BY n DESC LIMIT "+limit,null)) {
            while(c.moveToNext())out.add(c.getString(0));
        }
        return out;
    }
    public String symbolName(String symbol){
        for(String[] pair:UNIVERSE)if(pair[0].equalsIgnoreCase(symbol))return pair[1];
        return "";
    }

    public JSONObject snapshot(){return snapshot(7);}
    public JSONObject snapshot(int windowDays){
        int days=Math.max(1,Math.min(30,windowDays));
        JSONObject root=new JSONObject();JSONArray cards=new JSONArray();
        Map<String,List<News>> grouped=new HashMap<>();
        try(Cursor c=getReadableDatabase().rawQuery(
          "SELECT symbol,title,publisher,url,published,stance,origin,captured FROM coverage WHERE published >= ? AND published != '' ORDER BY published DESC LIMIT 5000",
          new String[]{LocalDate.now(ZoneOffset.UTC).minusDays(days-1).toString()})){
            while(c.moveToNext()){
                News n=new News(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7));
                if(!grouped.containsKey(n.symbol))grouped.put(n.symbol,new ArrayList<>());
                grouped.get(n.symbol).add(n);
            }
        }
        List<JSONObject> sorted=new ArrayList<>();
        for(String symbol:grouped.keySet())sorted.add(buildStock(symbol,grouped.get(symbol),days));
        Collections.sort(sorted,(a,b)->{
            int diff=b.optInt("rankScore")-a.optInt("rankScore");
            return diff!=0?diff:a.optString("symbol").compareTo(b.optString("symbol"));
        });
        for(JSONObject o:sorted)cards.put(o);
        try{
            root.put("stocks",cards);root.put("total",cards.length());root.put("generatedAt",now());
            root.put("windowDays",days);root.put("universe",UNIVERSE.length);
            JSONArray trusted=new JSONArray(),editorial=new JSONArray();
            for(String n:ResearchRules.brokers())trusted.put(n);
            for(String n:ResearchRules.editorial())editorial.put(n);
            root.put("curatedBrokers",trusted);root.put("curatedPublishers",editorial);
        }catch(Exception ignored){}
        return root;
    }
    private JSONObject buildStock(String symbol,List<News> articles,int windowDays){
        Map<String,Ranked> independent=new LinkedHashMap<>();
        Set<String> seenTitles=new HashSet<>();
        JSONArray evidence=new JSONArray();
        String newest="";
        for(News n:articles){
            ResearchRules.Source src=ResearchRules.source(n.title,n.publisher);
            ResearchRules.Signal signal=ResearchRules.classify(n.title,symbol,symbolName(symbol));
            String titleKey=normalize(n.title).replaceAll("\\b(today|this week|tomorrow)\\b","").trim();
            boolean duplicate=seenTitles.contains(titleKey);
            seenTitles.add(titleKey);
            String origin=src.tier.equals("NAMED_BROKER")||src.tier.equals("BROKER_PUBLISHER")
               ?"broker:"+normalize(src.name):"publisher:"+normalize(n.publisher);
            boolean eligible=signal.direction.equals("BUY")||signal.direction.equals("SELL");
            // An identical headline, or repeated content attributed to the same
            // original broker/publisher, can contribute at most one vote per stock.
            boolean counted=eligible&&!duplicate&&!independent.containsKey(origin);
            if(counted)independent.put(origin,new Ranked(n,src,signal));
            if(n.published!=null&&n.published.compareTo(newest)>0)newest=n.published;
            if(evidence.length()<70){
                JSONObject j=new JSONObject();
                try{
                    j.put("title",n.title);j.put("publisher",n.publisher);j.put("url",n.url);
                    j.put("published",n.published);j.put("captured",n.captured);
                    j.put("stance",signal.direction);j.put("reason",signal.reason);
                    j.put("counted",counted);j.put("sourceName",src.name);j.put("sourceTier",src.tier);
                    j.put("sourceWeight",src.weight);
                }catch(Exception ignored){}
                evidence.put(j);
            }
        }
        int buy=0,sell=0,known=0,other=0,withLevels=0;
        double weightedBuy=0,weightedSell=0,quality=0;
        for(Ranked r:independent.values()){
            if(r.signal.direction.equals("BUY")){buy++;weightedBuy+=r.src.weight;}
            else{sell++;weightedSell+=r.src.weight;}
            if(r.src.tier.equals("OTHER"))other++;else known++;
            quality+=r.src.weight;
            if(normalize(r.news.title).matches(".*\\b(target|stop loss|entry|upside|downside|rs [0-9]+)\\b.*"))withLevels++;
        }
        int count=buy+sell;
        String direction=count==0?"WATCH":weightedBuy==weightedSell?"MIXED":weightedBuy>weightedSell?"BUY":"SELL";
        int supports=direction.equals("BUY")?buy:direction.equals("SELL")?sell:Math.max(buy,sell);
        int opposes=direction.equals("BUY")?sell:direction.equals("SELL")?buy:Math.min(buy,sell);
        double agreement=count==0?0:20.0*Math.max(weightedBuy,weightedSell)/Math.max(.001,weightedBuy+weightedSell);
        double diversity=20.0*Math.min(4,Math.max(0,count-1))/4.0;
        double sourceQuality=count==0?0:25.0*quality/count;
        // Every article was filtered into the requested age window; narrower
        // windows award more freshness credit than older weekly/monthly evidence.
        double freshness=count==0?0:10.0*Math.max(0,1.0-(windowDays-1)/45.0);
        double directness=count==0?0:10.0; // direction is explicitly attributable near the stock name
        int confidence=count==0?0:(int)Math.round(agreement+diversity+sourceQuality+freshness+directness);
        confidence=Math.min(85,confidence);
        int ranking=confidence+Math.min(articles.size(),12)+(count==0?-25:0);
        JSONObject o=new JSONObject();
        try{
            o.put("symbol",symbol);o.put("name",symbolName(symbol));o.put("direction",direction);
            o.put("confidence",confidence);o.put("rankScore",ranking);
            o.put("buy",buy);o.put("sell",sell);o.put("sources",count);
            o.put("curatedSources",known);o.put("otherSources",other);
            o.put("coverage",articles.size());o.put("watch",Math.max(0,articles.size()-count));
            o.put("recent",count);o.put("withLevels",withLevels);
            o.put("lastPublished",newest);o.put("windowDays",windowDays);
            o.put("evidence",evidence);
            o.put("explanation",count==0?
                "Coverage found, but no explicit stock-attributable BUY or SELL call in this period.":
                supports+" supporting and "+opposes+" opposing independently grouped headline calls. "+
                known+" from curated source names, "+other+" from other publishers. "+
                "Trusted names receive more weight, not automatic proof of accuracy. "+
                "Score is evidence-based, capped at 85 without audited outcome data.");
        }catch(Exception ignored){}
        return o;
    }
    private static final class Ranked{
        final News news;final ResearchRules.Source src;final ResearchRules.Signal signal;
        Ranked(News n,ResearchRules.Source s,ResearchRules.Signal g){news=n;src=s;signal=g;}
    }
    private static final class News{
        final String symbol,title,publisher,url,published,stance,origin,captured;
        News(String s,String t,String p,String u,String d,String st,String org,String cap){
            symbol=s;title=t;publisher=p;url=u;published=d;stance=st;origin=org;captured=cap;
        }
    }
}
