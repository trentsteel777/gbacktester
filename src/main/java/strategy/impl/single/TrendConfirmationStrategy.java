package strategy.impl.single;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class TrendConfirmationStrategy extends Strategy {

    public TrendConfirmationStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        if (sp.getSma200() == null || sp.getSma50() == null || sp.getRsi14() == null) return;

        boolean isTrending = price > sp.getSma200();
        boolean isMomentumUp = sp.getSma50() > sp.getSma200();
        boolean isRsiPositive = sp.getRsi14() > 50;
        boolean isRsiWeak = sp.getRsi14() < 40;

        if (hasPosition(symbol)) {
            if (!isTrending || isRsiWeak) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            if (isTrending && isMomentumUp && isRsiPositive) {
                int qty = maxQuantity(price);
                if(qty > 0) {
                	addPosition(symbol, qty, sp);
                }
            }
        }
    }
}
