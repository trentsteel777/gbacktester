# gbacktester

Add strategies:
    PhilTown TA and FA
    Greenblat
        sort by highest EY and ROC
    SMA200
    SMA50_200_Crossover

Add odt document with all results
    TA on single stocks
    Add next experiments
        52 week high on listed stocks
            parameterized position limit 5-200
            precalculate is52WeekHigh

Somehow keep in code the setup of the experiments I am running
    Documentation through code
    gbtester.main.SingleStockTechnicalAnalysisStrategies
Centralize config variables
    strategy.impl with enum
    move single stock strategies to sub-package
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
