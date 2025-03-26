package gbacktester;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import domain.StockPrice;
import service.gbacktester.CsvLoaderService;
import strategy.Strategy;
import strategy.impl.BuyAndHoldWithTrailingStopStrategy;
import strategy.impl.BuyHoldStrategy;
import strategy.impl.CombinedLooseStrategy;
import strategy.impl.CombinedSmartStrategy;
import strategy.impl.DualMomentumStyleStrategy;
import strategy.impl.Rsi14Strategy;
import strategy.impl.Sma200RsiTrendStrategy;
import strategy.impl.Sma20Strategy;
import strategy.impl.SmaTrendWithMomentumStrategy;
import strategy.impl.SmaVolatilityFilterStrategy;
import strategy.impl.StackedEntryLooseExitStrategy;
import strategy.impl.TrendConfirmationStrategy;
import strategy.impl.VolatilityAwareStrategy;
import util.StopWatch;

public class Main {
    public static void main(String[] args) {
    	StopWatch sw = StopWatch.start();
    	
        String dataDir = "C:\\Users\\sbrennan\\workspace\\gbacktester\\src\\main\\resources\\data\\";
        CsvLoaderService loader = new CsvLoaderService();
        TreeMap<LocalDate, Map<String, StockPrice>> timeline = loader.indexByDateAndSymbol(dataDir);
        
        var watchlist = List.of("SPY");
        
        
        List<Strategy> strategies = List.of(
        		new BuyHoldStrategy(watchlist),
        		new Rsi14Strategy(watchlist),
        		new Sma20Strategy(watchlist),
        		new Sma200RsiTrendStrategy(watchlist),
        		new DualMomentumStyleStrategy(watchlist),
        		new SmaTrendWithMomentumStrategy(watchlist),
        		new SmaVolatilityFilterStrategy(watchlist),
        		new CombinedSmartStrategy(watchlist),
        		new CombinedLooseStrategy(watchlist),
        		new VolatilityAwareStrategy(watchlist),
        		new StackedEntryLooseExitStrategy(watchlist),
        		new BuyAndHoldWithTrailingStopStrategy(watchlist),
        		new TrendConfirmationStrategy(watchlist)
		);
        

        for(Entry<LocalDate, Map<String, StockPrice>> entry : timeline.entrySet()) {
        	LocalDate today = entry.getKey();
        	Map<String, StockPrice> marketData = entry.getValue();
        	
        	
        	for(Strategy strategy  : strategies) {
        		strategy.run(marketData);
        	}
        	
            String traces = marketData.values().stream()
                .flatMap(sp -> sp.getExplainList().stream())
                .collect(Collectors.joining(":"));
            System.out.println(today + " → " + traces);
        }
        
        Map<String, Double> marketPrices = getLastDayPrices(timeline);
        for(Strategy strategy  : strategies) {
    		strategy.calculateTotalPortfolioValue(marketPrices);
    		System.out.printf("%s : $%,.2f buy: %d sell: %d total: %d%n", 
    				strategy.getClass().getSimpleName(), strategy.getTotalPortfolioValue(), strategy.getBuyCount(), strategy.getSellCount(), strategy.getTotalCount());
    	}

        sw.printElapsed();
    }
    
    public static Map<String, Double> getLastDayPrices(TreeMap<LocalDate, Map<String, StockPrice>> timeline) {
    	Map.Entry<LocalDate, Map<String, StockPrice>> lastDayMarketData =  timeline.lastEntry();
    	return lastDayMarketData.getValue()
    		    .entrySet()
    		    .stream()
    		    .collect(Collectors.toMap(
    		        Map.Entry::getKey,               // symbol
    		        e -> e.getValue().getClose()     // StockPrice → close price
    		    ));

    	
    }
}
