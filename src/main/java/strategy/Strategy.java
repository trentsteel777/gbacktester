package strategy;

import java.time.LocalDate;
import java.util.HashMap;
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

    private double peakValue;
    private double maxDrawdown;
    private double maxGain;

    // Incremental metrics
    private int dailyReturnCount;
    private double dailyReturnMean;
    private double dailyReturnM2;

    private int negativeReturnCount;
    private double negativeReturnMean;
    private double negativeReturnM2;

    private Map<String, Position> positions;

    private int buyCount;
    private int sellCount;

    private LocalDate startDate;
    private LocalDate lastDate;

    private List<Double> closedTradePnLs = new LinkedList<>();

    private double previousValue;

    public Strategy(String symbol) {
        this.symbol = symbol;
        this.initialCapital = 100_000;
        this.cash = initialCapital;

        this.totalPortfolioValue = initialCapital;
        this.peakValue = initialCapital;

        this.positions = new HashMap<>();
        this.previousValue = initialCapital;
    }

    public abstract void run(Map<String, StockPrice> marketData);

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
        return getPosition(symbol).getQty();
    }

    protected Position getPosition(String symbol) {
        return positions.get(symbol); // O(1) lookup
    }

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
            positions.put(symbol, position);
        }
        position.addQuantity(qty, purchasePrice);

        cash -= cost;
        buyCount++;

        sp.addTrace(symbol, qty, purchasePrice, cash);
    }

    protected void reducePosition(String symbol, int qty, StockPrice sp) {
        double marketPrice = sp.getClose();
        Position position = getPosition(symbol);
        if (position == null) {
            throw new IllegalStateException("Cannot reduce position: " + symbol + " not held.");
        }

        double purchasePrice = position.getAvgEntryPrice();
        double tradePnL = (marketPrice - purchasePrice) * qty;

        position.reduceQuantity(qty);
        if (position.getQty() <= 0) {
            positions.remove(symbol);
        }

        cash += qty * marketPrice;
        sellCount++;

        sp.addTrace(symbol, -qty, marketPrice, cash);

        closedTradePnLs.add(tradePnL);
    }

    public void calculateTotalPortfolioValue(Map<String, Double> marketPrices, LocalDate date) {
        trackDates(date);

        totalPortfolioValue = cash;
        for (Position position : positions.values()) {
            totalPortfolioValue += position.getQty() * marketPrices.getOrDefault(position.getSymbol(), 0.0);
        }

        double dailyReturn = (previousValue == 0.0) ? 0.0 : (totalPortfolioValue - previousValue) / previousValue;
        previousValue = totalPortfolioValue;

        dailyReturnCount++;
        double delta = dailyReturn - dailyReturnMean;
        dailyReturnMean += delta / dailyReturnCount;
        dailyReturnM2 += delta * (dailyReturn - dailyReturnMean);

        if (dailyReturn < 0) {
            negativeReturnCount++;
            double deltaNeg = dailyReturn - negativeReturnMean;
            negativeReturnMean += deltaNeg / negativeReturnCount;
            negativeReturnM2 += deltaNeg * (dailyReturn - negativeReturnMean);
        }

        if (totalPortfolioValue > peakValue) {
            peakValue = totalPortfolioValue;
        }

        maxDrawdown = Math.max(maxDrawdown, (peakValue - totalPortfolioValue) / peakValue);
        maxGain = Math.max(maxGain, (totalPortfolioValue - initialCapital) / initialCapital);
    }

    public double getSharpeRatio() {
        if (dailyReturnCount < 2) return 0;
        double variance = dailyReturnM2 / (dailyReturnCount - 1);
        double stdAnnual = Math.sqrt(variance) * Math.sqrt(252);
        return stdAnnual == 0 ? 0 : (dailyReturnMean * 252 - 0.03) / stdAnnual;
    }

    public double getSortinoRatio() {
        if (negativeReturnCount < 2) return 999;
        double negVariance = negativeReturnM2 / (negativeReturnCount - 1);
        double negStdAnnual = Math.sqrt(negVariance) * Math.sqrt(252);
        return negStdAnnual == 0 ? 999 : (dailyReturnMean * 252 - 0.03) / negStdAnnual;
    }

    public double getCagr() {
        if (startDate == null || lastDate == null) return 0.0;
        double years = java.time.temporal.ChronoUnit.DAYS.between(startDate, lastDate) / 365.0;
        if (years < EPSILON) return 0.0;
        return Math.pow(totalPortfolioValue / initialCapital, 1.0 / years) - 1;
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
    
    public int getTotalCount() {
        return buyCount + sellCount;
    }
}
