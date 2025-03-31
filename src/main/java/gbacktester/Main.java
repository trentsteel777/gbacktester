package gbacktester;

import static java.util.Comparator.comparingDouble;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import domain.StockPrice;
import service.gbacktester.CsvLoaderService;
import strategy.Strategy;
import strategy.StrategyLoader;
import util.StopWatch;

public class Main {

	public static void main(String[] args) throws Exception {
		StopWatch sw = StopWatch.start();

		System.out.println("Create a fixed thread pool of size: " + Runtime.getRuntime().availableProcessors());
		ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

		//String dataDir = "C:\\Users\\sbrennan\\workspace\\gbacktester\\src\\main\\resources\\data\\calculated\\";
		String dataDir = "/home/trent/workspace/gbacktester/src/main/resources/data/calculated/";
		CsvLoaderService loader = new CsvLoaderService();
		TreeMap<LocalDate, Map<String, StockPrice>> timeline = loader.indexByDateAndSymbol(dataDir);

		List<String> symbols = getAllSymbolsFromFolder(dataDir);
		boolean isFewSymbols = symbols.size() <= 10;
		
		// Load all strategies for all symbols
		List<Strategy> strategies = new LinkedList<>();
		Set<Class<? extends Strategy>> strategyClasses = StrategyLoader.scanAnnotatedStrategies("strategy.impl");
		
		System.out.println("loaded csvs and scanned strategy classes - " + sw.lapInSeconds());
		for (String symbol : symbols) {
			for (Class<? extends Strategy> clazz : strategyClasses) {
				Strategy strat = clazz.getConstructor(String.class).newInstance(symbol);
				strategies.add(strat);
			}
		}
		System.out.println("created strategy objects - " + sw.lapInSeconds());

		// 2) For each day, time the processing, submit tasks to the executor, wait,
		// then print
		for (Entry<LocalDate, Map<String, StockPrice>> entry : timeline.entrySet()) {
			LocalDate today = entry.getKey();
			Map<String, StockPrice> marketData = entry.getValue();
			StopWatch dsw = StopWatch.start(); // Start timing the day's processing

			// 3) Submit work to the thread pool
			List<Future<?>> futures = new ArrayList<>();
			for (Strategy strategy : strategies) {
				StockPrice price = marketData.get(strategy.getSymbol());
				if (nonNull(price)) {
					// Submit a runnable that does both run() and calculateTotalPortfolioValue()
					futures.add(executor.submit(() -> {
						strategy.run(marketData);
						strategy.calculateTotalPortfolioValue(marketData, today);
					}));
				}
			}

			// 4) Wait for all tasks for this day to finish
			for (Future<?> f : futures) {
				f.get(); // ensures the day's work is done before proceeding
			}

			// Build traces if we have few symbols
			String traces = isFewSymbols ? marketData.values().stream()
					.flatMap(sp -> sp.getExplainList().stream())
					.collect(Collectors.joining(":")) : "";
			System.out.println(today + " → " + String.format("%.2f", dsw.stop()) + " → " + traces);
		}

		// After all days have run, shut down the executor
		executor.shutdown();

		// The rest of your final analytics stays the same
		if (isFewSymbols) {
			printStrategyAnalytics(strategies);
		}
		printOverallStrategyAnalytics(strategies);

		// Print total run time
		sw.printElapsed();
	}

	private static void printStrategyAnalytics(List<Strategy> strategies) {
		strategies.stream()
			.collect(groupingBy(Strategy::getSymbol)) // Group by symbol (e.g., SPY, ARKK, etc.)
			.forEach((symbol, stratList) -> {
				System.out.printf("%n=== %s ===%n", symbol);

				stratList.stream().sorted(comparingDouble(Strategy::getTotalPortfolioValue).reversed())
						.forEach(Main::print);
			});
	}

