package strategy;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import domain.Position;
import domain.StockPrice;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base abstract class for strategies with advanced metrics:
 *  - CAGR
 *  - Sortino Ratio
 *  - Calmar Ratio
 *  - Win rate, average win/loss, profit factor
 *  - Monthly/Yearly returns (demo)
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
public abstract class Strategy {

	private static final double EPSILON = 1e-6;
	
    protected String symbol;

    private double initialCapital;
    private double cash;
    private double totalPortfolioValue;

    // Track peak for drawdown
    private double peakValue;
    private double maxDrawdown;    // fraction, e.g., 0.20 = 20%
    private double maxGain;        // fraction, e.g., 2.0 = +200%

    // For daily returns computations
    private List<Double> dailyReturns = new LinkedList<>();
    private double previousValue;   // store yesterday's portfolio value

    protected List<Position> positions;
    private int buyCount;
    private int sellCount;

    // Track the first date and last date of the backtest to compute years for CAGR
    private LocalDate startDate;
    private LocalDate lastDate;

    // For Sortino ratio: store negative daily returns separately
    private List<Double> negativeReturns = new LinkedList<>();

    // For trade stats (win rate, average win/loss, profit factor)
    // We'll store each closed trade P&L
    private List<Double> closedTradePnLs = new LinkedList<>();
    private double openTradeValue;  // used temporarily in some strategies

    public Strategy(String symbol) {
        this.symbol = symbol;
        this.initialCapital = 100_000;
        this.cash = initialCapital;

        this.totalPortfolioValue = initialCapital;
        this.peakValue = initialCapital;
        this.maxDrawdown = 0.0;
        this.maxGain = 0.0;
        this.previousValue = initialCapital;

        this.positions = new LinkedList<>();
        this.buyCount = 0;
        this.sellCount = 0;
    }

    // Each child strategy must implement run(...) logic
    public abstract void run(Map<String, StockPrice> marketData);

    /**
     * On the first day we see data, record as startDate
     * On every day we see data, record as lastDate
     */
    public void trackDates(LocalDate date) {
        if (startDate == null) {
            startDate = date;
        }
        lastDate = date;
    }

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

