package strategy.impl;

import java.util.List;
import java.util.Map;

import domain.StockPrice;
import strategy.Strategy;

public class BuyHoldStrategy extends Strategy {

	private static final String SPY = "SPY";
	
    public BuyHoldStrategy(List<String> watchlist) {
        super(watchlist);
    }
	
	@Override
	public void run(Map<String, StockPrice> marketData) {
		if(!hasPosition(SPY)) {
			StockPrice spyStockPrice = marketData.get(SPY);
			double price = spyStockPrice.getClose();
			int qty = maxQuantity(price);
			addPosition(SPY, qty, price);
			spyStockPrice.addTrace(SPY, qty, price, cash);
		}
	}

}
