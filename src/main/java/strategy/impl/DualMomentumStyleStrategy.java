package strategy.impl;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class DualMomentumStyleStrategy extends Strategy {

    public DualMomentumStyleStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        boolean hasSma50 = sp.getSma50() != null;
        boolean hasSma200 = sp.getSma200() != null;

        if (hasSma50 && hasSma200) {
            if (hasPosition(symbol)) {
                if (sp.getSma50() < sp.getSma200() || price < sp.getSma200()) {
                    int qty = getPositionQty(symbol);
                    reducePosition(symbol, qty, sp);
                }
            } else {
                if (sp.getSma50() > sp.getSma200() && price > sp.getSma200()) {
                    int qty = maxQuantity(price);
                    addPosition(symbol, qty, sp);
                }
            }
        }
    }
}
