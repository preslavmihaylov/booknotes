import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Examples {
    public interface Synchronization {
        void mutateState();
        void readState();
    }

    public static class UnsynchronizedExample implements Synchronization {
        private boolean isReady = false;

        public void mutateState() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            isReady = true;
        }

        public void readState() {
            while (!isReady) {}
        }
    }

    public static class PiggybackedSynchronization implements Synchronization {
        private boolean isReady = false;
        private final BlockingQueue<Integer> queue = new LinkedBlockingQueue<>();

        public void mutateState() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }

            isReady = true;
            queue.add(1);
        }

        public void readState() {
            // There is no synchronization whatsoever on isReady and yet, this code is thread-safe.
            // The reason is that we are "piggybacking" the synchronization provided by the blocking queue.
            //
            // Java Memory Model details:
            // 1. "Program execution rule" -> any action in a single thread "happens-before" any subsequent action in the same thread
            // 2. "Monitor lock rule" -> an unlock "happens-before" all previous locks on the same monitor
            // 3. "Transitivity" -> If A "happens-before" B, B "happens-before" C, then A "happens-before" C
            //
            // The isReady mutation "happens-before" the push to the queue (via #1).
            // The pop from queue "happens-before" the push to the queuie (via #2).
            // Therefore, the isReady mutation "happens-before" the pop from the queue (via #3)
            while (!isReady) {
                if (!queue.isEmpty()) {
                    try {
                        queue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }
}
