package strategy.impl;

import java.util.List;
import java.util.Map;
import domain.StockPrice;
import strategy.Strategy;

public class DualMomentumStyleStrategy extends Strategy {

    private static final String SPY = "SPY";

    public DualMomentumStyleStrategy(List<String> watchlist) {
        super(watchlist);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(SPY);
        double price = sp.getClose();

        boolean hasSma50 = sp.getSma50() != null;
        boolean hasSma200 = sp.getSma200() != null;

        if (hasSma50 && hasSma200) {
            if (hasPosition(SPY)) {
                if (sp.getSma50() < sp.getSma200() || price < sp.getSma200()) {
                    int qty = getPositionQty(SPY);
                    reducePosition(SPY, qty, price);
                    sp.addTrace(SPY, -qty, price, cash);
                }
            } else {
                if (sp.getSma50() > sp.getSma200() && price > sp.getSma200()) {
                    int qty = maxQuantity(price);
                    addPosition(SPY, qty, price);
                    sp.addTrace(SPY, qty, price, cash);
                }
            }
        }
    }
}
