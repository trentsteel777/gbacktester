package strategy.impl;

import java.util.List;
import java.util.Map;
import domain.StockPrice;
import strategy.Strategy;

public class SmaVolatilityFilterStrategy extends Strategy {

    private static final String SPY = "SPY";
    private static final double ATR_PERCENT_THRESHOLD = 0.02; // 2%

    public SmaVolatilityFilterStrategy(List<String> watchlist) {
        super(watchlist);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(SPY);
        double price = sp.getClose();

        if (sp.getSma200() == null || sp.getVolatility() == null) return;

        double atrPercent = sp.getVolatility() / price;
        boolean trendUp = price > sp.getSma200();
        boolean lowVolatility = atrPercent < ATR_PERCENT_THRESHOLD;

        if (hasPosition(SPY)) {
            if (!trendUp || !lowVolatility) {
                int qty = getPositionQty(SPY);
                reducePosition(SPY, qty, price);
                sp.addTrace(SPY, -qty, price, cash);
            }
        } else {
            if (trendUp && lowVolatility) {
                int qty = maxQuantity(price);
                addPosition(SPY, qty, price);
                sp.addTrace(SPY, qty, price, cash);
            }
        }
    }
}
