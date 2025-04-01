package strategy.impl;

import java.util.Map;
import java.util.Objects;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class Sma50_200TrailingStopStrategy extends Strategy {

    // For a 20% trailing stop, set stopPct = 0.20
    private static final double stopPct = 0.10;

    // Track the highest close since we opened a position
    private double highestCloseSinceOpen = 0.0;

    public Sma50_200TrailingStopStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        if (sp == null) {
            return;
        }

        // Make sure sma50 and sma200 are not null
        if (Objects.isNull(sp.getSma50()) || Objects.isNull(sp.getSma200())) {
            return;
        }

        if (hasPosition(symbol)) {
            // Update the highest close
            double currentClose = sp.getClose();
            if (currentClose > highestCloseSinceOpen) {
                highestCloseSinceOpen = currentClose;
            }

            // Check trailing stop
            double stopPrice = highestCloseSinceOpen * (1.0 - stopPct);
            if (currentClose < stopPrice) {
                // Sell everything
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
                highestCloseSinceOpen = 0.0;  // reset for next time
            }

        } else {
            // If we have no position and 50-day is above 200-day, buy
            if (isBullishCross(sp)) {
                double price = sp.getClose();
                int qty = maxQuantity(price);
                if (qty > 0) {
                    addPosition(symbol, qty, sp);
                    // Initialize trailing stop reference
                    highestCloseSinceOpen = sp.getClose();
                }
            }
        }
    }

    private boolean isBullishCross(StockPrice sp) {
        return sp.getSma50() > sp.getSma200();
    }
}
