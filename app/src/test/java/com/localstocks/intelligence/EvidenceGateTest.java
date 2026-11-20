package com.localstocks.intelligence;

import org.junit.Test;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class EvidenceGateTest {
    private static List<EvidenceGate.Opinion> evidence(int buys,int sells,boolean rich){
        List<EvidenceGate.Opinion> opinions=new ArrayList<>();
        String now=OffsetDateTime.now(ZoneOffset.UTC).minusHours(6).toString();
        for(int i=0;i<buys;i++){
            String firm=i<2?"Independent Analyst "+i:"Institution "+i;
            String publisher="Publisher "+(i%5);
            Double t=rich&&i<6?1100.0+i*2:null;
            Double e=rich&&i<2?1000.0:null;
            Double sl=rich&&i<2?950.0:null;
            opinions.add(new EvidenceGate.Opinion(firm,"BUY",publisher,
                "https://source"+(i%6)+".example/report/"+i,now,e,t,sl,i<2));
        }
        for(int i=0;i<sells;i++){
            opinions.add(new EvidenceGate.Opinion("Bearish Firm "+i,"SELL","Bear Publisher",
                "https://bear"+i+".example/source",now,null,null,null,false));
        }
        return opinions;
    }
    @Test public void threeBuysNeverQualifyAsTen(){
        EvidenceGate.Result result=EvidenceGate.evaluate(evidence(3,0,true));
        assertEquals(3,result.buy);
        assertEquals(15,result.checks.size());
        assertFalse(result.qualifies(10));
        assertEquals("INSUFFICIENT EVIDENCE",result.recommendation);
    }
    @Test public void tenDistinctBuysWithStrongEvidenceQualifyAtTenNotTwelve(){
        EvidenceGate.Result result=EvidenceGate.evaluate(evidence(10,0,true));
        assertTrue(result.qualityPassed>=10);
        assertTrue(result.qualifies(10));
        assertFalse(result.qualifies(12));
        assertFalse(result.qualifies(15));
    }
    @Test public void twelveAndFifteenThresholdsRequireRealBreadth(){
        EvidenceGate.Result r12=EvidenceGate.evaluate(evidence(12,0,true));
        assertTrue(r12.qualifies(12));
        assertFalse(r12.qualifies(15));
        assertTrue(EvidenceGate.evaluate(evidence(15,0,true)).qualifies(15));
    }
    @Test public void repeatingSameBrokerAcrossArticlesCannotIncreaseVotes(){
        List<EvidenceGate.Opinion> calls=evidence(3,0,true);
        EvidenceGate.Opinion original=calls.get(0);
        for(int i=0;i<50;i++)calls.add(new EvidenceGate.Opinion(
            original.institution,"BUY","Syndicated publisher "+i,
            "https://duplicate"+i+".example/story",
            original.published,1000.0,1200.0,950.0,false));
        EvidenceGate.Result result=EvidenceGate.evaluate(calls);
        assertEquals(3,result.buy);
        assertFalse(result.qualifies(10));
    }
    @Test public void strongSellOppositionBlocksApproval(){
        EvidenceGate.Result result=EvidenceGate.evaluate(evidence(12,4,true));
        assertFalse(result.qualifies(10));
    }
    @Test public void noOpinionsMeansZeroPositiveChecks(){
        EvidenceGate.Result result=EvidenceGate.evaluate(new ArrayList<>());
        assertEquals(0,result.qualityPassed);
        assertEquals(15,result.checks.size());
        assertFalse(result.qualifies(10));
    }
}
