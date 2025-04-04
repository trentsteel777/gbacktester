package gbacktester.strategy.impl;

import java.util.Map;
import java.util.Objects;

import gbacktester.domain.StockPrice;
import gbacktester.strategy.Strategy;
import gbacktester.strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class PhilTownLenientStrategy extends Strategy {

    /**
     * Percent drop from highest close at which to trigger a stop-out, 
     * e.g. 0.80 means if price falls 20% from its highest point, exit.
     */
    private static final double TRAILING_STOP_FACTOR = 0.80;
    
    /**
     * Track each symbol’s highest close since the position opened,
     * so we know where to place the trailing stop.
     */
    private double highestCloseSinceOpen = 0.0;

    public PhilTownLenientStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        if (sp == null) {
            return; // No data for this symbol on this day
        }

        // Ensure the essential indicators are not null:
        // (sma10, sma30, macdLine, macdSignalLine, stochasticK, stochasticD)
        if (anyIndicatorMissing(sp)) {
            return; 
        }

        // Check our 2-of-3 bullish condition
        boolean bullish = isBullish2of3(sp);

        if (hasPosition(symbol)) {

            // 1) Update the "highest close" as price moves up
            double currentClose = sp.getClose();
            if (currentClose > highestCloseSinceOpen) {
                highestCloseSinceOpen = currentClose;
            }

            // 2) Check trailing stop
            double stopPrice = highestCloseSinceOpen * TRAILING_STOP_FACTOR;
            if (currentClose < stopPrice) {
                // Price has fallen 20% (example) from highest peak => exit
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
                highestCloseSinceOpen = 0.0; // reset for next time
                return;
            }

            // If you also want to exit when signals are *really* bad, you could do:
            // if (!bullish) { ... } 
            // but we omit that here, letting the trailing stop handle exits.

        } else {
            // No position => consider opening if "2-of-3" signals are bullish
            if (bullish) {
                double price = sp.getClose();
                int qty = maxQuantity(price);
                if (qty > 0) {
                    addPosition(symbol, qty, sp);

                    // Initialize the trailing stop reference
                    highestCloseSinceOpen = price;
                }
            }
        }
    }

    /**
     * Returns true if AT LEAST 2 of Phil Town's "3 Tools" are bullish:
     * 1) sma10 > sma30
     * 2) macdLine > macdSignalLine
     * 3) stochasticK > stochasticD
     * 
     * This is more lenient than requiring all 3.
     */
    private boolean isBullish2of3(StockPrice sp) {
        int bullishCount = 0;

        if (sp.getSma10() > sp.getSma30()) {
            bullishCount++;
        }
        if (sp.getMacdLine() > sp.getMacdSignalLine()) {
            bullishCount++;
        }
        if (sp.getStochasticK() > sp.getStochasticD()) {
            bullishCount++;
        }

        // Return true if 2 or more are bullish
        return (bullishCount >= 2);
    }

    /**
     * If ANY required indicator is missing, skip logic for the day.
     */
    private boolean anyIndicatorMissing(StockPrice sp) {
        return Objects.isNull(sp.getSma10())
            || Objects.isNull(sp.getSma30())
            || Objects.isNull(sp.getMacdLine())
            || Objects.isNull(sp.getMacdSignalLine())
            || Objects.isNull(sp.getStochasticK())
            || Objects.isNull(sp.getStochasticD());
    }
}
