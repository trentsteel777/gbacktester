package gbacktester.domain;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;

// /home/trent/workspace/gbacktester/src/main/resources/data
@Getter
@Setter
public class StockRecord {
    private String symbol;
    private LocalDate date;

    private double open;
    private double high;
    private double low;
    private double close;
    private long volume;

    // Momentum / oscillator
    private Double rsi14;

    // Moving Averages (Simple)
    private Double sma10;    // ADDED
    private Double sma20;
    private Double sma30;    // ADDED
    private Double sma50;
    private Double sma150;
    private Double sma200;

    // Moving Averages (Exponential)
    private Double ema20;
    private Double ema50;

    // Volatility
    private Double volatility; // e.g., ATR(14)

    // MACD (based on 12-day and 26-day EMAs, typically)
    private Double macdLine;         // ADDED
    private Double macdSignalLine;   // ADDED
    private Double macdHistogram;    // ADDED

    // Stochastic Oscillator
    private Double stochasticK;      // ADDED (often %K)
    private Double stochasticD;      // ADDED (often %D)
    
    private boolean is52WeekHigh;
}
