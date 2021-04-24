# Chapter 08 - Applying Thread Pools
This chapter focuses on advanced options & configuration for tuning thread pools, shows advanced examples for using Executor & demonstrates some hazards to watch out for.

## Implicit couplings between tasks & execution policies
The executor framework claims that it decouples tasks from the execution policy.

This was a bit of an overstatement as not all tasks are compatible with all execution policies.

Types of tasks which require specific execution policies:
 * Dependent tasks - tasks which depend on other tasks submitted to the executor
 * Tasks that exploit thread confinement - running these tasks require that they are not executed concurrently
 * Response-time sensitive tasks - Submitting a long-running time to a single threaded executor could get on the users' nerves
 * Tasks that use ThreadLocal - Executors are free to reuse threads as they see fit. Hence, it is not guaranteed that different tasks will use their own ThreadLocal context

Thread pools work best when tasks are homogenous & independent. Fortunately, requests in web-based applications usually meet these constraints.

**When your tasks depend on the execution policy, document this to ensure future maintainers don't change the execution policy.**

## Thread starvation deadlock
If tasks depend on other tasks in a thread pool, they can deadlock.

In a single-threaded executor, a task that submits another task in the same executor & waits for its result always deadlocks.  
This can also happen in larger thread pools when all running tasks are waiting for other tasks which are in the queue.

This is referred to as thread starvation deadlock. It can happen when a pool task is waiting on some condition which can only be reached via another pool task.

Example:
```java
public class ThreadDeadlock {
    ExecutorService exec = Executors.newSingleThreadExecutor();

    public class RenderPageTask implements Callable<String> {
        public String call() throws Exception {
            Future<String> header, footer;
            header = exec.submit(new LoadFileTask("header.html"));
            footer = exec.submit(new LoadFileTask("footer.html"));
            String page = renderBody();

            // Will deadlock -- task waiting for result of subtask
            return header.get() + page + footer.get();
        }
    }
}
```

If `RenderPageTask` is submitted to the same executor as the `LoadFileTask`s, this will always deadlock if the executor is single-threaded.

Tasks coordinating via a barrier could also deadlock if the pool is not big enough.

In addition to explicit bounds on the thread pool size, there might also be implicit bounds caused by dependent resources.

E.g. if you only have 10 JDBC connections available, a thread pool with more than 10 threads will not perform any better.

## Long-running tasks
A thread pool can get clogged even if there is no deadlock if it has some long-running tasks which hinder the responsiveness of even small tasks.
This can happen if the thread pool is not big enough.

One technique to mitigate this is for long-running tasks to use bounded resource waits instead of unbounded ones.
Most blocking methods in the platform library come with a timed & untimed version.

# Sizing thread pools
The ideal size depends on the types of tasks that will be submitted and the characteristics of the deployment system.

Thread pool sizes should rarely be hardcoded. They should be provided by a configuration or derived dynamically by consulting `Runtime.availableProcessors`.

It is not an exact science, but as long as you avoid making thread pools too big or too small, you're OK.
If a pool is too big, threads will fight over shared, limited hardware resources. If it is too small, throughput of tasks suffer.

Sizing a threadpool depends on:
 * What are the hardware resources of the deployment system
 * Do tasks perform mostly computation, I/O or some combination?

Consider using multiple thread pools if you have heterogeneous tasks as this way, the pools can be adjusted based on the nature of the executing tasks.

For compute-intensive tasks, a rule of thumb is using N+1 threads, where N = processor count.

For tasks that include I/O or other blocking operations, you want a larger pool, since not all tasks will be schedulable at all times.

A way to calculate the optimal pool size is to run the application multiple times & benchmarking what configuration yields the best results.

Alternatively, you can stick to this provided formulae:
```
N - CPU count
U - target CPU utilization (value between 0 and 1)
W/C - Ratio of wait time to compute time

optimal pool size = N * U * (1 + W/C)
```

# Configuring ThreadPoolExecutor
Most of the time, you can use the provided static factory methods for creating thread pools, configured out of the box.

This section covers the details of making your own thread pool configuration.

Here is the `ThreadPoolExecutor` constructor:
```java
public ThreadPoolExecutor(int corePoolSize,
                          int maximumPoolSize,
                          long keepAliveTime,
                          TimeUnit unit,
                          BlockingQueue<Runnable> workQueue,
                          ThreadFactory threadFactory,
                          RejectedExecutionHandler handler) { ... }
```

## Thread creation & teardown
The core pool size is the size the executor attempts to maintain the thread count at.
The maximum pool size governs the maximum threads the executor could create under heavy load.

