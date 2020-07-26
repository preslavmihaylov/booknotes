public class Examples {
    public static class NoVisibility {
        private boolean ready;
        private int number;

        public void run() {
            while (!ready) {}

            if (number != 42) {
                System.out.println("Got the wrong number! Expected: 42, Actual: " + number);
            }
        }

        public void initialize() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            number = 42;
            ready = true;
        }
    }
}
