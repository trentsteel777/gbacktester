package gbacktester.util;

public class StopWatch {

    private long startTime;
    private long endTime;

    private StopWatch() {}
    
    public static StopWatch start() {
        StopWatch sw = new StopWatch();
        sw.startTime = System.nanoTime();
        return sw;
    }

    public double stop() {
        this.endTime = System.nanoTime();
        return (endTime - startTime) / 1_000_000_000.0;
    }

    public double lapInSeconds() {
        return (System.nanoTime() - startTime) / 1_000_000_000.0;
    }
    
	private double getElapsedSeconds() {
        if (endTime == 0) {
            throw new IllegalStateException("StopWatch has not been stopped.");
        }
        return (endTime - startTime) / 1_000_000_000.0;
    }
    
    private double getElapsedMinutes() {
        if (endTime == 0) {
            throw new IllegalStateException("StopWatch has not been stopped.");
        }
        return (endTime - startTime) / 60_000_000_000.0;
    }

    public void printElapsed() {
        stop(); // Ensure stopwatch is stopped
        System.out.printf("Execution time: %.3f minutes%n", getElapsedMinutes());
    }
    
}
