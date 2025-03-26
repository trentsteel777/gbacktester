package strategy.impl;

import java.util.List;
import java.util.Map;
import domain.StockPrice;
import strategy.Strategy;

public class BuyAndHoldWithTrailingStopStrategy extends Strategy {

    private static final String SPY = "SPY";
    private static final double TRAILING_STOP_PCT = 0.20;

    private boolean invested = false;
    private double highestPrice = 0;
    private double lastPeakPrice = 0;

    public BuyAndHoldWithTrailingStopStrategy(List<String> watchlist) {
        super(watchlist);
    }

    @Override
    public void run(Map<String, StockPrice> marketData) {
        StockPrice sp = marketData.get(SPY);
        double price = sp.getClose();

        if (invested) {
            // Track the highest price since entry
            if (price > highestPrice) {
                highestPrice = price;
            }

            double drawdown = (highestPrice - price) / highestPrice;
            if (drawdown >= TRAILING_STOP_PCT) {
                int qty = getPositionQty(SPY);
                reducePosition(SPY, qty, price);
                sp.addTrace(SPY, -qty, price, cash);
                invested = false;
                lastPeakPrice = highestPrice;
                highestPrice = 0;
            }

        } else {
            // Wait until price exceeds last peak to re-enter
            if (price > lastPeakPrice) {
                int qty = maxQuantity(price);
                addPosition(SPY, qty, price);
                sp.addTrace(SPY, qty, price, cash);
                invested = true;
                highestPrice = price;
            }
        }
    }
}
