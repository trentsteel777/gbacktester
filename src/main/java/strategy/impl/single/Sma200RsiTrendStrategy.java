package strategy.impl.single;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class Sma200RsiTrendStrategy extends Strategy {

    public Sma200RsiTrendStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        boolean rsiAbove50 = sp.getRsi14() != null && sp.getRsi14() > 50;
        boolean rsiBelow40 = sp.getRsi14() != null && sp.getRsi14() < 40;
        boolean priceAboveSma200 = sp.getSma200() != null && price > sp.getSma200();
        boolean priceBelowSma200 = sp.getSma200() != null && price < sp.getSma200();

        if (hasPosition(symbol)) {
            if (priceBelowSma200 || rsiBelow40) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            if (priceAboveSma200 && rsiAbove50) {
                int qty = maxQuantity(price);
                if(qty > 0) {
                	addPosition(symbol, qty, sp);
                }
            }
        }
    }
}
