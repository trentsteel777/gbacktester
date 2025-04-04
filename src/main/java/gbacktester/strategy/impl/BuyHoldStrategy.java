package gbacktester.strategy.impl;

import java.util.Map;

import gbacktester.domain.StockPrice;
import gbacktester.strategy.Strategy;
import gbacktester.strategy.annotations.AutoLoadStrategy;
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
			if (qty > 0) {
				addPosition(symbol, qty, spyStockPrice);
			}
		}
	}

}
