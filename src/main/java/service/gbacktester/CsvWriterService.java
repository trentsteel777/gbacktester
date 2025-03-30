package service.gbacktester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
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
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import lombok.RequiredArgsConstructor;
import util.StopWatch;

public class CsvWriterService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    private static final String BASE_PATH = "/home/trent/workspace/gbacktester/src/main/resources/data/";
    private static final String FOLDER_PATH = BASE_PATH + "all/";
    private static final String OUTPUT_PATH = BASE_PATH + "calculated/";
    
  //  private static final String BASE_PATH = "C:\\Users\\sbrennan\\workspace\\gbacktester\\src\\main\\resources\\data\\";
  //  private static final String FOLDER_PATH = BASE_PATH + "all\\";
  //  private static final String OUTPUT_PATH = BASE_PATH + "calculated\\";
    
    // /home/trent/workspace/gbacktester/src/main/resources/data
    @RequiredArgsConstructor
    public static class StockRecord {
    	String symbol;
        LocalDate date;
        
        double open;     
        double high;     
        double low;     
        double close;     
        double adjClose;     
        long volume;
        
        Double rsi14;
        Double sma20;
        Double sma50;
        Double sma150;
        Double sma200;
        
        Double ema20;
        Double ema50;
        Double volatility; 
    }
	public static void main(String[] args) throws Exception {
		StopWatch sw = StopWatch.start();
		CsvWriterService writer = new CsvWriterService();
		writer.writePrecalculatedTechnicalAnalysis();
		sw.printElapsed();
	}
    
    public void writePrecalculatedTechnicalAnalysis() {
    	Map<String, List<StockRecord>> allStockRecords = loadAllStockRecords();
        // For each symbol, write out a new CSV
        for (Map.Entry<String, List<StockRecord>> entry : allStockRecords.entrySet()) {
            String symbol = entry.getKey();
            List<StockRecord> stockRecords = entry.getValue();

            // Build an output path (or name it however you'd like)
            String outputFilePath = OUTPUT_PATH + symbol + ".csv";

            writeStockPricesWithTA(outputFilePath, stockRecords);
            System.out.println("Wrote TA CSV for: " + symbol + " -> " + outputFilePath);
        }
    }
    
    private void writeStockPricesWithTA(String outputFilePath, List<StockRecord> records) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFilePath))) {
            // Write header
            writer.println("Date,Open,High,Low,Close,AdjClose,Volume,RSI14,SMA20,SMA50,SMA150,SMA200,EMA20,EMA50,Volatility");

            // Write each row
            for (StockRecord sr : records) {
                writer.printf("%s,%f,%f,%f,%f,%f,%d," +
                              "%f,%f,%f,%f,%f,%f,%f,%f%n",
                              // Date in dd/MM/yyyy format
                              sr.date.format(formatter),
                              sr.open,
                              sr.high,
                              sr.low,
                              sr.close,
                              sr.adjClose,
                              sr.volume,

                              // Handle possible nulls
                              sr.rsi14 == null ? Double.NaN : sr.rsi14,
                              sr.sma20 == null ? Double.NaN : sr.sma20,
                              sr.sma50 == null ? Double.NaN : sr.sma50,
                              sr.sma150 == null ? Double.NaN : sr.sma150,
                              sr.sma200 == null ? Double.NaN : sr.sma200,
                              sr.ema20 == null ? Double.NaN : sr.ema20,
                              sr.ema50 == null ? Double.NaN : sr.ema50,
                              sr.volatility == null ? Double.NaN : sr.volatility
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

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            br.readLine(); // skip header
            int lineNum = 2;

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
                StockRecord stockRecord = new StockRecord();
                stockRecord.symbol = seriesName;
                stockRecord.date = date;
                stockRecord.open = open;
                stockRecord.high = high;
                stockRecord.low = low;
                stockRecord.close = close;
                stockRecord.adjClose = adjClose;
                stockRecord.volume = volume;
                
                prices.add(stockRecord);

                ZonedDateTime endTime = date.atStartOfDay(ZoneId.systemDefault());
                Bar bar = new BaseBar(Duration.ofDays(1), endTime, adjClose, adjClose, adjClose, adjClose, volume);
                bars.add(bar);

                lineNum++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file: " + filePath, e);
        }

        BarSeries series = new BaseBarSeries(seriesName);
        for (Bar bar : bars) {
            series.addBar(bar);
        }

        ClosePriceIndicator close = new ClosePriceIndicator(series);
        RSIIndicator rsi = new RSIIndicator(close, 14);
        SMAIndicator sma20 = new SMAIndicator(close, 20);
        SMAIndicator sma50 = new SMAIndicator(close, 50);
        SMAIndicator sma150 = new SMAIndicator(close, 150);
        SMAIndicator sma200 = new SMAIndicator(close, 200);
        EMAIndicator ema20 = new EMAIndicator(close, 20);
        EMAIndicator ema50 = new EMAIndicator(close, 50);
        ATRIndicator atr = new ATRIndicator(series, 14); // Volatility indicator

        for (int i = 0; i < prices.size(); i++) {
            prices.get(i).rsi14 = i >= 13 ? rsi.getValue(i).doubleValue() : null;
            prices.get(i).sma20 = i >= 19 ? sma20.getValue(i).doubleValue() : null;
            prices.get(i).sma50 = i >= 49 ? sma50.getValue(i).doubleValue() : null;
            prices.get(i).sma150 = i >= 149 ? sma150.getValue(i).doubleValue() : null;
            prices.get(i).sma200 = i >= 199 ? sma200.getValue(i).doubleValue() : null;
            prices.get(i).ema20 = i >= 19 ? ema20.getValue(i).doubleValue() : null;
            prices.get(i).ema50 = i >= 49 ? ema50.getValue(i).doubleValue() : null;
            prices.get(i).volatility = i >= 13 ? atr.getValue(i).doubleValue() : null;
        }
        System.out.println("Finished calculating TA: " + seriesName );
        return prices;
    }
}
