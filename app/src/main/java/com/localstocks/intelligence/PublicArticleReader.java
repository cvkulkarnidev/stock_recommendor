package com.localstocks.intelligence;

import android.text.Html;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Opt-in article enrichment during a manual stock Deep Search.
 * Only two reviewed public HTML publishers are eligible. Robots rules are
 * fetched first; unknown/unreachable policy is a hard DENY, never a bypass.
 * No paywall, login, JavaScript automation, or publication of article bodies.
 */
public final class PublicArticleReader {
    private PublicArticleReader(){}
    public static final class Result {
        public final String status,excerpt,published,publisher;
        Result(String s,String e,String p,String publisher){
            status=s;excerpt=e;published=p;this.publisher=publisher;
        }
    }
    private static final String[] HOSTS={"businesstoday.in","business-standard.com"};
    private static final Pattern META_DATE=Pattern.compile(
        "<meta\\s+[^>]*?(?:property|name)\\s*=\\s*['\"](?:article:published_time|datePublished|pubdate|publish-date)['\"][^>]*?content\\s*=\\s*['\"]([^'\"]+)['\"]",
        Pattern.CASE_INSENSITIVE|Pattern.DOTALL);
    private static final Pattern LD_DATE=Pattern.compile(
        "\"datePublished\"\\s*:\\s*\"([^\"]+)\"",Pattern.CASE_INSENSITIVE);
    private static final Pattern BODY=Pattern.compile(
        "\"articleBody\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"",Pattern.DOTALL);
    private static final Pattern ARTICLE=Pattern.compile(
        "<article\\b[^>]*>([\\s\\S]*?)</article>",Pattern.CASE_INSENSITIVE);
    private static boolean hostAllowed(String name){
        String n=name.toLowerCase(Locale.ROOT);
        for(String host:HOSTS)if(n.equals(host)||n.equals("www."+host))return true;
        return false;
    }
    public static boolean supportsUrl(String link){
        try{
            URL u=new URL(link);
            return u.getProtocol().equalsIgnoreCase("https")&&hostAllowed(u.getHost());
        }catch(Exception e){return false;}
    }
    private static String read(String url,int max)throws Exception{
        HttpURLConnection c=null;
        try{
            c=(HttpURLConnection)new URL(url).openConnection();
            c.setConnectTimeout(5000);c.setReadTimeout(7000);
            c.setInstanceFollowRedirects(true);
            c.setRequestProperty("User-Agent","StockIntelligence/0.7 personal research public HTML");
            int status=c.getResponseCode();
            if(status!=200)throw new IllegalStateException("HTTP "+status);
            if(!hostAllowed(c.getURL().getHost()))throw new SecurityException("cross-host redirect");
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] buf=new byte[4096];int n;
                while((n=in.read(buf))!=-1 && out.size()<max){
                    out.write(buf,0,Math.min(n,max-out.size()));
                }
                return out.toString("UTF-8");
            }
        }finally{if(c!=null)c.disconnect();}
    }
    private static String iso(String input){
        try{return OffsetDateTime.parse(input.trim()).withOffsetSameInstant(ZoneOffset.UTC)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);}
        catch(Exception e){return "";}
    }
    static boolean allowedByRobots(String robots,String path){
        if(robots==null)return false;
        boolean inStar=false,seenStar=false;int best=-1;boolean allow=false;
        for(String raw:robots.split("\\r?\\n")){
            String line=raw.replaceAll("#.*$","").trim();
            if(line.isEmpty())continue;
            String low=line.toLowerCase(Locale.ROOT);
            if(low.startsWith("user-agent:")){
                String user=line.substring(line.indexOf(':')+1).trim();
                if(user.equals("*")){inStar=true;seenStar=true;}
                else if(inStar)inStar=false;
            }else if(inStar&&(low.startsWith("allow:")||low.startsWith("disallow:"))){
                boolean isAllow=low.startsWith("allow:");
                String pattern=line.substring(line.indexOf(':')+1).trim();
                if(pattern.isEmpty())continue;
                String beforeDollar=pattern.endsWith("$")?pattern.substring(0,pattern.length()-1):pattern;
                String[] sections=beforeDollar.split("\\*", -1);
                StringBuilder regex=new StringBuilder("^");
                for(int i=0;i<sections.length;i++){
                    if(i>0)regex.append(".*");
                    regex.append(Pattern.quote(sections[i]));
                }
                if(pattern.endsWith("$"))regex.append("$");
                else regex.append(".*");
                if(path.matches(regex.toString())&&pattern.length()>best){
                    best=pattern.length();allow=isAllow;
                }
            }
        }
        return seenStar&&(best==-1||allow);
    }
    public static Result readPublic(String link,String symbol,String company){
        try{
            URL url=new URL(link);
            if(!url.getProtocol().equalsIgnoreCase("https")||!hostAllowed(url.getHost()))
                return new Result("UNSUPPORTED_SOURCE","","","");
            String path=url.getFile();
            String robots=read("https://"+url.getHost()+"/robots.txt",60000);
            if(!allowedByRobots(robots,path))return new Result("ROBOTS_BLOCKED","","","");
            String html=read(link,620000);
            if(html.length()<1000)return new Result("EMPTY","","","");
            String published="";
            Matcher date=META_DATE.matcher(html);
            if(date.find())published=iso(date.group(1));
            if(published.isEmpty()){
                date=LD_DATE.matcher(html);
                if(date.find())published=iso(date.group(1));
            }
            String article="";
            Matcher body=BODY.matcher(html);
            if(body.find()){
                try{article=new JSONObject("{\"text\":\""+body.group(1)+"\"}").getString("text");}
                catch(Exception ignored){}
            }
            if(article.isEmpty()){
                Matcher a=ARTICLE.matcher(html);
                if(a.find())article=a.group(1);
            }
            if(article.isEmpty())return new Result("NO_STRUCTURED_BODY","",published,url.getHost());
            // Only retain short recommendation-related paragraphs; discard
            // all other copyrighted body text immediately after parsing.
            String plain=Html.fromHtml(article.replaceAll("(?is)<(?:script|style|nav|footer)\\b[^>]*>.*?</(?:script|style|nav|footer)>"," "),
                Html.FROM_HTML_MODE_COMPACT).toString().replace('\u00a0',' ');
            StringBuilder keep=new StringBuilder();
            for(String para:plain.split("[\\r\\n]+")){
                String p=para.replaceAll("\\s+"," ").trim();
                String low=p.toLowerCase(Locale.ROOT);
                if(p.length()<25||p.length()>1700)continue;
                boolean relevant=low.contains("hindalco") ||
                    (company!=null&&!company.isEmpty()&&low.contains(company.toLowerCase(Locale.ROOT))) ||
                    (symbol!=null&&!symbol.isEmpty()&&low.contains(symbol.toLowerCase(Locale.ROOT))) ||
                    low.matches(".*\\b(?:brokerage|rating|target price|reiterat|recommend|buy|sell)\\b.*");
                if(!relevant)continue;
                if(keep.length()+p.length()>2400)break;
                keep.append(p).append("\n");
            }
            if(keep.length()==0)return new Result("NO_RECOMMENDATION_TEXT","",published,url.getHost());
            return new Result("READ",keep.toString(),published,url.getHost());
        }catch(Exception e){return new Result("UNAVAILABLE_"+e.getClass().getSimpleName(),"","","");}
    }
}
