package gbacktester.strategy.impl;

import java.util.Map;
import java.util.Objects;

import gbacktester.domain.StockPrice;
import gbacktester.strategy.Strategy;
import gbacktester.strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class Sma50_200TrailingStopAlternativeStrategy extends Strategy {

    public Sma50_200TrailingStopAlternativeStrategy(String symbol) {
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
            double currentClose = sp.getClose();
            // or you could do "currentClose < (sp.getSma50() * 0.98)" for a 2% buffer
            //if (currentClose < sp.getSma50()) {
            if (currentClose < (sp.getSma50() * 0.97)) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }

        } else {
            // If we have no position and 50-day is above 200-day, buy
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
        return sp.getSma50() > sp.getSma200();
    }
}
