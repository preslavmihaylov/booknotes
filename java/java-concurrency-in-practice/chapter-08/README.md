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

Consider using multiple thread pools if you have heterogenous tasks as this way, the pools can be adjusted based on the nature of the executing tasks.

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
Priority can be defined by natural order or if the tasks implement `Comparable` - via a `Comparator`.

Bounding the thread pool or work queue is only suitable when tasks are independent.

> The `newCachedThreadPool` is a good first choice thread pool configuration as it is more efficient that a fixed thread pool. A fixed thread pool is viable if you want to limit the # of concurrently running tasks.

## Saturation policies
The saturation policy for a thread pool can be modified by configuring the `RejectedExecutionHandler`.
