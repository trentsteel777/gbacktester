package gbacktester.ta;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import gbacktester.domain.CsvStockRecord;

public class RollingWindowHighTest {

    /**
     * Helper to build a CsvStockRecord with a given high.
     */
    private CsvStockRecord buildRecord(String symbol, LocalDate date, double high) {
        CsvStockRecord rec = new CsvStockRecord();
        rec.setSymbol(symbol);
        rec.setDate(date);
        rec.setHigh(high);
        return rec;
    }

    /**
     * Test with a small window of size 3. 
     * We'll feed in a small sequence of bars, and check the "is52WeekHigh" logic.
     */
    @Test
    public void testSmallWindow() {
        RollingWindowHigh rollingHigh = new RollingWindowHigh(3);

        List<CsvStockRecord> prices = new ArrayList<>();
        // Build synthetic data with known "high" pattern
        // Day   High
        //  0 -> 10  (largest so far)
        //  1 -> 12  (new largest so far)
        //  2 ->  9
        //  3 -> 15  (new largest)
        //  4 -> 14
        //  5 ->  8
        // We'll see how the 3-day window logic catches new highs
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,1), 10));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,2), 12));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,3), 9));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,4), 15));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,5), 14));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,6), 8));

        // We'll store a boolean in parallel to see if we expect a "new high" within the last 3
        boolean[] expected = new boolean[6];

        // Because we set the warmup to i >= windowSize - 1 => i >= 2, we won't see "true" until i=2 or beyond.
        // Let's do the manual logic:
        //
        // i=0 -> window is incomplete. No "3-bar" window yet => set52WeekHigh(false
        // i=1 -> still incomplete => false
        // i=2 -> now we have 3 bars: highs = [10, 12, 9]. The largest is 12 at index=1.
        //        index=2's high=9 is not a new max => false
        // i=3 -> last 3 bars are indexes [1,2,3] => highs = [12, 9, 15]. The largest is 15 => index=3 => true
        // i=4 -> last 3 bars are [2,3,4] => [9, 15, 14]. The largest is 15 => index=3 => so i=4 => false
        // i=5 -> last 3 bars are [3,4,5] => [15, 14, 8]. The largest is 15 => index=3 => i=5 => false
        //
        // So only i=3 => true

        expected[0] = false;
        expected[1] = false;
        expected[2] = false; // Because the largest in that 3-bar window is index=1's 12
        expected[3] = true;  // 15 is new max in [1..3]
        expected[4] = false; // The largest in [2..4] is index=3 => 15
        expected[5] = false; // The largest in [3..5] is index=3 => 15

        for (int i = 0; i < prices.size(); i++) {
            boolean isHigh = rollingHigh.updateAndCheckIsHigh(prices, i);
            prices.get(i).set52WeekHigh(isHigh);
        }

        for (int i = 0; i < prices.size(); i++) {
            assertEquals(expected[i], prices.get(i).is52WeekHigh(), 
                "Mismatch at i=" + i + ", expected " + expected[i] + " got " + prices.get(i).is52WeekHigh());
        }
    }

    /**
     * Test the default constructor (window=252). With only 6 data points,
     * we won't see i >= (252-1) => i >= 251, so we expect all false.
     */
    @Test
    public void testDefaultConstructorNoWarmup() {
        RollingWindowHigh rollingHigh = new RollingWindowHigh(); // uses 252
        List<CsvStockRecord> prices = new ArrayList<>();
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,1), 10));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,2), 12));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,3),  9));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,4), 15));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,5), 14));
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,6),  8));

        for (int i = 0; i < prices.size(); i++) {
            boolean isHigh = rollingHigh.updateAndCheckIsHigh(prices, i);
            prices.get(i).set52WeekHigh(isHigh);
        }

        // Because i never >= 251, we expect all false
        for (int i = 0; i < prices.size(); i++) {
            assertFalse(prices.get(i).is52WeekHigh(),
                "Expected false for is52WeekHigh at i=" + i);
        }
    }

    @Test
    public void testDefaultConstructorWithMockedIndex() {
        RollingWindowHigh rollingHigh = new RollingWindowHigh(); // 252
        List<CsvStockRecord> prices = new ArrayList<>();

        // 1) Index=0 => high=10
        prices.add(buildRecord("TEST", LocalDate.of(2023,1,1), 10)); // i=0

        // 2) Fill bars 1..250 with dummy data (high=5).
        for (int idx = 1; idx < 251; idx++) {
            prices.add(buildRecord("TEST",
                LocalDate.of(2023, 1, 1).plusDays(idx),
                5.0
            ));
        }

        // Now we have 251 bars total (indices 0..250).

        // 3) Add a record at index=251 => high=20
        prices.add(buildRecord("TEST",
            LocalDate.of(2023, 1, 1).plusDays(251),
            20.0
        ));
        // Now prices.size() == 252, valid indices are 0..251

        // 4) Run your logic from i=0 up to i=251
        for (int i = 0; i < prices.size(); i++) {
            boolean isHigh = rollingHigh.updateAndCheckIsHigh(prices, i);
            prices.get(i).set52WeekHigh(isHigh);
        }

        // 5) Check the results
        //
        // - For i=0 => not warmup yet. Should be false if you enforce "i >= 251"
        // - For i in [1..250], still i < 251 => all false
        // - For i=251 => we expect true because now we have a full 252-bar window,
        //   and bar 251 has high=20, which is bigger than anything else (10 or 5).
        
        // So let's do some simple assertions:
        for (int i = 0; i < 251; i++) {
            assertFalse(prices.get(i).is52WeekHigh(),
                "Expected false at i=" + i + " (warmup not done or not highest).");
        }
        // The last bar should be recognized as the new high:
        assertTrue(prices.get(251).is52WeekHigh(),
            "Expected true at i=251 because it's the new 52-week high.");
    }

}
