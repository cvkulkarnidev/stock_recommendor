package com.localstocks.intelligence;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conservative headline-only classification for personal research. Curated
 * publication names are a source-quality hint, NOT a verification of authorship,
 * investment-adviser registration or trade accuracy.
 */
public final class ResearchRules {
    private ResearchRules() {}

    private static final String[] BROKERS = {
        "ICICI Direct","Motilal Oswal","HDFC Securities","Kotak Securities",
        "Axis Securities","Anand Rathi","Choice Broking","Prabhudas Lilladher",
        "Nuvama","Emkay Global","JM Financial","Sharekhan","Geojit",
        "SBI Securities","IIFL Securities","Angel One","Religare Broking",
        "BOB Capital","Elara Capital","Centrum Broking","YES Securities",
        "Systematix","KR Choksey","Ventura Securities","PhillipCapital",
        "Jefferies","Nomura","Morgan Stanley","Goldman Sachs","Citi",
        "UBS","Bernstein","HSBC","CLSA","Macquarie","Investec"
    };
    private static final String[] ANALYSTS = {
        "Sumeet Bagadia","Ganesh Dongre","Shiju Koothupalakkal",
        "Raja Venkatraman","Vaishali Parekh","Shrikant Chouhan"
    };
    private static final String[] EDITORIAL = {
        "Mint","LiveMint","Economic Times","ET Markets","Business Standard",
        "Moneycontrol","CNBC TV18","CNBC-TV18","NDTV Profit","Reuters",
        "Bloomberg","Financial Express","The Hindu BusinessLine","BusinessLine",
        "The Hindu","Hindustan Times","Zee Business","ET Now"
    };
    private static final String BUY_TERMS =
       "(?:buy(?:ing)?|accumulate|upgrad(?:e|ed|es) to (?:buy|outperform|overweight)|initiates? (?:with )?buy|outperform|overweight)";
    private static final String SELL_TERMS =
       "(?:sell(?:ing)?|exit|reduce|downgrad(?:e|ed|es) to (?:sell|underperform|underweight)|underperform|underweight|avoid)";
    public static final class Source {
        public final String name,tier;
        public final double weight;
        public Source(String n,String t,double w){name=n;tier=t;weight=w;}
    }
    public static final class Signal {
        public final String direction,reason;
        public Signal(String d,String r){direction=d;reason=r;}
    }
    private static String norm(String s){
        return s.toLowerCase(Locale.ROOT).replace('&',' ').replaceAll("[^a-z0-9]+"," ").replaceAll("\\s+"," ").trim();
    }
    private static boolean phrase(String value,String term){
        return (" "+norm(value)+" ").contains(" "+norm(term)+" ");
    }
    public static Source source(String title,String publisher){
        // Broker explicitly named IN the headline: identity attribution is only
        // claimed by the headline, not verified against a signed original report.
        for(String s:BROKERS)if(phrase(title,s))return new Source(s,"NAMED_BROKER",1.0);
        for(String s:BROKERS)if(phrase(publisher,s))return new Source(s,"BROKER_PUBLISHER",1.0);
        for(String s:ANALYSTS)if(phrase(title,s))return new Source(s,"NAMED_ANALYST",0.85);
        for(String s:EDITORIAL)if(phrase(publisher,s))return new Source(s,"CURATED_NEWS",0.76);
        return new Source(publisher==null||publisher.trim().isEmpty()?"Unknown source":publisher.trim(),"OTHER",0.47);
    }
    private static String[] aliases(String symbol,String name){
        if(symbol.equals("RELIANCE"))return new String[]{"reliance industries","reliance stock","reliance shares"};
        if(symbol.equals("TCS"))return new String[]{"tcs shares","tcs stock","tata consultancy services"};
        if(symbol.equals("ITC"))return new String[]{"itc shares","itc stock","itc ltd"};
        if(symbol.equals("LT"))return new String[]{"larsen toubro","larsen and toubro","l t shares"};
        if(symbol.equals("SBIN"))return new String[]{"sbi stock","sbi shares","state bank of india"};
        if(symbol.equals("M&M"))return new String[]{"mahindra mahindra","m m shares","mahindra shares"};
        if(symbol.equals("ZOMATO"))return new String[]{"zomato shares","zomato stock","eternal stock"};
        if(symbol.equals("PAYTM"))return new String[]{"paytm shares","paytm stock","one 97 communications"};
        if(symbol.equals("INDIGO"))return new String[]{"interglobe aviation","indigo shares","indigo airline"};
        if(symbol.equals("DMART"))return new String[]{"dmart shares","dmart stock","avenue supermarts"};
        if(symbol.equals("NYKAA"))return new String[]{"nykaa stock","nykaa shares","fsn e commerce"};
        if(symbol.equals("HINDUNILVR"))return new String[]{"hindustan unilever","hul stock","hul shares"};
        if(symbol.equals("BHARTIARTL"))return new String[]{"bharti airtel","airtel stock","airtel shares"};
        if(symbol.equals("BAJAJ-AUTO"))return new String[]{"bajaj auto"};
        if(symbol.equals("JIOFIN"))return new String[]{"jio financial"};
        if(symbol.equals("MARUTI"))return new String[]{"maruti suzuki"};
        if(symbol.equals("ADANIENT"))return new String[]{"adani enterprises"};
        if(symbol.equals("ADANIPORTS"))return new String[]{"adani ports"};
        if(symbol.equals("ADANIGREEN"))return new String[]{"adani green"};
        return new String[]{name,symbol.length()>3?symbol+" stock":name};
    }
    public static Signal classify(String headline,String symbol,String company){
        // Different clauses can describe different companies or positions.
        // Never inherit a BUY/SELL from an unrelated clause of a roundup.
        if(headline.contains(";")){
            for(String part:headline.split(";")){
                String n=norm(part);
                for(String alias:aliases(symbol,company)){
                    if(norm(alias).length()>=4&&phrase(n,alias))
                        return classify(part,symbol,company);
                }
            }
            return new Signal("WATCH","No stock-specific direction in separate clauses");
        }
        String t=norm(headline);
        // Stock-list headlines without explicit analyst attribution are only leads.
        if((t.contains("stocks to buy")||t.contains("stock picks")||t.contains("top picks"))
          && !t.matches(".*\\b(recommends?|recommended|rates|initiates?|target)\\b.*"))
            return new Signal("WATCH","Generic multi-stock headline without attributable call");
        // Questions, broad educational pieces and listicles without stock-specific
        // actionable attribution cannot be treated as recommendations.
        if(headline.contains("?") || t.matches(".*\\b(how to|what is|should you|explained|buy or sell|buy sell|buy hold or sell|stocks to watch)\\b.*"))
            return new Signal("WATCH","Question or generic market discussion");
        if(t.matches(".*\\b(?:do not|dont|not|never|avoid) buy\\b.*")||
           t.matches(".*\\b(?:do not|dont|not|never) sell\\b.*")||
           t.contains("avoid selling")||t.contains("avoid buying"))
            return new Signal("WATCH","Negated instruction is not a directional trade recommendation");
        // Directional rating changes: the final rating supersedes the former one.
        if(t.matches(".*\\b(?:from|previously) (?:sell|underperform) to buy\\b.*"))
            return new Signal("BUY","Explicit rating transition to buy");
        if(t.matches(".*\\b(?:from|previously) (?:buy|outperform) to sell\\b.*"))
            return new Signal("SELL","Explicit rating transition to sell");
        String buy="\\b"+BUY_TERMS+"\\b";
        String sell="\\b"+SELL_TERMS+"\\b";
        boolean b=Pattern.compile(buy).matcher(t).find();
        boolean s=Pattern.compile(sell).matcher(t).find();
        if(!b&&!s)return new Signal("WATCH","No explicit trading recommendation");
        if(b&&s)return new Signal("MIXED","Both buy and sell language; cannot reliably attribute to this stock");
        String[] aliases=aliases(symbol,company);
        int closest=999;
        for(String alias:aliases){
            String a=norm(alias);
            if(a.length()<4)continue;
            Matcher m=Pattern.compile("(?<![a-z0-9])"+Pattern.quote(a)+"(?![a-z0-9])").matcher(t);
            while(m.find()){
                String before=t.substring(Math.max(0,m.start()-85),m.start());
                String after=t.substring(m.end(),Math.min(t.length(),m.end()+85));
                Matcher direction=Pattern.compile(b?"\\b"+BUY_TERMS+"\\b":"\\b"+SELL_TERMS+"\\b").matcher(before);
                while(direction.find())closest=Math.min(closest,before.length()-direction.end());
                direction=Pattern.compile(b?"\\b"+BUY_TERMS+"\\b":"\\b"+SELL_TERMS+"\\b").matcher(after);
                if(direction.find())closest=Math.min(closest,direction.start());
            }
        }
        if(closest>48)return new Signal("WATCH","Headline mentions stock but recommendation is not directly attributable");
        // Price target alone is NOT a buy signal. "Upgrade" alone without a
        // stated rating can mean other things, hence require a named broker.
        String term=b?"BUY":"SELL";
        if(t.contains("rumour")||t.contains("rumor"))return new Signal("WATCH","Rumor, not an attributable trade call");
        return new Signal(term,"Explicit "+term+" term appears near stock name in headline");
    }
    public static String[] brokers(){return BROKERS.clone();}
    public static String[] editorial(){return EDITORIAL.clone();}
    public static String[] analysts(){return ANALYSTS.clone();}
}
