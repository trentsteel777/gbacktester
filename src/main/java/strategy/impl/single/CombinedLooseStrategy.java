package strategy.impl.single;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class CombinedLooseStrategy extends Strategy {
    
    private static final double ATR_BUY_THRESHOLD = 0.04; // 4%
    private static final double ATR_SELL_THRESHOLD = 0.06; // 6%

    public CombinedLooseStrategy(String symbol) {
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
        boolean rsiBuy = rsi > 40;
        boolean rsiSell = rsi < 35;
        boolean atrBuy = atrPercent < ATR_BUY_THRESHOLD;
        boolean atrSell = atrPercent > ATR_SELL_THRESHOLD;

        if (hasPosition(symbol)) {
            if (!trendUp || rsiSell || atrSell) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
            }
        } else {
            if (trendUp && rsiBuy && atrBuy) {
                int qty = maxQuantity(price);
                if(qty > 0) {
                	addPosition(symbol, qty, sp);
                }
            }
        }
    }
}
