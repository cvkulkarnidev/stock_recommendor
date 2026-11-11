package com.localstocks.intelligence;

import android.text.Html;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Locale;

/** Searches free, publicly exposed RSS metadata and excerpts only. */
public final class DeepResearch {
    private DeepResearch(){}
    public static final class Hit {
        public final String channel,provider,query,title,excerpt,url,publisher,published;
        Hit(String c,String p,String q,String t,String x,String u,String pub,String date){
            channel=c;provider=p;query=q;title=t;excerpt=x;url=u;publisher=pub;published=date;
        }
    }
    public static final class Report {
        public final List<Hit> hits=new ArrayList<>();
        public final List<String> failures=new ArrayList<>();
        public int queries=0,succeeded=0;
        public int google=0,bing=0;
        public int candidateDomains=0;
        public int observedDomains=0;
        public int skipped=0;
        public final Set<String> observedHosts=new HashSet<>();
    }
    public static Report run(String symbol,String company){
        Report report=new Report();
        List<DeepQueries.Query> queries=DeepQueries.build(symbol,company);
        report.candidateDomains=SourceDirectory.searchable().size();
        int consecutiveIndexFailures=0;
        for(DeepQueries.Query q:queries){
            // If the free index is refusing searches, don't hammer it or
            // silently report skipped requests as completed searches.
            if(q.provider.equals("BING")&&consecutiveIndexFailures>=4){
                report.skipped++;continue;
            }
            report.queries++;
            try{
                fetch(q,report);
                report.succeeded++;
                if(q.provider.equals("GOOGLE"))report.google++;else report.bing++;
                consecutiveIndexFailures=0;
            }catch(Exception e){
                if(report.failures.size()<15)
                    report.failures.add(q.channel+"/"+q.provider+": "+e.getClass().getSimpleName());
                if(q.provider.equals("BING"))consecutiveIndexFailures++;
            }
        }
        report.observedDomains=report.observedHosts.size();
        return report;
    }
    private static String iso(String input){
        if(input==null||input.isEmpty())return "";
        try{return ZonedDateTime.parse(input,DateTimeFormatter.RFC_1123_DATE_TIME)
            .withZoneSameInstant(ZoneOffset.UTC).toOffsetDateTime().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);}
        catch(Exception ignored){return "";}
    }
    private static String clean(String input){
        if(input==null)return "";
        try{
            String value=Html.fromHtml(input,Html.FROM_HTML_MODE_COMPACT).toString()
                .replace('\u00a0',' ').replaceAll("\\s+"," ").trim();
            return value.substring(0,Math.min(900,value.length()));
        }catch(Exception e){return input.substring(0,Math.min(input.length(),900));}
    }
    private static boolean allowedUrl(String link,String channel){
        try{
            URL u=new URL(link);
            if(!"https".equalsIgnoreCase(u.getProtocol()))return false;
            String host=u.getHost().toLowerCase(Locale.ROOT);
            if(channel.equals("REDDIT"))return host.equals("reddit.com")||host.endsWith(".reddit.com");
            if(channel.equals("X_INDEX"))return host.equals("x.com")||host.endsWith(".x.com")||host.equals("twitter.com")||host.endsWith(".twitter.com");
            return true;
        }catch(Exception ignored){return false;}
    }
    private static void fetch(DeepQueries.Query q,Report r)throws Exception {
        HttpURLConnection conn=null;
        try{
            String base=q.provider.equals("GOOGLE")
                ?"https://news.google.com/rss/search?q="+URLEncoder.encode(q.text,"UTF-8")+"&hl=en-IN&gl=IN&ceid=IN:en"
                :"https://www.bing.com/search?format=rss&q="+URLEncoder.encode(q.text,"UTF-8");
            conn=(HttpURLConnection)new URL(base).openConnection();
            conn.setConnectTimeout(7500);conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(true);
            conn.setRequestProperty("User-Agent","StockIntelligence/0.5 personal RSS research");
            if(conn.getResponseCode()!=200)throw new IllegalStateException("HTTP "+conn.getResponseCode());
            XmlPullParser parser=Xml.newPullParser();
            parser.setInput(conn.getInputStream(),"UTF-8");
            int event=parser.getEventType(),count=0;
            boolean inItem=false;String title="",link="",description="",date="",publisher="";
            while(event!=XmlPullParser.END_DOCUMENT&&count<35){
                if(event==XmlPullParser.START_TAG){
                    String tag=parser.getName();
                    if("item".equalsIgnoreCase(tag)){
                        inItem=true;title="";link="";description="";date="";publisher="";
                    }else if(inItem&&("title".equals(tag)||"link".equals(tag)||"description".equals(tag)||"pubDate".equals(tag)||"source".equals(tag))){
                        String val=parser.nextText();
                        if("title".equals(tag))title=val;
                        else if("link".equals(tag))link=val;
                        else if("description".equals(tag))description=val;
                        else if("pubDate".equals(tag))date=val;
                        else publisher=val;
                    }
                }else if(event==XmlPullParser.END_TAG&&"item".equalsIgnoreCase(parser.getName())){
                    inItem=false;count++;
                    if(publisher.isEmpty()&&q.provider.equals("GOOGLE")&&title.contains(" - ")){
                        publisher=title.substring(title.lastIndexOf(" - ")+3);
                        title=title.substring(0,title.lastIndexOf(" - "));
                    }
                    if(allowedUrl(link,q.channel)&&!title.isEmpty()){
                        // Bing's RSS date often describes indexing, not original post
                        // publication. It must not become a dated actionable signal.
                        String published=q.provider.equals("GOOGLE")?iso(date):"";
                        r.hits.add(new Hit(q.channel,q.provider,q.text,
                            clean(title),clean(description),link,publisher,published));
                        try{r.observedHosts.add(new URL(link).getHost().toLowerCase(Locale.ROOT));}
                        catch(Exception ignored){}
                    }
                }
                event=parser.next();
            }
        }finally{if(conn!=null)conn.disconnect();}
    }
}
