package gbacktester.strategy.impl.single;

import java.util.Map;

import gbacktester.domain.StockPrice;
import gbacktester.strategy.Strategy;
import gbacktester.strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class SmaTrendWithMomentumStrategy extends Strategy {

    public SmaTrendWithMomentumStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        boolean valid = sp.getSma200() != null && sp.getSma50() != null;

        if (!valid) return;

        boolean trendUp = price > sp.getSma200();
        boolean momentumConfirm = sp.getSma50() > sp.getSma200();

        if (hasPosition(symbol)) {
            if (!trendUp) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            if (trendUp && momentumConfirm) {
                int qty = maxQuantity(price);
                if(qty > 0) {
                	addPosition(symbol, qty, sp);
                }
            }
        }
    }
}
