
package gbacktester.util;

import java.io.File;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.MathContext;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import gbacktester.domain.CsvStockRecord;
import gbacktester.domain.CsvStockRecord;
import gbacktester.domain.fundamental.StockFundamentalData;
import gbacktester.service.CsvLoaderService;
import lombok.extern.slf4j.Slf4j;

/**
 * <h2>MagicFormulaCalculator</h2>
 *
 * <p>
 * Loads a <code>StockFundamentalData</code> JSON file, stitches together the
 * matching quarterly Balance‑Sheet and Income‑Statement rows, and computes the
 * Greenblatt Magic‑Formula metrics (Earnings Yield and Return on Capital) for
 * each period that has enough information.
 *
 * <p>
 * Any quarter that is missing <em>any</em> of the four critical inputs (EBIT,
 * Enterprise Value, Net Working Capital, Net Fixed Assets) is silently skipped
 * <strong>and</strong> logged at <code>WARNING</code> level so you can audit
 * the data‑quality gaps.
 * </p>
 */
@Slf4j
public final class MagicFormulaCalculator {


	private static final MathContext MC = MathContext.DECIMAL64;
	
	private List<CsvStockRecord> records;
	private TreeMap<LocalDate, CsvStockRecord> recordsMap;
	
	public MagicFormulaCalculator(List<CsvStockRecord> records) {
		this.records = records;
		this.recordsMap = records.stream()
				.collect(Collectors.toMap(CsvStockRecord::getDate, r -> r, (a, b) -> a, TreeMap::new));
		
		
		
	}
	
	private List<MagicFormulaMetrics> calculate(StockFundamentalData d) {
		var bsMap = Optional.ofNullable(d.getFinancials()).map(StockFundamentalData.Financials::getBalanceSheet)
				.map(StockFundamentalData.BalanceSheet::getQuarterly).orElse(Collections.emptyMap());

		var isMap = Optional.ofNullable(d.getFinancials()).map(StockFundamentalData.Financials::getIncomeStatement)
				.map(StockFundamentalData.IncomeStatement::getQuarterly).orElse(Collections.emptyMap());

		// Only process dates present in *both* statements
		var validDates = bsMap.keySet().stream().filter(isMap::containsKey)
				.collect(Collectors.toCollection(TreeSet::new));

		List<MagicFormulaMetrics> out = new ArrayList<>();
		for (String date : validDates) {
			StockFundamentalData.BalanceQuarter bq = bsMap.get(date);
			StockFundamentalData.IncomeQuarter iq = isMap.get(date);
			MagicFormulaMetrics m = buildMetrics(date, bq, iq, d);
			if (m != null)
				out.add(m);
		}
		return out;
	}

	/* ‑‑‑ internal helpers ‑‑‑ */
	/* ── 1️⃣ change the signature so we still have the root object ───────────── */
	private MagicFormulaMetrics buildMetrics(String date, StockFundamentalData.BalanceQuarter bq,
			StockFundamentalData.IncomeQuarter iq, StockFundamentalData root) {

		/*
		 * ── 2️⃣ translate the yyyy-MM-dd String into the LocalDate your ── timeline
		 * map is keyed on (timeline is a TreeMap<LocalDate, …>)
		 */
		LocalDate ld = LocalDate.parse(date);
		String code = root.getGeneral().getCode(); // e.g. "AAPL"
		CsvStockRecord px = mostRecentPrice(code, ld); 

		/* ── 3️⃣ build a per-quarter EV: EV = market-cap + net-debt */
		BigDecimal ev = getEv(bq, px); // ▼ replacement for root.getValuation()

		/* ── 4️⃣ the rest of the method is unchanged … */
//        BigDecimal ebit = safeEbit(iq);
		BigDecimal ebit = ebitTTM(date, root); // ⬅️ new TTM EBIT
		BigDecimal nfa = bq.getPropertyPlantAndEquipmentNet();
		BigDecimal nwc = safeNwc(bq);

		if (ebit == null || nfa == null || nwc == null || ev == null || ev.signum() == 0) {
			System.out.println(String.format("Skipping %s – missing data (EBIT=%s, NWC=%s, NFA=%s, EV=%s)", date, ebit,
					nwc, nfa, ev));
			return null;
		}
		BigDecimal ey = ebit.divide(ev, MC);
		BigDecimal ic = nwc.add(nfa);
		if (ic.signum() == 0) {
			log.warn("Skipping " + date + " – invested capital is zero");
			return null;
		}
		BigDecimal roc = ebit.divide(ic, MC);
		return new MagicFormulaMetrics(ld, ey, roc);
	}

