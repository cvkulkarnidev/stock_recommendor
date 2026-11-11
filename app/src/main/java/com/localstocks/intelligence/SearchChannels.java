package com.localstocks.intelligence;

import org.xmlpull.v1.XmlPullParser;
import android.util.Xml;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Free search-engine index lookups. NOT a direct Reddit or X API.
 * Publisher search terms and results are publicly indexable metadata;
 * no site login, scraping, proxies, or restricted post-body retrieval.
 */
public final class SearchChannels {
    private SearchChannels(){}
    public static final class Hit {
        public final String channel,title,url,description,published;
        Hit(String c,String t,String u,String d,String p){channel=c;title=t;url=u;description=d;published=p;}
    }
    public static final class Result {
        public final List<Hit> hits=new ArrayList<>();
        public final List<String> failures=new ArrayList<>();
        public int queries=0;
    }
    private static final String[][] QUERIES={
      {"REDDIT","site:reddit.com/r/IndianStreetBets stocks buy sell NSE India"},
      {"REDDIT","site:reddit.com/r/IndianStockMarket stock target stop loss"},
      {"REDDIT","site:reddit.com/r/stocks India NSE buy sell swing"},
      {"X_INDEX","site:x.com Indian stocks target price stop loss analyst"},
      {"X_INDEX","site:twitter.com NSE buy sell stock target price"},
      {"X_INDEX","site:x.com Indian share market buy sell recommendation"},
      {"INDEPENDENT","\"Sumeet Bagadia\" stocks buy target stop loss"},
      {"INDEPENDENT","\"Ganesh Dongre\" stock buy target stop loss"},
      {"INDEPENDENT","\"Shiju Koothupalakkal\" stocks buy target stop loss"},
      {"INDEPENDENT","\"Raja Venkatraman\" stocks buy target"},
      {"INDEPENDENT","\"Vaishali Parekh\" stocks buy stop loss"}
    };
    public static Result search(){
        Result out=new Result();
        for(String[] q:QUERIES){
            out.queries++;
            HttpURLConnection con=null;
            try {
                String uri="https://www.bing.com/search?format=rss&q="+URLEncoder.encode(q[1],"UTF-8");
                con=(HttpURLConnection)new URL(uri).openConnection();
                con.setConnectTimeout(6500);con.setReadTimeout(8500);
                con.setRequestProperty("User-Agent","StockIntelligence/0.4 personal research RSS client");
                if(con.getResponseCode()!=200)throw new IllegalStateException("HTTP "+con.getResponseCode());
                XmlPullParser parser=Xml.newPullParser();
                parser.setInput(con.getInputStream(),"UTF-8");
                int event=parser.getEventType();boolean item=false;
                String title="",link="",description="",date="";
                int count=0;
                while(event!=XmlPullParser.END_DOCUMENT&&count<25){
                    if(event==XmlPullParser.START_TAG){
                        String tag=parser.getName();
                        if("item".equalsIgnoreCase(tag)){item=true;title="";link="";description="";date="";}
                        else if(item&&("title".equals(tag)||"link".equals(tag)||"description".equals(tag)||"pubDate".equals(tag))){
                            String val=parser.nextText();
                            if("title".equals(tag))title=val;
                            else if("link".equals(tag))link=val;
                            else if("description".equals(tag))description=val;
                            else date=val;
                        }
                    }else if(event==XmlPullParser.END_TAG&&"item".equalsIgnoreCase(parser.getName())){
                        item=false;count++;
                        if(link.startsWith("https://")&&belongs(link,q[0]))out.hits.add(new Hit(q[0],title,link,description,date));
                    }
                    event=parser.next();
                }
            }catch(Exception e){out.failures.add(q[0]+": "+e.getClass().getSimpleName());}
            finally{if(con!=null)con.disconnect();}
        }
        return out;
    }
    private static boolean belongs(String link,String channel){
        try{
            String host=new URL(link).getHost().toLowerCase(java.util.Locale.ROOT);
            if("REDDIT".equals(channel))return host.equals("reddit.com")||host.endsWith(".reddit.com");
            if("X_INDEX".equals(channel))return host.equals("x.com")||host.endsWith(".x.com")||host.equals("twitter.com")||host.endsWith(".twitter.com");
            return true;
        }catch(Exception e){return false;}
    }
}
