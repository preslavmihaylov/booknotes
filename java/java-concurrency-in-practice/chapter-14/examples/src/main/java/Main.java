import java.util.concurrent.*;
import java.util.function.Supplier;

public class Main extends Thread {
    private static boolean isStopped = false;

    public static void main(String[] args) throws InterruptedException {
        testQueue(() -> new Examples.IntrinsicConditionQueue<>(10));
        testQueue(() -> new Examples.ExplicitConditionQueue<>(10));
        testQueue(() -> new Examples.AQS<>(10));
    }

    public static void testQueue(Supplier<Examples.Stack<Integer>> queueSupplier)
            throws InterruptedException {
        System.out.printf("\nTesting %s...\n", queueSupplier.get().getClass().getName());
        System.out.println("\nTesting blocking pop...");
        Thread.sleep(2000);
        testBlockingPop(queueSupplier);

        System.out.println("\nTesting blocking push...");
        Thread.sleep(2000);
        testBlockingPush(queueSupplier);
        Thread.sleep(2000);
    }

    public static void testBlockingPop(Supplier<Examples.Stack<Integer>> queueSupplier)
            throws InterruptedException {
        ExecutorService svc = Executors.newFixedThreadPool(10);

        Examples.Stack<Integer> queue = queueSupplier.get();
        Runnable popFunc = () -> {
            try {
                System.out.println("\t"+Thread.currentThread().getName() + " - attempting to pop...");
                queue.pop();
                System.out.println("\t"+Thread.currentThread().getName() + " - pop successful!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Runnable pushFunc = () -> {
            try {
                System.out.println("\t"+Thread.currentThread().getName() + " - attempting to push...");
                queue.push(1);
                System.out.println("\t"+Thread.currentThread().getName() + " - push successful!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        testBlocking(svc, popFunc, pushFunc);
    }

    public static void testBlockingPush(Supplier<Examples.Stack<Integer>> queueSupplier)
            throws InterruptedException {
        ExecutorService svc = Executors.newFixedThreadPool(10);

        Examples.Stack<Integer> queue = queueSupplier.get();
        Runnable popFunc = () -> {
            try {
                System.out.println("\t"+Thread.currentThread().getName() + " - attempting to pop...");
                queue.pop();
                System.out.println("\t"+Thread.currentThread().getName() + " - pop successful!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        Runnable pushFunc = () -> {
            try {
                System.out.println("\t"+Thread.currentThread().getName() + " - attempting to push...");
                queue.push(1);
                System.out.println("\t"+Thread.currentThread().getName() + " - push successful!");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        for (int i = 0; i < queue.getBound(); i++) {
            queue.push(1);
        }

        testBlocking(svc, pushFunc, popFunc);
    }

    private static void testBlocking(ExecutorService svc, Runnable first, Runnable second)
            throws InterruptedException {
        svc.execute(first);
        svc.execute(first);

        Thread.sleep(3000);
        System.out.println("\nWaiting...");
        Thread.sleep(5000);
        svc.execute(second);
        svc.execute(second);

        Thread.sleep(5000);
        svc.shutdown();
        svc.awaitTermination(5, TimeUnit.SECONDS);
    }
}
