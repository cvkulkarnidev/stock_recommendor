package com.localstocks.intelligence;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Analytics {
    private Analytics(){}
    public static final class Score {
        public final String symbol, direction, explanation;
        public final int value, supports, opposes;
        public Score(String s,String d,int v,int a,int b,String e) {symbol=s;direction=d;value=v;supports=a;opposes=b;explanation=e;}
    }
    // One underlying analyst/institution per symbol and day is counted once.
    // Do not mistake an evidence confidence score for a probability of profit.
    public static Score score(String symbol,List<Store.Call> all) {
        Map<String,Store.Call> latest=new HashMap<>();
        for(Store.Call c:all) {
            if(!symbol.equals(c.symbol))continue;
            String key=c.sourceKey+"|"+c.published.substring(0,10);
            Store.Call previous=latest.get(key);
            if(previous==null||c.published.compareTo(previous.published)>0)latest.put(key,c);
        }
        int buys=0,sells=0,recent=0,complete=0;List<Double> levels=new ArrayList<>();
        LocalDate today=LocalDate.now();
        for(Store.Call c:latest.values()) {
            if(c.direction.equals("BUY"))buys++;else sells++;
            try {long age=ChronoUnit.DAYS.between(LocalDate.parse(c.published.substring(0,10)),today);if(age>=0&&age<=14)recent++;} catch(Exception ignored){}
            if(c.entry>0&&c.target>0&&c.stop>0)complete++;
            levels.add(c.entry);
        }
        int total=buys+sells;
        if(total==0)return new Score(symbol,"UNKNOWN",0,0,0,"No verified calls yet. Search results alone never count as trade signals.");
        boolean buy=buys>sells;boolean tie=buys==sells;
        int supports=Math.max(buys,sells),opposes=Math.min(buys,sells);
        double agreement=30.0*supports/total;
        double freshness=15.0*recent/total;
        double completeness=15.0*complete/total;
        double independent=15.0*Math.min(supports,4)/4.0;
        // Historical reliability and level-consensus weights are deliberately withheld
        // until measurable outcomes and comparable entry ranges are available.
        int value=(int)Math.round(agreement+freshness+completeness+independent);
        String dir=tie?"MIXED":buy?"BUY":"SELL";
        String reason=supports+" supporting, "+opposes+" opposing independent analyst-day opinions; "
                +recent+" recent (14d), "+complete+" with complete price levels. "
                +"Historical reliability not yet scored; score is provisional (maximum 75).";
        return new Score(symbol,dir,value,supports,opposes,reason);
    }
    public static final class Outcome {
        public final String status;public final Double pct;public final String day;
        Outcome(String s,Double p,String d){status=s;pct=p;day=d;}
    }
    public static Outcome evaluate(Store.Call c,List<Store.Bar> bars) {
        if(bars.isEmpty())return new Outcome("NO PRICES",null,"");
        boolean entered=false;String entryDay="";String last="";
        for(Store.Bar b:bars){
            // Publication timestamp may be during the session. Begin on next calendar day
            // to avoid look-ahead and same-day price ambiguity.
            if(b.day.compareTo(c.published.substring(0,10))<=0)continue;
            last=b.day;
            if(!entered) {
                if(c.entry<b.low||c.entry>b.high)continue;
                entered=true;entryDay=b.day;
                // No assumptions about price sequence inside entry day.
                continue;
            }
            boolean hitTarget=c.direction.equals("BUY")?b.high>=c.target:b.low<=c.target;
            boolean hitStop=c.direction.equals("BUY")?b.low<=c.stop:b.high>=c.stop;
            if(hitStop&&hitTarget)return new Outcome("AMBIGUOUS BOTH HIT",null,b.day);
            if(hitStop)return new Outcome("STOP",signedReturn(c,c.stop),b.day);
            if(hitTarget)return new Outcome("TARGET",signedReturn(c,c.target),b.day);
        }
        if(!entered)return new Outcome("ENTRY NOT REACHED",null,last);
        Store.Bar newest=bars.get(bars.size()-1);
        return new Outcome("OPEN since "+entryDay,signedReturn(c,newest.close),newest.day);
    }
    private static double signedReturn(Store.Call c,double exit) {
        double direction=c.direction.equals("BUY")?1:-1;
        return direction*(exit-c.entry)/c.entry*100.0;
    }
}
