package strategy.impl;

import java.util.List;
import java.util.Map;
import domain.StockPrice;
import strategy.Strategy;

public class CombinedLooseStrategy extends Strategy {

    private static final String SPY = "SPY";
    private static final double ATR_BUY_THRESHOLD = 0.04; // 4%
    private static final double ATR_SELL_THRESHOLD = 0.06; // 6%

    public CombinedLooseStrategy(List<String> watchlist) {
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
        boolean rsiBuy = rsi > 40;
        boolean rsiSell = rsi < 35;
        boolean atrBuy = atrPercent < ATR_BUY_THRESHOLD;
        boolean atrSell = atrPercent > ATR_SELL_THRESHOLD;

        if (hasPosition(SPY)) {
            if (!trendUp || rsiSell || atrSell) {
                int qty = getPositionQty(SPY);
                reducePosition(SPY, qty, price);
                sp.addTrace(SPY, -qty, price, cash);
            }
        } else {
            if (trendUp && rsiBuy && atrBuy) {
                int qty = maxQuantity(price);
                addPosition(SPY, qty, price);
                sp.addTrace(SPY, qty, price, cash);
            }
        }
    }
}
