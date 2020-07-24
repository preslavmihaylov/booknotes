import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Chapter02 {
    public static void main(String[] args) {
        System.out.println("HitCounter test.");

        System.out.println("\nNon Thread-Safe example (hit 10000 times):");
        Examples.UnsafeHitCounter unsafe = new Examples.UnsafeHitCounter();
        parallelize(10000, unsafe::hit);

        // probably won't be 10000! (not thread-safe)
        System.out.println(unsafe.getHits());

        System.out.println("\nThread-Safe example (hit 10000 times):");
        Examples.SafeHitCounter safe = new Examples.SafeHitCounter();
        parallelize(10000, safe::hit);

        // should be 10000 (thread-safe)
        System.out.println(safe.getHits());

        System.out.println("\nNon-thread-safe example with unsafe cache");
        Examples.UnsafeCache unsafeCache = new Examples.UnsafeCache();
        testCache(unsafeCache);

        System.out.println("Non-thread-safe example with non-atomic compound actions cache");
        Examples.NonAtomicCompoundCache nonAtomicCompoundCache = new Examples.NonAtomicCompoundCache();
        testCache(nonAtomicCompoundCache);

        System.out.println("Thread-safe cache example");
        Examples.SafeCache threadSafeCache = new Examples.SafeCache();
        testCache(threadSafeCache);
    }

    public static void testCache(Examples.Cache cache) {
        AtomicInteger cnt1 = new AtomicInteger(0);
        AtomicInteger cnt2 = new AtomicInteger(0);
        parallelize(10000,
                () -> cache.setValue(cnt1.incrementAndGet()),
                () -> cache.setValue(cnt2.incrementAndGet()));

        int violationsCnt = 0;
        for (int i = 1; i <= cnt1.get(); i++) {
            int actual = cache.getValue(i);
            if (actual != 1) {
                violationsCnt++;
            }
        }

        if (violationsCnt == 0) {
            System.out.println("Cache is thread-safe!\n");
        } else {
            System.out.println("Cache is NOT thread-safe with " + violationsCnt + " violations!\n");
        }
    }

    public static void parallelize(int executionsCnt, Runnable ...rs) {
        ExecutorService svc = Executors.newFixedThreadPool(10);
        for (int i = 0; i < executionsCnt; i++) {
            for (Runnable r : rs) {
                svc.execute(r);
            }
        }

        svc.shutdown();
        try {
            svc.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
