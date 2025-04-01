package strategy.impl;

import java.util.Map;
import java.util.Objects;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class Sma50_200CrossoverStrategy extends Strategy {

    public Sma50_200CrossoverStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);

        // Make sure sma50 and sma200 are not null
        if (Objects.isNull(sp.getSma50()) || Objects.isNull(sp.getSma200())) {
            return; 
        }

        // If we already hold a position, decide whether to keep or close it.
        if (hasPosition(symbol)) {
            // If 50-day SMA dips back below 200-day SMA, close the position.
            if (isBearishCross(sp)) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            // If 50-day SMA is above 200-day SMA, open a position.
            if (isBullishCross(sp)) {
                double price = sp.getClose();
                int qty = maxQuantity(price);
                if (qty > 0) {
                    addPosition(symbol, qty, sp);
                }
            }
        }
    }

    private boolean isBullishCross(StockPrice sp) {
        if (sp.getSma50() == null || sp.getSma200() == null) {
            return false;
        }
        return sp.getSma50() > sp.getSma200();
    }

    private boolean isBearishCross(StockPrice sp) {
        if (sp.getSma50() == null || sp.getSma200() == null) {
            return false;
        }
        return sp.getSma50() < sp.getSma200();
    }
}
