import java.util.concurrent.*;
import java.util.function.Supplier;

public class Main extends Thread {
    private static ThreadLocal<Integer> globalCnt = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args) throws InterruptedException {
        attemptDeadlock("NoDeadlockExample", Examples.NoDeadlockExample::new, 5, 10);
        attemptDeadlock("DeadlockExample", Examples.DeadlockExample::new, 5, 10);
        System.out.println("End of program. If deadlock was present, abrupt shutdown is required.");
        System.out.println("Alternatively, request a thread dump with \"kill -3 <process_pid>\"");
    }

    public static void attemptDeadlock(String msg, Supplier<Examples.LeftRight> supplier, int times, int threadsCnt)
            throws InterruptedException {
        for (int i = 0; i < times; i++) {
            ExecutorService svc = Executors.newFixedThreadPool(10);
            Examples.LeftRight cls = supplier.get();
            for (int j = 0; j < threadsCnt; j++) {
                svc.execute(() -> {
                    try {
                        cls.leftRight();
                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                });
                svc.execute(() -> {
                    try {
                        cls.rightLeft();
                    } catch (InterruptedException e) {
                        currentThread().interrupt();
                    }
                });
            }

            System.out.println(msg + " - Attempting graceful shutdown...");
            svc.shutdown();


            if (!svc.awaitTermination(5, TimeUnit.SECONDS)) {
                System.out.println(msg + " - couldn't shutdown executor. Deadlock present!");
            } else {
                System.out.println(msg + " - Graceful shutdown success");
            }
        }
    }
}
