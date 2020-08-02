import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class Examples {
    // This is not thread-safe as compound actions (setLower, setUpper) are not atomically executed.
    // A proper way to do this would be to use the java monitor pattern (synchronized on every method)
    public static class UnsafeNumberRange {
        // INVARIANT: lower <= upper
        private final AtomicInteger lower;
        private final AtomicInteger upper;

        public UnsafeNumberRange(int lower, int upper) {
            this.lower = new AtomicInteger(lower);
            this.upper = new AtomicInteger(upper);
        }

        public int getLower() {
            return lower.get();
        }

        public void setLower(int i) {
            // Warning -- unsafe check-then-act
            if (i > upper.get())
                throw new IllegalArgumentException("can’t set lower to " + i + " > upper");

            lower.set(i);
        }

        public int getUpper() {
            return upper.get();
        }

        public void setUpper(int i) {
            // Warning -- unsafe check-then-act
            if (i < lower.get())
                throw new IllegalArgumentException("can’t set upper to " + i + " < lower");
            upper.set(i);
        }
    }

    // This is not thread-safe as the lock used is not the same as the lock on the synchronized list.
    // The correct way to do this is by using the synchronized list's intrinsic lock
    public static class ListHelper<E> {
        public List<E> list = Collections.synchronizedList(new ArrayList<E>());

        public synchronized boolean putIfAbsent(E x) {
            boolean absent = !list.contains(x);
            if (absent)
                list.add(x);

            return absent;
        }
    }
}
