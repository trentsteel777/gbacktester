package strategy.impl;

import java.util.Map;
import java.util.Objects;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class Rsi14Strategy extends Strategy {
	
	private double stopLoss = Double.MAX_VALUE;
	private double lastSalePrice = Double.MIN_VALUE;
	
    public Rsi14Strategy(String symbol) {
        super(symbol);
    }
    
	@Override
	public void run(Map<String, StockPrice> marketData) {
		StockPrice sp = marketData.get(symbol);
		double price = sp.getClose();
		
		if(hasPosition(symbol)) {
			if(price < stopLoss) {
				int qty = getPositionQty(symbol);
				reducePosition(symbol, qty, sp);
				lastSalePrice = price;
			}
			else {
				double trailingStopLoss = price * 0.9;
				if (stopLoss > 0 && price >= stopLoss * 1.10 && trailingStopLoss > stopLoss) {
					stopLoss = trailingStopLoss;
				}
			}
		}
		else {
			if(price > lastSalePrice || isRsiDip(sp)) {
				int qty = maxQuantity(price);
				if(qty > 0) {
					addPosition(symbol, qty, sp);
					stopLoss = price * 0.97;
				}
			}
		}
	}
	
	public boolean isRsiDip(StockPrice spyStockPrice) {
		return Objects.nonNull(spyStockPrice.getRsi14()) && spyStockPrice.getRsi14() < 30;
	}

}