    /**
     * Opening a position (BUY).
     */
    protected void addPosition(String symbol, int qty, StockPrice sp) {
        double purchasePrice = sp.getClose();
        double cost = qty * purchasePrice;
        if (cost - cash > EPSILON) {
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

        // Add an optional trace
        sp.addTrace(symbol, qty, purchasePrice, cash);
    }

    /**
     * Closing or reducing a position (SELL).
     */
    protected void reducePosition(String symbol, int qty, StockPrice sp) {
        double marketPrice = sp.getClose();
        Position position = getPosition(symbol);
        if (position == null) {
            throw new IllegalStateException("Cannot reduce position: " + symbol + " not held.");
        }

        // Here we can track trade P&L
        // For a full close: record a new closedTradePnLs
        double purchasePrice = position.getAvgEntryPrice(); // if you want a rough P&L
        double tradePnL = (marketPrice - purchasePrice) * qty;  // simplistic

        position.reduceQuantity(qty);
        if (position.getQty() <= 0) {
            positions.remove(position);
        }

        double saleAmount = qty * marketPrice;
        cash += saleAmount;
        sellCount++;

        sp.addTrace(symbol, -qty, marketPrice, cash);

        // record the PnL of this partial or full sale
        closedTradePnLs.add(tradePnL);
    }

    /**
     * Recompute total portfolio value: cash + sum of (qty * currentPrice)
     * Then update metrics:
     *   - daily returns (for Sharpe, Sortino, etc.)
     *   - peakValue, maxDrawdown, maxGain
     */
    public void calculateTotalPortfolioValue(Map<String, Double> marketPrices, LocalDate date) {
        trackDates(date);  // record start & end dates

        double total = cash;
        for (Position position : positions) {
            String sym = position.getSymbol();
            Double currentPrice = marketPrices.get(sym);
            if (currentPrice == null) {
                log.warn("No market price for symbol: {}", sym);
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

        // If negative, store for Sortino
        if (dailyReturn < 0) {
            negativeReturns.add(dailyReturn);
        }

        // 2) store 'yesterday' value
        previousValue = total;

        // 3) update peak, drawdown, etc.
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

    // Existing Sharpe:
    public double getSharpeRatio() {
        double riskFreeRate = 0.03;
        if (dailyReturns.size() < 2) {
            return 0.0;
        }

        double avgDaily = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = dailyReturns.stream().mapToDouble(r -> r - avgDaily)
                                      .map(diff -> diff * diff)
                                      .sum() / (dailyReturns.size() - 1);
        double stdDaily = Math.sqrt(variance);

        double daysPerYear = 252.0;
        double avgAnnual = avgDaily * daysPerYear;
        double stdAnnual = stdDaily * Math.sqrt(daysPerYear);

        if (stdAnnual == 0.0) return 0.0;

        return (avgAnnual - riskFreeRate) / stdAnnual;
    }

    // 1) CAGR
    // we assume we ended on lastDate and started on startDate
    public double getCagr() {
        if (startDate == null || lastDate == null) return 0.0;
        long days = java.time.temporal.ChronoUnit.DAYS.between(startDate, lastDate);
        if (days < 1) return 0.0;

        double years = days / 365.0; // approximate, or use 252 trading days approach
        // final / initial:
        double ratio = (totalPortfolioValue / initialCapital);
        // CAGR = (ratio^(1/years)) - 1
        if (ratio <= 0.0 || years <= 0.0) return 0.0;
        return Math.pow(ratio, 1.0 / years) - 1.0;
    }

    // 2) Sortino ratio (like Sharpe but only uses negative returns in the denominator)
    public double getSortinoRatio() {
        double riskFreeRate = 0.03;
        if (dailyReturns.size() < 2) {
            return 0.0;
        }
        double avgDaily = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        // annualize average
        double avgAnnual = avgDaily * 252.0;

        // only negative returns stdev
        if (negativeReturns.isEmpty()) {
            // if we never had negative returns, sortino is huge
            return 999.0;
        }
        double negAvg = negativeReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double varSum = 0.0;
        for (double r : negativeReturns) {
            double diff = r - negAvg;
            varSum += diff * diff;
        }
        double negVariance = varSum / (negativeReturns.size() - 1);
        double negStdDaily = Math.sqrt(negVariance);
        double negStdAnnual = negStdDaily * Math.sqrt(252.0);

        if (negStdAnnual == 0.0) {
            // no negative volatility => big ratio
            return 999.0;
        }

        return (avgAnnual - riskFreeRate) / negStdAnnual;
    }

    // 3) Calmar ratio = CAGR / maxDrawdown (maxDrawdown is fraction)
    public double getCalmarRatio() {
        double cagr = getCagr();
        if (maxDrawdown == 0.0) {
            return 999.0; // no drawdown => large ratio
        }
        return cagr / maxDrawdown;
    }

    // 4) Win rate, average win/loss, profit factor
    public double getWinRate() {
        if (closedTradePnLs.isEmpty()) return 0.0;
        long wins = closedTradePnLs.stream().filter(pnl -> pnl > 0.0).count();
        return (double) wins / closedTradePnLs.size();
    }

    public double getAverageWin() {
        // average of positive pnls
        List<Double> wins = closedTradePnLs.stream().filter(p -> p > 0).collect(java.util.stream.Collectors.toList());
        if (wins.isEmpty()) return 0.0;
        return wins.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double getAverageLoss() {
        // average of negative pnls
        List<Double> losses = closedTradePnLs.stream().filter(p -> p < 0).collect(java.util.stream.Collectors.toList());
        if (losses.isEmpty()) return 0.0;
        return losses.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double getProfitFactor() {
        double totalWins = closedTradePnLs.stream().filter(p -> p > 0).mapToDouble(Double::doubleValue).sum();
        double totalLoss = closedTradePnLs.stream().filter(p -> p < 0).mapToDouble(Double::doubleValue).sum();
        // negative sum for losses, so factor = sum(wins)/abs(sum(losses))
        if (totalLoss == 0.0) return 999.0;
        return totalWins / Math.abs(totalLoss);
    }

    // 5) (Optional) monthly/yearly returns breakdown
    // We'll store daily returns in dailyReturns, so you can group them by month or year
    // But we also need the date for each dailyReturn. 
    // => you might keep a parallel list or a structure to store (date, dailyReturn).
    // We'll just show a skeleton function here:
    public void printMonthlyReturns() {
        // You'd need a List<LocalDate> dailyDates, or store the returns in a map of date->return
        // Then group by YearMonth and sum or compound them.
        System.out.println("Monthly returns not implemented, but you get the idea!");
    }
    
    public int getTotalCount() {
        return buyCount + sellCount;
    }
}
