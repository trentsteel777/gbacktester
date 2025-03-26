package strategy.impl;

import java.util.List;
import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;

public class TrendConfirmationStrategy extends Strategy {

    private static final String SPY = "SPY";

    public TrendConfirmationStrategy(List<String> watchlist) {
        super(watchlist);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(SPY);
        double price = sp.getClose();

        if (sp.getSma200() == null || sp.getSma50() == null || sp.getRsi14() == null) return;

        boolean isTrending = price > sp.getSma200();
        boolean isMomentumUp = sp.getSma50() > sp.getSma200();
        boolean isRsiPositive = sp.getRsi14() > 50;
        boolean isRsiWeak = sp.getRsi14() < 40;

        if (hasPosition(SPY)) {
            if (!isTrending || isRsiWeak) {
                int qty = getPositionQty(SPY);
                reducePosition(SPY, qty, price);
                sp.addTrace(SPY, -qty, price, cash);
            }
        } else {
            if (isTrending && isMomentumUp && isRsiPositive) {
                int qty = maxQuantity(price);
                addPosition(SPY, qty, price);
                sp.addTrace(SPY, qty, price, cash);
            }
        }
    }
}
