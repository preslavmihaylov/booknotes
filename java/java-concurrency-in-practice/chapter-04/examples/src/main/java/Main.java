import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Main extends Thread {
    private static ThreadLocal<Integer> globalCnt = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args) throws InterruptedException {
        // Both issues don't reproduce every time. It typically happens once every 4-5 runs
        System.out.println("Test #1 - non-atomic compound actions");
        testInvariantViolation(100000, () -> new Examples.UnsafeNumberRange(0, 10), (nr) -> {
            if (nr.getLower() > nr.getUpper()) {
                System.out.printf("Invariant violated. Range(%d-%d)\n", nr.getLower(), nr.getUpper());
            }

        }, (nr) -> nr.setLower(5), (nr) -> nr.setUpper(4));

        System.out.println("Test #2 - bad client-side locking");
        testInvariantViolation(100000, () -> new Examples.ListHelper<>(), (lh) -> {
            if (lh.list.lastIndexOf(42) != lh.list.indexOf(42)) {
                System.out.println("Invariant violated. 42 contained more than once in list.");
            }
        }, (lh) -> lh.list.add(42), (lh) -> lh.putIfAbsent(42));
        System.exit(0);
    }

    public static <T> void testInvariantViolation(
            int times, Supplier<T> init, Consumer<T> verifyFunc, Consumer<T>...consumers) throws InterruptedException {
        for (int i = 0; i < times; i++) {
            ExecutorService executor = Executors.newFixedThreadPool(5);
            T cls = init.get();
            for (Consumer<T> consumer : consumers) {
                try {
                    executor.execute(ignoreException(() -> consumer.accept(cls)));
                } catch (IllegalArgumentException ignored) {
                }
            }

            executor.shutdown();
            executor.awaitTermination(1, TimeUnit.SECONDS);
            verifyFunc.accept(cls);
        }
    }

    public static Runnable ignoreException(Runnable r) {
        return () -> {
            try {
                r.run();
            } catch (IllegalArgumentException ignored) {
            }
        };
    }
}
