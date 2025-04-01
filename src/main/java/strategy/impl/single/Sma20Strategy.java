package strategy.impl.single;

import java.util.Map;
import java.util.Objects;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class Sma20Strategy extends Strategy {
	
    public Sma20Strategy(String symbol) {
        super(symbol);
    }
    
	@Override
	public void run(Map<String, StockPrice> marketData) {
		StockPrice sp = marketData.get(symbol);
		double price = sp.getClose();
		
		if(hasPosition(symbol)) {
			if(isSmaDip(sp)) {
				int qty = getPositionQty(symbol);
				reducePosition(symbol, qty, sp);
			}
		}
		else {
			if(isSmaBreach(sp)) {
				int qty = maxQuantity(price);
				if(qty > 0) {
					addPosition(symbol, qty, sp);
				}
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
