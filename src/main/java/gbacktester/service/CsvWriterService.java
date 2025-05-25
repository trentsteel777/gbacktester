package gbacktester.service;

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

import gbacktester.MagicFormulaCalculator;
import gbacktester.domain.CsvStockRecord;
import gbacktester.ta.RollingWindowHigh;
import gbacktester.util.StopWatch;

public class CsvWriterService {
    private static final int DAYS_52_WEEK = 252;
    private static final double EPSILON = 1e-6;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Path base = Paths.get("src", "main", "resources", "data").toAbsolutePath();
    private static final String FOLDER_PATH = base.resolve("all").toString();
    private static final String OUTPUT_PATH = base.resolve("calculated").toString();

    public static void main(String[] args) throws Exception {
        StopWatch sw = StopWatch.start();
        CsvWriterService writer = new CsvWriterService();
        writer.writePrecalculatedTechnicalAnalysis();
        // writer.validatorCsvFiles();
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
            } catch(Exception e) {
                System.out.println(symbol);
            }
        }
    }

    public void writePrecalculatedTechnicalAnalysis() {
		/* Map<String, List<CsvStockRecord>> allStockRecords = */
    	loadAllStockRecords();

        // For each symbol, write out a new CSV
        /*for (Map.Entry<String, List<CsvStockRecord>> entry : allStockRecords.entrySet()) {
            String symbol = entry.getKey();
            List<CsvStockRecord> stockRecords = entry.getValue();

            // Build an output path
            String outputFilePath = Paths.get(OUTPUT_PATH, symbol + ".csv")
                    .toAbsolutePath()
                    .toString();

            writeStockPricesWithTA(outputFilePath, stockRecords);
            System.out.println("Wrote TA CSV for: " + symbol + " -> " + outputFilePath);
        }*/
    }

    private void writeStockPricesWithTA(String symbol, String outputFilePath, List<CsvStockRecord> records) {
    	MagicFormulaCalculator mfc = new MagicFormulaCalculator(records);
    	mfc.loadAndCalculate(symbol);
    	
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            // Write header (note the added comma before is_52_week_high)
            writer.println("Date,Open,High,Low,Close,Volume," +
                           "RSI14,SMA10,SMA20,SMA30,SMA50,SMA150,SMA200," +
                           "EMA20,EMA50,Volatility," +
                           "MACDLine,MACDSignalLine,MACDHistogram," +
                           "StochasticK,StochasticD," +
                           "is_52_week_high");

            // Write each row
            for (CsvStockRecord sr : records) {
                writer.printf(
                    // Basic fields
                    "%s,%f,%f,%f,%f,%d," +
                    // RSI14, SMAs
                    "%f,%f,%f,%f,%f,%f,%f," +
                    // EMA20, EMA50, Volatility
                    "%f,%f,%f," +
                    // MACD
                    "%f,%f,%f," +
                    // Stoch K, D
                    "%f,%f," +
                    // 52-week-high
                    "%b%n",

                    // 1) Basic
                    sr.getDate().format(formatter),
                    sr.getOpen(),
                    sr.getHigh(),
                    sr.getLow(),
                    sr.getClose(),
                    sr.getVolume(),

                    // 2) RSI14, SMAs
                    (sr.getRsi14()    == null ? Double.NaN : sr.getRsi14()),
                    (sr.getSma10()    == null ? Double.NaN : sr.getSma10()),
                    (sr.getSma20()    == null ? Double.NaN : sr.getSma20()),
                    (sr.getSma30()    == null ? Double.NaN : sr.getSma30()),
                    (sr.getSma50()    == null ? Double.NaN : sr.getSma50()),
                    (sr.getSma150()   == null ? Double.NaN : sr.getSma150()),
                    (sr.getSma200()   == null ? Double.NaN : sr.getSma200()),

                    // 3) EMA20, EMA50, Volatility
                    (sr.getEma20()    == null ? Double.NaN : sr.getEma20()),
                    (sr.getEma50()    == null ? Double.NaN : sr.getEma50()),
                    (sr.getVolatility() == null ? Double.NaN : sr.getVolatility()),

                    // 4) MACD
                    (sr.getMacdLine()       == null ? Double.NaN : sr.getMacdLine()),
                    (sr.getMacdSignalLine() == null ? Double.NaN : sr.getMacdSignalLine()),
                    (sr.getMacdHistogram()  == null ? Double.NaN : sr.getMacdHistogram()),

                    // 5) Stochastic
                    (sr.getStochasticK() == null ? Double.NaN : sr.getStochasticK()),
                    (sr.getStochasticD() == null ? Double.NaN : sr.getStochasticD()),

                    // 6) 52-week high boolean
                    sr.is52WeekHigh()
                );
            }
        } catch (IOException e) {
            throw new RuntimeException("Error writing TA CSV file: " + outputFilePath, e);
        }
    }

	public /*Map<String, List<CsvStockRecord>> */ void loadAllStockRecords() {
        File folder = new File(FOLDER_PATH);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + FOLDER_PATH);
        }
        System.out.println("Create a fixed thread pool of size: " +
                           Runtime.getRuntime().availableProcessors());

        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );

        ConcurrentMap<String, List<CsvStockRecord>> allData = new ConcurrentHashMap<>();
        List<Future<?>> futures = new ArrayList<>();

        for (File file : files) {
            String symbol = file.getName().replace(".csv", "");
            String outputFilePath = Paths.get(OUTPUT_PATH, symbol + ".csv")
                    .toAbsolutePath()
                    .toString();

            File outputFile = new File(outputFilePath);
            if (outputFile.exists()) {
                System.out.println("⚠️ Skipping " + symbol + " — already exists at " + outputFilePath);
                continue;
            }

            futures.add(executor.submit(() -> {
                List<CsvStockRecord> stockRecords = loadStockRecords(file.getAbsolutePath());
                writeStockPricesWithTA(symbol, outputFilePath, stockRecords);
                System.out.println("✅ Wrote TA CSV for: " + symbol + " -> " + outputFilePath);
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
        // return allData
    }

    private List<CsvStockRecord> loadStockRecords(String filePath) {
        String seriesName = new File(filePath).getName().replace(".csv", "");

        List<CsvStockRecord> prices = new LinkedList<>();
        List<Bar> bars = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Skip header
            br.readLine();
            int lineNum = 2;
            double lastRatio = 0.0;

            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (v.length < 7) {
                    throw new RuntimeException(
                        "Invalid line at row " + lineNum + ": " + line
                    );
                }

                LocalDate date = LocalDate.parse(v[0], formatter);
                double open = Double.parseDouble(v[1]);
                double high = Double.parseDouble(v[2]);
                double low  = Double.parseDouble(v[3]);
                double close = Double.parseDouble(v[4]);
                double adjClose = Double.parseDouble(v[5]);
                long volume = Long.parseLong(v[6]);

                boolean isOpenZero  = (open  == 0.0);
                boolean isHighZero  = (high  == 0.0);
                boolean isLowZero   = (low   == 0.0);
                boolean isCloseZero = (close == 0.0);
                boolean isAdjCloseZero = (adjClose == 0.0);

                if (isAdjCloseZero) {
                    if (!isCloseZero && lastRatio > EPSILON) {
                        System.out.printf("Patching adjClose for %s at %s using ratio %.6f%n",
                                          seriesName, v[0], lastRatio);
                        adjClose = close * lastRatio;
                    } else {
                        throw new IllegalArgumentException(
                            "adjClose is 0.0 for symbol " + seriesName + " at date " + v[0]
                        );
                    }
                }

                if (isOpenZero && isHighZero && isLowZero && isCloseZero) {
                    throw new IllegalArgumentException(
                        "All OHLC values are 0.0 for symbol " + seriesName + " at date " + v[0]
                    );
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
                double adjLow  = low  * ratio;

                if (adjOpen < 0 || adjHigh < 0 || adjLow < 0 || adjClose < 0) {
                    throw new IllegalArgumentException(String.format(
                        "Negative adjusted OHLC for %s at %s: open=%.4f, high=%.4f, low=%.4f, close=%.4f",
                        seriesName, v[0], adjOpen, adjHigh, adjLow, adjClose
                    ));
                }

                CsvStockRecord stockRecord = new CsvStockRecord();
                // Use setters
                stockRecord.setSymbol(seriesName);
                stockRecord.setDate(date);
                stockRecord.setOpen(adjOpen);
                stockRecord.setHigh(adjHigh);
                stockRecord.setLow(adjLow);
                stockRecord.setClose(adjClose);
                stockRecord.setVolume(volume);

                prices.add(stockRecord);

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

        // 3) Indicators
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

        RollingWindowHigh fiftyTwoWeekHigh = new RollingWindowHigh();
        
        // 4) Loop over each bar/price and fill in the derived indicator values
        for (int i = 0; i < prices.size(); i++) {
            CsvStockRecord rec = prices.get(i);

            // RSI requires at least 14 data points
            if (i >= 13) {
                rec.setRsi14(rsi.getValue(i).doubleValue());
            } else {
                rec.setRsi14(null);
            }

            // SMAs
            rec.setSma10(  (i >=  9) ? sma10.getValue(i).doubleValue() : null);
            rec.setSma20(  (i >= 19) ? sma20.getValue(i).doubleValue() : null);
            rec.setSma30(  (i >= 29) ? sma30.getValue(i).doubleValue() : null);
            rec.setSma50(  (i >= 49) ? sma50.getValue(i).doubleValue() : null);
            rec.setSma150( (i >=149) ? sma150.getValue(i).doubleValue() : null);
            rec.setSma200( (i >=199) ? sma200.getValue(i).doubleValue() : null);

            // EMAs
            rec.setEma20((i >= 19) ? ema20.getValue(i).doubleValue() : null);
            rec.setEma50((i >= 49) ? ema50.getValue(i).doubleValue() : null);

            // ATR
            rec.setVolatility((i >= 13) ? atr14.getValue(i).doubleValue() : null);

            // MACD
            Double macdVal       = (i >= 25) ? macd.getValue(i).doubleValue() : null;
            Double macdSignalVal = (i >= 34) ? macdSignal.getValue(i).doubleValue() : null;
            Double macdHistVal   = (macdVal != null && macdSignalVal != null)
                                   ? macdVal - macdSignalVal
                                   : null;

            rec.setMacdLine(macdVal);
            rec.setMacdSignalLine(macdSignalVal);
            rec.setMacdHistogram(macdHistVal);

            // Stochastic K & D
            rec.setStochasticK((i >= 13) ? stochK.getValue(i).doubleValue() : null);
            rec.setStochasticD((i >= 15) ? stochD.getValue(i).doubleValue() : null);

            validateIndicators(seriesName, rec.getDate(), rec);

            // Mark 52-week high
            boolean isHigh = fiftyTwoWeekHigh.updateAndCheckIsHigh(prices, i);
            rec.set52WeekHigh(isHigh);
        }

        System.out.println("Finished calculating TA: " + seriesName);
        return prices;
    }

    private void validateIndicators(String symbol, LocalDate date, CsvStockRecord rec) {
        // Example of getter usage:
        if (rec.getRsi14() != null && (rec.getRsi14() < 0 || rec.getRsi14() > 100)) {
            throw new IllegalArgumentException(symbol + " " + date + " -> Invalid RSI: " + rec.getRsi14());
        }

        if (rec.getVolatility() != null && rec.getVolatility() < 0) {
            throw new IllegalArgumentException(symbol + " " + date + " -> Negative ATR: " + rec.getVolatility());
        }

        checkPositive("SMA10",  symbol, date, rec.getSma10());
        checkPositive("SMA20",  symbol, date, rec.getSma20());
        checkPositive("SMA30",  symbol, date, rec.getSma30());
        checkPositive("SMA50",  symbol, date, rec.getSma50());
        checkPositive("SMA150", symbol, date, rec.getSma150());
        checkPositive("SMA200", symbol, date, rec.getSma200());

        checkPositive("EMA20",  symbol, date, rec.getEma20());
        checkPositive("EMA50",  symbol, date, rec.getEma50());

        checkPercentageRange("StochasticK", symbol, date, rec.getStochasticK());
        checkPercentageRange("StochasticD", symbol, date, rec.getStochasticD());
        
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
