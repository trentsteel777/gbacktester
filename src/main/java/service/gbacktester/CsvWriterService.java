package service.gbacktester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.StochasticOscillatorDIndicator;
import org.ta4j.core.indicators.StochasticOscillatorKIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import lombok.RequiredArgsConstructor;
import util.StopWatch;

public class CsvWriterService {
	private static final double EPSILON = 1e-6;
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private static final Path base = Paths.get("src", "main", "resources", "data").toAbsolutePath();
	private static final String FOLDER_PATH = base.resolve("all").toString();
	private static final String OUTPUT_PATH = base.resolve("calculated").toString();

    // /home/trent/workspace/gbacktester/src/main/resources/data
	@RequiredArgsConstructor
	public static class StockRecord {
	    String symbol;
	    LocalDate date;

	    double open;
	    double high;
	    double low;
	    double close;
	    long volume;

	    // Momentum / oscillator
	    Double rsi14;

	    // Moving Averages (Simple)
	    Double sma10;    // ADDED
	    Double sma20;
	    Double sma30;    // ADDED
	    Double sma50;
	    Double sma150;
	    Double sma200;

	    // Moving Averages (Exponential)
	    Double ema20;
	    Double ema50;

	    // Volatility
	    Double volatility; // e.g., ATR(14)

	    // MACD (based on 12-day and 26-day EMAs, typically)
	    Double macdLine;         // ADDED
	    Double macdSignalLine;   // ADDED
	    Double macdHistogram;    // ADDED

	    // Stochastic Oscillator
	    Double stochasticK;      // ADDED (often %K)
	    Double stochasticD;      // ADDED (often %D)
	}

	public static void main(String[] args) throws Exception {
		StopWatch sw = StopWatch.start();
		CsvWriterService writer = new CsvWriterService();
		writer.writePrecalculatedTechnicalAnalysis();
		//writer.validatorCsvFiles();
		sw.printElapsed();
	}
    
