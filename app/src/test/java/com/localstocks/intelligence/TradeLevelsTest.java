package com.localstocks.intelligence;
import org.junit.Test;
import static org.junit.Assert.*;
public class TradeLevelsTest {
    @Test public void extractsCompleteBuyLevels(){
        TradeLevels.Levels x=TradeLevels.extract("Broker recommends buy HDFC Bank at Rs 1,250, target price of Rs 1,420, stop-loss Rs 1,180","BUY");
        assertEquals(1250.0,x.entry,0.001);
        assertEquals(1420.0,x.target,0.001);
        assertEquals(1180.0,x.stop,0.001);
    }
    @Test public void extractsSellLevels(){
        TradeLevels.Levels x=TradeLevels.extract("Sell Infosys at ₹1,550 with target ₹1,420 and stop-loss ₹1,605","SELL");
        assertEquals(1550.0,x.entry,0.001);
        assertEquals(1420.0,x.target,0.001);
        assertEquals(1605.0,x.stop,0.001);
    }
    @Test public void doesNotInventPrices(){
        TradeLevels.Levels x=TradeLevels.extract("Buy Reliance for 2026 with strong upside","BUY");
        assertFalse(x.hasAny());
        TradeLevels.Levels y=TradeLevels.extract("Target year 2027 and stop-loss unknown","BUY");
        assertFalse(y.hasAny());
    }
    @Test public void suppressesContradictoryLevels(){
        TradeLevels.Levels x=TradeLevels.extract("Buy TCS at Rs 3500 target Rs 3000 stop-loss Rs 3600","BUY");
        assertEquals(3500.0,x.entry,0.001);
        assertNull(x.target);assertNull(x.stop);
    }
    @Test public void uncertainDirectionHasNoLevels(){
        assertFalse(TradeLevels.extract("Buy at ₹100 target ₹120 stop ₹90","WATCH").hasAny());
    }
}