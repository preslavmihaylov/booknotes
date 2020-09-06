import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class Examples {
    public static class DeadlockPrevention {
        private final Lock left = new ReentrantLock();
        private final Lock right = new ReentrantLock();
        private ThreadLocal<Integer> failedLocksCnt = ThreadLocal.withInitial(() -> 0);

        public void leftRight() {
            doubleLock(left, right);
        }

        public void rightLeft() {
            doubleLock(right, left);
        }

        private void doubleLock(Lock first, Lock second) {
            withTimeout(() -> attemptLocks(first, second), 1);
            System.out.printf("\t%s - retried %d times before succeeding...\n",
                    Thread.currentThread().getName(),
                    failedLocksCnt.get());
        }

        private boolean attemptLocks(Lock first, Lock second) {
            if (first.tryLock()) {
                try {
                    if (second.tryLock()) {
                        try {
                            Thread.sleep(100);
                            return true;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            second.unlock();
                        }
                    } else {
                        failedLocksCnt.set(failedLocksCnt.get()+1);
                    }
                } finally {
                    first.unlock();
                }
            } else {
                failedLocksCnt.set(failedLocksCnt.get()+1);
            }

            return false;
        }

        private void withTimeout(Supplier<Boolean> s, int timeoutInS) {
            long stopTime = System.nanoTime() + (timeoutInS * 1_000_000_000);
            Random rand = new Random();
            int cnt = 0;
            while (!s.get()) {
                if (System.nanoTime() <= stopTime) {
                    break;
                }

                try {
                    Thread.sleep(20 + rand.nextInt(20));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    public static class InterruptibleLock {
        private final Lock lock = new ReentrantLock();

        public void acquireLock() {
            lock.lock();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        public void acquireLockInterruptibly() {
            try {
                lock.lockInterruptibly();
                // do something with lock held...
            } catch (InterruptedException e) {
                System.out.println(Thread.currentThread().getName() +
                        " - Interruptible lock got interrupted...");
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }
    }

    public static class TimeBudget {
        private final Lock lock = new ReentrantLock();

        public void acquireLock() {
            lock.lock();
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }

        public void acquireLockWithTimeout(int timeoutInS) throws InterruptedException {
            if (!lock.tryLock(timeoutInS, TimeUnit.SECONDS)) {
                System.out.println(Thread.currentThread().getName() +
                        " - Timeout for acquiring lock reached. Exiting gracefully...");
                return;
            }

            try {
                System.out.println("Lock acquired successfully in allocated time budget!");
            } finally {
                lock.unlock();
            }
        }
    }
}