	private CsvStockRecord mostRecentPrice(String code, LocalDate asOf) {
		Entry<LocalDate, CsvStockRecord> e = recordsMap.floorEntry(asOf); // ≤ asOf
		while (e != null) {
			CsvStockRecord px = e.getValue();
			if (px != null)
				return px; // found a usable price
			e = recordsMap.lowerEntry(e.getKey()); // strict predecessor (< current key)
		}
		return null; // walked off the map
	}

	/** Enterprise Value at quarter-end: price × shares + net-debt */
	private BigDecimal getEv(StockFundamentalData.BalanceQuarter bq, CsvStockRecord px) {

		if (bq == null || px == null)
			return null;

		// ➊ Market-capitalisation
		BigDecimal price = BigDecimal.valueOf(px.getClose());
		BigDecimal shares = bq.getCommonStockSharesOutstanding();
		if (shares == null)
			return null; // guard-rail
		BigDecimal marketCap = price.multiply(shares, MC);

		// ➋ Net-debt (if vendor already gives NetDebt just use it, otherwise build it)
		BigDecimal netDebt = Optional.ofNullable(bq.getNetDebt()).orElseGet(() -> {
			BigDecimal debtTotal = Optional.ofNullable(bq.getShortLongTermDebtTotal()).orElse(BigDecimal.ZERO)
					.add(Optional.ofNullable(bq.getLongTermDebtTotal()).orElse(BigDecimal.ZERO));
			BigDecimal cash = Optional.ofNullable(bq.getCashAndShortTermInvestments()).orElse(BigDecimal.ZERO);
			return debtTotal.subtract(cash); // debt – cash
		});

		return marketCap.add(netDebt);
	}

	private BigDecimal safeEbit(StockFundamentalData.IncomeQuarter iq) {
		if (iq == null)
			return null;
		if (iq.getEbit() != null)
			return iq.getEbit();
		// Fallback: operating income + net interest (if operatingIncome field exists
		// later)
		try {
			java.lang.reflect.Field f = iq.getClass().getDeclaredField("operatingIncome");
			f.setAccessible(true);
			BigDecimal opInc = (BigDecimal) f.get(iq);
			if (opInc != null) {
				BigDecimal interestIncome = getField(iq, "interestIncome");
				BigDecimal interestExpense = iq.getInterestExpense();
				BigDecimal netInt = Optional.ofNullable(interestIncome).orElse(BigDecimal.ZERO)
						.subtract(Optional.ofNullable(interestExpense).orElse(BigDecimal.ZERO));
				return opInc.add(netInt);
			}
		} catch (Exception ignore) {
		}
		return null;
	}