When the load is not as heavy, they will be teared down until the `corePoolSize` is reached.
The `keepAliveTime` is the time an idle thread is kept alive before becoming a candidate for teardown if current thread count > `corePoolSize`.

`newFixedThreadPool` sets `corePoolSize` == `maximumPoolSize`, effectively making the `keepAliveTime` infinite.
`newCachedThreadPool` sets the `maximumPoolSize` to `Integet.MAX_VALUE` and `corePoolSize` to 0.

## Managing queued tasks
Bounded thread pools limit the number of tasks that can be executed concurrently.

When tasks are queued for execution as there are no idle threads to pick them up, they are enqueued in the `workQueue`.
If the workQueue is unbounded, as is the case with the `newFixedThreadPool` and `newSingleThreadExecutor` factory methods, you might run out of memory.

You have three options:
 * Unbounded queue - via a `LinkedBlockingQueue`
 * Bounded queue - via an `ArrayBlockingQueue`
 * Synchronous hand-off - via `SynchronousQueue`

When using bounded queues, the question arises - what to do with submitted tasks when the queue is full? See next section for saturation policies.

When using synchronous hand-off, the thread submitting the task hands it off to a thread waiting on the queue directly. 

If there are no available threads but the `maximumPoolSize` is not reached, a new thread is created which accepts the hand-off.
If there are no available threads and the maximum size is reached, the task is rejected in accordance to the saturation policy.

Additionally, using synchronous hand-off is more efficient than using a normal queue as tasks are handed to threads directly rather than being enqueued first.

The `newCachedThreadPool` factory uses synchronous hand-off. With it, tasks can't be rejected as the work queue is unbounded.

In addition to these mechanisms, you could use a `PriorityBlockingQueue` if you'd like certain tasks to have more priority than others.  
Priority can be defined by natural order or if the task implements `Comparable` - via a `Comparator`.

Bounding the thread pool or work queue is only suitable when tasks are independent.

> The `newCachedThreadPool` is a good first choice thread pool configuration as it is more efficient that a fixed thread pool. A fixed thread pool is viable if you want to limit the # of concurrently running tasks.

## Saturation policies
The saturation policy for a thread pool can be modified by configuring the `RejectedExecutionHandler`.

That handler is invoked when the job queue is full or when a task is submitted to an executor which is shutdown.

There are several available policies:
 * AbortPolicy - throws the unchecked `RejectedExecutionException`
 * DiscardPolicy - silently discards the rejected task
 * DiscardOldestPolicy - silently discards the oldest submitted & not yet processed task. The one which was supposed to be executed next. Note that if the work queue is a priority queue, the highest priority element will be discarded.
 * CallerRuns - implements a form of throttling. It neither discards nor throws an exception. Instead, it executes the task in the caller's thread, effectively blocking the caller & making him execute the work.

There is no predefined way to make `execute` block on overflow, but it can be achieved by using a bounded executor using a semaphore:
```java
@ThreadSafe
public class BoundedExecutor {
    private final Executor exec;
    private final Semaphore semaphore;

    public BoundedExecutor(Executor exec, int bound) {
        this.exec = exec;
        this.semaphore = new Semaphore(bound);
    }

    public void submitTask(final Runnable command) throws InterruptedException {
        semaphore.acquire();
        try {
            exec.execute(new Runnable() {
                public void run() {
                    try {
                        command.run();
                    } finally {
                        semaphore.release();
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            semaphore.release();
        }
    }
}
```

## Thread factories
Whenever a thread pool wants to create a thread, it does so via a thread factory:
```java
public interface ThreadFactory {
    Thread newThread(Runnable r);
}
```

The default thread factory creates a normal non-daemon thread.

Reasons to use a thread factory:
 * Specify an `UncaughtExceptionHandler` for the worker threads
 * Instantiate an instance of a custom thread class which implements something extra (e.g. debug logging)
 * Modify the thread priority (although generally not a good idea. Covered in chapter 10)
 * Create a daemon thread (not a good idea again. Chapter 7)
 * Give the threads a more meaningful name to simplify logging & thread dumps

Example of a thread factory implementation:
```java
public class MyThreadFactory implements ThreadFactory {
    private final String poolName;
    public MyThreadFactory(String poolName) {
        this.poolName = poolName;
    }

    public Thread newThread(Runnable runnable) {
        return new MyAppThread(runnable, poolName);
    }
}
```

