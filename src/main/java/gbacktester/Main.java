package gbacktester;

import static java.util.Comparator.comparingDouble;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import domain.StockPrice;
import service.gbacktester.CsvLoaderService;
import strategy.Strategy;
import strategy.StrategyLoader;
import util.StopWatch;

public class Main {
	
    public static void main(String[] args) throws Exception {
    	StopWatch sw = StopWatch.start();
    	
        String dataDir = "C:\\Users\\sbrennan\\workspace\\gbacktester\\src\\main\\resources\\data\\";
        CsvLoaderService loader = new CsvLoaderService();
        TreeMap<LocalDate, Map<String, StockPrice>> timeline = loader.indexByDateAndSymbol(dataDir);
        
        List<String> symbols = getAllSymbolsFromFolder(dataDir);
        
        List<Strategy> strategies = new ArrayList<>(10000);
        Set<Class<? extends Strategy>> strategyClasses = StrategyLoader.scanAnnotatedStrategies("strategy.impl");

        for (String symbol : symbols) {
        	for (Class<? extends Strategy> clazz : strategyClasses) {
        		Strategy strat = clazz.getConstructor(String.class).newInstance(symbol);
        		strategies.add(strat);
        	}
        }

        for(Entry<LocalDate, Map<String, StockPrice>> entry : timeline.entrySet()) {
        	LocalDate today = entry.getKey();
        	Map<String, StockPrice> marketData = entry.getValue();
        	
        	for(Strategy strategy : strategies) {
        		if(nonNull(marketData.get(strategy.getSymbol()))) {
        			strategy.run(marketData);
        			strategy.calculateTotalPortfolioValue(getPrices(marketData));
        		}
        	}
        	
            String traces = marketData.values().stream()
                .flatMap(sp -> sp.getExplainList().stream())
                .collect(Collectors.joining(":"));
            System.out.println(today + " → " + traces);
        }
        
        strategies.stream()
	        .collect(groupingBy(Strategy::getSymbol))  // Group by symbol (e.g., SPY, ARKK, etc.)
	        .forEach((symbol, stratList) -> {
	            System.out.printf("%n=== %s ===%n", symbol);
	
	            stratList.stream()
	                .sorted(comparingDouble(Strategy::getTotalPortfolioValue).reversed())
	                .forEach(Main::print);
	        });

        sw.printElapsed();
    }
	
    private static void print(Strategy strategy) {
        System.out.printf(
                "%-35s : $%,15.2f peak: $%,15.2f sharpe: %6.2f  buy: %3d  sell: %3d  total: %3d  mDraw: %6.2f%%  mGain: %8.2f%% %n",
                strategy.getClass().getSimpleName(), 
                strategy.getTotalPortfolioValue(), 
                strategy.getPeakValue(),
                strategy.getSharpeRatio(),
                strategy.getBuyCount(), 
                strategy.getSellCount(), 
                strategy.getTotalCount(),
                strategy.getMaxDrawdownPct() * 100, 
                strategy.getMaxGainPct() * 100
            );
    }
	private static Map<String, Double> getPrices(Map<String, StockPrice> marketData) {
		return marketData
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,               // symbol
						e -> e.getValue().getClose()     // StockPrice → close price
						));
	}
	
	public static List<String> getAllSymbolsFromFolder(String folderPath) {
	    File folder = new File(folderPath);
	    File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

	    if (files == null) {
	        throw new IllegalArgumentException("Folder not found or empty: " + folderPath);
	    }

	    return Arrays.stream(files)
	                 .map(file -> file.getName().replace(".csv", ""))
	                 .collect(Collectors.toList());
	}

}
