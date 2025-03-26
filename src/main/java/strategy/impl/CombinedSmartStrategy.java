package strategy.impl;

import java.util.List;
import java.util.Map;
import domain.StockPrice;
import strategy.Strategy;

public class CombinedSmartStrategy extends Strategy {

    private static final String SPY = "SPY";
    private static final double ATR_BUY_THRESHOLD = 0.02;
    private static final double ATR_SELL_THRESHOLD = 0.03;

    public CombinedSmartStrategy(List<String> watchlist) {
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
        boolean rsiBuy = rsi > 50;
        boolean rsiSell = rsi < 40;
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
