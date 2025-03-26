package strategy.impl;

import java.util.List;
import java.util.Map;
import domain.StockPrice;
import strategy.Strategy;

public class SmaTrendWithMomentumStrategy extends Strategy {

    private static final String SPY = "SPY";

    public SmaTrendWithMomentumStrategy(List<String> watchlist) {
        super(watchlist);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(SPY);
        double price = sp.getClose();

        boolean valid = sp.getSma200() != null && sp.getSma50() != null;

        if (!valid) return;

        boolean trendUp = price > sp.getSma200();
        boolean momentumConfirm = sp.getSma50() > sp.getSma200();

        if (hasPosition(SPY)) {
            if (!trendUp) {
                int qty = getPositionQty(SPY);
                reducePosition(SPY, qty, price);
                sp.addTrace(SPY, -qty, price, cash);
            }
        } else {
            if (trendUp && momentumConfirm) {
                int qty = maxQuantity(price);
                addPosition(SPY, qty, price);
                sp.addTrace(SPY, qty, price, cash);
            }
        }
    }
}
