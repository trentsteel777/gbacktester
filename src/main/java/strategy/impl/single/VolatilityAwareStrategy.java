package strategy.impl.single;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class VolatilityAwareStrategy extends Strategy {
    
    private static final double ATR_SELL_THRESHOLD = 0.05;

    public VolatilityAwareStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        if (sp.getSma200() == null || sp.getRsi14() == null || sp.getVolatility() == null) return;

        double atrPercent = sp.getVolatility() / price;
        double rsi = sp.getRsi14();

        boolean trendUp = price > sp.getSma200();
        boolean rsiOkay = rsi > 50;

        if (hasPosition(symbol)) {
            if (!trendUp || atrPercent > ATR_SELL_THRESHOLD) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            if (trendUp && rsiOkay) {
                int qty = maxQuantity(price);
                if(qty > 0) {
                	addPosition(symbol, qty, sp);
                }
            }
        }
    }
}
