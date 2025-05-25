package gbacktester.domain.fundamental;

import com.google.gson.annotations.SerializedName;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * <h2>StockFundamentalData – pure Gson/Lombok model</h2>
 *
 * <p>Every field, including nested ones, is explicitly linked to its JSON
 * property via {@link SerializedName}.  That means you can deserialize with a
 * vanilla Gson instance:
 *
 * <pre>StockFundamentalData d = new Gson().fromJson(reader, StockFundamentalData.class);</pre>
 *
 * <p>The class is delivered as a single source file with <code>static</code>
 * inner classes.  Feel free to split it into separate files if that better
 * suits your code‑base.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockFundamentalData {

    /* ─────────────────────────────────  Top level  ───────────────────────────────── */

    @SerializedName("General")             private General general;
    @SerializedName("Highlights")          private Highlights highlights;
    @SerializedName("Valuation")           private Valuation valuation;
    @SerializedName("SharesStats")         private SharesStats sharesStats;
    @SerializedName("Technicals")          private Technicals technicals;
    @SerializedName("SplitsDividends")     private SplitsDividends splitsDividends;
    @SerializedName("AnalystRatings")      private AnalystRatings analystRatings;
    @SerializedName("Holders")             private Holders holders;
    @SerializedName("InsiderTransactions") private Map<String, InsiderTransaction> insiderTransactions;
    @SerializedName("ESGScores")           private ESGScores esgScores;
    @SerializedName("Earnings")            private Earnings earnings;
    @SerializedName("Financials")          private Financials financials;

    /* ───────────────────────────────  General  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class General {
        @SerializedName("Code")                  private String code;
        @SerializedName("Type")                  private String type;
        @SerializedName("Name")                  private String name;
        @SerializedName("Exchange")              private String exchange;
        @SerializedName("CurrencyCode")          private String currencyCode;
        @SerializedName("CurrencyName")          private String currencyName;
        @SerializedName("CurrencySymbol")        private String currencySymbol;
        @SerializedName("CountryName")           private String countryName;
        @SerializedName("CountryISO")            private String countryISO;
        @SerializedName("OpenFigi")              private String openFigi;
        @SerializedName("ISIN")                  private String isin;
        @SerializedName("LEI")                   private String lei;
        @SerializedName("PrimaryTicker")         private String primaryTicker;
        @SerializedName("CUSIP")                 private String cusip;
        @SerializedName("CIK")                   private String cik;
        @SerializedName("EmployerIdNumber")      private String employerIdNumber;
        @SerializedName("FiscalYearEnd")         private String fiscalYearEnd;
        @SerializedName("IPODate")               private String ipoDate;
        @SerializedName("InternationalDomestic") private String internationalDomestic;
        @SerializedName("Sector")                private String sector;
        @SerializedName("Industry")              private String industry;
        @SerializedName("GicSector")             private String gicSector;
        @SerializedName("GicGroup")              private String gicGroup;
        @SerializedName("GicIndustry")           private String gicIndustry;
        @SerializedName("GicSubIndustry")        private String gicSubIndustry;
        @SerializedName("HomeCategory")          private String homeCategory;
        @SerializedName("IsDelisted")            private Boolean isDelisted;
        @SerializedName("Description")           private String description;
        @SerializedName("Address")               private String address;
        @SerializedName("AddressData")           private AddressData addressData;
        @SerializedName("Listings")              private Map<String, Listing> listings;
        @SerializedName("Officers")              private Map<String, Officer> officers;
        @SerializedName("Phone")                 private String phone;
        @SerializedName("WebURL")                private String webURL;
        @SerializedName("LogoURL")               private String logoURL;
        @SerializedName("FullTimeEmployees")     private Long fullTimeEmployees;
        @SerializedName("UpdatedAt")             private String updatedAt;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AddressData {
        @SerializedName("Street")  private String street;
        @SerializedName("City")    private String city;
        @SerializedName("State")   private String state;
        @SerializedName("Country") private String country;
        @SerializedName("ZIP")     private String zip;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Listing {
        @SerializedName("Code")     private String code;
        @SerializedName("Exchange") private String exchange;
        @SerializedName("Name")     private String name;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Officer {
        @SerializedName("Name")     private String name;
        @SerializedName("Title")    private String title;
        @SerializedName("YearBorn") private String yearBorn;
    }

    /* ───────────────────────────────  Highlights  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Highlights {
        @SerializedName("MarketCapitalization")         private BigDecimal marketCapitalization;
        @SerializedName("MarketCapitalizationMln")      private BigDecimal marketCapitalizationMln;
        @SerializedName("EBITDA")                        private BigDecimal ebitda;
        @SerializedName("PERatio")                       private BigDecimal peRatio;
        @SerializedName("PEGRatio")                      private BigDecimal pegRatio;
        @SerializedName("WallStreetTargetPrice")         private BigDecimal wallStreetTargetPrice;
        @SerializedName("BookValue")                     private BigDecimal bookValue;
        @SerializedName("DividendShare")                 private BigDecimal dividendShare;
        @SerializedName("DividendYield")                 private BigDecimal dividendYield;
        @SerializedName("EarningsShare")                 private BigDecimal earningsShare;
        @SerializedName("EPSEstimateCurrentYear")        private BigDecimal epsEstimateCurrentYear;
        @SerializedName("EPSEstimateNextYear")           private BigDecimal epsEstimateNextYear;
        @SerializedName("EPSEstimateNextQuarter")        private BigDecimal epsEstimateNextQuarter;
        @SerializedName("EPSEstimateCurrentQuarter")     private BigDecimal epsEstimateCurrentQuarter;
        @SerializedName("MostRecentQuarter")             private String mostRecentQuarter;
        @SerializedName("ProfitMargin")                  private BigDecimal profitMargin;
        @SerializedName("OperatingMarginTTM")            private BigDecimal operatingMarginTTM;
        @SerializedName("ReturnOnAssetsTTM")             private BigDecimal returnOnAssetsTTM;
        @SerializedName("ReturnOnEquityTTM")             private BigDecimal returnOnEquityTTM;
        @SerializedName("RevenueTTM")                    private BigDecimal revenueTTM;
        @SerializedName("RevenuePerShareTTM")            private BigDecimal revenuePerShareTTM;
        @SerializedName("QuarterlyRevenueGrowthYOY")     private BigDecimal quarterlyRevenueGrowthYOY;
        @SerializedName("GrossProfitTTM")                private BigDecimal grossProfitTTM;
        @SerializedName("DilutedEpsTTM")                 private BigDecimal dilutedEpsTTM;
        @SerializedName("QuarterlyEarningsGrowthYOY")    private BigDecimal quarterlyEarningsGrowthYOY;
    }

    /* ───────────────────────────────  Valuation  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Valuation {
        @SerializedName("TrailingPE")             private BigDecimal trailingPE;
        @SerializedName("ForwardPE")              private BigDecimal forwardPE;
        @SerializedName("PriceSalesTTM")          private BigDecimal priceSalesTTM;
        @SerializedName("PriceBookMRQ")           private BigDecimal priceBookMRQ;
        @SerializedName("EnterpriseValue")        private BigDecimal enterpriseValue;
        @SerializedName("EnterpriseValueRevenue") private BigDecimal enterpriseValueRevenue;
        @SerializedName("EnterpriseValueEbitda")  private BigDecimal enterpriseValueEbitda;
    }

    /* ───────────────────────────────  Shares Stats  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SharesStats {
        @SerializedName("SharesOutstanding")         private BigDecimal sharesOutstanding;
        @SerializedName("SharesFloat")               private BigDecimal sharesFloat;
        @SerializedName("PercentInsiders")           private BigDecimal percentInsiders;
        @SerializedName("PercentInstitutions")       private BigDecimal percentInstitutions;
        @SerializedName("SharesShort")               private BigDecimal sharesShort;
        @SerializedName("SharesShortPriorMonth")     private BigDecimal sharesShortPriorMonth;
        @SerializedName("ShortRatio")                private BigDecimal shortRatio;
        @SerializedName("ShortPercentOutstanding")   private BigDecimal shortPercentOutstanding;
        @SerializedName("ShortPercentFloat")         private BigDecimal shortPercentFloat;
    }

    /* ───────────────────────────────  Technicals  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Technicals {
        @SerializedName("Beta")        private BigDecimal beta;
        @SerializedName("52WeekHigh")  private BigDecimal week52High;
        @SerializedName("52WeekLow")   private BigDecimal week52Low;
        @SerializedName("50DayMA")     private BigDecimal day50MA;
        @SerializedName("200DayMA")    private BigDecimal day200MA;
    }

    /* ───────────────────────────────  Splits & Dividends  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class SplitsDividends {
        @SerializedName("ForwardAnnualDividendRate")  private BigDecimal forwardAnnualDividendRate;
        @SerializedName("ForwardAnnualDividendYield") private BigDecimal forwardAnnualDividendYield;
        @SerializedName("PayoutRatio")                private BigDecimal payoutRatio;
        @SerializedName("DividendDate")               private String dividendDate;
        @SerializedName("ExDividendDate")             private String exDividendDate;
        @SerializedName("LastSplitFactor")            private String lastSplitFactor;
        @SerializedName("LastSplitDate")              private String lastSplitDate;
        @SerializedName("NumberDividendsByYear")      private Map<String, NumberDividendsByYear> numberDividendsByYear;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class NumberDividendsByYear {
        @SerializedName("Year")  private Integer year;
        @SerializedName("Count") private Integer count;
    }

    /* ───────────────────────────────  Analyst Ratings  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AnalystRatings {
        @SerializedName("Rating")      private BigDecimal rating;
        @SerializedName("TargetPrice") private BigDecimal targetPrice;
        @SerializedName("StrongBuy")   private Integer strongBuy;
        @SerializedName("Buy")         private Integer buy;
        @SerializedName("Hold")        private Integer hold;
        @SerializedName("Sell")        private Integer sell;
        @SerializedName("StrongSell")  private Integer strongSell;
    }

    /* ───────────────────────────────  Holders  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Holders {
        @SerializedName("Institutions") private Map<String, HolderEntry> institutions;
        @SerializedName("Funds")        private Map<String, HolderEntry> funds;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HolderEntry {
        @SerializedName("name")        private String name;
        @SerializedName("date")        private String date;
        @SerializedName("totalShares") private BigDecimal totalShares;
        @SerializedName("totalAssets") private BigDecimal totalAssets;
        @SerializedName("currentShares") private BigDecimal currentShares;
        @SerializedName("change")       private BigDecimal change;
        @SerializedName("change_p")     private BigDecimal changePercent;
    }

    /* ───────────────────────────────  Insider Transactions  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InsiderTransaction {
        @SerializedName("date")                       private String date;
        @SerializedName("ownerCik")                   private String ownerCik;
        @SerializedName("ownerName")                  private String ownerName;
        @SerializedName("transactionDate")            private String transactionDate;
        @SerializedName("transactionCode")            private String transactionCode;
        @SerializedName("transactionAmount")          private BigDecimal transactionAmount;
        @SerializedName("transactionPrice")           private BigDecimal transactionPrice;
        @SerializedName("transactionAcquiredDisposed") private String transactionAcquiredDisposed;
        @SerializedName("postTransactionAmount")      private BigDecimal postTransactionAmount;
        @SerializedName("secLink")                    private String secLink;
    }

    /* ───────────────────────────────  ESG  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ESGScores {
        @SerializedName("Disclaimer")                 private String disclaimer;
        @SerializedName("RatingDate")                 private String ratingDate;
        @SerializedName("TotalEsg")                   private BigDecimal totalEsg;
        @SerializedName("TotalEsgPercentile")         private BigDecimal totalEsgPercentile;
        @SerializedName("EnvironmentScore")           private BigDecimal environmentScore;
        @SerializedName("EnvironmentScorePercentile") private BigDecimal environmentScorePercentile;
        @SerializedName("SocialScore")                private BigDecimal socialScore;
        @SerializedName("SocialScorePercentile")      private BigDecimal socialScorePercentile;
        @SerializedName("GovernanceScore")            private BigDecimal governanceScore;
        @SerializedName("GovernanceScorePercentile")  private BigDecimal governanceScorePercentile;
        @SerializedName("ControversyLevel")           private Integer controversyLevel;
        @SerializedName("ActivitiesInvolvement")      private Map<String, ActivityInvolvement> activitiesInvolvement;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ActivityInvolvement {
        @SerializedName("Activity")    private String activity;
        @SerializedName("Involvement") private String involvement;
    }

    /* ───────────────────────────────  Earnings  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Earnings {
        @SerializedName("History") private Map<String, HistoryEntry> history;
        @SerializedName("Trend")   private Map<String, TrendEntry> trend;
        @SerializedName("Annual")  private Map<String, AnnualEntry> annual;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HistoryEntry {
        @SerializedName("reportDate")     private String reportDate;
        @SerializedName("date")           private String date;
        @SerializedName("beforeAfterMarket") private String beforeAfterMarket;
        @SerializedName("currency")       private String currency;
        @SerializedName("epsActual")      private BigDecimal epsActual;
        @SerializedName("epsEstimate")    private BigDecimal epsEstimate;
        @SerializedName("epsDifference")  private BigDecimal epsDifference;
        @SerializedName("surprisePercent") private BigDecimal surprisePercent;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TrendEntry {
        @SerializedName("date")                         private String date;
        @SerializedName("period")                       private String period;
        @SerializedName("growth")                       private BigDecimal growth;
        @SerializedName("earningsEstimateAvg")          private BigDecimal earningsEstimateAvg;
        @SerializedName("earningsEstimateLow")          private BigDecimal earningsEstimateLow;
        @SerializedName("earningsEstimateHigh")         private BigDecimal earningsEstimateHigh;
        @SerializedName("earningsEstimateYearAgoEps")   private BigDecimal earningsEstimateYearAgoEps;
        @SerializedName("earningsEstimateNumberOfAnalysts") private BigDecimal earningsEstimateNumberOfAnalysts;
        @SerializedName("earningsEstimateGrowth")       private BigDecimal earningsEstimateGrowth;
        @SerializedName("revenueEstimateAvg")           private BigDecimal revenueEstimateAvg;
        @SerializedName("revenueEstimateLow")           private BigDecimal revenueEstimateLow;
        @SerializedName("revenueEstimateHigh")          private BigDecimal revenueEstimateHigh;
        @SerializedName("revenueEstimateYearAgoEps")    private BigDecimal revenueEstimateYearAgoEps;
        @SerializedName("revenueEstimateNumberOfAnalysts") private BigDecimal revenueEstimateNumberOfAnalysts;
        @SerializedName("revenueEstimateGrowth")        private BigDecimal revenueEstimateGrowth;
        @SerializedName("epsTrendCurrent")              private BigDecimal epsTrendCurrent;
        @SerializedName("epsTrend7daysAgo")             private BigDecimal epsTrend7daysAgo;
        @SerializedName("epsTrend30daysAgo")            private BigDecimal epsTrend30daysAgo;
        @SerializedName("epsTrend60daysAgo")            private BigDecimal epsTrend60daysAgo;
        @SerializedName("epsTrend90daysAgo")            private BigDecimal epsTrend90daysAgo;
        @SerializedName("epsRevisionsUpLast7days")      private BigDecimal epsRevisionsUpLast7days;
        @SerializedName("epsRevisionsUpLast30days")     private BigDecimal epsRevisionsUpLast30days;
        @SerializedName("epsRevisionsDownLast7days")    private BigDecimal epsRevisionsDownLast7days;
        @SerializedName("epsRevisionsDownLast30days")   private BigDecimal epsRevisionsDownLast30days;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AnnualEntry {
        @SerializedName("date")     private String date;
        @SerializedName("epsActual") private BigDecimal epsActual;
    }

    /* ───────────────────────────────  Financials  ─────────────────────────────── */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Financials {
        @SerializedName("Balance_Sheet")   private BalanceSheet balanceSheet;
        @SerializedName("Income_Statement") private IncomeStatement incomeStatement;
    }

    /* Balance Sheet ----------------------------------------------------------- */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BalanceSheet {
        @SerializedName("currency_symbol") private String currencySymbol;
        @SerializedName("quarterly")       private Map<String, BalanceQuarter> quarterly;
    }


    /**
     * Balance‑sheet snapshot for a single quarter.
     * <p>Every numeric JSON key is mapped; missing ones can be added later without breaking deserialisation.</p>
     */
    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class BalanceQuarter {
        @SerializedName("date")                         private String date;
        @SerializedName("filing_date")                  private String filingDate;
        @SerializedName("currency_symbol")              private String currencySymbol;

        /* Core totals */
        @SerializedName("totalAssets")                  private BigDecimal totalAssets;
        @SerializedName("totalLiab")                    private BigDecimal totalLiabilities;
        @SerializedName("totalStockholderEquity")       private BigDecimal totalStockholderEquity;

        /* Current assets / liabilities */
        @SerializedName("totalCurrentAssets")           private BigDecimal totalCurrentAssets;
        @SerializedName("totalCurrentLiabilities")      private BigDecimal totalCurrentLiabilities;
        @SerializedName("otherCurrentAssets")           private BigDecimal otherCurrentAssets;
        @SerializedName("otherCurrentLiab")             private BigDecimal otherCurrentLiabilities;
        @SerializedName("currentDeferredRevenue")       private BigDecimal currentDeferredRevenue;

        /* Cash & investments */
        @SerializedName("cash")                         private BigDecimal cash;
        @SerializedName("cashAndEquivalents")           private BigDecimal cashAndEquivalents;
        @SerializedName("cashAndShortTermInvestments")  private BigDecimal cashAndShortTermInvestments;
        @SerializedName("shortTermInvestments")         private BigDecimal shortTermInvestments;
        @SerializedName("longTermInvestments")          private BigDecimal longTermInvestments;

        /* Working‑capital detail */
        @SerializedName("inventory")                    private BigDecimal inventory;
        @SerializedName("netReceivables")               private BigDecimal netReceivables;
        @SerializedName("accountsPayable")              private BigDecimal accountsPayable;
        @SerializedName("netWorkingCapital")            private BigDecimal netWorkingCapital;

        /* Debt */
        @SerializedName("netDebt")                      private BigDecimal netDebt;
        @SerializedName("shortTermDebt")                private BigDecimal shortTermDebt;
        @SerializedName("longTermDebt")                 private BigDecimal longTermDebt;
        @SerializedName("shortLongTermDebt")            private BigDecimal shortLongTermDebt;
        @SerializedName("shortLongTermDebtTotal")       private BigDecimal shortLongTermDebtTotal;
        @SerializedName("longTermDebtTotal")            private BigDecimal longTermDebtTotal;
        @SerializedName("capitalLeaseObligations")      private BigDecimal capitalLeaseObligations;

        /* PP&E */
        @SerializedName("propertyPlantAndEquipmentGross") private BigDecimal propertyPlantAndEquipmentGross;
        @SerializedName("propertyPlantAndEquipmentNet")  private BigDecimal propertyPlantAndEquipmentNet;
        @SerializedName("propertyPlantEquipment")         private BigDecimal propertyPlantEquipment;
        @SerializedName("accumulatedDepreciation")        private BigDecimal accumulatedDepreciation;
        @SerializedName("accumulatedAmortization")        private BigDecimal accumulatedAmortization;

        /* Intangibles & goodwill */
        @SerializedName("intangibleAssets")             private BigDecimal intangibleAssets;
        @SerializedName("goodWill")                     private BigDecimal goodwill;
        @SerializedName("earningAssets")                private BigDecimal earningAssets;

        /* Other assets / liabilities */
        @SerializedName("otherAssets")                  private BigDecimal otherAssets;
        @SerializedName("nonCurrrentAssetsOther")       private BigDecimal nonCurrentAssetsOther;
        @SerializedName("deferredLongTermAssetCharges") private BigDecimal deferredLongTermAssetCharges;
        @SerializedName("nonCurrentAssetsTotal")        private BigDecimal nonCurrentAssetsTotal;
        @SerializedName("deferredLongTermLiab")         private BigDecimal deferredLongTermLiabilities;
        @SerializedName("nonCurrentLiabilitiesOther")   private BigDecimal nonCurrentLiabilitiesOther;
        @SerializedName("nonCurrentLiabilitiesTotal")   private BigDecimal nonCurrentLiabilitiesTotal;
        @SerializedName("otherLiab")                    private BigDecimal otherLiabilities;
        @SerializedName("noncontrollingInterestInConsolidatedEntity") private BigDecimal noncontrollingInterest;
        @SerializedName("temporaryEquityRedeemableNoncontrollingInterests") private BigDecimal temporaryEquity;

        /* Equity detail */
        @SerializedName("commonStock")                  private BigDecimal commonStock;
        @SerializedName("capitalStock")                 private BigDecimal capitalStock;
        @SerializedName("retainedEarnings")             private BigDecimal retainedEarnings;
        @SerializedName("additionalPaidInCapital")      private BigDecimal additionalPaidInCapital;
        @SerializedName("commonStockTotalEquity")       private BigDecimal commonStockTotalEquity;
        @SerializedName("preferredStockTotalEquity")    private BigDecimal preferredStockTotalEquity;
        @SerializedName("retainedEarningsTotalEquity")  private BigDecimal retainedEarningsTotalEquity;
        @SerializedName("otherStockholderEquity")       private BigDecimal otherStockholderEquity;
        @SerializedName("otherItems")                   private BigDecimal otherItems;         // future‑proof
        @SerializedName("accumulatedOtherComprehensiveIncome") private BigDecimal aoci;
        @SerializedName("totalPermanentEquity")         private BigDecimal totalPermanentEquity;
        @SerializedName("capitalSurpluse")              private BigDecimal capitalSurpluse; // vendor typo preserved
        @SerializedName("treasuryStock")                private BigDecimal treasuryStock;

        /* Derived / summary fields */
        @SerializedName("liabilitiesAndStockholdersEquity") private BigDecimal liabAndEquityTotal;
        @SerializedName("netInvestedCapital")           private BigDecimal netInvestedCapital;
        @SerializedName("negativeGoodwill")             private BigDecimal negativeGoodwill;
        @SerializedName("warrants")                     private BigDecimal warrants;
        @SerializedName("preferredStockRedeemable")     private BigDecimal preferredStockRedeemable;
        @SerializedName("commonStockSharesOutstanding") private BigDecimal commonStockSharesOutstanding;
    }

    /* Income Statement -------------------------------------------------------- */

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class IncomeStatement {
        @SerializedName("currency_symbol") private String currencySymbol;
        @SerializedName("quarterly")       private Map<String, IncomeQuarter> quarterly;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class IncomeQuarter {
        @SerializedName("date")            private String date;
        @SerializedName("filing_date")     private String filingDate;
        @SerializedName("totalRevenue")    private BigDecimal totalRevenue;
        @SerializedName("ebit")            private BigDecimal ebit;
        @SerializedName("operatingIncome") private BigDecimal operatingIncome;
        @SerializedName("interestIncome")  private BigDecimal interestIncome;  
        @SerializedName("interestExpense") private BigDecimal interestExpense;
        @SerializedName("ebitda")          private BigDecimal ebitda;
    }
}
