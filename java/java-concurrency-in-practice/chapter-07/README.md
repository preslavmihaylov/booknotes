# Chapter 07 - Cancellation & Shutdown

This chapter focuses on how to cancel & interrupt threads & tasks when needed.

Tasks & threads in java are stopped cooperatively - you can't force a task to stop execution, you can just kindly ask it to. It is up to the task to interpret that request & acknowledge it.

# Task cancellation

An activity is cancellable is external code can move it to completion before it ends.

Why you might want to cancel a task:
 * User-requested (e.g. user clicks the "Cancel" button)
 * Time-limited activities - e.g. you want to execute something for 5s and return results. Greedy algorithms/NP-Complete problems.
 * Application events - You stop concurrent tasks running when one of the tasks finds the solution you're looking for
 * Errors - e.g. the disk is full & you want to cancel all tasks which write to it
 * Shutdown - the application is requested to shutdown

There is no safe way to preemptively force a thread or task in Java to stop execution.

An example cancellation policy is to set a boolean flag `cancelled`:
```java
@ThreadSafe
public class PrimeGenerator implements Runnable {
    @GuardedBy("this")
    private final List<BigInteger> primes = new ArrayList<BigInteger>();
    private volatile boolean cancelled;

    public void run() {
        BigInteger p = BigInteger.ONE;
        while (!cancelled) {
            p = p.nextProbablePrime();
            synchronized (this) {
                primes.add(p);
            }
        }
    }

    public void cancel() { cancelled = true; }

    public synchronized List<BigInteger> get() {
        return new ArrayList<BigInteger>(primes);
    }
}
```

## Interruption

The cancellation policy via a boolean flag might not work properly in certain scenarios.
For example, if a thread is blocked on a `BlockingQueue::take` and the producers have stopped putting work into the queue, the task will hang forever.

Example of this problem:
```java
class BrokenPrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;
    private volatile boolean cancelled = false;

    BrokenPrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!cancelled)
                queue.put(p = p.nextProbablePrime());
        } catch (InterruptedException consumed) { }
    }

    public void cancel() { cancelled = true; }
}

void consumePrimes() throws InterruptedException {
    BlockingQueue<BigInteger> primes = ...;
    BrokenPrimeProducer producer = new BrokenPrimeProducer(primes);
    producer.start();
    try {
        while (needMorePrimes())
        consume(primes.take());
    } finally {
        producer.cancel();
    }
}
```

An alternative way to handle interruption is by relying the `Thread` class' `isInterrupted()` status.
Tasks implementing this interruption policy can poll for this status & trigger it via Thread's instance method `interrupt`.

Thread interruption API:
```java
public class Thread {
    public void interrupt() { ... }
    public boolean isInterrupted() { ... }
    public static boolean interrupted() { ... }
    ...
}
```

Calling `interrupt` doesn't necessarily stop the target thread from doing its work. It merely delivers the message that interruption has been requested.

> Interruption is usually the most sensible way to implement cancellation.

Good example of interruption for the `PrimeProducer`:
```java
class PrimeProducer extends Thread {
    private final BlockingQueue<BigInteger> queue;

    PrimeProducer(BlockingQueue<BigInteger> queue) {
        this.queue = queue;
    }

    public void run() {
        try {
            BigInteger p = BigInteger.ONE;
            while (!Thread.currentThread().isInterrupted())
                queue.put(p = p.nextProbablePrime());
        } catch (InterruptedException consumed) {
        /* Allow thread to exit */
        }
    }

    public void cancel() { interrupt(); }
}
```

## Interruption policies

Tasks should have cancellation policies. Threads should have an interruption policy.

An interruption policy determines how a thread interprets an interruption request.  
The most sensible interruption policy is some form of service-level cancellation - exit as quickly as possible, clean up if necessary, notify interested entities, etc.

Code that doesn't own the thread should be careful to preserce the interrupted status of the thread so that the owning code can eventually act on it.

An example of a thread you don't own is a task executing in a thread pool. The task doesn't own the thread it is running on, the thread pool does.

Most blocking library classes throw an `InterruptedException` when interrupted as they never execute in a thread they own.

