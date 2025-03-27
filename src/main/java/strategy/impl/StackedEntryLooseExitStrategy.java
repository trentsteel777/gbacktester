package strategy.impl;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class StackedEntryLooseExitStrategy extends Strategy {

    private static final double ATR_EXIT_THRESHOLD = 0.06;

    public StackedEntryLooseExitStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        if (sp.getSma200() == null || sp.getRsi14() == null || sp.getVolatility() == null) return;

        double atrPercent = sp.getVolatility() / price;
        double rsi = sp.getRsi14();

        boolean strongTrend = price > sp.getSma200();
        boolean momentum = rsi > 50;
        boolean lowVolatility = atrPercent < 0.03;

        if (hasPosition(symbol)) {
            // Only exit on major trend break or big spike in volatility
            if (!strongTrend || atrPercent > ATR_EXIT_THRESHOLD) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            if (strongTrend && momentum && lowVolatility) {
                int qty = maxQuantity(price);
                addPosition(symbol, qty, sp);
            }
        }
    }
}
