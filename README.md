# gbacktester

VM Args: -Xms8G -Xmx32G
	Otherwise GC grinds the application to a halt

Add strategies:
	DONE - PhilTown TA 
    	and FA
    Greenblat
        sort by highest EY and ROC
    DONE - SMA200
    DONE - SMA50_200_Crossover

Add odt document with all results
    DONE - TA on single stocks
    Add next experiments
        52 week high on listed stocks
            parameterized position limit 5-200
            precalculate is52WeekHigh

Somehow keep in code the setup of the experiments I am running
    Documentation through code
    gbtester.main.SingleStockTechnicalAnalysisStrategies
Centralize config variables
    strategy.impl with enum
    DONE - move single stock strategies to sub-package

Strategies with multiple watchlists
Remove OTC stocks
Get fundamental data for listed stocks
Output yearly percentage return

Darvas vs. BuyAndHold?

Fix package names

DONE:
    DONE - isWindows? Auto-detection
    Precompute TA
    Memcahce - process
    Add sqlite

    
    
    
EBIT (12-month)	= Operating Income + Interest Income – Interest Expense	
Enterprise Value =	Market Cap + Net Debt – Cash & Equivalents	


Greenblatt
3. Using them together (Magic Formula ranking)
	Rank the universe by EY (highest → lowest).
	Rank again by ROC (highest → lowest).
	Add the two ranks; pick the lowest combined scores.
	The sweet spot is a stock that is cheap to buy (high EY) and high-quality (high ROC)—e.g., EY ≈ 12 %, ROC ≈ 30 %.