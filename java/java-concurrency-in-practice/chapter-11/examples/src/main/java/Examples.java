import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Examples {
    public interface LockScopeExample {
        void addUserLocation(String user, String loc);
        boolean userLocationMatches(String user, String regexp);
    }

    public static class BigLockScopeExample implements LockScopeExample {
        private final Map<String, String> locations = new HashMap<>();

        public synchronized void addUserLocation(String user, String loc) {
            locations.put(user, loc);
        }

        public synchronized boolean userLocationMatches(String user, String regexp) {
            String loc = locations.get(user);
            if (loc == null)
                return false;
            else
                return Pattern.matches(regexp, loc);
        }
    }

    public static class SmallLockScopeExample implements LockScopeExample {
        private final Map<String, String> locations = new HashMap<>();

        public synchronized void addUserLocation(String user, String loc) {
            locations.put(user, loc);
        }

        public boolean userLocationMatches(String user, String regexp) {
            String location;
            synchronized (this) {
                location = locations.get(user);
            }

            if (location == null)
                return false;
            else
                return Pattern.matches(regexp, location);
        }
    }

    public static class LockStripingExample implements LockScopeExample {
        // ConcurrentHashMap implements lock striping. No need to implement your own :))
        private final Map<String, String> locations = new ConcurrentHashMap<>();

        public void addUserLocation(String user, String loc) {
            locations.put(user, loc);
        }

        public boolean userLocationMatches(String user, String regexp) {
            String location = locations.get(user);
            if (location == null)
                return false;
            else
                return Pattern.matches(regexp, location);
        }
    }
}
