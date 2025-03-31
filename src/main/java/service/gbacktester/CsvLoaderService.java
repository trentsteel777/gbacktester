package service.gbacktester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import domain.StockPrice;

public class CsvLoaderService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TreeMap<LocalDate, Map<String, StockPrice>> indexByDateAndSymbol(String folderPath) {
        Map<String, List<StockPrice>> allStockData = loadAllStockPrices(folderPath);
        TreeMap<LocalDate, Map<String, StockPrice>> dateIndex = new TreeMap<>();

        for (Map.Entry<String, List<StockPrice>> entry : allStockData.entrySet()) {
            String symbol = entry.getKey();
            List<StockPrice> stockPrices = entry.getValue();

            for (StockPrice sp : stockPrices) {
                dateIndex
                    .computeIfAbsent(sp.getDate(), d -> new HashMap<>())
                    .put(symbol, sp);
            }
        }

        return dateIndex;
    }
    
    public void validatorCsvFiles(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + folderPath);
        }

        for (File file : files) {
        	String symbol = file.getName().replace(".csv", "");
        	try {
        		loadStockPrices(file.getAbsolutePath());
        	}
        	catch(Exception e) {
        		System.out.println(symbol);
        	}
        }
    }
    
    public Map<String, List<StockPrice>> loadNthStockPrices(String folderPath) {
        Map<String, List<StockPrice>> allData = new HashMap<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + folderPath);
        }

        // Sort the files to make the "every 4th" deterministic
        Arrays.sort(files);

        for (int i = 3; i < files.length; i += 4) { // Start at index 3 (the 4th file), step by 4
            File file = files[i];
            String symbol = file.getName().replace(".csv", "");
            List<StockPrice> prices = loadStockPrices(file.getAbsolutePath());
            allData.put(symbol, prices);
        }

        return allData;
    }

    
    public Map<String, List<StockPrice>> loadAllStockPrices(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + folderPath);
        }

        // A thread pool with as many threads as processors, for example:
        ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors()
        );

        // Use a ConcurrentMap for thread-safe writes
        ConcurrentMap<String, List<StockPrice>> allData = new ConcurrentHashMap<>();

        // List of Futures to wait on
        List<Future<?>> futures = new ArrayList<>();

        for (File file : files) {
            futures.add(executor.submit(() -> {
                String symbol = file.getName().replace(".csv", "");
                List<StockPrice> prices = loadStockPrices(file.getAbsolutePath());
                allData.put(symbol, prices);
            }));
        }

        // Wait for all tasks to finish
        for (Future<?> f : futures) {
            try {
                f.get();  // Blocks until the task completes or throws
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // restore interrupt flag
                throw new RuntimeException("Thread was interrupted", e);
            } catch (ExecutionException e) {
                // If the load task threw an exception, rethrow here
                throw new RuntimeException("Failed loading CSV in parallel", e);
            }
        }

        executor.shutdown();
        return allData;
    }

    public List<StockPrice> loadStockPrices(String filePath) {
        String seriesName = new File(filePath).getName().replace(".csv", "");

        List<StockPrice> prices = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            // Example CSV header (15 columns):
            // Date,Open,High,Low,Close,AdjClose,Volume,RSI14,SMA20,SMA50,SMA150,SMA200,EMA20,EMA50,Volatility
            // 31/10/2014,20.420000,20.420000,20.380000,20.380000,18.390000,2700,NaN,NaN,NaN,NaN,NaN,NaN,NaN,NaN

            String header = br.readLine(); // skip (or validate) header row
            String line;
            int lineNum = 2;

            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (v.length < 15) {
                    throw new RuntimeException("Invalid line at row " + lineNum + ": " + line);
                }

                LocalDate date = LocalDate.parse(v[0], formatter);
                double close    = Double.parseDouble(v[5]);
                long volume     = Long.parseLong(v[6]);

                Double rsi14      = parseDoubleOrNull(v[7]);
                Double sma20      = parseDoubleOrNull(v[8]);
                Double sma50      = parseDoubleOrNull(v[9]);
                Double sma150     = parseDoubleOrNull(v[10]);
                Double sma200     = parseDoubleOrNull(v[11]);
                Double ema20      = parseDoubleOrNull(v[12]);
                Double ema50      = parseDoubleOrNull(v[13]);
                Double volatility = parseDoubleOrNull(v[14]);

                // Create your StockPrice domain object, 
                // then set the fields (constructor or setters, depending on your implementation).
                StockPrice sp = new StockPrice(seriesName, date, close, volume);

                sp.setRsi14(rsi14);
                sp.setSma20(sma20);
                sp.setSma50(sma50);
                sp.setSma150(sma150);
                sp.setSma200(sma200);
                sp.setEma20(ema20);
                sp.setEma50(ema50);
                sp.setVolatility(volatility);

                prices.add(sp);
                lineNum++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file: " + filePath, e);
        }

        // No TA calculations here, since the file already has them.
        return prices;
    }
    
    /**
     * Safely parse a String to Double, treating empty string or "NaN" as null.
     */
    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isEmpty() || "NaN".equalsIgnoreCase(s)) {
            return null;
        }
        return Double.parseDouble(s);
    }
    
}
