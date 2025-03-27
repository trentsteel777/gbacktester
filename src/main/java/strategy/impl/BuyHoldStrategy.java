package strategy.impl;

import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;
@AutoLoadStrategy
public class BuyHoldStrategy extends Strategy {

    public BuyHoldStrategy(String symbol) {
        super(symbol);
    }
	
	@Override
	public void run(Map<String, StockPrice> marketData) {
		if(!hasPosition(symbol)) {
			StockPrice spyStockPrice = marketData.get(symbol);
			double price = spyStockPrice.getClose();
			int qty = maxQuantity(price);
			addPosition(symbol, qty, spyStockPrice);
		}
	}

}
