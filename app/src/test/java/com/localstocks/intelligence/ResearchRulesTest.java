package com.localstocks.intelligence;

import org.junit.Test;
import static org.junit.Assert.*;

public class ResearchRulesTest {
    @Test public void namedBrokerWeightedAboveUnknownPublisher() {
        ResearchRules.Source broker=ResearchRules.source("Motilal Oswal recommends buying HDFC Bank","Some publication");
        ResearchRules.Source other=ResearchRules.source("HDFC Bank recommended","Unknown Blog");
        assertEquals("NAMED_BROKER",broker.tier);
        assertTrue(broker.weight>other.weight);
    }
    @Test public void curatedNewsRecognized() {
        ResearchRules.Source editorial=ResearchRules.source("HDFC Bank outlook","Moneycontrol");
        assertEquals("CURATED_NEWS",editorial.tier);
    }
    @Test public void clearBuyNearStock() {
        assertEquals("BUY",ResearchRules.classify("Motilal Oswal recommends buy on HDFC Bank after technical breakout","HDFCBANK","HDFC Bank").direction);
    }
    @Test public void clearSellNearStock() {
        assertEquals("SELL",ResearchRules.classify("Broker downgrades Infosys to sell after quarterly results","INFY","Infosys").direction);
    }
    @Test public void genericStockMentionIsNotEnough() {
        assertEquals("WATCH",ResearchRules.classify("Stocks to buy today: Infosys earnings later this week","INFY","Infosys").direction);
    }
    @Test public void mixedAndQuestionsDoNotBecomeRecommendations() {
        assertEquals("WATCH",ResearchRules.classify("Should you buy or sell Reliance Industries?","RELIANCE","Reliance Industries").direction);
        assertEquals("MIXED",ResearchRules.classify("HDFC Bank: brokerage says buy, rival says sell","HDFCBANK","HDFC Bank").direction);
    }
    @Test public void newsPublisherIsNotAnAnalyst() {
        ResearchRules.Source editorial=ResearchRules.source("Reliance Industries gets coverage","Reuters");
        assertEquals("CURATED_NEWS",editorial.tier);
        assertTrue(editorial.weight<1.0);
    }
}
