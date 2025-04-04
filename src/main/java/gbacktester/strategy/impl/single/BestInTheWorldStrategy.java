package gbacktester.strategy.impl.single;
import java.util.Map;

import gbacktester.domain.StockPrice;
import gbacktester.strategy.Strategy;
import gbacktester.strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class BestInTheWorldStrategy extends Strategy {

    // ATR thresholds
    private static final double ATR_ENTER_THRESHOLD = 0.05; // 5% 
    private static final double ATR_EXIT_THRESHOLD = 0.10;  // 10%

    // Wide trailing stop
    private static final double TRAILING_STOP = 0.30; // 30%

    // Track if we’re currently in symbol
    private boolean holdingSpy = false;
    // Track the highest price since last entry (for trailing stop)
    private double highestPrice = 0;

    public BestInTheWorldStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice spSpy = marketData.get(symbol);
        if (spSpy == null) return;

        double price = spSpy.getClose();
        // If no valid price or missing indicators, skip
        if (price <= 0 || spSpy.getSma150() == null || spSpy.getSma50() == null
            || spSpy.getRsi14() == null || spSpy.getVolatility() == null) {
            return;
        }

        // Gather signals
        double sma150 = spSpy.getSma150();
        double sma50  = spSpy.getSma50();
        double rsi    = spSpy.getRsi14();
        double atrPct = spSpy.getVolatility() / price;

        boolean priceAboveSma150  = price > sma150;
        boolean sma50AboveSma150  = sma50 > sma150;
        boolean rsiAbove50        = rsi > 50;  // or 55
        boolean atrBelowThreshold = atrPct < ATR_ENTER_THRESHOLD;

        // For entry, we count how many signals are "true"
        int signalCount = 0;
        if (priceAboveSma150)  signalCount++;
        if (sma50AboveSma150)  signalCount++;
        if (rsiAbove50)        signalCount++;
        if (atrBelowThreshold) signalCount++;

        if (holdingSpy) {
            // Update highest price
            if (price > highestPrice) {
                highestPrice = price;
            }
            // Check trailing stop
            double drawdown = (highestPrice - price) / highestPrice;
            boolean trailingStopHit = (drawdown >= TRAILING_STOP);

            // For exit, we can require that fewer than 2 signals are valid (strict condition),
            // OR the trailing stop is triggered,
            // OR ATR is extremely high (≥ ATR_EXIT_THRESHOLD).
            boolean notEnoughSignals = (signalCount < 2);
            boolean extremeVolatility = (atrPct >= ATR_EXIT_THRESHOLD);

            if (notEnoughSignals || trailingStopHit || extremeVolatility) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, spSpy);
                holdingSpy = false;
                highestPrice = 0;
            }

        } else {
            // Try to enter if we have at least 3 of 4 signals
            // That typically means strong trend + normal volatility
            boolean entrySignal = (signalCount >= 3);
            if (entrySignal) {
                int qty = maxQuantity(price);
                if (qty > 0) {
                    addPosition(symbol, qty, spSpy);
                    holdingSpy = true;
                    highestPrice = price;
                }
            }
        }
    }
}
