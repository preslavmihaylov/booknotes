import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

public class Main extends Thread {
    private static ThreadLocal<Integer> globalCnt = ThreadLocal.withInitial(() -> 0);

    public static void main(String[] args) throws InterruptedException, BrokenBarrierException {
        measureLockScopeExample(new Examples.BigLockScopeExample(), 16);
        measureLockScopeExample(new Examples.SmallLockScopeExample(), 16);
        measureLockScopeExample(new Examples.LockStripingExample(), 16);
    }

    public static void measureLockScopeExample(Examples.LockScopeExample example, int threadsCnt)
            throws InterruptedException, BrokenBarrierException {
        ExecutorService exec = Executors.newFixedThreadPool(threadsCnt);

        BlockingQueue<Long> queue = new LinkedBlockingQueue<>();
        CyclicBarrier barrier = new CyclicBarrier(threadsCnt+1);
        for (int i = 0; i < threadsCnt; i++) {
            exec.execute(() -> {
                List<String> users = new ArrayList<>();
                List<String> patterns = new ArrayList<>();
                for (int j = 0; j < 10000; j++) {
                    users.add(UUID.randomUUID().toString());
                    example.addUserLocation(users.get(users.size() - 1), UUID.randomUUID().toString());
                    patterns.add(UUID.randomUUID().toString());
                }

                try {
                    barrier.await();
                } catch (InterruptedException e) {
                    currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }

                long start = System.nanoTime();
                int seed = (int)System.nanoTime();
                for (int j = 0; j < 50000; j++) {
                    int rand1 = nextRand(seed) % users.size();
                    seed = nextRand(seed);

                    int rand2 = nextRand(seed) % users.size();
                    seed = nextRand(seed);

                    example.userLocationMatches(users.get(rand1), patterns.get(rand2));
                }

                long total = (System.nanoTime() - start) / 1000000; // milliseconds
                try {
                    barrier.await();
                    queue.put(total);
                } catch (InterruptedException e) {
                    currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }
            });
        }

        barrier.await();
        barrier.await();

        exec.shutdown();
        exec.awaitTermination(5, TimeUnit.SECONDS);
        int averageTime = 0;
        for (int i = 0; i < threadsCnt; i++) {
            averageTime += queue.take();
        }

        System.out.println(example.getClass().getName() +
                " - average time per task = " + averageTime/threadsCnt + "ms");
    }

    private static int nextRand(int y) {
        y ^= (y << 6);
        y ^= (y >>> 21);
        y ^= (y << 7);

        return Math.abs(y);
    }

}
