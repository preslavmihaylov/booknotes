import java.util.concurrent.*;

public class Main extends Thread {
    private static ThreadLocal<Integer> globalCnt = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args) throws InterruptedException {
        shutdownHookExample();
        cancellableThreadExample();
        threadCancellationViaInterruptionExample();
        cancellationViaFutureExample();
    }

    public static void shutdownHookExample() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("I'm executing in a shutdown hook!");
        }));
    }

    public static void cancellableThreadExample() throws InterruptedException {
        Thread cancellableThread = new Thread(Examples.threadCancellation());
        System.out.println("\n[EXAMPLE #1] Thread cancellation");
        cancellableThread.start();
        for (int i = 0; i < 5; i++) {
            System.out.println("\tMain - Waiting for " + (5-i) + " seconds");
            Thread.sleep(1000);
        }

        System.out.println("\tMain - Interrupting thread...");
        cancellableThread.interrupt();
        Thread.sleep(5000);
    }

    public static void threadCancellationViaInterruptionExample() throws InterruptedException {
        Thread cancellableThread = new Thread(() -> {
            try {
                Examples.cancellationViaInterruptedException();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        System.out.println("\n[EXAMPLE #2] Cancellation via Interruption");
        cancellableThread.start();
        for (int i = 0; i < 5; i++) {
            System.out.println("\tMain - Waiting for " + (5-i) + " seconds");
            Thread.sleep(1000);
        }

        System.out.println("\tMain - Interrupting thread...");
        cancellableThread.interrupt();
        Thread.sleep(5000);
    }

    public static void cancellationViaFutureExample() throws InterruptedException {
        System.out.println("\n[EXAMPLE #3] Cancellation via Future");
        ExecutorService exec = Executors.newCachedThreadPool();
        Future<?> f = exec.submit(() -> {
            try {
                Examples.cancellationViaInterruptedException();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        try {
            System.out.println("\tMain - starting future with 5s timeout");
            f.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException | TimeoutException e) {
            // fallback to cancelling task
        } catch (ExecutionException e) {
            System.out.println("\tThread exited abnormally: " + e.getMessage());
        } finally {
            System.out.println("\tMain - cancelling task...");
            f.cancel(true);
            exec.shutdown();
            exec.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
