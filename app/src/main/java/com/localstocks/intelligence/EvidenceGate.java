package com.localstocks.intelligence;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Provenance-aware evidence gate, NOT a probability of future returns. */
public final class EvidenceGate {
    private EvidenceGate(){}
    public static final int[] THRESHOLDS={10,12,15};
    public static final class Opinion {
        public final String institution,direction,publisher,url,published;
        public final Double entry,target,stop;
        public final boolean analyst;
        public Opinion(String i,String d,String p,String u,String date,Double en,Double t,Double sl,boolean a){
            institution=i;direction=d;publisher=p;url=u;published=date;
            entry=en;target=t;stop=sl;analyst=a;
        }
    }
    public static final class Check {
        public final String label,explanation;
        public final boolean passed;
        public Check(String l,String e,boolean p){label=l;explanation=e;passed=p;}
    }
    public static final class Result {
        public final int buy,sell,qualityPassed;
        public final List<Check> checks;
        public final String recommendation;
        public Result(int b,int s,List<Check> checks){
            buy=b;sell=s;this.checks=checks;
            int n=0;for(Check c:checks)if(c.passed)n++;
            qualityPassed=n;
            // Conservative default: never claim strong independent buy coverage
            // merely because a few repeated news stories agree.
            recommendation=qualifies(10)?"EVIDENCE SUFFICIENT":"INSUFFICIENT EVIDENCE";
        }
        public boolean qualifies(int required){
            if(required!=10&&required!=12&&required!=15)required=10;
            return buy>=required && qualityPassed>=10 && sell*4<=buy;
        }
    }
    private static String host(String url){
        try {return new java.net.URL(url).getHost().toLowerCase(Locale.ROOT).replaceFirst("^www\\.","");}
        catch(Exception ignored){return "";}
    }
    private static boolean recent(String value,int days){
        try {
            long h=Duration.between(OffsetDateTime.parse(value),OffsetDateTime.now(ZoneOffset.UTC)).toHours();
            return h>=0&&h<=days*24L;
        }catch(Exception ignored){return false;}
    }
    public static Result evaluate(List<Opinion> input){
        Set<String> seenInstitutions=new HashSet<>();
        Set<String> publishers=new HashSet<>(),hosts=new HashSet<>();
        List<Double> targets=new ArrayList<>();
        int buys=0,sells=0,analysts=0,brokers=0,entry=0,stop=0,target=0,recent3=0,recent7=0,sourceLinks=0;
        // Caller may pass duplicate articles. This function itself deduplicates
        // underlying institution names; a repeated report cannot add votes.
        for(Opinion o:input){
            if(o==null||o.institution==null||o.institution.trim().isEmpty())continue;
            String key=o.institution.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+"," ").trim();
            if(!seenInstitutions.add(key))continue;
            if("BUY".equals(o.direction)){
                buys++;
                if(o.analyst)analysts++;else brokers++;
                if(o.entry!=null&&o.entry>0)entry++;
                if(o.stop!=null&&o.stop>0)stop++;
                if(o.target!=null&&o.target>0){target++;targets.add(o.target);}
                if(recent(o.published,3))recent3++;
                if(recent(o.published,7))recent7++;
                if(o.publisher!=null&&!o.publisher.trim().isEmpty())publishers.add(o.publisher.toLowerCase(Locale.ROOT).trim());
                String h=host(o.url);
                if(!h.isEmpty()){hosts.add(h);sourceLinks++;}
            }else if("SELL".equals(o.direction))sells++;
        }
        boolean targetCluster=false;
        if(targets.size()>=3){
            java.util.Collections.sort(targets);
            double med=targets.get(targets.size()/2);
            int within=0;
            for(double t:targets)if(Math.abs(t-med)/med<=0.2)within++;
            targetCluster=within>=3;
        }
        List<Check> checks=new ArrayList<>();
        add(checks,"At least 3 distinct BUY institutions","Entry-level agreement; repeated articles count once",buys>=3);
        add(checks,"At least 5 distinct BUY institutions","Broader independent research",buys>=5);
        add(checks,"At least 10 distinct BUY institutions","Minimum independent-opinion target",buys>=10);
        add(checks,"At least 12 distinct BUY institutions","Stronger breadth milestone",buys>=12);
        add(checks,"At least 15 distinct BUY institutions","Highest breadth milestone",buys>=15);
        add(checks,"Few contradictory SELL opinions","At most one opposing SELL for every four BUYs",buys>0&&sells*4<=buys);
        add(checks,"At least 3 institutional brokerage BUYs","Named firms, not repeated news publishers",brokers>=3);
        add(checks,"At least 2 named independent analyst BUYs","Diversity beyond brokerage calls",analysts>=2);
        add(checks,"At least 2 reporting publishers","Publication diversity; not extra analyst votes",publishers.size()>=2);
        add(checks,"At least 4 reporting publishers","Wider corroborating coverage",publishers.size()>=4);
        add(checks,"At least 3 distinct evidence URL hosts","Multiple accessible references",hosts.size()>=3&&sourceLinks>=3);
        add(checks,"At least 3 dated BUYs in last 3 days","Freshness for swing trading",recent3>=3);
        add(checks,"At least 5 dated BUYs in last 7 days","Recent continued research interest",recent7>=5);
        add(checks,"At least 3 explicit target prices","Publication gives actual target values",target>=3);
        add(checks,"Three targets within 20% of median","Broad target alignment, not target accuracy",targetCluster);
        add(checks,"At least one explicit entry and stop-loss","Risk parameters provided, not invented",entry>=1&&stop>=1);
        return new Result(buys,sells,checks);
    }
    private static void add(List<Check> checks,String title,String note,boolean good){
        checks.add(new Check(title,note,good));
    }
}