The most sensible action for clients of such classes would be to not handle the exception and let it propagate to the caller.
Alternatively, if you want to handle it and do some clean up, make sure to set the current thread's `interrupted` status:
```java
Thread.currentThread().interrupt();
```

## Responding to interruption
Two practical strategies for dealing with `InterruptedException`:
 * Propagate the exception
 * Restore the interrupted status of the thread & handle the exception

Example propagation of the exception:
```java
BlockingQueue<Task> queue;
...
public Task getNextTask() throws InterruptedException {
    return queue.take();
}
```

You should never swallow the exception & do nothing about it. This is only acceptable if your code is implementing the interruption policy for a thread.

If you have activities that don't support cancellation, you should preserve the interruption status when encountered & restore it on exit:
```java
public Task getNextTask(BlockingQueue<Task> queue) {
    boolean interrupted = false;
    try {
        while (true) {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                interrupted = true;
                // fall through and retry
            }
        }
    } finally {
        if (interrupted)
            Thread.currentThread().interrupt();
    }
}
```

Choosing a polling policy for checking the interrupted status is a trade-off between efficiency and responsiveness.

## Example: timed run
This example shows a prime generator which runs for one second only. 

The goal is to handle execution exceptions which might occur & propagate them to the caller. If the runnable is executing in a separate thread, the execution exception will not propagate.

