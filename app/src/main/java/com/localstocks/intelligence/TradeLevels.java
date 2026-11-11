package com.localstocks.intelligence;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Extract prices only when explicitly labeled in a single-stock public headline. */
public final class TradeLevels {
    private TradeLevels(){}
    public static final class Levels {
        public final Double entry, target, stop;
        public final String entryLabel;
        Levels(Double e, Double t, Double s, String label){entry=e;target=t;stop=s;entryLabel=label;}
        public boolean hasAny(){return entry!=null||target!=null||stop!=null;}
    }
    private static final String MONEY="(?:₹|Rs\\.?\\s*|INR\\s*)\\s*([0-9]{1,3}(?:,[0-9]{3})+|[0-9]{2,6})(?:\\.([0-9]{1,2}))?";
    private static final Pattern AMOUNT=Pattern.compile(MONEY,Pattern.CASE_INSENSITIVE);
    private static Double value(String s) {
        Matcher m=AMOUNT.matcher(s);
        if(!m.find())return null;
        try{
            double amount=Double.parseDouble(m.group(1).replace(",","")+(m.group(2)==null?"":"."+m.group(2)));
            return amount>=10&&amount<=1000000?amount:null;
        }catch(Exception e){return null;}
    }
    private static Double tagged(String headline,String tags){
        Pattern p=Pattern.compile("\\b(?:"+tags+")\\b\\s*(?:price|of|at|near|around|:|is|set at|placed at|of around|up to|to|levels?)?\\s*[:\\-]?\\s*("+MONEY+")",Pattern.CASE_INSENSITIVE);
        Matcher m=p.matcher(headline);
        if(!m.find())return null;
        return value(m.group(1));
    }
    private static Double reverse(String headline,String tags){
        Pattern p=Pattern.compile("("+MONEY+")\\s*(?:as|for|is)?\\s*(?:"+tags+")\\b",Pattern.CASE_INSENSITIVE);
        Matcher m=p.matcher(headline);
        return m.find()?value(m.group(1)):null;
    }
    public static Levels extract(String headline,String direction){
        if(!"BUY".equals(direction)&&!"SELL".equals(direction))
            return new Levels(null,null,null,"");
        Double entry=tagged(headline,"(?:buy|sell|entry|enter|buying|selling)\\s*(?:zone|range|price|level)?");
        if(entry==null)entry=reverse(headline,"(?:buy|sell|entry)(?:\\s+level)?");
        Double target=tagged(headline,"(?:target|targets|target price|price target|tp|objective)");
        if(target==null)target=reverse(headline,"(?:target|target price|price target)");
        Double stop=tagged(headline,"(?:stop[ -]?loss|sl|stop)");
        if(stop==null)stop=reverse(headline,"(?:stop[ -]?loss|sl)");
        // Never call a regular number (e.g. 2026) a target without Rs/INR/₹.
        // Cross-check only if all relevant values are present; contradictory
        // levels are hidden rather than presented as actionable.
        if(entry!=null&&target!=null){
            if(("BUY".equals(direction)&&target<=entry)||("SELL".equals(direction)&&target>=entry))target=null;
        }
        if(entry!=null&&stop!=null){
            if(("BUY".equals(direction)&&stop>=entry)||("SELL".equals(direction)&&stop<=entry))stop=null;
        }
        return new Levels(entry,target,stop,"Explicit public headline");
    }
}
