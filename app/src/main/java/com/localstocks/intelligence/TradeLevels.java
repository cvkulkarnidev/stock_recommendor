package com.localstocks.intelligence;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Strict extraction from visible, attributed text snippets. The caller must
 * have already identified exactly one stock and an unambiguous trade direction.
 * Values are NOT verified against an original signed research report.
 */
public final class TradeLevels {
    private TradeLevels(){}
    public static final class Levels {
        public final Double entry, target, stop;
        public final String entryLabel;
        public Levels(Double e,Double t,Double s,String l){entry=e;target=t;stop=s;entryLabel=l;}
        public boolean hasAny(){return entry!=null||target!=null||stop!=null;}
        public boolean complete(){return entry!=null&&target!=null&&stop!=null;}
    }
    private static final String NUM="([0-9]{1,3}(?:,[0-9]{3})+(?:\\.[0-9]{1,2})?|[0-9]{2,6}(?:\\.[0-9]{1,2})?)";
    private static final String CUR="(?:₹\\s*|Rs\\.?\\s*|INR\\s*)";
    private static final String PRICE="(?:"+CUR+")?"+NUM+"(?![0-9%])";
    private static final Pattern FIGURE=Pattern.compile(PRICE,Pattern.CASE_INSENSITIVE);
    private static final String TARGET="(?:target(?:s| price)?|price target|tgt|tp\\b|objective)";
    private static final String STOP="(?:stop[\\s-]?loss|stoploss|s\\s*/\\s*l|sl\\b)";
    private static final String ENTRY="(?:entry(?: price| level| zone)?|buy(?:ing)?(?: price| level| zone)?|sell(?:ing)?(?: price| level| zone)?|short(?: at| near)?)";
    private static Pattern labelled(String label) {
        return Pattern.compile("(?i)\\b(?:"+label+")\\b\\s*(?:(?:price|levels?|at|of|is|set at|near|around|to|:|=|@|₹)\\s*){0,3}("+PRICE+")");
    }
    private static Double read(Matcher m,int group){
        try{
            Matcher n=FIGURE.matcher(m.group(group));
            if(!n.find())return null;
            double x=Double.parseDouble(n.group(1).replace(",",""));
            return x>=10&&x<=1000000?x:null;
        }catch(Exception ignored){return null;}
    }
    private static Double labelledAmount(String text,String label){
        Matcher m=labelled(label).matcher(text);
        if(m.find())return read(m,1);
        return null;
    }
    private static Double reversedAmount(String text,String label){
        Matcher m=Pattern.compile("(?i)("+PRICE+")\\s*(?:as|for|is|the)?\\s*\\b(?:"+label+")\\b").matcher(text);
        if(m.find())return read(m,1);
        return null;
    }
    private static Double entryNearAction(String text){
        // "Buy HDFC Bank at Rs 1,250" -- excludes an intervening target/SL,
        // second recommendation, date, or lengthy unrelated prose.
        String between="(?:(?!\\b(?:target|tgt|objective|stop|loss|sl|tp|buy|sell|short)\\b)[^.;\\n]){0,56}";
        Pattern pattern=Pattern.compile("(?i)\\b(?:buy|sell|short|accumulate)\\b"+between+
            "\\b(?:at|near|around|above|below|between)\\b\\s*("+PRICE+")");
        Matcher m=pattern.matcher(text);
        return m.find()?read(m,1):null;
    }
    private static boolean unsafeText(String t){
        String n=t.toLowerCase(Locale.ROOT);
        return n.contains("?")||n.matches(".*\\b(?:rumour|rumor|hypothetical|example|illustrative|not a recommendation|no trade)\\b.*");
    }
    public static Levels extract(String text,String direction){
        if(text==null||(!"BUY".equals(direction)&&!"SELL".equals(direction))||unsafeText(text))
            return new Levels(null,null,null,"");
        Double entry=labelledAmount(text,ENTRY);
        if(entry==null)entry=entryNearAction(text);
        if(entry==null)entry=reversedAmount(text,ENTRY);
        Double target=labelledAmount(text,TARGET);
        if(target==null)target=reversedAmount(text,TARGET);
        Double stop=labelledAmount(text,STOP);
        if(stop==null)stop=reversedAmount(text,STOP);
        // Reject invalid trading geometry instead of silently converting the
        // first random price in the story into an entry/stop.
        if(entry!=null&&target!=null&&(
            ("BUY".equals(direction)&&target<=entry)||
            ("SELL".equals(direction)&&target>=entry))) target=null;
        if(entry!=null&&stop!=null&&(
            ("BUY".equals(direction)&&stop>=entry)||
            ("SELL".equals(direction)&&stop<=entry)))stop=null;
        return new Levels(entry,target,stop,"Explicit attributed public text");
    }
}
