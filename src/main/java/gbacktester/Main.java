package gbacktester;

import static java.util.Comparator.comparingDouble;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;

import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

		String dataDir = Paths.get("src", "main", "resources", "data", "calculated")
                .toAbsolutePath()
                .toString();
		
		List<String> watchlist = 
			 //getSpyWatchlist(); 
			 //getFavWatchlist();
			//getEtfWatchlist();
			getBadWatchlist();

		CsvLoaderService loader = new CsvLoaderService(dataDir,watchlist);
		TreeMap<LocalDate, Map<String, StockPrice>> timeline = loader.indexByDateAndSymbol();

		List<String> symbols = loader.getWatchList();
		boolean isFewSymbols = symbols.size() <= 100;
		
		// Load all strategies for all symbols
		List<Strategy> strategies = new LinkedList<>();
		Set<Class<? extends Strategy>> strategyClasses = 
				/*
				 * Set.of(PhilTownLenientStrategy.class, PhilTownStrictStrategy.class,
				 * Sma50_200CrossoverStrategy.class, Sma20_200CrossoverStrategy.class,
				 * Rsi14Strategy.class, BuyHoldStrategy.class);
				 */
				StrategyLoader.scanAnnotatedStrategies("strategy.impl");
		
		System.out.println("loaded csvs and scanned " + strategyClasses.size() + " strategy classes - " + sw.lapInSeconds());
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
						if(dsw.lapInSeconds() > 3) {
							System.out.println(strategy.getClass().getSimpleName() + " → " + strategy.getSymbol());
						}
					}));
				}
			}

			// 4) Wait for all tasks for this day to finish
			for (Future<?> f : futures) {
				f.get(); // ensures the day's work is done before proceeding
			}

			// Build traces if we have few symbols
			String traces = /*
							 * isFewSymbols ? marketData.values().stream() .flatMap(sp ->
							 * sp.getExplainList().stream()) .collect(Collectors.joining(":")) :
							 */ "";
			System.out.println(today + " → " + String.format("%.2f", dsw.stop()) + " → " + traces);
		}

		// After all days have run, shut down the executor
		executor.shutdown();

		// The rest of your final analytics stays the same
		if (isFewSymbols) {
			printStrategyAnalytics(strategies);
		}
		else {
			List<Strategy> spyStrategies = strategies.stream().filter(s -> "SPY".equals(s.getSymbol())).toList();
			printStrategyAnalytics(spyStrategies);
		}
		
		printOverallStrategyAnalytics(strategies);
		// Print total run time
		sw.printElapsed();
	}

	private static void printStrategyAnalytics(List<Strategy> strategies) {
	    NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US);
	    currencyFormatter.setMaximumFractionDigits(0);
	    currencyFormatter.setMinimumFractionDigits(0);
		strategies.stream()
			.collect(groupingBy(Strategy::getSymbol)) // Group by symbol (e.g., SPY, ARKK, etc.)
			.forEach((symbol, stratList) -> {
				Strategy strat = stratList.getFirst();
				System.out.printf("%n=== %s - %s - %s ===%n", symbol, strat.getStartDate(), currencyFormatter.format(strat.getCashDeposits()));
				
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
				//.sorted((a, b) -> Double.compare(averageSharpe(b.getValue()), averageSharpe(a.getValue())))
				.sorted((a, b) -> Double.compare(averagePortfolioValue(b.getValue()), averagePortfolioValue(a.getValue())))
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
							"%-35s : final=$%,13.0f | Sharpe=%5.2f | Sortino=%7.2f | CAGR=%3.0f%% | Calmar=%10.2f | WinRate=%5.2f%% | PF=%6.2f | Drawdown=%3.0f%% | Gain=%5.0f%%%n",
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

	// AVERAGE of each stat across the list of strategies
	private static double averagePortfolioValue(List<Strategy> strats) {
		return strats.stream().mapToDouble(Strategy::getTotalPortfolioValue).average().orElse(0.0);
	}

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
	
	private static List<String> getSpyWatchlist() {
		String spy ="A,AAPL,ABBV,ABNB,ABT,ACGL,ACN,ADBE,ADI,ADM,ADP,ADSK,AEE,AEP,AES,AFL,AIG,AIZ,AJG,AKAM,ALB,ALGN,ALL,ALLE,AMAT,AMCR,AMD,AME,AMGN,AMP,AMT,AMZN,ANET,ANSS,AON,AOS,APA,APD,APH,APO,APTV,ARE,ATO,AVB,AVGO,AVY,AWK,AXON,AXP,AZO,BA,BAC,BALL,BAX,BBY,BDX,BEN,BF.B,BG,BIIB,BK,BKNG,BKR,BLDR,BLK,BMY,BR,BRK.B,BRO,BSX,BX,BXP,C,CAG,CAH,CARR,CAT,CB,CBOE,CBRE,CCI,CCL,CDNS,CDW,CEG,CF,CFG,CHD,CHRW,CHTR,CI,CINF,CL,CLX,CMCSA,CME,CMG,CMI,CMS,CNC,CNP,COF,COO,COP,COR,COST,CPAY,CPB,CPRT,CPT,CRL,CRM,CRWD,CSCO,CSGP,CSX,CTAS,CTRA,CTSH,CTVA,CVS,CVX,CZR,D,DAL,DASH,DAY,DD,DE,DECK,DELL,DFS,DG,DGX,DHI,DHR,DIS,DLR,DLTR,DOC,DOV,DOW,DPZ,DRI,DTE,DUK,DVA,DVN,DXCM,EA,EBAY,ECL,ED,EFX,EG,EIX,EL,ELV,EMN,EMR,ENPH,EOG,EPAM,EQIX,EQR,EQT,ERIE,ES,ESS,ETN,ETR,EVRG,EW,EXC,EXE,EXPD,EXPE,EXR,F,FANG,FAST,FCX,FDS,FDX,FE,FFIV,FI,FICO,FIS,FITB,FOX,FOXA,FRT,FSLR,FTNT,FTV,GD,GDDY,GE,GEHC,GEN,GEV,GILD,GIS,GL,GLW,GM,GNRC,GOOG,GOOGL,GPC,GPN,GRMN,GS,GWW,HAL,HAS,HBAN,HCA,HD,HES,HIG,HII,HLT,HOLX,HON,HPE,HPQ,HRL,HSIC,HST,HSY,HUBB,HUM,HWM,IBM,ICE,IDXX,IEX,IFF,INCY,INTC,INTU,INVH,IP,IPG,IQV,IR,IRM,ISRG,IT,ITW,IVZ,J,JBHT,JBL,JCI,JKHY,JNJ,JNPR,JPM,K,KDP,KEY,KEYS,KHC,KIM,KKR,KLAC,KMB,KMI,KMX,KO,KR,KVUE,L,LDOS,LEN,LH,LHX,LII,LIN,LKQ,LLY,LMT,LNT,LOW,LRCX,LULU,LUV,LVS,LW,LYB,LYV,MA,MAA,MAR,MAS,MCD,MCHP,MCK,MCO,MDLZ,MDT,MET,META,MGM,MHK,MKC,MKTX,MLM,MMC,MMM,MNST,MO,MOH,MOS,MPC,MPWR,MRK,MRNA,MS,MSCI,MSFT,MSI,MTB,MTCH,MTD,MU,NCLH,NDAQ,NDSN,NEE,NEM,NFLX,NI,NKE,NOC,NOW,NRG,NSC,NTAP,NTRS,NUE,NVDA,NVR,NWS,NWSA,NXPI,O,ODFL,OKE,OMC,ON,ORCL,ORLY,OTIS,OXY,PANW,PARA,PAYC,PAYX,PCAR,PCG,PEG,PEP,PFE,PFG,PG,PGR,PH,PHM,PKG,PLD,PLTR,PM,PNC,PNR,PNW,PODD,POOL,PPG,PPL,PRU,PSA,PSX,PTC,PWR,PYPL,QCOM,RCL,REG,REGN,RF,RJF,RL,RMD,ROK,ROL,ROP,ROST,RSG,RTX,RVTY,SBAC,SBUX,SCHW,SHW,SJM,SLB,SMCI,SNA,SNPS,SO,SOLV,SPG,SPGI,SRE,STE,STLD,STT,STX,STZ,SW,SWK,SWKS,SYF,SYK,SYY,T,TAP,TDG,TDY,TECH,TEL,TER,TFC,TGT,TJX,TKO,TMO,TMUS,TPL,TPR,TRGP,TRMB,TROW,TRV,TSCO,TSLA,TSN,TT,TTWO,TXN,TXT,TYL,UAL,UBER,UDR,UHS,ULTA,UNH,UNP,UPS,URI,USB,V,VICI,VLO,VLTO,VMC,VRSK,VRSN,VRTX,VST,VTR,VTRS,VZ,WAB,WAT,WBA,WBD,WDAY,WDC,WEC,WELL,WFC,WM,WMB,WMT,WRB,WSM,WST,WTW,WY,WYNN,XEL,XOM,XYL,YUM,ZBH,ZBRA,ZTS";
		String[] spyArr = spy.split(",");
		List<String> spyList = new LinkedList<>();
		for(String symbol : spyArr) {
			spyList.add(symbol);
		}
		return spyList;
	}
	private static List<String> getFavWatchlist() {
		 return new ArrayList<>(Arrays.asList(
				    "SPY", "NVDA", "SHOP", "COIN", "SBUX", "XLF",
				     "MNDY", "PANW", "PLTR", "TSLA", "AAPL", 
				    "ZM", "AMZN", "ABNB", "SNOW", "COKE"
				));
	}
	private static List<String> getBadWatchlist() {
		return new ArrayList<>(Arrays.asList(
			    "GE",    // General Electric
			    "M",     // Macy's
			    "UA",    // Under Armour
			    "KHC",   // Kraft Heinz
			    "CCL",   // Carnival Corporation
			    "BIIB",  // Biogen
			    "CVS",   // CVS Health
			    "NOV",   // NOV Inc.
			    "PARA",  // Paramount Global
			    "PRGO",  // Perrigo Company
			    "SLB",   // Schlumberger
			    "WBA",   // Walgreens Boots Alliance
			    "LUMN",  // Lumen Technologies
			    "PCG",   // Pacific Gas & Electric
			    "F",     // Ford
			    "XOM",   // Exxon Mobil
			    "IBM",   // IBM
			    "T",     // AT&T
			    "KO",    // Coca-Cola
			    "INTC"   // Intel
				));
	}
	private static List<String> getEtfWatchlist() {
		return new ArrayList<>(Arrays.asList(
			    // Broad Market
			    "SPY",   // S&P 500
			    "VOO",   // S&P 500 (Vanguard)
			    "VTI",   // Total Stock Market
			    "QQQ",   // Nasdaq-100
			    "DIA",   // Dow Jones Industrial Average

			    // Sector SPDR ETFs
			    "XLF",   // Financials
			    "XLK",   // Technology
			    "XLV",   // Health Care
			    "XLY",   // Consumer Discretionary
			    "XLP",   // Consumer Staples
			    "XLE",   // Energy
			    "XLI",   // Industrials
			    "XLRE",  // Real Estate
			    "XLU",   // Utilities
			    "XLB",   // Materials
			    "XTN",   // Transportation (SPDR)

			    // Dividend & Value ETFs
			    "VYM",   // Vanguard High Dividend Yield
			    "SCHD",  // Schwab U.S. Dividend Equity
			    "DVY",   // iShares Select Dividend ETF
			    "VTV",   // Vanguard Value
			    "DGRO",  // iShares Core Dividend Growth

			    // Growth ETFs
			    "VUG",   // Vanguard Growth
			    "IWF",   // iShares Russell 1000 Growth

			    // International / Emerging Markets
			    "VXUS",  // Vanguard Total International Stock
			    "VEA",   // Vanguard FTSE Developed Markets
			    "VWO",   // Vanguard FTSE Emerging Markets
			    "EFA",   // iShares MSCI EAFE
			    "EEM",   // iShares MSCI Emerging Markets

			    // Bond ETFs
			    "BND",   // Vanguard Total Bond Market
			    "AGG",   // iShares Core U.S. Aggregate Bond
			    "TLT",   // iShares 20+ Year Treasury Bond

			    // Thematic / Quality
			    "QUAL",  // iShares MSCI USA Quality Factor
			    "MTUM",  // iShares MSCI USA Momentum Factor
			    "ARKK"   // ARK Innovation ETF (more volatile but popular)
				));
	}

}
