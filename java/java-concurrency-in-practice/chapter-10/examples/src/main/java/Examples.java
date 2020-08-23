public class Examples {
    public interface LeftRight {
        public void leftRight() throws InterruptedException;
        public void rightLeft() throws InterruptedException;
    }

    public static class DeadlockExample implements LeftRight {
        private final Object left = new Object();
        private final Object right = new Object();

        public void leftRight() throws InterruptedException {
            synchronized (left) {
                synchronized (right) {
                    Thread.sleep(100);
                }
            }
        }

        public void rightLeft() throws InterruptedException {
            synchronized (right) {
                synchronized (left) {
                    Thread.sleep(100);
                }
            }
        }
    }

    public static class NoDeadlockExample implements LeftRight {
        private final Object left = new Object();
        private final Object right = new Object();

        public void leftRight() throws InterruptedException {
            synchronized (left) {
                synchronized (right) {
                    Thread.sleep(100);
                }
            }
        }

        public void rightLeft() throws InterruptedException {
            synchronized (left) {
                synchronized (right) {
                    Thread.sleep(100);
                }
            }
        }
    }
}
