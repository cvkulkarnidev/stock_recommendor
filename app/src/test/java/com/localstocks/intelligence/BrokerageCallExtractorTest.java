package com.localstocks.intelligence;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/** Regression corpus based on public Indian-market reporting patterns. */
public class BrokerageCallExtractorTest {
    private static BrokerageCallExtractor.Call get(List<BrokerageCallExtractor.Call> calls,String name){
        for(BrokerageCallExtractor.Call c:calls)if(c.institution.contains(name))return c;
        return null;
    }
    @Test public void moneycontrolSingleBrokerHeadline(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Buy Hindalco; target of Rs 1180: Motilal Oswal",
          "Motilal Oswal reiterated BUY rating in its research report dated November 07, 2026.",
          "Moneycontrol","HINDALCO","Hindalco Industries");
        assertNotNull(get(r,"Motilal Oswal"));
        assertEquals("BUY",get(r,"Motilal Oswal").direction);
        assertEquals(1180.0,get(r,"Motilal Oswal").target,0.01);
        assertNull(get(r,"Motilal Oswal").entry);
    }
    @Test public void twoGlobalBrokersInOneArticle(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Hindalco shares slip; UBS and Nomura reiterate Buy calls",
          "UBS maintained its Buy rating on Hindalco with a target price of Rs 1,325. "+
          "Nomura also retained its Buy recommendation on Hindalco with a target of Rs 1,100.",
          "Moneycontrol","HINDALCO","Hindalco Industries");
        assertNotNull(get(r,"UBS"));assertNotNull(get(r,"Nomura"));
        assertEquals(1325.0,get(r,"UBS").target,0.01);
        assertEquals(1100.0,get(r,"Nomura").target,0.01);
        assertEquals(2,r.size());
    }
    @Test public void targetCutIsStillBuy(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Hindalco shares: brokerages keep Buy rating",
          "Elara Securities reiterated its Buy rating on Hindalco but lowered its target price to Rs 1,227 from Rs 1,270. "+
          "Axis Securities also maintained its Buy rating on Hindalco but cut its target price to Rs 1,060 from Rs 1,155.",
          "Business Today","HINDALCO","Hindalco Industries");
        assertEquals("BUY",get(r,"Elara Securities").direction);
        assertEquals(1227.0,get(r,"Elara Securities").target,0.01);
        assertEquals("BUY",get(r,"Axis Securities").direction);
        assertEquals(1060.0,get(r,"Axis Securities").target,0.01);
    }
    @Test public void upgradesToBuyRatherThanTargetCutMisread(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Hindalco shares: brokerages reiterate calls",
          "Nuvama Institutional Equities upgraded the stock to Buy and raised its target price to Rs 1,184 from Rs 1,108.",
          "Business Today","HINDALCO","Hindalco Industries");
        assertNotNull(get(r,"Nuvama"));
        assertEquals("BUY",get(r,"Nuvama").direction);
        assertEquals(1184.0,get(r,"Nuvama").target,0.01);
    }
    @Test public void groupOfBrokersDoesNotMixPriceLevels(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Hindalco shares: brokerages stick to Buy",
          "Elara Securities and Axis Securities reiterated their Buy ratings on Hindalco.",
          "Business Today","HINDALCO","Hindalco Industries");
        assertEquals(2,r.size());
        assertNull(get(r,"Elara Securities").target);
        assertNull(get(r,"Axis Securities").target);
    }
    @Test public void sellOpinionMustNotDisappear(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Jefferies downgrades Hindalco to Sell, target price Rs 780",
          "Jefferies downgraded Hindalco to a sell recommendation and reduced its target price to Rs 780.",
          "Investing.com","HINDALCO","Hindalco Industries");
        assertEquals("SELL",get(r,"Jefferies").direction);
        assertEquals(780.0,get(r,"Jefferies").target,0.01);
    }
    @Test public void currentPriceIsNotAnEntry(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "UBS maintains Buy on Hindalco",
          "UBS reiterated Buy on Hindalco with CMP Rs 960, fair value Rs 1,200.",
          "Moneycontrol","HINDALCO","Hindalco Industries");
        assertEquals(1200.0,get(r,"UBS").target,0.01);
        assertNull(get(r,"UBS").entry);
        assertNull(get(r,"UBS").stop);
    }
    @Test public void unrelatedStockDoesNotAcquireRating(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Brokerages discuss Hindalco",
          "UBS reiterates Buy on Britannia with a target of Rs 5,800. "+
          "Hindalco shares traded flat.",
          "Financial News","HINDALCO","Hindalco Industries");
        assertTrue(r.isEmpty());
    }
    @Test public void genericPublisherNotCountedAsResearchBroker(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Hindalco shares slip after Q2 earnings",
          "Stocks to buy today: Hindalco, Britannia and ICICI Bank.",
          "Moneycontrol","HINDALCO","Hindalco Industries");
        assertTrue(r.isEmpty());
    }
    @Test public void bylinedKotakResearchExtractsFairValueNotEntry(){
        List<BrokerageCallExtractor.Call> calls=BrokerageCallExtractor.extract(
          "Hindalco, Airtel among 2 stock recommendations by Kotak's Shrikant Chouhan",
          "Hindalco Industries Ltd (HNDL) – BUY CMP – 960 FV – 1,200. "+
          "Bharti Airtel – BUY CMP – 2100 FV – 2400.",
          "Business Standard","HINDALCO","Hindalco Industries");
        assertNotNull(get(calls,"Shrikant Chouhan"));
        assertEquals("BUY",get(calls,"Shrikant Chouhan").direction);
        assertEquals(1200.0,get(calls,"Shrikant Chouhan").target,0.01);
        assertNull(get(calls,"Shrikant Chouhan").entry);
    }
    @Test public void contradictoryHeadlineAndExcerptTargetIsWithheld(){
        // Real-world class of issue: headline says target 1204 while body says 1200.
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Buy Hindalco Industries: target of Rs 1204: ICICI Securities",
          "ICICI Securities recommended BUY on Hindalco Industries with a target price of Rs 1,200.",
          "Moneycontrol","HINDALCO","Hindalco Industries");
        assertNotNull(get(r,"ICICI"));
        assertEquals("BUY",get(r,"ICICI").direction);
        assertTrue(get(r,"ICICI").conflictingTarget);
        assertNull(get(r,"ICICI").target);
    }
    @Test public void historicalAndRevisedTargetsKeepNewTarget(){
        List<BrokerageCallExtractor.Call> r=BrokerageCallExtractor.extract(
          "Hindalco shares rise after results",
          "Axis Securities maintained its Buy rating on Hindalco but reduced target price to Rs 1,060 from Rs 1,155.",
          "Business Today","HINDALCO","Hindalco Industries");
        assertEquals(1060.0,get(r,"Axis Securities").target,0.01);
    }
}