This is a bad example of thread interruption as there is a scheduled interrupt on a borrowed thread (the caller's).
```java
private static final ScheduledExecutorService cancelExec = ...;

public static void timedRun(Runnable r, long timeout, TimeUnit unit) {
    final Thread taskThread = Thread.currentThread();
    cancelExec.schedule(new Runnable() {
        public void run() { taskThread.interrupt(); }
    }, timeout, unit);

    r.run();
}
```

The problem is that the interrupt can trigger after the task has finished & we don't know what code can be executing at that point.

## Cancellation via Future
The best way to handle the previous problem is via a `FutureTask`. It supports receiving the execution exception & handling it & also supports cancellation:
```java
public static void timedRun(Runnable r, long timeout, TimeUnit unit) throws InterruptedException {
    Future<?> task = taskExec.submit(r);
    try {
        task.get(timeout, unit);
    } catch (TimeoutException e) {
        // task will be cancelled below
    } catch (ExecutionException e) {
        // exception thrown in task; rethrow
        throw launderThrowable(e.getCause());
    } finally {
        // Harmless if task already completed
        task.cancel(true); // interrupt if running
    }
}
```

## Dealing with non-interruptible blocking
Sometimes, you might be doing some work which is non-interruptible & needs special care:
 * Synchronous socker I/O in java.io - The read and write methods are not responsive to interruption, but closing the underlying socket makes blocked threads throw a `SocketException`
 * Synchronous I/O in java.nio
 * Asynchronous I/O with Selector - If a thread is blocked on Selector.select, you have to close the underlying channel to unblock
 * Lock acquisition - If a thread is blocked waiting for an intrinsic lock, there is nothing for you to do to stop it from acquiring the lock

These are all problems which either require special clean up before being interrupted (interrupting the thread is not enough) or cannot be interrupted at all.

To handle this non-standard interruption, you could extend the `interrupt` method of the `Thread` class:
```java
public class ReaderThread extends Thread {
    private final Socket socket;
    private final InputStream in;

    public ReaderThread(Socket socket) throws IOException {
        this.socket = socket;
        this.in = socket.getInputStream();
    }

    public void interrupt() {
        try {
            socket.close();
        }
        catch (IOException ignored) { }
        finally {
            super.interrupt();
        }
    }

    public void run() {
        try {
            byte[] buf = new byte[BUFSZ];
            while (true) {
                int count = in.read(buf);
                if (count < 0)
                    break;
                else if (count > 0)
                    processBuffer(buf, count);
            }
        } catch (IOException e) { /* Allow thread to exit */ }
    }
}
```

## Encapsulating non-standard interruption with newTaskFor
You can extend the `FutureTask` & implement a `CancellableFuture` which has special interruption logic for solving the above problems as well:
```java
public interface CancellableTask<T> extends Callable<T> {
    void cancel();
    RunnableFuture<T> newTask();
}

@ThreadSafe
public class CancellingExecutor extends ThreadPoolExecutor {
    ...

    protected<T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        if (callable instanceof CancellableTask)
            return ((CancellableTask<T>) callable).newTask();
        else
            return super.newTaskFor(callable);
    }
}

public abstract class SocketUsingTask<T> implements CancellableTask<T> {
    @GuardedBy("this") private Socket socket;

    protected synchronized void setSocket(Socket s) { socket = s; }

    public synchronized void cancel() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignored) { }
    }

    public RunnableFuture<T> newTask() {
        return new FutureTask<T>(this) {
            public boolean cancel(boolean mayInterruptIfRunning) {
                try {
                    SocketUsingTask.this.cancel();
                } finally {
                    return super.cancel(mayInterruptIfRunning);
                }
            }
        };
    }
}
```

# Stopping a thread-based service
Threads have owners & the owner is the one who created the thread.

In many occassions, that's the thread pool implementation you are using. Since it is the owner, you shouldn't attempt to stop its threads yourself.

Instead, the service should provide lifecycle methods for managing the threads. The `ExecutorService` has the `shutdown` and `shutdownNow` methods for dealing with that.

## Example: a logging service
`LogWriter` is a simple logging utility which logs lines in a separate thread. It receives the logs via a blocking queue:
```java
public class LogWriter {
    private final BlockingQueue<String> queue;
    private final LoggerThread logger;

    public LogWriter(Writer writer) {
        this.queue = new LinkedBlockingQueue<String>(CAPACITY);
        this.logger = new LoggerThread(writer);
    }

    public void start() { logger.start(); }
    public void log(String msg) throws InterruptedException {
        queue.put(msg);
    }

    private class LoggerThread extends Thread {
        private final PrintWriter writer;
        ...
        public void run() {
            try {
                while (true)
                    writer.println(queue.take());
            } catch(InterruptedException ignored) {
            } finally {
                writer.close();
            }
        }
    }
}
```

The problem with this implementation is that there is no way to shutdown the logger gracefully. A bad example is:
```java
public void log(String msg) throws InterruptedException {
    if (!shutdownRequested)
        queue.put(msg);
    else
        throw new IllegalStateException("logger is shut down");
}
```

This is not satisfactory as some buffered messages could be dropped & not logged at all. Additionally, there is a `check-then-act` race condition.

A proper way to shutdown the logger is by implementing "logging reservations" & using intrinsic lock to avoid the race conditions:
```java
public class LogService {
    private final BlockingQueue<String> queue;
    private final LoggerThread loggerThread;
    private final PrintWriter writer;
    @GuardedBy("this") private boolean isShutdown;
    @GuardedBy("this") private int reservations;

    public void start() { loggerThread.start(); }
    public void stop() {
        synchronized (this) { isShutdown = true; }
        loggerThread.interrupt();
    }

    public void log(String msg) throws InterruptedException {
        synchronized (this) {
            if (isShutdown)
                throw new IllegalStateException(...);
            ++reservations;
        }
        queue.put(msg);
    }

    private class LoggerThread extends Thread {
        public void run() {
            try {
                while (true) {
                    try {
                        synchronized (LogService.this) {
                            if (isShutdown && reservations == 0)
                                break;
                        }
                        String msg = queue.take();
                        synchronized (LogService.this) {
                            --reservations;
                        }
                        writer.println(msg);
                    } catch (InterruptedException e) { /* retry */ }
                }
            } finally {
                writer.close();
            }
        }
    }
}
```

## ExecutorService shutdownn
The executor service has the `shutdown` and `shutdownNow` utilities for graceful & not so graceful shutdown.

Simple programs can get away with using a global executor service, initialized from main.

More sophisticated applications might encapsulate the executors in higher-level services. These services should also provide lifecycle methods for managing their underlying executor service.

Example:
```java
public class LogService {
    private final ExecutorService exec = newSingleThreadExecutor();
    ...
    public void start() { }
    public void stop() throws InterruptedException {
        try {
            exec.shutdown();
            exec.awaitTermination(TIMEOUT, UNIT);
        } finally {
            writer.close();
        }
    }

    public void log(String msg) {
        try {
            exec.execute(new WriteTask(msg));
        } catch (RejectedExecutionException ignored) { }
    }
}
```

## Poison pills
Another way to convince a producer-consumer components to shutdown is by designating a "poison pill" object. Once the consumer receives it, it shuts down its execution.

Example service:
```java
public class IndexingService {
    private static final File POISON = new File("");
    private final IndexerThread consumer = new IndexerThread();
    private final CrawlerThread producer = new CrawlerThread();
    private final BlockingQueue<File> queue;
    private final FileFilter fileFilter;
    private final File root;
    class CrawlerThread extends Thread { /* Listing 7.18 */ }
    class IndexerThread extends Thread { /* Listing 7.19 */ }

    public void start() {
        producer.start();
        consumer.start();
    }

    public void stop() { producer.interrupt(); }
    public void awaitTermination() throws InterruptedException {
        consumer.join();
    }
}
```

Example producer:
```java
public class CrawlerThread extends Thread {
    public void run() {
        try {
            crawl(root);
        } catch (InterruptedException e) { /* fall through */ }
        finally {
            while (true) {
                try {
                    queue.put(POISON);
                    break;
                } catch (InterruptedException e1) { /* retry */ }
            }
        }
    }

    private void crawl(File root) throws InterruptedException {
        ...
    }
}
```

Example consumer:
```java
public class IndexerThread extends Thread {
    public void run() {
        try {
            while (true) {
                File file = queue.take();
                if (file == POISON)
                    break;
                else
                    indexFile(file);
            }
        } catch (InterruptedException consumed) { }
    }
}
```

This technique works well when the number of producers & consumers is known.
If there are more producers, the consumer can stop once it has received N pills.
If there are more consumers, the producers can put N pills on the queue to notify all consumers.

This works well only with unbounded queues.

## Example: A one-shot execution service
If you need to process something in parallel in the context of a single method, you can use a private executor, which you shutdown before method exit:
```java
boolean checkMail(Set<String> hosts, long timeout, TimeUnit unit) throws InterruptedException {
    ExecutorService exec = Executors.newCachedThreadPool();
    final AtomicBoolean hasNewMail = new AtomicBoolean(false);
    try {
        for (final String host : hosts)
            exec.execute(new Runnable() {
                public void run() {
                    if (checkMail(host))
                        hasNewMail.set(true);
                }
            });
    } finally {
        exec.shutdown();
        exec.awaitTermination(timeout, unit);
    }

    return hasNewMail.get();
}
```

## Limitations of shutdownNow
When you invoke `shutdownNow`, it attempts to shutdown the tasks which were in progress. It also returns a list of the tasks which were never started.

There is no out-of-the-box way, however, to get the tasks which were in progress & never finished. This can be achieved by extending the executor service:
```java
public class TrackingExecutor extends AbstractExecutorService {
    private final ExecutorService exec;
    private final Set<Runnable> tasksCancelledAtShutdown = Collections.synchronizedSet(new HashSet<Runnable>());
    ...

    public List<Runnable> getCancelledTasks() {
        if (!exec.isTerminated())
            throw new IllegalStateException(...);
        return new ArrayList<Runnable>(tasksCancelledAtShutdown);
    }

    public void execute(final Runnable runnable) {
        exec.execute(new Runnable() {
            public void run() {
                try {
                    runnable.run();
                } finally {
                    if (isShutdown() && Thread.currentThread().isInterrupted())
                        tasksCancelledAtShutdown.add(runnable);
                }
            }
        });
    }

    // delegate other ExecutorService methods to exec
}
```

Example usage in web crawler:
```java
public abstract class WebCrawler {
    private volatile TrackingExecutor exec;
    @GuardedBy("this")
    private final Set<URL> urlsToCrawl = new HashSet<URL>();
    ...

    public synchronized void start() {
        exec = new TrackingExecutor(
        Executors.newCachedThreadPool());
        for (URL url : urlsToCrawl) submitCrawlTask(url);
            urlsToCrawl.clear();
    }

    public synchronized void stop() throws InterruptedException {
        try {
            saveUncrawled(exec.shutdownNow());
            if (exec.awaitTermination(TIMEOUT, UNIT))
                saveUncrawled(exec.getCancelledTasks());
        } finally {
            exec = null;
        }
    }

    protected abstract List<URL> processPage(URL url);

    private void saveUncrawled(List<Runnable> uncrawled) {
        for (Runnable task : uncrawled)
            urlsToCrawl.add(((CrawlTask) task).getPage());
    }

    private void submitCrawlTask(URL u) {
        exec.execute(new CrawlTask(u));
    }

    private class CrawlTask implements Runnable {
        private final URL url;
        ...

        public void run() {
            for (URL link : processPage(url)) {
                if (Thread.currentThread().isInterrupted())
                    return;
                submitCrawlTask(link);
            }
        }

        public URL getPage() { return url; }
    }
}
```

There is an unavoidable race condition - if the thread pool is shutdown before the task exits but after its last instruction, it would be marked uncompleted but be completed regardless.  
This can be fixed by making sure the submitted tasks are idempotent.

# Handling abnormal thread termination

If a thread fails, its stack trace is printed on the terminal, but there is no way to catch it in another thread & handle it. Losing a thread like this can be disastrous in certain cases.
Unless someone is monitoring the terminal all the time, you might miss some important shortcomings of your application.

Losing a thread in a thread pool might mean that now you have 49 instead of 50 threads to work with, but losing a thread in the GUI, might block your entire UI.

Here is a technique for worker threads calling untrusted external code:
```java
public void run() {
    Throwable thrown = null;
    try {
        while (!isInterrupted())
            runTask(getTaskFromWorkQueue());
    } catch (Throwable e) {
        thrown = e;
    } finally {
        threadExited(this, thrown);
    }
}
```

## Uncaught exception handlers
Another way to deal with uncaught exceptions is via the Thread API's uncaught exception handler:
```java
public interface UncaughtExceptionHandler {
    void uncaughtException(Thread t, Throwable e);
}
```

Example usage:
```java
public class UEHLogger implements Thread.UncaughtExceptionHandler {
    public void uncaughtException(Thread t, Throwable e) {
        Logger logger = Logger.getAnonymousLogger();
        logger.log(Level.SEVERE, "Thread terminated with exception: " + t.getName(), e);
    }
}
```

To instantiate an uncaught exception handler, you could provide a `ThreadFactory` to the thread pool constructor.

Tasks submitted with `submit` shouldn't bother with this mechanism as their exception is part of the "return status". You can handle it with a `Future` instance.

# JVM Shutdown
The JVM can shutdown in an orderly or an abrupt manner. An orderly shutdown is when all non-daemon threads exit or something external is stopping the application (user clicks Ctrl+C).

## Shutdown hooks
In an orderly shutdown, the JVM starts all registered shutdown hooks. You can use this to make any clean up logic on exit.
Shutdown hooks can be registed with `Runtime.addShutdownHook`.

The JVM makes no guarantees on the order of execution of shutdown hooks.
If the shutdown hooks or finalizers don't exit, than the JVM hangs and has to be stopped abruptly.

Shutdown hooks should be thread-safe & exit as quickly as possible. Sine shutdown hooks run in parallel, so they shouldn't depend on services that could be shutdown in another hook.
To avoid this problem, you could encapsulate your entire shutdown mechanism in a single hook so that everything is executed synchronously in a single thread.

Example usage:
```java
public void start() {
    Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
            try { LogService.this.stop(); }
            catch (InterruptedException ignored) {}
        }
    });
}
```

## Daemon threads
Daemon threads can perform some helper function but they don't prevent the JVM from shutting down.

The garbage collector is a daemon thread. You should avoid using I/O in daemon threads.

## Finalizers
Finalizers can be used to do some clean up after a class is garbage collected.

However, there is no guarantee when they will execute or whether they will execute at all.
It is very hard to write finalizers correctly

You should only use them when tearing down objects that manage resources acquired by native methods.

> Avoid using finalizers
