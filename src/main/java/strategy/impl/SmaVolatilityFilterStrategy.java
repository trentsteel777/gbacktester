package strategy.impl;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class SmaVolatilityFilterStrategy extends Strategy {
    
    private static final double ATR_PERCENT_THRESHOLD = 0.02; // 2%

    public SmaVolatilityFilterStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        if (sp.getSma200() == null || sp.getVolatility() == null) return;

        double atrPercent = sp.getVolatility() / price;
        boolean trendUp = price > sp.getSma200();
        boolean lowVolatility = atrPercent < ATR_PERCENT_THRESHOLD;

        if (hasPosition(symbol)) {
            if (!trendUp || !lowVolatility) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            if (trendUp && lowVolatility) {
                int qty = maxQuantity(price);
                addPosition(symbol, qty, sp);
            }
        }
    }
}
