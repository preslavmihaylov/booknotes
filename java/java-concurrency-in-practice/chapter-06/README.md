# Chapter 06 - Task Execution

- [Executing tasks in threads](#executing-tasks-in-threads)
  - [Executing tasks sequentially](#executing-tasks-sequentially)
  - [Explicitly creating threads for tasks](#explicitly-creating-threads-for-tasks)
  - [Disadvantages of unbounded thread creation](#disadvantages-of-unbounded-thread-creation)
- [The Executor framework](#the-executor-framework)
  - [Example: web server using Executor](#example-web-server-using-executor)
  - [Execution policies](#execution-policies)
  - [Thread pools](#thread-pools)
  - [Executor lifecycle](#executor-lifecycle)
  - [Delayed and periodic tasks](#delayed-and-periodic-tasks)
- [Finding exploitable parallelism](#finding-exploitable-parallelism)
  - [Example: sequential page renderer](#example-sequential-page-renderer)
  - [Result-bearing tasks: Callable and Future](#result-bearing-tasks-callable-and-future)
  - [Example: page renderer with Future](#example-page-renderer-with-future)
  - [Limitations of parallelizing heterogeneous tasks](#limitations-of-parallelizing-heterogeneous-tasks)
  - [CompletionService: Executor meets BlockingQueue](#completionservice-executor-meets-blockingqueue)
  - [Example: page renderer with CompletionService](#example-page-renderer-with-completionservice)
  - [Placing time limits on tasks](#placing-time-limits-on-tasks)
  - [Example: a travel reservations portal](#example-a-travel-reservations-portal)
- [Summary](#summary)

Most concurrent applications are structured in task execution - discrete chunks of computations.  

Organizing your program in tasks simplifies program structure and promotes concurrency as a natural structure for parallelizing work.

# Executing tasks in threads
First one needs to specify sensible task boundaries. Ideally, tasks are independent chunks of work - not dependent on the application's state. For greater flexibility in performance tuning & throughput, ideally tasks should also be small.

The task execution policy should be specified as well.

## Executing tasks sequentially
One possible task execution policy is executing tasks sequentially in a single thread.

Example:
```java
class SingleThreadWebServer {
    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(80);
        while (true) {
            Socket connection = socket.accept();
            handleRequest(connection);
        }
    }
}
```

For most server applications, however, this doesn't achieve the desired throughput as only a single request can be handled at a time.

## Explicitly creating threads for tasks
A more responsive approach is creating a new thread for every new request that comes in.

Example:
```java
class ThreadPerTaskWebServer {
    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(80);
        while (true) {
            final Socket connection = socket.accept();
            Runnable task = new Runnable() {
                public void run() {
                    handleRequest(connection);
                }
            };
            new Thread(task).start();
        }
    }
}
```

This enables tasks to be executed in parallel. Task-handling code has to be thread-safe, though.

This approach is fine for small to medium traffic. As long as the incoming requests don't exceed the server's capacity, this approach is an improvement.

## Disadvantages of unbounded thread creation
For production use, creating threads unboundedly has some drawbacks:
 * Thread lifecycle overhead - creating & managing threads has some overhead, it is not free. If the threads are too many, the multi-threaded application might become slower than the single-threaded one.
 * Resource consumption - active threads consume system resources, especially memory.
 * Stability - there is a limit on how many threads one can create. This varies by platform, but once you hit it, you would get an `OutOfMemoryException`.

Up to a certain point, creating threads improve your application's throughput, but beyond it, more threads start getting in the way.

# The Executor framework
A way to bound your thread creation is by using a thread pool. This is provided by the Executor framework with the `Executor` interface:
```java
public interface Executor {
    void execute(Runnable command);
}
```

This helps you implement the producer-consumer pattern in your system's design and the Executor framework is usually the easiest way to do it.

## Example: web server using Executor
```java
class TaskExecutionWebServer {
    private static final int NTHREADS = 100;
    private static final Executor exec = Executors.newFixedThreadPool(NTHREADS);

    public static void main(String[] args) throws IOException {
        ServerSocket socket = new ServerSocket(80);
        while (true) {
            final Socket connection = socket.accept();
            Runnable task = new Runnable() {
                public void run() {
                    handleRequest(connection);
                }
            };
            exec.execute(task);
        }
    }
}
```

## Execution policies
Decoupling task submission from execution lets you easily change the execution policy for a given class of tasks.

It let's you, for example, to implement even a single-threaded execution policy without that having an effect on the task handling logic.

Whenever you see a `new Thread(r).run()`, in most cases you'll probably want to use an Executor instead.

## Thread pools
A thread pool manages a homogenous pool of worker threads.

The main advantage is that you can reuse existing threads rather than create new threads for each task.

Available static factory methods in the Executor for creating thread pools:
 * `newFixedThreadPool` - creates threads as tasks are submitted up to a specified maximum
 * `newCachedThreadPool` - has more flexibility to kill threads when the current thread count exceeds current demand, but doesn't have a maximum bound
 * `newSingleThreadExecutor` - A single-threaded executor. The thread is substituted for a new one if it unexpectedly dies. Tasks are processed in-order
 * `newScheduledThreadPool` - A fixed size thread pool, which supports delayed/scheduled task execution

## Executor lifecycle
The JVM will not exit until all pending non-daemon threads are shutdown. Hence, your application won't exit until your executor has shutdown.

The executor has some lifecycle methods to support graceful (and not so graceful) shutdown:
```java
public interface ExecutorService extends Executor {
    void shutdown();
    List<Runnable> shutdownNow();
    boolean isShutdown();
    boolean isTerminated();
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;
    // ... additional convenience methods for task submission
}
```

The `shutdown` method initiates a graceful shutdown - no new tasks are accepted & the currently running ones are allowed to complete.
`shutdownNow` on the other hand, initiates an abrupt shutdown. Running tasks are attempted to be cancelled.

Tasks submitted after a shutdown is initiated are handled by the rejected execution handler. Behavior can vary - e.g. tasks are silently discarded or an exception is thrown.

It is common to follow a `shutdown` with an `awaitTermination` to sequentially shutdown the executor.

## Delayed and periodic tasks
In the past, the `Timer` class was used for scheduling periodic or delayed tasks. After Java 5.0, there is no reason to use that anymore.

Use a `ScheduledThreadPoolExecutor` instead. There are some problems with how `Timer` works & is deprecated.

# Finding exploitable parallelism
This section covers developing a component with varying degrees of concurrency. It is an HTML page-rendering component.

## Example: sequential page renderer
```java
public class SingleThreadRenderer {
    void renderPage(CharSequence source) {
        renderText(source);
        List<ImageData> imageData = new ArrayList<ImageData>();
        for (ImageInfo imageInfo : scanForImageInfo(source))
            imageData.add(imageInfo.downloadImage());
        for (ImageData data : imageData)
            renderImage(data);
    }
}
```

The problem with this component is that there is an untapped concurrency opportunity - downloading image data can be made in parallel to text rendering.

## Result-bearing tasks: Callable and Future
Executor supports executing `Runnable`s which are a fairly basic task abstraction. Some use-cases require result-bearing tasks, for example.

For these use-cases, prefer using a `Callable`, which supports returning values and throwing exceptions.
`Future`, on the other hand, represents the lifecycle of a task & allows you to inspect whether the task was cancelled, has completed, cancel it explicitly, etc.

If you simply execute `get` on the `Future`, then you are blocking until the result has been computed.

`Callable` and `Future` interfaces:
```java
public interface Callable<V> {
    V call() throws Exception;
}

public interface Future<V> {
    boolean cancel(boolean mayInterruptIfRunning);
    boolean isCancelled();
    boolean isDone();
    V get() throws InterruptedException, ExecutionException, CancellationException;
    V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, CancellationException, TimeoutException;
}
```

The executor framework supports submitting futures via the `submit` method. You could also explicitly instantiate a `FutureTask` & `execute` it (as it implements `Runnable`).

Finally, you can create a `FutureTask` from a `Callable`:
```java
protected <T> RunnableFuture<T> newTaskFor(Callable<T> task) {
    return new FutureTask<T>(task);
}
```

Submitting a `Runnable` or a `Callable` to an `Executor` constitutes safe publication.

## Example: page renderer with Future
To make the page renderer more concurrent, it can be divided in two tasks - one for fetching images & one for rendering text.
```java
public class FutureRenderer {
    private final ExecutorService executor = ...;

    void renderPage(CharSequence source) {
        final List<ImageInfo> imageInfos = scanForImageInfo(source);
        Callable<List<ImageData>> task = new Callable<List<ImageData>>() {
            public List<ImageData> call() {
                List<ImageData> result = new ArrayList<ImageData>();
                for (ImageInfo imageInfo : imageInfos)
                    result.add(imageInfo.downloadImage());
                return result;
            }
        };

        Future<List<ImageData>> future = executor.submit(task);
        renderText(source);
        try {
            List<ImageData> imageData = future.get();
            for (ImageData data : imageData)
                renderImage(data);
        } catch (InterruptedException e) {
            // Re-assert the thread’s interrupted status
            Thread.currentThread().interrupt();

            // We don’t need the result, so cancel the task too
            future.cancel(true);
        } catch (ExecutionException e) {
            throw launderThrowable(e.getCause());
        }
    }
}
```

This is an improvement as the page can be rendered while the images are being downloaded. But we can do better as users would probably prefer seeing images being rendered as they are downloaded.

## Limitations of parallelizing heterogeneous tasks
Trying to parallelize sequential heterogeneous (very different) tasks can be tricky to do & yield not much improvement in the end.
Since there are two very distinct tasks at hand (rendering text & downloading images), trying to parallelize this to more than two threads is tricky.

Additionally, if task A takes ten times as much time as task B, then the overall performance improvement is not great.
In the previous example, the overall improvement might not be great as text rendering can be very fast, while image download can be disproportionately slower.

> The real performance gain from parallelizing work comes from parallelizing independent homogenous (similar) tasks.

## CompletionService: Executor meets BlockingQueue
The `CompletionService` combines the functionality of an executor and a blocking queue.

You can submit tasks for execution to the service & retrieve them in a queue-like manner as they are completed.

## Example: page renderer with CompletionService
In this example, image download is parallelized using the `CompletionService` & images are rendered as they are downloaded:
```java
public class Renderer {
    private final ExecutorService executor;

    Renderer(ExecutorService executor) { this.executor = executor; }
    void renderPage(CharSequence source) {
        List<ImageInfo> info = scanForImageInfo(source);
        CompletionService<ImageData> completionService = new ExecutorCompletionService<ImageData>(executor);
        for (final ImageInfo imageInfo : info)
            completionService.submit(new Callable<ImageData>() {
                public ImageData call() {
                    return imageInfo.downloadImage();
                }
            });

        renderText(source);
        try {
            for (int t = 0, n = info.size(); t < n; t++) {
                Future<ImageData> f = completionService.take();
                ImageData imageData = f.get();
                renderImage(imageData);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            throw launderThrowable(e.getCause());
        }
    }
}
```

## Placing time limits on tasks
Sometimes, you might want to timeout if a task doesn't complete in a given time interval.

This can also be achieved via `Future`'s `get` method \w time-related parameters.

If a timed `get` completes with a `TimeoutException`, you can cancel the task via the future.
If it completes on time, normal execution continues.

In the scenario below, there is some ad being downloaded from an external vendor, but if it is not loaded on time, a default ad is shown:
```java
Page renderPageWithAd() throws InterruptedException {
    long endNanos = System.nanoTime() + TIME_BUDGET;

    Future<Ad> f = exec.submit(new FetchAdTask());
    // Render the page while waiting for the ad
    Page page = renderPageBody();
    Ad ad;
    try {
        // Only wait for the remaining time budget
        long timeLeft = endNanos - System.nanoTime();
        ad = f.get(timeLeft, NANOSECONDS);
    } catch (ExecutionException e) {
        ad = DEFAULT_AD;
    } catch (TimeoutException e) {
        ad = DEFAULT_AD;
        f.cancel(true);
    }

    page.setAd(ad);
    return page;
}
```

## Example: a travel reservations portal
In this example, the time-budgeting solution is generalized to a set of tasks (rather than a single task).

A travel reservations portal shown bids from various companies to a user, who has input a travel date (e.g. Booking.com).  
Depending on the company, fetching the bid might be very slow.

Rather than letting the response time for the page be driven by the slowest bid response, you could display only the information available within a given time budget.

Fetching a bid from one company is independent from fetching from another so that task can be easily & effectively parallelized.

This solution leverages `invokeAll` which allows you to submit a set of tasks at once (rather than submitting them one at a time & appending the `Future` in some list).
The returned collection from `invokeAll` has the same order as the input collection of tasks, allowing you to associate a task to a future.

```java
private class QuoteTask implements Callable<TravelQuote> {
    private final TravelCompany company;
    private final TravelInfo travelInfo;
    ...

    public TravelQuote call() throws Exception {
        return company.solicitQuote(travelInfo);
    }
}

public List<TravelQuote> getRankedTravelQuotes(TravelInfo travelInfo, Set<TravelCompany> companies,
                                               Comparator<TravelQuote> ranking, long time, TimeUnit unit)
        throws InterruptedException {
    List<QuoteTask> tasks = new ArrayList<QuoteTask>();
    for (TravelCompany company : companies)
        tasks.add(new QuoteTask(company, travelInfo));

    List<Future<TravelQuote>> futures = exec.invokeAll(tasks, time, unit);
    List<TravelQuote> quotes = new ArrayList<TravelQuote>(tasks.size());
    Iterator<QuoteTask> taskIter = tasks.iterator();
    for (Future<TravelQuote> f : futures) {
        QuoteTask task = taskIter.next();
        try {
            quotes.add(f.get());
        } catch (ExecutionException e) {
            quotes.add(task.getFailureQuote(e.getCause()));
        } catch (CancellationException e) {
            quotes.add(task.getTimeoutQuote(e));
        }
    }

    Collections.sort(quotes, ranking);
    return quotes;
}
```

# Summary
Structuring applications around the execution of tasks can simplify development and facilitate concurrency. 

The Executor framework permits you to decouple task submission from execution policy and supports a rich variety of execution policies; 

whenever you find yourself creating threads to perform tasks, consider using an Executor instead. 
To maximize the benefit of decomposing an application into tasks, you must identify sensible task boundaries. 

In some applications, the obvious task boundaries work well, whereas in others some analysis may be required to uncover finer-grained exploitable parallelism
