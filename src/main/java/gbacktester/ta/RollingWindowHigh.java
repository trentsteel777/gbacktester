package gbacktester.ta;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import gbacktester.domain.CsvStockRecord;

/**
 * Maintains a rolling-window max of the "high" field in CsvStockRecord
 * using a monotonic deque for O(1) amortized lookups.
 */
public class RollingWindowHigh {

    private final Deque<Integer> maxDeque = new ArrayDeque<>();
    private final int windowSize;  // e.g., 252 for a "52-week" window

    /**
     * Default constructor sets windowSize to 252 (typical "52-week" scenario).
     */
    public RollingWindowHigh() {
        this.windowSize = 252;
    }

    /**
     * Custom constructor allowing smaller window sizes (e.g. for tests).
     */
    public RollingWindowHigh(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * Updates the internal deque to account for the bar at index i.
     * Returns true if this bar is the highest in the last `windowSize` bars,
     * provided we have "warmed up" (i >= windowSize-1).
     *
     * If you'd prefer to label partial windows as a "high so far," remove
     * the warm-up check at the bottom.
     *
     * @param prices The full list of CsvStockRecord
     * @param i      The current index in that list
     * @return       true if `prices.get(i).high` is the max in [i - (windowSize-1) .. i]
     */
    public boolean updateAndCheckIsHigh(List<CsvStockRecord> prices, int i) {
        double currentHigh = prices.get(i).getHigh();

        // 1) Remove from back while this bar's high >= the back's high
        while (!maxDeque.isEmpty() && prices.get(maxDeque.getLast()).getHigh() <= currentHigh) {
            maxDeque.removeLast();
        }

        // 2) Add the current index to the back
        maxDeque.addLast(i);

        // 3) Evict the front if it’s out of the window
        int windowStart = i - (windowSize - 1);
        if (maxDeque.getFirst() < windowStart) {
            maxDeque.removeFirst();
        }

        // 4) If the front is i, this bar is the highest in the last windowSize bars.
        //    But only count it as a "rolling high" if i >= (windowSize-1) ("warmup").
        if (maxDeque.getFirst() == i && i >= (windowSize - 1)) {
            return true;
        }
        return false;
    }
}
