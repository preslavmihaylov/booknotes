import java.util.concurrent.*;
import java.util.function.Supplier;

public class Main extends Thread {
    private static boolean isStopped = false;

    public static void main(String[] args) throws InterruptedException {
        attemptDeadlock("DeadlockPrevention", Examples.DeadlockPrevention::new, 5, 10);
        testInterruptibleLock();
        testLockWithTimeBudget();
    }

    public static void attemptDeadlock(String msg, Supplier<Examples.DeadlockPrevention> supplier, int times, int threadsCnt)
            throws InterruptedException {
        for (int i = 0; i < times; i++) {
            ExecutorService svc = Executors.newFixedThreadPool(10);
            Examples.DeadlockPrevention cls = supplier.get();
            for (int j = 0; j < threadsCnt; j++) {
                svc.execute(cls::leftRight);
                svc.execute(cls::rightLeft);
            }

            System.out.println(msg + " - Attempting graceful shutdown...");

            Thread.sleep(100);
            svc.shutdown();
            if (!svc.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println(msg + " - couldn't shutdown executor. Deadlock present!");
            } else {
                System.out.println(msg + " - Graceful shutdown success");
            }
        }
    }

    public static void testInterruptibleLock() throws InterruptedException {
        System.out.println("\nTesting interruptible lock...");
        Examples.InterruptibleLock interruptibleLock = new Examples.InterruptibleLock();
        ExecutorService exec = Executors.newCachedThreadPool();

        exec.execute(interruptibleLock::acquireLock);
        Thread.sleep(100);
        Future<?> f = exec.submit(interruptibleLock::acquireLockInterruptibly);

        Thread.sleep(500);
        f.cancel(true);

        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
    }

    public static void testLockWithTimeBudget() throws InterruptedException {
        System.out.println("\nTesting lock with time budget...");
        Examples.TimeBudget timeBudgetExample = new Examples.TimeBudget();
        ExecutorService exec = Executors.newCachedThreadPool();

        exec.execute(timeBudgetExample::acquireLock);
        Thread.sleep(100);
        Future<?> f = exec.submit(() -> {
            try {
                timeBudgetExample.acquireLockWithTimeout(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread.sleep(5000);
        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
    }
}
