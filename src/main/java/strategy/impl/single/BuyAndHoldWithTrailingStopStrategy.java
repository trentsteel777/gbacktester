package strategy.impl.single;
import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class BuyAndHoldWithTrailingStopStrategy extends Strategy {
    
    private static final double TRAILING_STOP_PCT = 0.20;

    private boolean invested = false;
    private double highestPrice = 0;
    private double lastPeakPrice = 0;

    public BuyAndHoldWithTrailingStopStrategy(String symbol) {
        super(symbol);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(symbol);
        double price = sp.getClose();

        if (invested) {
            // Track the highest price since entry
            if (price > highestPrice) {
                highestPrice = price;
            }

            double drawdown = (highestPrice - price) / highestPrice;
            if (drawdown >= TRAILING_STOP_PCT) {
                int qty = getPositionQty(symbol);
                reducePosition(symbol, qty, sp);
                invested = false;
                lastPeakPrice = highestPrice;
                highestPrice = 0;
            }

        } else {
            // Wait until price exceeds last peak to re-enter
            if (price > lastPeakPrice) {
                int qty = maxQuantity(price);
                if (qty > 0) {
                	addPosition(symbol, qty, sp);
                	invested = true;
                	highestPrice = price;
                }
            }
        }
    }
}