Example of the passed thread, which implements user-friendly thread names, logging, metric tracking:
```java
public class MyAppThread extends Thread {
    public static final String DEFAULT_NAME = "MyAppThread";
    private static volatile boolean debugLifecycle = false;
    private static final AtomicInteger created = new AtomicInteger();
    private static final AtomicInteger alive = new AtomicInteger();
    private static final Logger log = Logger.getAnonymousLogger();

    public MyAppThread(Runnable r) { this(r, DEFAULT_NAME); }
    public MyAppThread(Runnable runnable, String name) {
        super(runnable, name + "-" + created.incrementAndGet());
        setUncaughtExceptionHandler(
            new Thread.UncaughtExceptionHandler() {
                public void uncaughtException(Thread t, Throwable e) {
                    log.log(Level.SEVERE, "UNCAUGHT in thread " + t.getName(), e);
                }
        });
    }

    public void run() {
        // Copy debug flag to ensure consistent value throughout.
        boolean debug = debugLifecycle;
        if (debug) log.log(Level.FINE, "Created "+getName());
        try {
            alive.incrementAndGet();
            super.run();
        } finally {
            alive.decrementAndGet();
            if (debug) log.log(Level.FINE, "Exiting "+getName());
        }
    }

    public static int getThreadsCreated() { return created.get(); }
    public static int getThreadsAlive() { return alive.get(); }
    public static boolean getDebug() { return debugLifecycle; }
    public static void setDebug(boolean b) { debugLifecycle = b; }
}
```

## Customizing ThreadPoolExecutor after creation
Most of the options available in the constructor can be modified after creation via setter methods.

If you don't want to allow your thread pool to be configurable after creation, you can use the `unconfigurableExecutorService` factory method.

The single-threaded executor uses this technique.

# Extending ThreadPoolExecutor
`ThreadPoolExecutor` was designed for extension, providing several hooks subclasses could attach to:
 * `beforeExecute`, `afterExecute` and `terminated`

`beforeExecute` and `afterExecute` are called in the thread that executes a task & can be used to add logging, timing, monitoring, etc. These execution hooks are called in the same thread, hence `ThreadLocal` can be used to share values between the hooks.

The `terminated` hook is called when the executor completes shutdown and there are no running tasks.

## Example: adding statistics to a thread pool
Example of instrumenting a thread pool with metrics by extending it:
```java
public class TimingThreadPool extends ThreadPoolExecutor {
    private final ThreadLocal<Long> startTime = new ThreadLocal<Long>();
    private final Logger log = Logger.getLogger("TimingThreadPool");
    private final AtomicLong numTasks = new AtomicLong();
    private final AtomicLong totalTime = new AtomicLong();

    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        log.fine(String.format("Thread %s: start %s", t, r));
        startTime.set(System.nanoTime());
    }

    protected void afterExecute(Runnable r, Throwable t) {
        try {
            long endTime = System.nanoTime();
            long taskTime = endTime - startTime.get();
            numTasks.incrementAndGet();
            totalTime.addAndGet(taskTime);
            log.fine(String.format("Thread %s: end %s, time=%dns", t, r, taskTime));
        } finally {
            super.afterExecute(r, t);
        }
    }

    protected void terminated() {
        try {
            log.info(String.format("Terminated: avg time=%dns",
            totalTime.get() / numTasks.get()));
        } finally {
            super.terminated();
        }
    }
}
```

# Parallelizing recursive algorithms
We can usually exploit loops & turn them into parallel versions of themselves via an executor:
```java
void processSequentially(List<Element> elements) {
    for (Element e : elements)
        process(e);
}

void processInParallel(Executor exec, List<Element> elements) {
    for (final Element e : elements)
        exec.execute(new Runnable() {
            public void run() { process(e); }
        });
}
```

Parallelizing a loop is viable when the computing executed in a loop is independent & the work is significant enough to justify creating new threads.

Example of turning a DFS algorithm on a tree to a parallel version:
```java
public<T> void sequentialRecursive(List<Node<T>> nodes, Collection<T> results) {
    for (Node<T> n : nodes) {
        results.add(n.compute());
        sequentialRecursive(n.getChildren(), results);
    }
}

public<T> void parallelRecursive(final Executor exec, List<Node<T>> nodes, final Collection<T> results) {
    for (final Node<T> n : nodes) {
        exec.execute(new Runnable() {
            public void run() {
                results.add(n.compute());
            }
        });
        parallelRecursive(exec, n.getChildren(), results);
    }
}
```

Example of client code, waiting for parallel results:
```java
public<T> Collection<T> getParallelResults(List<Node<T>> nodes) throws InterruptedException {
    ExecutorService exec = Executors.newCachedThreadPool();
    
    Queue<T> resultQueue = new ConcurrentLinkedQueue<T>();
    parallelRecursive(exec, nodes, resultQueue);

    exec.shutdown();
    exec.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);

    return resultQueue;
}
```

