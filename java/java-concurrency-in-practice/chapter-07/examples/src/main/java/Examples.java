public class Examples {
    public static Runnable threadCancellation() {
        return () -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("\t" + Thread.currentThread().getName() + " - cancellable thread was interrupted");
        };
    }

    public static void cancellationViaInterruptedException() throws InterruptedException {
        try {
            while (true) {
                Thread.sleep(1000);
            }

        } finally {
            System.out.println("\t" + Thread.currentThread().getName() + " - thread was interrupted");
        }
    }
}
