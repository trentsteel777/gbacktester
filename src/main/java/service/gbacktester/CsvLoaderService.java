package service.gbacktester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBar;
import org.ta4j.core.BaseBarSeries;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;

import domain.StockPrice;

public class CsvLoaderService {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public TreeMap<LocalDate, Map<String, StockPrice>> indexByDateAndSymbol(String folderPath) {
        Map<String, List<StockPrice>> allStockData = loadNthStockPrices(folderPath);
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
        Map<String, List<StockPrice>> allData = new HashMap<>();

        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

        if (files == null || files.length == 0) {
            throw new RuntimeException("No CSV files found in directory: " + folderPath);
        }

        for (File file : files) {
            String symbol = file.getName().replace(".csv", "");
            List<StockPrice> prices = loadStockPrices(file.getAbsolutePath());
            allData.put(symbol, prices);
        }

        return allData;
    }

    public List<StockPrice> loadStockPrices(String filePath) {
        String seriesName = new File(filePath).getName().replace(".csv", "");

        List<StockPrice> prices = new LinkedList<>();
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
                double adjClose = Double.parseDouble(v[5]);
                long volume = Long.parseLong(v[6]);
                prices.add(new StockPrice(seriesName, date, adjClose, volume));

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
            prices.get(i).setRsi14(i >= 13 ? rsi.getValue(i).doubleValue() : null);
            prices.get(i).setSma20(i >= 19 ? sma20.getValue(i).doubleValue() : null);
            prices.get(i).setSma50(i >= 49 ? sma50.getValue(i).doubleValue() : null);
            prices.get(i).setSma150(i >= 149 ? sma150.getValue(i).doubleValue() : null);
            prices.get(i).setSma200(i >= 199 ? sma200.getValue(i).doubleValue() : null);
            prices.get(i).setEma20(i >= 19 ? ema20.getValue(i).doubleValue() : null);
            prices.get(i).setEma50(i >= 49 ? ema50.getValue(i).doubleValue() : null);
            prices.get(i).setVolatility(i >= 13 ? atr.getValue(i).doubleValue() : null);
        }



        return prices;
    }
}
