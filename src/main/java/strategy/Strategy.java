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
	protected String symbol;
	private double cash;
    private double totalPortfolioValue;
    private double peakValue;
    private double maxDrawdown;
    private double maxGain;
    private double initialCapital;

    // Keep track of daily returns for Sharpe
    private List<Double> dailyReturns = new ArrayList<>(20_000);
    private double previousValue;  // store yesterday's portfolio value

    protected List<Position> positions;
    private int buyCount;
    private int sellCount;

    public Strategy(String symbol) {
    	this.symbol = symbol;
        this.initialCapital = 100_000;
        this.cash = initialCapital;
        this.peakValue = initialCapital;
        this.totalPortfolioValue = initialCapital;
        this.maxDrawdown = 0.0;
        this.maxGain = 0.0;
        this.previousValue = initialCapital;  // so day-1 return is 0

        this.positions = new ArrayList<>(20_000);
        this.buyCount = 0;
        this.sellCount = 0;
    }

    // Each child strategy must implement its own run(...) logic
    public abstract void run(Map<String, StockPrice> marketData);

    // Basic checks/helpers
    protected boolean hasCash(int qty, double purchasePrice) {
        return cash >= qty * purchasePrice;
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
            .orElseThrow(() -> new IllegalStateException("No position for symbol: " + symbol))
            .getQty();
    }

    protected Position getPosition(String symbol) {
        return positions.stream()
            .filter(p -> p.getSymbol().equals(symbol))
            .findFirst()
            .orElse(null);
    }

    protected void addPosition(String symbol, int qty, StockPrice sp) {
    	double purchasePrice = sp.getClose();
        double cost = qty * purchasePrice;
        if (cost > cash) {
            throw new IllegalStateException(String.format(
                "Not enough cash to buy %d of %s at $%.2f (cost=%.2f, cash=%.2f)",
                qty, symbol, purchasePrice, cost, cash
            ));
        }

        Position position = getPosition(symbol);
        if (position == null) {
            position = new Position(symbol);
            positions.add(position);
        }
        position.addQuantity(qty, purchasePrice);

        cash -= cost;
        buyCount++;
        
        sp.addTrace(symbol, qty, purchasePrice, cash);
    }

    protected void reducePosition(String symbol, int qty,  StockPrice sp) {
    	double marketPrice = sp.getClose();
        Position position = getPosition(symbol);
        if (position == null) {
            throw new IllegalStateException("Cannot reduce position: " + symbol + " not held.");
        }

        position.reduceQuantity(qty);
        if (position.getQty() <= 0) {
            positions.remove(position);
        }

        double saleAmount = qty * marketPrice;
        cash += saleAmount;
        sellCount++;
        sp.addTrace(symbol, -qty, marketPrice, cash);
    }

    /**
     * Recompute total portfolio value: cash + sum of (qty * currentPrice) for all positions.
     * Then update:
     *  - peakValue (if new high)
     *  - maxDrawdown (worst drop from peak, fraction)
     *  - maxGain (largest fraction above initial capital)
     */
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

        // 1) daily return
        double dailyReturn = (previousValue == 0.0) 
            ? 0.0 
            : (total - previousValue) / previousValue;
        dailyReturns.add(dailyReturn);

        // 2) store "yesterday" value for next iteration
        previousValue = total;

        // 3) update peak, drawdown, etc...
        if (total > peakValue) {
            peakValue = total;
        }
        double currentDrawdown = (peakValue - total) / peakValue;
        if (currentDrawdown > maxDrawdown) {
            maxDrawdown = currentDrawdown;
        }
        double currentGain = (total - initialCapital) / initialCapital;
        if (currentGain > maxGain) {
            maxGain = currentGain;
        }
    }

    // Compute the Sharpe ratio at the end of the backtest
    public double getSharpeRatio() {
    	double riskFreeRate = 0.03;
        if (dailyReturns.size() < 2) {
            return 0.0;
        }

        double sum = 0.0;
        for (double r : dailyReturns) {
            sum += r;
        }
        double avgDaily = sum / dailyReturns.size();

        double varianceSum = 0.0;
        for (double r : dailyReturns) {
            double diff = r - avgDaily;
            varianceSum += diff * diff;
        }
        double stdDaily = Math.sqrt(varianceSum / (dailyReturns.size() - 1));

        double daysPerYear = 252.0;
        double avgAnnual = avgDaily * daysPerYear;
        double stdAnnual = stdDaily * Math.sqrt(daysPerYear);

        if (stdAnnual == 0.0) return 0.0;

        // Subtract risk-free rate
        return (avgAnnual - riskFreeRate) / stdAnnual;
    }

    
    /**
     * @return max drawdown as decimal fraction, e.g. 0.20 = 20%
     */
    public double getMaxDrawdownPct() {
        return maxDrawdown;
    }

    /**
     * @return max gain as decimal fraction, e.g. 2.0 = +200% from initial
     */
    public double getMaxGainPct() {
        return maxGain;
    }

    /**
     * If you want the absolute highest portfolio value so far
     */
    public double getPeakValue() {
        return peakValue;
    }

    public int getTotalCount() {
        return buyCount + sellCount;
    }
}
