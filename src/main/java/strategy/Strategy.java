package strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import domain.Position;
import domain.StockPrice;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
public abstract class Strategy {

	protected double cash;
	private int buyCount = 0;
	private int sellCount = 0;

    private double totalPortfolioValue;
    
    protected List<String> watchlist;
    protected List<Position> positions;
    
    public Strategy(List<String> watchlist) {
    	this.watchlist = watchlist;
        this.cash = 100_000;
        this.totalPortfolioValue = cash;
        this.positions = new ArrayList<>(20000);
    }

    public abstract void run(Map<String, StockPrice> marketData);
    
    protected boolean hasCash(int qty, double purchasePrice) {
    	return cash > qty * purchasePrice;
    }
    
    protected int maxQuantity(double price) {
    	return (int) Math.floor(cash / price);
    }
    protected boolean hasPosition(String symbol) {
    	return Objects.nonNull(getPosition(symbol));
    }
    
    protected int getPositionQty(String symbol) {
		return positions.stream()
		         .filter(p -> p.getSymbol().equals(symbol))
		         .findFirst()
		         .get()
		         .getQty();
	}
    
    protected Position getPosition(String symbol) {
		return positions.stream()
		         .filter(p -> p.getSymbol().equals(symbol))
		         .findFirst()
		         .orElse(null);
	}
    
    protected void addPosition(String symbol, int qty, double purchasePrice) {
    	double cost = qty * purchasePrice;
    	if(cost > cash) {
    		throw new IllegalStateException(String.format(
    			    "Not enough cash to buy %d of %s at $%.2f each (cost = $%.2f, cash = $%.2f)",
    			    qty, symbol, purchasePrice, cost, cash
    			));
    	}
    	
		Position position = getPosition(symbol);
		if(position == null) {
			position = new Position(symbol);
			positions.add(position);
		}
		position.addQuantity(qty, purchasePrice);
		
		cash -= cost;
		buyCount += 1;
		
	}
    
    protected void reducePosition(String symbol, int qty, double marketPrice) {
    	Position position = getPosition(symbol);

    	if (position == null) {
    	    throw new IllegalStateException("Cannot reduce position: " + symbol + " not held.");
    	}

    	position.reduceQuantity(qty);
    	if(position.getQty() <= 0) {
    		positions.remove(position);
    	}
    	double saleAmount = qty * marketPrice;
    	cash += saleAmount;
    	
    	sellCount += 1;
	}
    
	
	public void calculateTotalPortfolioValue(Map<String, Double> marketPrices) {
	    double total = cash;

	    for (Position position : positions) {
	        String symbol = position.getSymbol();
	        Double currentPrice = marketPrices.get(symbol);

	        if (currentPrice == null) {
	            log.warn("No market price for symbol: {}", symbol);
	            continue;
	        }

	        total += position.getQty() * currentPrice;
	    }

	    this.totalPortfolioValue = total;
	}
	
	public int getTotalCount() {
		return buyCount + sellCount;
	}
	
	
}
