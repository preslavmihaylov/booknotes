import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Main extends Thread {
    private static ThreadLocal<Integer> globalCnt = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args) {
        testVisibility(1,
                Examples.NoVisibility::new,
                Examples.NoVisibility::run,
                Examples.NoVisibility::initialize);
        testThreadLocal(10, 100);
        System.exit(0);
    }

    public static <T> void testVisibility(int times, Supplier<T> setupFunc, Consumer<T> c1, Consumer<T> c2) {
        for (int i = 0; i < times; i++) {
            T cls = setupFunc.get();
            ExecutorService executor = Executors.newFixedThreadPool(2);
            executor.execute(() ->  c1.accept(cls));
            executor.execute(() ->  c2.accept(cls));

            executor.shutdown();
            try {
                Instant starts = Instant.now();
                executor.awaitTermination(5, TimeUnit.SECONDS);
                Instant ends = Instant.now();
                if (Duration.between(starts, ends).getSeconds() > 4) {
                    System.out.println("Operation timed out! There was an infinite loop");
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void testThreadLocal(int threadsCnt, int incrementTimes) {
        ExecutorService executor = Executors.newFixedThreadPool(threadsCnt);
        for (int i = 0; i < threadsCnt; i++) {
            for (int j = 0; j < incrementTimes; j++) {
                executor.execute(() -> {
                    globalCnt.set(globalCnt.get() + 1);
                    if (globalCnt.get() == incrementTimes) {
                        System.out.printf("Counter reached %d in thread %d\n", incrementTimes, Thread.currentThread().getId());
                    }
                });
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
