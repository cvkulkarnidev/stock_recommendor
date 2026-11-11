package com.localstocks.intelligence;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeepQueriesTest {
    @Test public void expandsAcrossAllRequestedSourceCategories(){
        List<DeepQueries.Query> q=DeepQueries.build("HDFCBANK","HDFC Bank");
        Set<String> groups=new HashSet<>(),keys=new HashSet<>();
        for(DeepQueries.Query item:q){
            groups.add(item.channel);assertTrue(item.text.contains("HDFC Bank")||item.text.contains("HDFCBANK"));
            assertTrue(keys.add(item.provider+":"+item.text));
        }
        assertTrue("Should expand beyond 25 queries",q.size()>=50);
        assertTrue(groups.contains("BROKERAGES"));assertTrue(groups.contains("NEWS"));
        assertTrue(groups.contains("ANALYSTS"));assertTrue(groups.contains("REDDIT"));
        assertTrue(groups.contains("X_INDEX"));
        Set<String> siteTargets=new HashSet<>();
        java.util.regex.Pattern pattern=java.util.regex.Pattern.compile("site:([a-z0-9.-]+)");
        for(DeepQueries.Query item:q){
            java.util.regex.Matcher matcher=pattern.matcher(item.text);
            while(matcher.find())siteTargets.add(matcher.group(1));
        }
        assertTrue("Should target 100+ distinct sites",siteTargets.size()>=100);
    }
    @Test public void invalidTickerIsRejected(){
        try{DeepQueries.build("../secret","Test");fail("Must reject path chars");}
        catch(IllegalArgumentException expected){}
    }
    @Test public void nameIsSanitizedAndLengthLimited(){
        List<DeepQueries.Query> q=DeepQueries.build("TCS","Tata Consultancy Services<script>");
        assertFalse(q.toString().contains("<script>"));
        for(DeepQueries.Query item:q){
            assertFalse(item.text.contains("<"));assertFalse(item.text.contains(">"));
        }
    }
    @Test public void differentStockGetsDifferentSearch(){
        assertNotEquals(DeepQueries.build("INFY","Infosys").get(0).text,
            DeepQueries.build("RELIANCE","Reliance Industries").get(0).text);
    }
}
