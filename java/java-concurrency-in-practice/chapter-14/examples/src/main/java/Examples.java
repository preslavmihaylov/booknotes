import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Examples {
    public interface Stack<T> {
        T pop() throws InterruptedException;
        void push(T value) throws InterruptedException;
        int getBound();
    }

    public static class IntrinsicConditionQueue<T> implements Stack<T> {
        private final List<T> elems = new ArrayList<>();
        private final int bound;

        public IntrinsicConditionQueue(int bound) {
            this.bound = bound;
        }

        @Override
        public synchronized T pop() throws InterruptedException {
            while (isEmpty()) {
                wait();
            }

            T r = elems.get(elems.size()-1);
            elems.remove(elems.size()-1);
            notifyAll();

            return r;
        }

        @Override
        public synchronized void push(T value) throws InterruptedException {
            while (isFull()) {
                wait();
            }

            elems.add(value);
            notifyAll();
        }

        @Override
        public int getBound() {
            return bound;
        }


        private boolean isEmpty() {
            return elems.size() == 0;
        }

        private boolean isFull() {
            return elems.size() == bound;
        }
    }

    public static class ExplicitConditionQueue<T> implements Stack<T> {
        private final Lock lock = new ReentrantLock();
        private final Condition hasElementsCondition = lock.newCondition();
        private final Condition notFullCondition = lock.newCondition();

        private final List<T> elems = new ArrayList<>();
        private final int bound;

        public ExplicitConditionQueue(int bound) {
            this.bound = bound;
        }

        @Override
        public T pop() throws InterruptedException {
            lock.lock();
            try {
                while (isEmpty()) {
                    hasElementsCondition.await();
                }

                T r = elems.get(elems.size() - 1);
                elems.remove(elems.size() - 1);
                notFullCondition.signal();

                return r;
            } finally {
                lock.unlock();
            }
        }

        @Override
        public void push(T value) throws InterruptedException {
            lock.lock();
            try {
                while (isFull()) {
                    notFullCondition.await();
                }

                elems.add(value);
                hasElementsCondition.signal();
            } finally {
                lock.unlock();
            }
        }

        @Override
        public int getBound() {
            return bound;
        }


        private boolean isEmpty() {
            return elems.size() == 0;
        }

        private boolean isFull() {
            return elems.size() == bound;
        }
    }

    public static class AQS<T> implements Stack<T> {
        private final List<T> elems = new ArrayList<>();
        private final int bound;
        private final Sync hasElementsSync;
        private final Sync notFullSync;

        public AQS(int bound) {
            this.bound = bound;
            hasElementsSync = new Sync();
            notFullSync = new Sync(bound);
        }

        @Override
        public T pop() throws InterruptedException {
            hasElementsSync.acquireSharedInterruptibly(0);
            synchronized (this) {
                T r = elems.get(elems.size() - 1);
                elems.remove(elems.size() - 1);
                notFullSync.releaseShared(0);

                return r;
            }
        }

        @Override
        public void push(T value) throws InterruptedException {
            notFullSync.acquireSharedInterruptibly(0);
            synchronized (this) {
                elems.add(value);
                hasElementsSync.releaseShared(0);
            }
        }

        @Override
        public int getBound() {
            return bound;
        }

        private static class Sync extends AbstractQueuedSynchronizer {
            protected Sync() {
                this(0);
            }

            protected Sync(int initialState) {
                setState(initialState);
            }

            protected int tryAcquireShared(int ignored) {
                while (true) {
                    int state = getState();
                    int target = state-1;
                    if (target < 0 || compareAndSetState(state, target)) {
                        return target;
                    }
                }
            }

            protected boolean tryReleaseShared(int ignored) {
                while (true) {
                    int state = getState();
                    if (compareAndSetState(state, state+1))
                        return true;
                }
            }
        }
    }
}
