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
      "India stocks to buy today brokerage analysts target price when:14d",
      "India stocks to sell exit reduce analysts recommendation when:14d",
      "NSE swing trade stock buy recommendation target stop loss when:14d",
      "Indian stock market brokerages top picks buy call when:14d",
      "ICICI Direct Motilal Oswal buy sell stock technical call when:14d",
      "Prabhudas Lilladher Choice Broking Anand Rathi buy stocks when:14d",
      "Indian midcap stocks buy recommendations analysts when:14d",
      "Indian largecap stocks brokerage upgrade downgrade when:14d",
      "NSE bank IT pharma stocks buy sell recommendation when:14d",
      "Indian equity market technical stock picks for next week when:14d"
    };
    public static final class RefreshReport {
        public int articles, inserted, failures, requests;
        public final List<String> errors=new ArrayList<>();
    }
    private static final class Mention {
        final String symbol, name;
        Mention(String s,String n){symbol=s;name=n;}
    }
    public DiscoveryEngine(Context c){super(c,"discovery_v2.db",null,1);}
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE coverage(symbol TEXT NOT NULL, title TEXT NOT NULL, publisher TEXT NOT NULL, url TEXT NOT NULL, published TEXT NOT NULL, captured TEXT NOT NULL, stance TEXT NOT NULL, origin TEXT NOT NULL, PRIMARY KEY(symbol,url))");
        db.execSQL("CREATE INDEX idx_cov_symbol ON coverage(symbol)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
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
    public RefreshReport refresh() {
        RefreshReport report=new RefreshReport();
        for(String q:QUERIES) fetch(q,report);
        // Targeted investigations for stocks discovered from real article headlines.
        List<String> candidates=topSymbols(8);
        for(String symbol:candidates){
            String name=symbolName(symbol);
            fetch("\""+name+"\" stock buy sell target analyst NSE when:14d",report);
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
    public JSONObject snapshot(){
        JSONObject root=new JSONObject();JSONArray cards=new JSONArray();
        Map<String,List<News>> grouped=new HashMap<>();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT symbol,title,publisher,url,published,stance,origin FROM coverage ORDER BY published DESC LIMIT 2500",null)){
            while(c.moveToNext()){
                News n=new News(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6));
                if(!grouped.containsKey(n.symbol))grouped.put(n.symbol,new ArrayList<>());
                grouped.get(n.symbol).add(n);
            }
        }
        List<JSONObject> sorted=new ArrayList<>();
        for(String symbol:grouped.keySet()){
            List<News> news=grouped.get(symbol);
            JSONObject o=buildStock(symbol,news);
            sorted.add(o);
        }
        Collections.sort(sorted,(a,b)->{
            int diff=b.optInt("rankScore")-a.optInt("rankScore");
            return diff!=0?diff:a.optString("symbol").compareTo(b.optString("symbol"));
        });
        for(JSONObject o:sorted)cards.put(o);
        try{root.put("stocks",cards);root.put("total",cards.length());root.put("updated",now());root.put("universe",UNIVERSE.length);}catch(Exception ignored){}
        return root;
    }
    private JSONObject buildStock(String symbol,List<News> articles){
        // One counted direction per apparent underlying institution or publisher;
        // exact headline syndications cannot become separate votes.
        Map<String,News> byOrigin=new LinkedHashMap<>();
        Set<String> duplicateTitles=new HashSet<>();
        int watching=0;
        for(News n:articles){
            if(n.stance.equals("WATCH")||n.stance.equals("MIXED")){watching++;continue;}
            String titleKey=normalize(n.title).replaceAll("\\b(today|this week|tomorrow)\\b","").trim();
            if(duplicateTitles.contains(titleKey))continue;
            duplicateTitles.add(titleKey);
            if(!byOrigin.containsKey(n.origin))byOrigin.put(n.origin,n);
        }
        int buy=0,sell=0,recent=0,explicit=0;
        JSONArray evidence=new JSONArray();
        for(News n:byOrigin.values()){
            if(n.stance.equals("BUY"))buy++;else sell++;
            try{long age=ChronoUnit.DAYS.between(LocalDate.parse(n.published.substring(0,10)),LocalDate.now(ZoneOffset.UTC));if(age>=0&&age<=14)recent++;}catch(Exception ignored){}
            if(n.title.toLowerCase(Locale.ROOT).matches(".*\\b(target|stop.loss|entry|upside|downside)\\b.*"))explicit++;
        }
        int count=buy+sell,dominant=Math.max(buy,sell),opposing=Math.min(buy,sell);
        String direction=count==0?"WATCH":buy==sell?"MIXED":buy>sell?"BUY":"SELL";
        // Evidence quality only; no historical analyst track record or expected return.
        // Missing factors are explicitly withheld: evidence score capped at 70.
        int confidence=count==0?0:(int)Math.round(
           25.0*dominant/count
          +20.0*Math.min(dominant,4)/4.0
          +15.0*recent/count
          +10.0*explicit/count
        );
        int rank=confidence+Math.min(articles.size(),12)+(count==0?-20:0);
        for(News n:articles){
            JSONObject j=new JSONObject();
            try{j.put("title",n.title);j.put("publisher",n.publisher);j.put("url",n.url);j.put("published",n.published);j.put("stance",n.stance);j.put("counted",byOrigin.get(n.origin)==n);}
            catch(Exception ignored){}
            evidence.put(j);
            if(evidence.length()>=35)break;
        }
        JSONObject o=new JSONObject();
        try {
            o.put("symbol",symbol);o.put("name",symbolName(symbol));o.put("direction",direction);
            o.put("confidence",confidence);o.put("rankScore",rank);
            o.put("buy",buy);o.put("sell",sell);o.put("sources",count);
            o.put("coverage",articles.size());o.put("watch",watching);
            o.put("recent",recent);o.put("withLevels",explicit);
            o.put("evidence",evidence);
            o.put("explanation",count==0?"Coverage found; no explicit, independently grouped BUY or SELL headline.":(
               dominant+" supporting vs "+opposing+" opposing grouped headline signals; "+recent+" published within 14 days; "+explicit+" mention a target/entry/stop in the headline. Score capped at 70 without audited trade outcomes."));
        }catch(Exception ignored){}
        return o;
    }
    private static final class News{
        final String symbol,title,publisher,url,published,stance,origin;
        News(String s,String t,String p,String u,String d,String st,String org){symbol=s;title=t;publisher=p;url=u;published=d;stance=st;origin=org;}
    }
}