## Example: a puzzle framework
An appealing application of this technique is solving puzzles that involve finding a sequence of transformations from some initial state to reach a goal state,  
such as the familiar “sliding block puzzles”, “Hi-Q”, “Instant Insanity”, and other solitaire puzzles

Puzzle definition:
```java
public interface Puzzle<P, M> {
    P initialPosition();
    boolean isGoal(P position);
    Set<M> legalMoves(P position);
    P move(P position, M move);
}
```

The class representing the position that has been reached via some steps:
```java
@Immutable
static class Node<P, M> {
    final P pos;
    final M move;
    final Node<P, M> prev;

    Node(P pos, M move, Node<P, M> prev) {...}

    List<M> asMoveList() {
        List<M> solution = new LinkedList<M>();
        for (Node<P, M> n = this; n.move != null; n = n.prev)
            solution.add(0, n.move);
        return solution;
    }
}
```

Sequential version of the solver using DFS:
```java
public class SequentialPuzzleSolver<P, M> {
    private final Puzzle<P, M> puzzle;
    private final Set<P> seen = new HashSet<P>();

    public SequentialPuzzleSolver(Puzzle<P, M> puzzle) {
        this.puzzle = puzzle;
    }

    public List<M> solve() {
        P pos = puzzle.initialPosition();
        return search(new Node<P, M>(pos, null, null));
    }

    private List<M> search(Node<P, M> node) {
        if (seen.contains(node.pos)) {
            return null
        }

        seen.add(node.pos);
        if (puzzle.isGoal(node.pos))
            return node.asMoveList();

        for (M move : puzzle.legalMoves(node.pos)) {
            P pos = puzzle.move(node.pos, move);
            Node<P, M> child = new Node<P, M>(pos, move, node);
            List<M> result = search(child);
            if (result != null)
                return result;
        }

        return null;
    }

    static class Node<P, M> { /* Listing 8.14 */ }
}
```

Concurrent version using BFS:
```java
public class ConcurrentPuzzleSolver<P, M> {
    private final Puzzle<P, M> puzzle;
    private final ExecutorService exec;
    private final ConcurrentMap<P, Boolean> seen;
    final ValueLatch<Node<P, M>> solution = new ValueLatch<Node<P, M>>();
    ...

    public List<M> solve() throws InterruptedException {
        try {
            P p = puzzle.initialPosition();
            exec.execute(newTask(p, null, null));
            // block until solution found
            Node<P, M> solnNode = solution.getValue();
            return (solnNode == null) ? null : solnNode.asMoveList();
        } finally {
            exec.shutdown();
        }
    }

    protected Runnable newTask(P p, M m, Node<P,M> n) {
        return new SolverTask(p, m, n);
    }

    class SolverTask extends Node<P, M> implements Runnable {
        ...
        public void run() {
            if (solution.isSet() || seen.putIfAbsent(pos, true) != null)
                return; // already solved or seen this position

            if (puzzle.isGoal(pos))
                solution.setValue(this);
            else
                for (M m : puzzle.legalMoves(pos))
                    exec.execute(newTask(puzzle.move(pos, m), m, this));
        }
    }
}
```

Notable difference is using the ConcurrentMap to manage already seen positions in order to maintain thread-safety.
Additionally, the use of a `ValueLatch` ensures that once the solution is set, all tasks will stop executing any further.

The `ValueLatch` class:
```java
@ThreadSafe
public class ValueLatch<T> {
    @GuardedBy("this") private T value = null;
    private final CountDownLatch done = new CountDownLatch(1);

    public boolean isSet() {
        return (done.getCount() == 0);
    }

    public synchronized void setValue(T newValue) {
        if (!isSet()) {
            value = newValue;
            done.countDown();
        }
    }

    public T getValue() throws InterruptedException {
        done.await();
        synchronized (this) {
            return value;
        }
    }
}
```

One problem with this approach is that the program will block forever if no solution is found.
This can be managed by keeping track of the running tasks & setting the solution to `null` if it drops to zero.

This can be managed by a custom task which sets the solution to zero upon finishing its work:
```java
public class PuzzleSolver<P,M> extends ConcurrentPuzzleSolver<P,M> {
    ...
    private final AtomicInteger taskCount = new AtomicInteger(0);

    protected Runnable newTask(P p, M m, Node<P,M> n) {
        return new CountingSolverTask(p, m, n);
    }

    class CountingSolverTask extends SolverTask {
        CountingSolverTask(P pos, M move, Node<P, M> prev) {
            super(pos, move, prev);
            taskCount.incrementAndGet();
        }

        public void run() {
            try {
                super.run();
            } finally {
                if (taskCount.decrementAndGet() == 0)
                    solution.setValue(null);
            }
        }
    }
}
```
