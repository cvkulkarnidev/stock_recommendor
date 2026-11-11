package com.localstocks.intelligence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative per-institution extractor for publicly provided article titles
 * and RSS excerpts. It never assumes a publisher is the reporting brokerage.
 * It does not claim to have retrieved the full article body or signed PDF.
 */
public final class BrokerageCallExtractor {
    private BrokerageCallExtractor(){}
    public static final class Call {
        public final String institution,direction,evidence;
        public final Double target,entry,stop;
        public final String targetLabel;
        public Call(String i,String d,String e,Double t,Double en,Double sl,String lbl){
            institution=i;direction=d;evidence=e;target=t;entry=en;stop=sl;targetLabel=lbl;
        }
    }
    private static final String[] INSTITUTIONS={
      "ICICI Securities","ICICI Direct","Motilal Oswal","Elara Securities","Elara Capital",
      "Axis Securities","Axis Direct","Nuvama Institutional Equities","Nuvama",
      "UBS","Nomura","CLSA","JPMorgan","J.P. Morgan","JP Morgan",
      "Jefferies","Kotak Securities","Kotak Neo","HDFC Securities",
      "Morgan Stanley","Goldman Sachs","Citi","Anand Rathi",
      "Choice Broking","Prabhudas Lilladher","Sharekhan","Emkay",
      "JM Financial","HSBC","Bernstein","Investec","IIFL Securities",
      "Sumeet Bagadia","Ganesh Dongre","Shiju Koothupalakkal","Raja Venkatraman",
      "Vaishali Parekh"
    };
    private static final Pattern BUY=Pattern.compile(
      "(?i)\\b(?:buy(?:ing)?|accumulate|outperform|overweight|add rating|upgrad(?:ed|e|es)\\s+(?:the\\s+stock\\s+)?to\\s+buy)\\b");
    private static final Pattern SELL=Pattern.compile(
      "(?i)\\b(?:sell(?:ing)?|underperform|underweight|reduce|downgrad(?:ed|e|es)\\s+(?:the\\s+stock\\s+)?to\\s+sell)\\b");
    private static final Pattern CONTRARY=Pattern.compile(
      "(?i)\\b(?:buy\\s+or\\s+sell|sell\\s+or\\s+buy|not\\s+(?:a\\s+)?buy|not\\s+(?:a\\s+)?sell|do\\s+not\\s+buy|do\\s+not\\s+sell|avoid\\s+buying|avoid\\s+selling)\\b");
    private static final Pattern ACTION=Pattern.compile(
      "(?i)\\b(?:rated|rates|rating|reiterat(?:e|es|ed|ing)|maintain(?:s|ed)?|retain(?:s|ed)?|recommend(?:s|ed)?|upgrad(?:e|ed|es)|downgrad(?:e|ed|es)|initiates?|call|advises?)\\b");
    private static final Pattern EXPLICIT_TARGET=Pattern.compile(
      "(?i)\\b(?:target(?:\\s+price)?|tp|fair\\s+value|fv)\\s*(?:of|at|to|\\:|-|is|near)?\\s*(?:₹|rs\\.?|inr)?\\s*([1-9][0-9,]{1,7}(?:\\.[0-9]{1,2})?)");
    private static final Pattern CURRENT_PRICE=Pattern.compile(
      "(?i)\\b(?:cmp|current\\s+market\\s+price|ltp)\\s*[-:]?\\s*(?:₹|rs\\.?|inr)?\\s*([1-9][0-9,]{1,7}(?:\\.[0-9]{1,2})?)");
    private static final Pattern SIMPLE_ENTRY=Pattern.compile(
      "(?i)\\b(?:entry(?:\\s+price)?|buy\\s+at|sell\\s+at)\\s*(?:of|at|near|:|-)?\\s*(?:₹|rs\\.?|inr)?\\s*([1-9][0-9,]{1,7}(?:\\.[0-9]{1,2})?)");
    private static final Pattern SIMPLE_STOP=Pattern.compile(
      "(?i)\\b(?:stop\\s*-?\\s*loss|sl)\\s*(?:at|of|:|-)?\\s*(?:₹|rs\\.?|inr)?\\s*([1-9][0-9,]{1,7}(?:\\.[0-9]{1,2})?)");
    private static String norm(String v){
        return v.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
    private static boolean hasPhrase(String text,String phrase){
        return (" "+norm(text)+" ").contains(" "+norm(phrase)+" ");
    }
    private static boolean hasStock(String text,String symbol,String name){
        String n=norm(text);
        if(name!=null&&norm(name).length()>3&&hasPhrase(n,name))return true;
        if(symbol!=null&&symbol.length()>3&&hasPhrase(n,symbol))return true;
        if("HINDALCO".equals(symbol)&&hasPhrase(n,"Hindalco"))return true;
        return false;
    }
    private static Double number(Matcher m){
        if(!m.find())return null;
        try{
            double value=Double.parseDouble(m.group(1).replace(",",""));
            return value>=10&&value<=100000?value:null;
        }catch(Exception e){return null;}
    }
    private static Double find(Pattern p,String text){return number(p.matcher(text));}
    private static String canonical(String name){
        String n=norm(name);
        if(n.contains("icici"))return "ICICI Securities / ICICI Direct";
        if(n.contains("nuvama"))return "Nuvama";
        if(n.contains("axis"))return "Axis Securities";
        if(n.contains("elara"))return "Elara Securities";
        if(n.contains("jp morgan")||n.contains("jpmorgan"))return "JPMorgan";
        if(n.contains("kotak"))return "Kotak Securities";
        return name;
    }
    private static String decide(String text){
        if(CONTRARY.matcher(text).find()||text.contains("?"))return "WATCH";
        String n=norm(text);
        // Evaluate the final rating on a change, not the old rating or a reduced target.
        if(n.matches(".*\\b(?:from|previously)\\s+(?:sell|underperform)\\s+to\\s+buy\\b.*"))return "BUY";
        if(n.matches(".*\\b(?:from|previously)\\s+(?:buy|outperform)\\s+to\\s+sell\\b.*"))return "SELL";
        if(n.matches(".*\\b(?:from|previously)\\s+(?:buy|outperform)\\s+to\\s+hold\\b.*"))return "HOLD";
        if(n.matches(".*\\b(?:from|previously)\\s+(?:sell|underperform)\\s+to\\s+hold\\b.*"))return "HOLD";
        if(n.matches(".*\\b(?:rated|rating|reiterated|maintains|maintained|retained|recommended)\\s+hold\\b.*"))return "HOLD";
        boolean b=BUY.matcher(text).find(),s=SELL.matcher(text).find();
        // Headlines often say "target cut" even while BUY remains: not SELL.
        if(b&&s)return "MIXED";
        if(b)return "BUY";
        if(s)return "SELL";
        return "WATCH";
    }
    private static void put(Map<String,Call> calls,String institution,String direction,String evidence){
        if(direction.equals("WATCH")||direction.equals("MIXED"))return;
        String k=norm(canonical(institution));
        if(calls.containsKey(k))return;
        Double target=find(EXPLICIT_TARGET,evidence);
        Double entry=find(SIMPLE_ENTRY,evidence);
        Double stop=find(SIMPLE_STOP,evidence);
        // CMP is a reference quote, not a recommendation to enter at that price.
        if(entry!=null&&target!=null){
            if(direction.equals("BUY")&&target<=entry)target=null;
            if(direction.equals("SELL")&&target>=entry)target=null;
        }
        if(entry!=null&&stop!=null){
            if(direction.equals("BUY")&&stop>=entry)stop=null;
            if(direction.equals("SELL")&&stop<=entry)stop=null;
        }
        calls.put(k,new Call(canonical(institution),direction,evidence,target,entry,stop,
            evidence.toLowerCase(Locale.ROOT).contains("fv")?"Fair value":"Published target"));
    }
    public static List<Call> extract(String title,String excerpt,String publisher,String symbol,String company){
        Map<String,Call> result=new LinkedHashMap<>();
        String head=title==null?"":title;
        String summary=excerpt==null?"":excerpt;
        boolean titleAboutStock=hasStock(head,symbol,company);
        // Headline roundups cannot lend a trade view on one stock to a different stock.
        boolean multiHeadline=head.matches("(?i).*(?:among\\s+[2-9]|and\\s+others|stocks\\s+to\\s+buy|share\\s+stock\\s+recommendations\\s+on|multiple\\s+stocks).*");
        String text=(head+"; "+summary).replaceAll("\\s+"," ").trim();
        String[] pieces=text.split("(?<=[.;])\\s+|\\n+");
        for(String piece:pieces){
            if(piece.length()<12)continue;
            String direction=decide(piece);
            if(direction.equals("WATCH")||direction.equals("MIXED"))continue;
            boolean stockHere=hasStock(piece,symbol,company);
            if(!stockHere && (!titleAboutStock||multiHeadline))continue;
            List<String> names=new ArrayList<>();
            for(String institution:INSTITUTIONS)if(hasPhrase(piece,institution)) {
                String canonical=canonical(institution);
                if(!names.contains(canonical))names.add(canonical);
            }
            // Only attribute broker groups when the same recommendation/rating
            // clause addresses those institutions. Otherwise too ambiguous.
            for(String name:names) {
                int index=norm(piece).indexOf(norm(name));
                if(index<0)continue;
                // Source text must describe an actual rating/recommendation,
                // or the single-stock headline directly state Buy or Sell.
                boolean direct=ACTION.matcher(piece).find() || (stockHere&&names.size()==1);
                if(!direct)continue;
                put(result,name,direction,piece);
            }
        }
        // Structured pages with a single named original brokerage may use
        // "Buy Hindalco; target of Rs 1180: Motilal Oswal" format.
        if(result.isEmpty() && titleAboutStock && !multiHeadline){
            String direction=decide(head);
            if(direction.equals("BUY")||direction.equals("SELL")||direction.equals("HOLD")){
                for(String name:INSTITUTIONS)if(hasPhrase(head,name)){
                    put(result,name,direction,head);
                    break;
                }
            }
        }
        return new ArrayList<>(result.values());
    }
}