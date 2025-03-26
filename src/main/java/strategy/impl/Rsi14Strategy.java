package strategy.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import domain.StockPrice;
import strategy.Strategy;

public class Rsi14Strategy extends Strategy {

	private static final String SPY = "SPY";
	
	private double stopLoss = Double.MAX_VALUE;
	private double lastSalePrice = Double.MIN_VALUE;
	
    public Rsi14Strategy(List<String> watchlist) {
        super(watchlist);
    }
    
	@Override
	public void run(Map<String, StockPrice> marketData) {
		StockPrice spyStockPrice = marketData.get(SPY);
		double price = spyStockPrice.getClose();
		
		if(hasPosition(SPY)) {
			if(price < stopLoss) {
				int qty = getPositionQty(SPY);
				reducePosition(SPY, qty, price);
				lastSalePrice = price;
				spyStockPrice.addTrace(SPY, -qty, price, cash, stopLoss);
			}
			else {
				double trailingStopLoss = price * 0.9;
				if (stopLoss > 0 && price >= stopLoss * 1.10 && trailingStopLoss > stopLoss) {
					stopLoss = trailingStopLoss;
				}
			}
		}
		else {
			if(price > lastSalePrice || isRsiDip(spyStockPrice)) {
				int qty = maxQuantity(price);
				addPosition(SPY, qty, price);
				stopLoss = price * 0.97;
				spyStockPrice.addTrace(SPY, qty, price, cash);
			}
		}
	}
	
	public boolean isRsiDip(StockPrice spyStockPrice) {
		return Objects.nonNull(spyStockPrice.getRsi14()) && spyStockPrice.getRsi14() < 30;
	}

}
