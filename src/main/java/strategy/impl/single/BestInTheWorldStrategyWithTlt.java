package strategy.impl.single;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class BestInTheWorldStrategyWithTlt extends Strategy {

    private static final String TLT = "TLT";

    // ATR thresholds
    private static final double ATR_ENTER_THRESHOLD = 0.05; // 5%
    private static final double ATR_EXIT_THRESHOLD = 0.10;  // 10%

    // Wide trailing stop
    private static final double TRAILING_STOP = 0.30; // 30%

    // Track which symbol we're currently holding (null if none)
    private String holdingSymbol = null;
    // Track highest price since last entry
    private double highestPrice = 0;

    public BestInTheWorldStrategyWithTlt(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice spSpy = marketData.get(symbol);
        StockPrice spTlt = marketData.get(TLT);

        // If symbol data is missing or invalid, skip
        if (spSpy == null || spSpy.getClose() <= 0 
            || spSpy.getSma150() == null || spSpy.getSma50() == null 
            || spSpy.getRsi14() == null || spSpy.getVolatility() == null) 
        {
            return;
        }

        // If TLT data is missing, we'll treat TLT's signal score as 0
        // (Could also skip TLT logic until TLT is valid.)
        int tltScore = 0;
        double tltPrice = 0, tltAtrPct = 0;
        if (spTlt != null && spTlt.getClose() > 0 
            && spTlt.getSma150() != null && spTlt.getSma50() != null
            && spTlt.getRsi14() != null && spTlt.getVolatility() != null) 
        {
            tltPrice = spTlt.getClose();
            tltAtrPct = spTlt.getVolatility() / tltPrice;
            tltScore = getSignalScore(spTlt, ATR_ENTER_THRESHOLD);
        }

        // Calculate symbol score
        double spyPrice   = spSpy.getClose();
        int spyScore      = getSignalScore(spSpy, ATR_ENTER_THRESHOLD);
        double spyAtrPct  = spSpy.getVolatility() / spyPrice;

        if (holdingSymbol == null) {
            // Currently in cash — decide if we buy symbol or TLT
            if (spyScore >= 3 || tltScore >= 3) {
                // If both pass, pick the higher score
                if (spyScore > tltScore) {
                    enterPosition(symbol, spyPrice, spSpy);
                } else if (tltScore > spyScore) {
                    enterPosition(TLT, tltPrice, spTlt);
                } else {
                    // Scores are equal — prefer symbol by default (up to you)
                    enterPosition(symbol, spyPrice, spSpy);
                }
            }
        } else {
            // We are holding something — either symbol or TLT
            if (holdingSymbol.equals(symbol)) {
                handlePosition(spSpy, symbol, spyPrice, spyScore, spyAtrPct);
                // After handlePosition, we might have sold
                // If we sold, check if TLT is better:
                if (holdingSymbol == null && tltScore >= 3) {
                    enterPosition(TLT, tltPrice, spTlt);
                }
            } else {
                // Holding TLT
                handlePosition(spTlt, TLT, tltPrice, tltScore, tltAtrPct);
                if (holdingSymbol == null && spyScore >= 3) {
                    enterPosition(symbol, spyPrice, spSpy);
                }
            }
        }
    }

    // Evaluate trailing stop & signals for the currently held asset
    private void handlePosition(StockPrice sp, String symbol, double price, int score, double atrPct) {
        if (price > highestPrice) {
            highestPrice = price;
        }
        double drawdown = (highestPrice - price) / highestPrice;
        boolean trailingStopHit = (drawdown >= TRAILING_STOP);

        // If fewer than 2 signals remain, or ATR% >= 10%, or trailing stop triggered => sell
        boolean exitSignal = (score < 2) || (atrPct >= ATR_EXIT_THRESHOLD) || trailingStopHit;

        if (exitSignal) {
            int qty = getPositionQty(symbol);
            reducePosition(symbol, qty, sp);
            holdingSymbol = null;
            highestPrice = 0;
        }
    }

    // Helper: open a position in symbol
    private void enterPosition(String symbol, double price, StockPrice sp) {
        int qty = maxQuantity(price);
        if (qty > 0) {
            addPosition(symbol, qty, sp);
            holdingSymbol = symbol;
            highestPrice = price;
        }
    }

    // Compute how many signals are "true" for given stock
    // Return an int in 0..4
    private int getSignalScore(StockPrice sp, double atrEnterThreshold) {
        int score = 0;

        double price = sp.getClose();
        double sma50 = sp.getSma50();
        double sma150 = sp.getSma150();
        double rsi = sp.getRsi14();
        double atrPct = sp.getVolatility() / price;

        // Condition 1: Price > SMA150
        if (price > sma150) score++;
        // Condition 2: SMA50 > SMA150
        if (sma50 > sma150) score++;
        // Condition 3: RSI > 50
        if (rsi > 50) score++;
        // Condition 4: ATR < threshold
        if (atrPct < atrEnterThreshold) score++;

        return score;
    }
}
