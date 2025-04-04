package gbacktester.strategy.impl;

import java.util.Map;
import java.util.Objects;

import gbacktester.domain.StockPrice;
import gbacktester.strategy.Strategy;
import gbacktester.strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class PhilTownStrictStrategy extends Strategy {

    public PhilTownStrictStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        if (sp == null) {
            return; // No data for symbol
        }

        // Ensure the essential indicators are not null:
        // (sma10, sma30, macdLine, macdSignalLine, stochasticK, stochasticD)
        if (anyIndicatorMissing(sp)) {
            return; 
        }

        boolean bullish = isBullish(sp);

        if (hasPosition(symbol)) {
            // If we hold a position and the signals are no longer bullish, exit.
            if (!bullish) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            // If we do not have a position and all signals are bullish, enter.
            if (bullish) {
                double price = sp.getClose();
                int qty = maxQuantity(price);
                if (qty > 0) {
                    addPosition(symbol, qty, sp);
                }
            }
        }
    }

    /**
     * Returns true if ALL of Phil Town's "3 Tools" are bullish:
     * 1) sma10 > sma30
     * 2) macdLine > macdSignalLine
     * 3) stochasticK > stochasticD
     */
    private boolean isBullish(StockPrice sp) {
        // A basic triple condition check
        return sp.getSma10() > sp.getSma30()
            && sp.getMacdLine() > sp.getMacdSignalLine()
            && sp.getStochasticK() > sp.getStochasticD();
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
