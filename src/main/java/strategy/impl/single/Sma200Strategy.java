package strategy.impl.single;

import java.util.Map;
import java.util.Objects;

import domain.StockPrice;
import strategy.Strategy;
import strategy.annotations.AutoLoadStrategy;

@AutoLoadStrategy
public class Sma200Strategy extends Strategy {
	
    public Sma200Strategy(String symbol) {
        super(symbol);
    }
    
	@Override
	public void run(Map<String, StockPrice> marketData) {
		StockPrice sp = marketData.get(symbol);
		double price = sp.getClose();
		
        if (Objects.isNull(sp.getSma200())) {
            return; 
        }
		
		if(hasPosition(symbol)) {
			if(isBearishCross(sp)) {
				int qty = getPositionQty(symbol);
				reducePosition(symbol, qty, sp);
			}
		}
		else {
			if(isBullishCross(sp)) {
				int qty = maxQuantity(price);
				if(qty > 0) {
					addPosition(symbol, qty, sp);
				}
			}
		}
	}
	
	private boolean isBearishCross(StockPrice sp) {
		return sp.getClose() < sp.getSma200();
	}
	
	private boolean isBullishCross(StockPrice sp) {
		return sp.getClose() > sp.getSma200();
	}

}
