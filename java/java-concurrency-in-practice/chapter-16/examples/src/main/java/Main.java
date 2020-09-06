import java.util.concurrent.*;

public class Main extends Thread {
    private static boolean isStopped = false;

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        testSynchronization(new Examples.UnsynchronizedExample(), 5);
        testSynchronization(new Examples.PiggybackedSynchronization(), 6);
    }

    private static void testSynchronization(Examples.Synchronization cls, int attempts) throws InterruptedException {
        System.out.println("Testing " + cls.getClass().getName() + "...");
        for (int i = 0; i < attempts; i++) {
            System.out.println("\tExecuting attempt " + (i+1) + "...");
            ExecutorService exec = Executors.newCachedThreadPool();
            exec.execute(cls::mutateState);
            exec.execute(cls::readState);

            exec.shutdown();
            if (!exec.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println("\tCouldn't terminate thread normally, a thread-safety violation is present. " +
                        "Abrupt program termination required...");
                return;
            }
        }

        System.out.println("\tTest finished. No thread-safety violations found!");
    }

}
