import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Chapter01 {
    public static void main(String[] args) {
        Examples.UnsafeSequence unsafeSeq = new Examples.UnsafeSequence();
        parallelize(unsafeSeq::getNext);

        // probably won't be 10000! (not thread-safe)
        System.out.println(unsafeSeq.getNext());

        Examples.SafeSequence safeSeq = new Examples.SafeSequence();
        parallelize(safeSeq::getNext);

        // should be 10000 (thread-safe)
        System.out.println(safeSeq.getNext());
    }

    public static void parallelize(Runnable r) {
        ExecutorService svc = Executors.newFixedThreadPool(10);
        for (int i = 0; i < 10000; i++) {
            svc.execute(r);
        }

        svc.shutdown();
        try {
            svc.awaitTermination(60, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