	/**
	 * Trailing-Twelve-Month EBIT
	 *
	 * @param asOf quarter-end in yyyy-MM-dd (same string you already pass to
	 *             buildMetrics)
	 * @param root the StockFundamentalData object that owns all quarters
	 * @return sum of EBIT for the four most-recent quarters ≤ asOf, or null if you
	 *         don’t have a full set / any quarter is missing EBIT
	 */
	private BigDecimal ebitTTM(String asOf, StockFundamentalData root) {
		// ➊ Pull *all* quarterly income statements
		Map<String, StockFundamentalData.IncomeQuarter> iqMap = Optional
				.ofNullable(root.getFinancials().getIncomeStatement())
				.map(StockFundamentalData.IncomeStatement::getQuarterly) // adjust to your real getter
				.orElse(null);

		if (iqMap == null || iqMap.isEmpty())
			return null;

		// ➋ Collect the 4 quarters ending with `asOf`
		List<BigDecimal> last4 = iqMap.entrySet().stream().filter(e -> e.getKey().compareTo(asOf) <= 0) // only past /
																										// present
																										// quarters
				.sorted(Map.Entry.<String, StockFundamentalData.IncomeQuarter>comparingByKey(Comparator.reverseOrder())) // newest
																															// first
				.limit(4).map(Map.Entry::getValue).map(this::safeEbit) // your existing per-quarter
																							// helper
				.collect(Collectors.toList());

		// ➌ Require all four quarters to be present & non-null
		if (last4.size() < 4 || last4.stream().anyMatch(Objects::isNull))
			return null;

		// ➍ Sum with the same MathContext you already use elsewhere
		return last4.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static BigDecimal safeNwc(StockFundamentalData.BalanceQuarter bq) {
		if (bq == null)
			return null;
		if (bq.getNetWorkingCapital() != null)
			return bq.getNetWorkingCapital();
		BigDecimal ca = bq.getTotalCurrentAssets();
		BigDecimal cli = bq.getTotalCurrentLiabilities();
		return (ca != null && cli != null) ? ca.subtract(cli) : null;
	}

	@SuppressWarnings("unchecked")
	private BigDecimal getField(Object obj, String field) {
		try {
			java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
			f.setAccessible(true);
			return (BigDecimal) f.get(obj);
		} catch (Exception e) {
			return null;
		}
	}

	/* ‑‑‑ DTO for the result set ‑‑‑ */
	public record MagicFormulaMetrics(LocalDate periodEnd, BigDecimal earningsYield, BigDecimal returnOnCapital) {
		@Override
		public String toString() {
			return periodEnd + "  EY=" + earningsYield + "  ROC=" + returnOnCapital;
		}
	}
	
	public List<MagicFormulaMetrics> loadAndCalculate(String symbol) throws Exception {
		Path jsonFile = Paths.get("src", "main", "resources", "data", "fundamentals", symbol + ".json").toAbsolutePath();
		try (Reader r = Files.newBufferedReader(jsonFile, StandardCharsets.UTF_8)) {
			Gson gson = new GsonBuilder().create();
			StockFundamentalData d = gson.fromJson(r, StockFundamentalData.class);
			return calculate(d);
		}
	}
	
    private Map<String, List<MagicFormulaMetrics>> loadFundamental(List<String> watchlist, String folderPath) throws Exception {
    	String dataDir = Paths.get("src", "main", "resources", "data", "calculated").toAbsolutePath().toString();
    	
    	File folder = new File(folderPath);
    	File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
    	
    	if (files == null || files.length == 0) {
    		throw new RuntimeException("No json files found in directory: " + folderPath);
    	}
    	
    	// Sort the files to make the "every 4th" deterministic
    	Arrays.sort(files);
    	
    	Map<String, List<MagicFormulaMetrics>> result = new LinkedHashMap<>();
    	for (int i = 0; i < files.length; i++) { // Start at index 3 (the 4th file), step by 4
    		File file = files[i];
    		String symbol = file.getName().replace(".json", "");
    		System.out.println(symbol);
    		Path json = Path.of("/home/trent/workspace/gbacktester/src/main/resources/data/fundamentals/AAPL.json");
    		List<MagicFormulaMetrics> list = loadAndCalculate(symbol);
    		list.forEach(System.out::println);
    		result.put(symbol, list);
    	}
        return result;
    }
	
	public static void main(String[] args) throws Exception {
//		String folderPath = "/home/trent/workspace/gbacktester/src/main/resources/data/fundamentals/";
//		List<String> watchlist = List.of("AAPL");
//		MagicFormulaCalculator mfc = new MagicFormulaCalculator();
//		
//		Map<String, List<MagicFormulaMetrics>> m = mfc.loadFundamental(watchlist, folderPath);
//		for(Map.Entry<String, List<MagicFormulaMetrics>> e : m.entrySet()) {
//			System.out.println(e.getKey() + " : " + e.getValue());			
//		}
		
	}
}
