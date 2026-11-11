package com.localstocks.intelligence;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Adversarial regression corpus: explicit Indian price typography, reversed
 * labels, negations, invalid geometry, dates, percentages, and mixed sources.
 */
public class TradeLevelsTest {
    private static void eq(Double actual,double expected){
        assertNotNull("Expected an explicitly labelled price",actual);
        assertEquals(expected,actual,0.001);
    }
    @Test public void fullBuyWithIndianCommas(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Broker recommends buy HDFC Bank at Rs 1,250, target price of Rs 1,420, stop-loss Rs 1,180","BUY");
        eq(x.entry,1250);eq(x.target,1420);eq(x.stop,1180);assertTrue(x.complete());
    }
    @Test public void fullSellWithRupeeSign(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Sell Infosys at ₹1,550 with target ₹1,420 and stop-loss ₹1,605","SELL");
        eq(x.entry,1550);eq(x.target,1420);eq(x.stop,1605);
    }
    @Test public void compactLabels(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Buy TCS @ ₹3450; TGT ₹3700; SL ₹3350","BUY");
        eq(x.entry,3450);eq(x.target,3700);eq(x.stop,3350);
    }
    @Test public void uppercaseInrAndDecimals(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Buy Wipro at INR 245.50, target INR 265.25, stop loss INR 239.50","BUY");
        eq(x.entry,245.50);eq(x.target,265.25);eq(x.stop,239.50);
    }
    @Test public void entryTargetStopWithPlainNumbers(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Buy HDFC Bank at 1250, target price 1420, stoploss 1180","BUY");
        eq(x.entry,1250);eq(x.target,1420);eq(x.stop,1180);
    }
    @Test public void reverseTargetAndStopLabels(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Buy Reliance Industries at ₹1250; ₹1430 target; ₹1200 stop-loss","BUY");
        eq(x.entry,1250);eq(x.target,1430);eq(x.stop,1200);
    }
    @Test public void targetWithoutEntryIsAllowedButIncomplete(){
        TradeLevels.Levels x=TradeLevels.extract("Buy Titan target ₹3500","BUY");
        assertNull(x.entry);eq(x.target,3500);assertNull(x.stop);assertFalse(x.complete());
    }
    @Test public void rejectsContradictoryBuy(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Buy TCS at Rs 3500 target Rs 3000 stop-loss Rs 3600","BUY");
        eq(x.entry,3500);assertNull(x.target);assertNull(x.stop);
    }
    @Test public void rejectsContradictorySell(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Sell Infosys at ₹1550 target ₹1650 stop-loss ₹1450","SELL");
        eq(x.entry,1550);assertNull(x.target);assertNull(x.stop);
    }
    @Test public void ignoresUnlabelledYearAndPercentages(){
        assertFalse(TradeLevels.extract("Buy Reliance for 2026 with 20% upside","BUY").hasAny());
        assertFalse(TradeLevels.extract("Stop loss unknown and target year 2027","BUY").hasAny());
        assertFalse(TradeLevels.extract("Buy TCS, 15% target expected","BUY").hasAny());
    }
    @Test public void neverInventFromQuestionOrRumor(){
        assertFalse(TradeLevels.extract("Should you buy Infosys at ₹1300 target ₹1500?","BUY").hasAny());
        assertFalse(TradeLevels.extract("Rumour: Buy TCS at ₹3400 target ₹3600","BUY").hasAny());
        assertFalse(TradeLevels.extract("Illustrative buy HDFC Bank at ₹1500","BUY").hasAny());
    }
    @Test public void unresolvedDirectionDoesNotExtract(){
        assertFalse(TradeLevels.extract("Buy at ₹100 target ₹120 stop ₹90","WATCH").hasAny());
        assertFalse(TradeLevels.extract("Buy at ₹100 target ₹120 stop ₹90","MIXED").hasAny());
    }
    @Test public void entryIsNotStolenFromTargetOnly(){
        TradeLevels.Levels x=TradeLevels.extract("Buy Titan; target ₹3500, stop-loss ₹3300","BUY");
        assertNull(x.entry);eq(x.target,3500);eq(x.stop,3300);
    }
    @Test public void nearAndAroundPhrases(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Accumulation suggestion: Buy Maruti Suzuki near ₹12000, target ₹13000, SL ₹11600","BUY");
        eq(x.entry,12000);eq(x.target,13000);eq(x.stop,11600);
    }
    @Test public void noFabricatedStopWhenOnlyTarget(){
        TradeLevels.Levels x=TradeLevels.extract("Sell Infosys at Rs 1500 target Rs 1420","SELL");
        eq(x.entry,1500);eq(x.target,1420);assertNull(x.stop);
    }
    @Test public void punctuationAndWhitespace(){
        TradeLevels.Levels x=TradeLevels.extract(
          "Buy   HDFC Bank at   ₹1,234.50; target: ₹1,350; stop-loss: ₹1,190","BUY");
        eq(x.entry,1234.50);eq(x.target,1350);eq(x.stop,1190);
    }
    @Test public void rejectingInvalidCurrencyAndUnrelatedNumbers(){
        TradeLevels.Levels x=TradeLevels.extract("Buy Titan during FY2026; 25% growth expected","BUY");
        assertFalse(x.hasAny());
        TradeLevels.Levels y=TradeLevels.extract("Buy Titan, stop-loss at 5% from current price","BUY");
        assertFalse(y.hasAny());
    }
}