    public void validatorCsvFiles() {
        File folder = new File(FOLDER_PATH);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + FOLDER_PATH);
        }

        for (File file : files) {
        	String symbol = file.getName().replace(".csv", "");
        	try {
        		loadStockRecords(file.getAbsolutePath());
        	}
        	catch(Exception e) {
        		System.out.println(symbol);
        	}
        }
    }
	
    public void writePrecalculatedTechnicalAnalysis() {
    	Map<String, List<StockRecord>> allStockRecords = loadAllStockRecords();
        // For each symbol, write out a new CSV
        for (Map.Entry<String, List<StockRecord>> entry : allStockRecords.entrySet()) {
            String symbol = entry.getKey();
            List<StockRecord> stockRecords = entry.getValue();

            // Build an output path (or name it however you'd like)
            String outputFilePath = Paths.get(OUTPUT_PATH, symbol + ".csv")
                    .toAbsolutePath()
                    .toString();

            writeStockPricesWithTA(outputFilePath, stockRecords);
            System.out.println("Wrote TA CSV for: " + symbol + " -> " + outputFilePath);
        }
    }
    
    private void writeStockPricesWithTA(String outputFilePath, List<StockRecord> records) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            // Write header
            writer.println("Date,Open,High,Low,Close,AdjClose,Volume," +
                           "RSI14,SMA10,SMA20,SMA30,SMA50,SMA150,SMA200," +
                           "EMA20,EMA50,Volatility," +
                           "MACDLine,MACDSignalLine,MACDHistogram," +
                           "StochasticK,StochasticD");

            // Write each row
            for (StockRecord sr : records) {
                writer.printf(
                    "%s,%f,%f,%f,%f,%d," +    // Date, Open, High, Low, Close, Volume
                    "%f,%f,%f,%f,%f,%f,%f," +    // RSI14, SMA10, SMA20, SMA30, SMA50, SMA150, SMA200
                    "%f,%f,%f," +                // EMA20, EMA50, Volatility
                    "%f,%f,%f," +                // MACDLine, MACDSignalLine, MACDHistogram
                    "%f,%f%n",                   // StochasticK, StochasticD

                    // 1) Basic
                    sr.date.format(formatter),
                    sr.open,
                    sr.high,
                    sr.low,
                    sr.close,
                    sr.volume,

                    // 2) RSI14, SMAs (handle null by writing NaN)
                    sr.rsi14            == null ? Double.NaN : sr.rsi14,
                    sr.sma10            == null ? Double.NaN : sr.sma10,
                    sr.sma20            == null ? Double.NaN : sr.sma20,
                    sr.sma30            == null ? Double.NaN : sr.sma30,
                    sr.sma50            == null ? Double.NaN : sr.sma50,
                    sr.sma150           == null ? Double.NaN : sr.sma150,
                    sr.sma200           == null ? Double.NaN : sr.sma200,

                    // 3) EMA20, EMA50, Volatility
                    sr.ema20            == null ? Double.NaN : sr.ema20,
                    sr.ema50            == null ? Double.NaN : sr.ema50,
                    sr.volatility       == null ? Double.NaN : sr.volatility,

                    // 4) MACD (Line, Signal, Histogram)
                    sr.macdLine         == null ? Double.NaN : sr.macdLine,
                    sr.macdSignalLine   == null ? Double.NaN : sr.macdSignalLine,
                    sr.macdHistogram    == null ? Double.NaN : sr.macdHistogram,

                    // 5) Stochastic (%K, %D)
                    sr.stochasticK      == null ? Double.NaN : sr.stochasticK,
                    sr.stochasticD      == null ? Double.NaN : sr.stochasticD
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing TA CSV file: " + outputFilePath, e);
        }
    }

    
    public Map<String, List<StockRecord>> loadAllStockRecords() {
        File folder = new File(FOLDER_PATH);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));
        
        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + FOLDER_PATH);
        }
		System.out.println("Create a fixed thread pool of size: " + Runtime.getRuntime().availableProcessors());
        // Thread pool; pick size based on CPU/disk considerations
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );
        
        ConcurrentMap<String, List<StockRecord>> allData = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (File file : files) {
            futures.add(executor.submit(() -> {
                String symbol = file.getName().replace(".csv", "");
                // Use your method that reads one CSV and returns List<StockRecord>
                List<StockRecord> records = loadStockRecords(file.getAbsolutePath());
                allData.put(symbol, records);
            }));
        }

        // Wait for all tasks
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread was interrupted", e);
            } catch (ExecutionException e) {
                throw new RuntimeException("Error loading records in parallel", e);
            }
        }

        executor.shutdown();
        return allData;
    }

    private List<StockRecord> loadStockRecords(String filePath) {
        String seriesName = new File(filePath).getName().replace(".csv", "");

        List<StockRecord> prices = new LinkedList<>();
        List<Bar> bars = new LinkedList<>();

        // 1) Read the raw CSV data and build a list of Bar objects
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip header
            br.readLine(); 
            int lineNum = 2;
            double lastRatio = 0.0;
            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (v.length < 7) {
                    throw new RuntimeException("Invalid line at row " + lineNum + ": " + line);
                }

                LocalDate date = LocalDate.parse(v[0], formatter);
                double open = Double.parseDouble(v[1]);
                double high = Double.parseDouble(v[2]);
                double low = Double.parseDouble(v[3]);
                double close = Double.parseDouble(v[4]);
                double adjClose = Double.parseDouble(v[5]);
                long volume = Long.parseLong(v[6]);

                boolean isOpenZero  = open == 0.0;
                boolean isHighZero  = high == 0.0;
                boolean isLowZero   = low == 0.0;
                boolean isCloseZero = close == 0.0;
                boolean isAdjCloseZero = adjClose == 0.0;

                if (isAdjCloseZero) {
                	if(!isCloseZero && lastRatio > EPSILON) {
                		System.out.printf("Patching adjClose for %s at %s using ratio %.6f%n", seriesName, v[0], lastRatio);
                		adjClose = close * lastRatio;
                	}
                	else {
                		throw new IllegalArgumentException("adjClose is 0.0 for symbol " + seriesName + " at date " + v[0]);
                	}
                }
                
                if (isOpenZero && isHighZero && isLowZero && isCloseZero) {
                    throw new IllegalArgumentException("All OHLC values are 0.0 for symbol " + seriesName + " at date " + v[0]);
                }

                if (isHighZero) {
                	 System.out.printf("Patching HIGH for %s on %s%n", seriesName, v[0]);
                    high = Math.max(Math.max(open, low), close);
                }
                if (isLowZero) {
                	 System.out.printf("Patching LOW for %s on %s%n", seriesName, v[0]);
                    low = Math.min(Math.min(open, high), close);
                }
                if (isOpenZero) {
                	 System.out.printf("Patching OPEN for %s on %s%n", seriesName, v[0]);
                    open = averageOfNonZero(high, low, close);
                }
                if (isCloseZero) {
                	 System.out.printf("Patching CLOSE for %s on %s%n", seriesName, v[0]);
                    close = averageOfNonZero(open, high, low);
                }
                
                
                double ratio = adjClose / close;
                if (close <= 0 || ratio <= 0 || ratio > 10) {
                    throw new IllegalArgumentException(String.format(
                        "Invalid adjustment ratio for %s at %s: adjClose=%.6f, close=%.6f, ratio=%.6f",
                        seriesName, v[0], adjClose, close, ratio
                    ));
                }
                lastRatio = ratio;

                double adjOpen = open * ratio;
                double adjHigh = high * ratio;
                double adjLow  = low * ratio;
                
                if (adjOpen < 0 || adjHigh < 0 || adjLow < 0 || adjClose < 0) {
                    throw new IllegalArgumentException(String.format(
                        "Negative adjusted OHLC for %s at %s: open=%.4f, high=%.4f, low=%.4f, close=%.4f",
                        seriesName, v[0], adjOpen, adjHigh, adjLow, adjClose
                    ));
                }
                
                StockRecord stockRecord = new StockRecord();
                stockRecord.symbol = seriesName;
                stockRecord.date = date;
                stockRecord.open = adjOpen;
                stockRecord.high = adjHigh;
                stockRecord.low = adjLow;
                stockRecord.close = adjClose;
                stockRecord.volume = volume;

                prices.add(stockRecord);

                // TA4J needs a Bar with open/high/low/close/volume. 
                // Use adjClose for consistency if that is how you price everything.
                ZonedDateTime endTime = date.atStartOfDay(ZoneId.systemDefault());
                Bar bar = new BaseBar(
                    Duration.ofDays(1), 
                    endTime, 
                    adjOpen, 
                    adjHigh, 
                    adjLow, 
                    adjClose, 
                    volume
                );
                bars.add(bar);

                lineNum++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file: " + filePath, e);
        }

        // 2) Create a BarSeries and add all bars
        BarSeries series = new BaseBarSeries(seriesName);
        for (Bar bar : bars) {
            series.addBar(bar);
        }

        // 3) Create the TA4J indicators

        // Close-price-based indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

        // Simple Moving Averages
        SMAIndicator sma10  = new SMAIndicator(closePrice, 10);
        SMAIndicator sma20  = new SMAIndicator(closePrice, 20);
        SMAIndicator sma30  = new SMAIndicator(closePrice, 30);
        SMAIndicator sma50  = new SMAIndicator(closePrice, 50);
        SMAIndicator sma150 = new SMAIndicator(closePrice, 150);
        SMAIndicator sma200 = new SMAIndicator(closePrice, 200);

        // Exponential MAs
        EMAIndicator ema20 = new EMAIndicator(closePrice, 20);
        EMAIndicator ema50 = new EMAIndicator(closePrice, 50);

        // RSI (14)
        RSIIndicator rsi = new RSIIndicator(closePrice, 14);

        // ATR(14) for volatility
        ATRIndicator atr14 = new ATRIndicator(series, 14);

        // MACD (12, 26, 9)
        // - macdLine = difference between 12-EMA and 26-EMA
        // - macdSignalLine = EMA of macdLine over 9 bars
        // - macdHistogram = macdLine - macdSignalLine
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);

        // Stochastic (using 14 for %K, and 3 for %D as default). 
        // If you prefer a different period, adjust as needed.
        StochasticOscillatorKIndicator stochK = new StochasticOscillatorKIndicator(series, 14);
        StochasticOscillatorDIndicator stochD = new StochasticOscillatorDIndicator(stochK);

        // 4) Loop over each bar/price and fill in the derived indicator values
        for (int i = 0; i < prices.size(); i++) {
            StockRecord rec = prices.get(i);

            // RSI requires at least 14 data points
            rec.rsi14   = i >= 13 ? rsi.getValue(i).doubleValue() : null;

            // SMAs
            rec.sma10   = i >= 9  ? sma10.getValue(i).doubleValue() : null;
            rec.sma20   = i >= 19 ? sma20.getValue(i).doubleValue() : null;
            rec.sma30   = i >= 29 ? sma30.getValue(i).doubleValue() : null;
            rec.sma50   = i >= 49 ? sma50.getValue(i).doubleValue() : null;
            rec.sma150  = i >= 149? sma150.getValue(i).doubleValue() : null;
            rec.sma200  = i >= 199? sma200.getValue(i).doubleValue() : null;

            // EMAs
            rec.ema20   = i >= 19 ? ema20.getValue(i).doubleValue() : null;
            rec.ema50   = i >= 49 ? ema50.getValue(i).doubleValue() : null;

            // ATR for volatility
            rec.volatility = i >= 13 ? atr14.getValue(i).doubleValue() : null;

            // MACD
            //   Macd line typically needs 26 bars to be "fully" established
            //   Macd signal line typically needs 26+9=35 bars for full data
            Double macdVal = i >= 25 ? macd.getValue(i).doubleValue() : null;
            Double macdSignalVal = i >= 34 ? macdSignal.getValue(i).doubleValue() : null;
            Double macdHistVal = (macdVal != null && macdSignalVal != null)
                                 ? macdVal - macdSignalVal
                                 : null;

            rec.macdLine        = macdVal;
            rec.macdSignalLine  = macdSignalVal;
            rec.macdHistogram   = macdHistVal;

            // Stochastic K & D
            //   Typically needs 14 bars to get the first full %K
            //   and stochD is a 3-bar SMA of stochK, so it can require up to 16-17 bars
            rec.stochasticK = i >= 13 ? stochK.getValue(i).doubleValue() : null;
            rec.stochasticD = i >= 15 ? stochD.getValue(i).doubleValue() : null;
            
            validateIndicators(seriesName, rec.date, rec);

        }

        System.out.println("Finished calculating TA: " + seriesName);
        return prices;
    }
    
    private void validateIndicators(String symbol, LocalDate date, StockRecord rec) {
        if (rec.rsi14 != null && (rec.rsi14 < 0 || rec.rsi14 > 100)) {
            throw new IllegalArgumentException(symbol + " " + date + " -> Invalid RSI: " + rec.rsi14);
        }

        if (rec.volatility != null && rec.volatility < 0) {
            throw new IllegalArgumentException(symbol + " " + date + " -> Negative ATR: " + rec.volatility);
        }

        checkPositive("SMA10", symbol, date, rec.sma10);
        checkPositive("SMA20", symbol, date, rec.sma20);
        checkPositive("SMA30", symbol, date, rec.sma30);
        checkPositive("SMA50", symbol, date, rec.sma50);
        checkPositive("SMA150", symbol, date, rec.sma150);
        checkPositive("SMA200", symbol, date, rec.sma200);

        checkPositive("EMA20", symbol, date, rec.ema20);
        checkPositive("EMA50", symbol, date, rec.ema50);

        checkPercentageRange("StochasticK", symbol, date, rec.stochasticK);
        checkPercentageRange("StochasticD", symbol, date, rec.stochasticD);

        // MACD can be negative or positive, so we don’t restrict it
    }
    
    private void checkPositive(String label, String symbol, LocalDate date, Double value) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(symbol + " " + date + " -> " + label + " < 0");
        }
    }

    private void checkPercentageRange(String label, String symbol, LocalDate date, Double value) {
        if (value != null && (value < -EPSILON || value > 100.0 + EPSILON)) {
            throw new IllegalArgumentException(symbol + " " + date + " -> Invalid " + label + ": " + value);
        }
    }
    private double averageOfNonZero(double... values) {
        double sum = 0.0;
        int count = 0;
        for (double val : values) {
            if (val != 0.0) {
                sum += val;
                count++;
            }
        }
        if (count == 0) {
            throw new IllegalArgumentException("Cannot average from all-zero values");
        }
        return sum / count;
    }

}
