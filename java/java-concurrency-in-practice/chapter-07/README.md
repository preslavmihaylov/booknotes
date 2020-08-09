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