	private static void printOverallStrategyAnalytics(List<Strategy> strategies) {
		System.out.println("\n=== OVERALL STRATEGY ANALYTICS ===");

		// Group all strategies by their concrete class name
		Map<String, List<Strategy>> strategiesByType = strategies.stream()
				.collect(Collectors.groupingBy(s -> s.getClass().getSimpleName()));

		// We sort by average Sharpe ratio (descending) for overall ranking
		strategiesByType.entrySet().stream()
				.sorted((a, b) -> Double.compare(averageSharpe(b.getValue()), averageSharpe(a.getValue())))
				.forEach(entry -> {
					String strategyName = entry.getKey();
					List<Strategy> list = entry.getValue();

					// Compute aggregated stats
					double avgFinalValue = list.stream().mapToDouble(Strategy::getTotalPortfolioValue).average()
							.orElse(0);
					double avgSharpe = averageSharpe(list);
					double avgSortino = averageSortino(list);
					double avgCagr = averageCagr(list);
					double avgCalmar = averageCalmar(list);
					double avgWinRate = averageWinRate(list);
					double avgProfitFactor = averageProfitFactor(list);
					double avgDrawdown = list.stream().mapToDouble(Strategy::getMaxDrawdown).average().orElse(0);
					double avgGain = list.stream().mapToDouble(Strategy::getMaxGain).average().orElse(0);

					System.out.printf(
							// Strategy type, finalValue, Sharpe, etc.
							"%-35s : final=$%,13.0f | Sharpe=%5.2f | Sortino=%5.2f | CAGR=%3.0f%% | Calmar=%5.2f | WinRate=%5.2f%% | PF=%6.2f | Drawdown=%3.0f%% | Gain=%5.0f%%%n",
							strategyName, avgFinalValue, avgSharpe, avgSortino, avgCagr * 100, avgCalmar,
							avgWinRate * 100, avgProfitFactor, avgDrawdown * 100, avgGain);
				});
	}

	private static void print(Strategy strategy) {
		System.out.printf(
				"%-35s : $%,13.0f | Peak=$%,12.0f | Sharpe=%5.2f | Sortino=%5.2f | Cagr=%3.0f%% | Calmar=%5.2f | WinRate%%=%5.2f | PF=%6.2f | Buy=%3d | Sell=%3d | Total=%3d | mDraw=%3.0f%% | mGain=%5.0f%%%n",
				strategy.getClass().getSimpleName(), strategy.getTotalPortfolioValue(), strategy.getPeakValue(),
				strategy.getSharpeRatio(), strategy.getSortinoRatio(), strategy.getCagr() * 100,
				strategy.getCalmarRatio(), strategy.getWinRate() * 100, strategy.getProfitFactor(),
				strategy.getBuyCount(), strategy.getSellCount(), strategy.getTotalCount(),
				strategy.getMaxDrawdown() * 100, strategy.getMaxGain() * 100);
	}

	public static List<String> getAllSymbolsFromFolder(String folderPath) {
		File folder = new File(folderPath);
		File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".csv"));

		if (files == null) {
			throw new IllegalArgumentException("Folder not found or empty: " + folderPath);
		}

		return Arrays.stream(files).map(file -> file.getName().replace(".csv", "")).collect(Collectors.toList());
	}

	// AVERAGE of each stat across the list of strategies
	private static double averageSharpe(List<Strategy> strats) {
		return strats.stream().mapToDouble(Strategy::getSharpeRatio).average().orElse(0.0);
	}

	private static double averageSortino(List<Strategy> strats) {
		return strats.stream().mapToDouble(Strategy::getSortinoRatio).average().orElse(0.0);
	}

	private static double averageCagr(List<Strategy> strats) {
		return strats.stream().mapToDouble(Strategy::getCagr).average().orElse(0.0);
	}

	private static double averageCalmar(List<Strategy> strats) {
		return strats.stream().mapToDouble(Strategy::getCalmarRatio).average().orElse(0.0);
	}

	private static double averageWinRate(List<Strategy> strats) {
		return strats.stream().mapToDouble(Strategy::getWinRate).average().orElse(0.0);
	}

	private static double averageProfitFactor(List<Strategy> strats) {
		return strats.stream().mapToDouble(Strategy::getProfitFactor).average().orElse(0.0);
	}

}
