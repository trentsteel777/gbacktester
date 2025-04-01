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
import java.util.stream.Collectors;

import domain.StockPrice;

public class CsvLoaderService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * A watchlist of symbols we want to load (e.g., ["SPY", "AAPL"]).
     * If null or empty, we load ALL CSV files from the folder.
     */
    private final List<String> watchList;

    private final String folderPath;

    public List<String> getWatchList() {
    	if(watchList != null) {
    		return watchList;
    	}
    	else {
    		return getAllSymbolsFromFolder();
    	}
	}

	// -------------------------------------------------------------------------
    // 1) Default constructor: no watchlist => loads ALL files
    // -------------------------------------------------------------------------
    public CsvLoaderService(String folderPath) {
    	this.folderPath = folderPath;
        this.watchList = null; // Means "no filtering"
    }

    // -------------------------------------------------------------------------
    // 2) Constructor accepting a watchlist => load only the listed symbols
    // -------------------------------------------------------------------------
    public CsvLoaderService(String folderPath, List<String> watchList) {
    	this.folderPath = folderPath;
        this.watchList = (watchList == null || watchList.isEmpty())
            ? null // treat null/empty watchlist as "no filtering"
            : new ArrayList<>(watchList); // store a copy
    }
    
    public TreeMap<LocalDate, Map<String, StockPrice>> indexByDateAndSymbol() {
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
    
    private Map<String, List<StockPrice>> loadNthStockPrices(String folderPath) {
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

    
    private Map<String, List<StockPrice>> loadAllStockPrices(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + folderPath);
        }

        // If we have a watchlist, filter 'files' so we only load what's in the watchlist,
        // and also print any watchlist symbol that doesn't exist in the folder.
        if (watchList != null && !watchList.isEmpty()) {
            // Gather the set of symbols actually present as CSV files
            // e.g., "SPY.csv" -> "SPY"
            Map<String, File> symbolFileMap = new HashMap<>();
            for (File file : files) {
                String symbolFound = file.getName().replace(".csv", "");
                symbolFileMap.put(symbolFound, file);
            }

            // Print out any symbol from the watchlist that isn't in the folder
            for (String desiredSymbol : watchList) {
                if (!symbolFileMap.containsKey(desiredSymbol)) {
                    System.out.println("Watchlist Symbol Missing: " + desiredSymbol + ".csv");
                }
            }

            // Filter the files array to keep only those that match the watchlist
            List<File> filtered = new ArrayList<>();
            for (String desiredSymbol : watchList) {
                if (symbolFileMap.containsKey(desiredSymbol)) {
                    filtered.add(symbolFileMap.get(desiredSymbol));
                }
            }

            if (filtered.isEmpty()) {
                throw new RuntimeException("No watchlist files found in folder: " + folderPath);
            }

            files = filtered.toArray(File[]::new);
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

    private List<StockPrice> loadStockPrices(String filePath) {
        String seriesName = new File(filePath).getName().replace(".csv", "");
        List<StockPrice> prices = new LinkedList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String header = br.readLine(); // Skip the CSV header
            String line;
            int lineNum = 2;

            while ((line = br.readLine()) != null) {
                String[] v = line.split(",");
                if (v.length < 21) {
                    throw new RuntimeException("Invalid line at row " + lineNum + ": " + line);
                }

                LocalDate date     = LocalDate.parse(v[0], formatter);
                double close       = Double.parseDouble(v[4]); // AdjClose as actual close
                long volume        = Long.parseLong(v[5]);

                // TA values (null-safe)
                Double rsi14          = parseDoubleOrNull(v[6]);
                Double sma10          = parseDoubleOrNull(v[7]);
                Double sma20          = parseDoubleOrNull(v[8]);
                Double sma30          = parseDoubleOrNull(v[9]);
                Double sma50          = parseDoubleOrNull(v[10]);
                Double sma150         = parseDoubleOrNull(v[11]);
                Double sma200         = parseDoubleOrNull(v[12]);
                Double ema20          = parseDoubleOrNull(v[13]);
                Double ema50          = parseDoubleOrNull(v[14]);
                Double volatility     = parseDoubleOrNull(v[15]);
                Double macdLine       = parseDoubleOrNull(v[16]);
                Double macdSignalLine = parseDoubleOrNull(v[17]);
                Double macdHistogram  = parseDoubleOrNull(v[18]);
                Double stochasticK    = parseDoubleOrNull(v[19]);
                Double stochasticD    = parseDoubleOrNull(v[20]);

                // Construct object and assign indicators
                StockPrice sp = new StockPrice(seriesName, date, close, volume);

                sp.setRsi14(rsi14);
                sp.setSma10(sma10);
                sp.setSma20(sma20);
                sp.setSma30(sma30);
                sp.setSma50(sma50);
                sp.setSma150(sma150);
                sp.setSma200(sma200);
                sp.setEma20(ema20);
                sp.setEma50(ema50);
                sp.setVolatility(volatility);
                sp.setMacdLine(macdLine);
                sp.setMacdSignalLine(macdSignalLine);
                sp.setMacdHistogram(macdHistogram);
                sp.setStochasticK(stochasticK);
                sp.setStochasticD(stochasticD);

                prices.add(sp);
                lineNum++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load file: " + filePath, e);
        }

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
    
    private List<String> getAllSymbolsFromFolder() {
		File folder = new File(folderPath);
		File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

		if (files == null) {
			throw new IllegalArgumentException("Folder not found or empty: " + folderPath);
		}

		return Arrays.stream(files).map(file -> file.getName().replace(".csv", "")).collect(Collectors.toList());
	}
    
}
