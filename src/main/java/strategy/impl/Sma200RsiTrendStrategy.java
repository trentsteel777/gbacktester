package strategy.impl;

import java.util.List;
import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;

public class Sma200RsiTrendStrategy extends Strategy {

    private static final String SPY = "SPY";

    public Sma200RsiTrendStrategy(List<String> watchlist) {
        super(watchlist);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(SPY);
        double price = sp.getClose();

        boolean rsiAbove50 = sp.getRsi14() != null && sp.getRsi14() > 50;
        boolean rsiBelow40 = sp.getRsi14() != null && sp.getRsi14() < 40;
        boolean priceAboveSma200 = sp.getSma200() != null && price > sp.getSma200();
        boolean priceBelowSma200 = sp.getSma200() != null && price < sp.getSma200();

        if (hasPosition(SPY)) {
            if (priceBelowSma200 || rsiBelow40) {
                int qty = getPositionQty(SPY);
                reducePosition(SPY, qty, price);
                sp.addTrace(SPY, -qty, price, cash);
            }
        } else {
            if (priceAboveSma200 && rsiAbove50) {
                int qty = maxQuantity(price);
                addPosition(SPY, qty, price);
                sp.addTrace(SPY, qty, price, cash);
            }
        }
    }
}
