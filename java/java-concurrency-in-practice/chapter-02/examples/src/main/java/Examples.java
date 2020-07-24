import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Examples {
    static class UnsafeHitCounter {
        private long hits;

        public long getHits() {
            return hits;
        }

        public void hit() {
            ++hits;
        }
    }

    static class SafeHitCounter {
        private AtomicLong hits = new AtomicLong(0);

        public long getHits() {
            return hits.get();
        }

        public void hit() {
            hits.getAndIncrement();
        }
    }

    interface Cache {
        int getValue(int key);
        void setValue(int key);
    }

    // Not exactly the same example as in the book, but its the same principle
    // This is an example of using non thread-safe class (HashMap)
    // If the cache is thread-safe, it should increment a key only once
    static class UnsafeCache implements Cache {
        Map<Integer, Integer> cache = new HashMap<>();

        public int getValue(int key) {
            return cache.getOrDefault(key, 0);
        }

        public void setValue(int key) {
            if (!cache.containsKey(key)) {
                cache.put(key, cache.getOrDefault(key, 0) + 1);
            }
        }
    }

    // This uses a thread-safe class, but compound actions aren't synchronized.
    // Hence, the class is still non thread-safe
    static class NonAtomicCompoundCache implements Cache {
        Map<Integer, Integer> cache = new ConcurrentHashMap<>();

        public int getValue(int key) {
            return cache.getOrDefault(key, 0);
        }

        public void setValue(int key) {
            if (!cache.containsKey(key)) {
                cache.put(key, cache.getOrDefault(key, 0) + 1);
            }
        }
    }

    static class SafeCache implements Cache {
        Map<Integer, Integer> cache = new ConcurrentHashMap<>();

        public int getValue(int key) {
            return cache.getOrDefault(key, 0);
        }

        public void setValue(int key) {
            cache.putIfAbsent(key, cache.getOrDefault(key, 0) + 1);
        }
    }
}
