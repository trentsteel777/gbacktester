package strategy.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import domain.StockPrice;
import strategy.Strategy;

public class Sma20Strategy extends Strategy {

	private static final String SPY = "SPY";
	
    public Sma20Strategy(List<String> watchlist) {
        super(watchlist);
    }
    
	@Override
	public void run(Map<String, StockPrice> marketData) {
		StockPrice sp = marketData.get(SPY);
		double price = sp.getClose();
		
		if(hasPosition(SPY)) {
			if(isSmaDip(sp)) {
				int qty = getPositionQty(SPY);
				reducePosition(SPY, qty, price);
				sp.addTrace(SPY, -qty, price, cash);
			}
		}
		else {
			if(isSmaBreach(sp)) {
				int qty = maxQuantity(price);
				addPosition(SPY, qty, price);
				sp.addTrace(SPY, qty, price, cash);
			}
		}
	}
	
	public boolean isSmaDip(StockPrice spyStockPrice) {
		return Objects.nonNull(spyStockPrice.getSma20()) && spyStockPrice.getClose() < spyStockPrice.getSma20();
	}
	
	public boolean isSmaBreach(StockPrice spyStockPrice) {
		return Objects.nonNull(spyStockPrice.getSma20()) && spyStockPrice.getClose() > spyStockPrice.getSma20();
	}

}
