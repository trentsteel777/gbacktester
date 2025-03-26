package domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class StockPrice {
	private final String symbol;
    private final LocalDate date;
    private final double close;     // We use Adj Close here as the "true" close
    private final long volume;
    private Double rsi14;     // Nullable
    private Double sma20;
    
    private Double sma50;
    private Double sma200;
    private Double ema20;
    private Double ema50;
    private Double volatility; // e.g., ATR(14)
    
    private List<String> explainList = new ArrayList<>(10000);
    
    
    public void addTrace(String symbol, int qty, double price, double cash, double stopPrice) {
    	if(!this.symbol.equals(symbol)) {
    		throw new IllegalStateException("symbol passed doesnt match this price");
    	}
    	explainList.add(String.format("%s,%d,%.2f,%.2f,%.2f", symbol, qty, price, cash, stopPrice));
    }
    
    public void addTrace(String symbol, int qty, double price, double cash) {
    	if(!this.symbol.equals(symbol)) {
    		throw new IllegalStateException("symbol passed doesnt match this price");
    	}
    	explainList.add(String.format("%s,%d,%.2f,%.2f", symbol, qty, price, cash));
    }
    
}