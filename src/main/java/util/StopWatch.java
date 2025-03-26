package util;

public class StopWatch {

    private long startTime;
    private long endTime;

    public static StopWatch start() {
        StopWatch sw = new StopWatch();
        sw.startTime = System.nanoTime();
        return sw;
    }

    public void stop() {
        this.endTime = System.nanoTime();
    }

    public double getElapsedSeconds() {
        if (endTime == 0) {
            throw new IllegalStateException("StopWatch has not been stopped.");
        }
        return (endTime - startTime) / 1_000_000_000.0;
    }

    public void printElapsed() {
        stop(); // Ensure stopwatch is stopped
        System.out.printf("Execution time: %.3f seconds%n", getElapsedSeconds());
    }
}
