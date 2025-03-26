package strategy.impl;

import java.util.List;
import java.util.Map;
import domain.StockPrice;
import strategy.Strategy;

public class VolatilityAwareStrategy extends Strategy {

    private static final String SPY = "SPY";
    private static final double ATR_SELL_THRESHOLD = 0.05;

    public VolatilityAwareStrategy(List<String> watchlist) {
        super(watchlist);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(SPY);
        double price = sp.getClose();

        if (sp.getSma200() == null || sp.getRsi14() == null || sp.getVolatility() == null) return;

        double atrPercent = sp.getVolatility() / price;
        double rsi = sp.getRsi14();

        boolean trendUp = price > sp.getSma200();
        boolean rsiOkay = rsi > 50;

        if (hasPosition(SPY)) {
            if (!trendUp || atrPercent > ATR_SELL_THRESHOLD) {
                int qty = getPositionQty(SPY);
                reducePosition(SPY, qty, price);
                sp.addTrace(SPY, -qty, price, cash);
            }
        } else {
            if (trendUp && rsiOkay) {
                int qty = maxQuantity(price);
                addPosition(SPY, qty, price);
                sp.addTrace(SPY, qty, price, cash);
            }
        }
    }
}
