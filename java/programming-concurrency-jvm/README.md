# Programming Concurrency on the JVM

# Chapter 1: The Power and Perils of Concurrency
Programming Concurrency is hard but worthwhile.

## The Power of Concurrency
Two reasons to use concurrency:
 * Make the application more responsive -> improve user experience
 * Make it faster

### Making Apps More Responsive
Oftentimes, when we have UI applications, we have a main thread which is responsible for managing the UI and background threads which can do any kind of background jobs (e.g. making an http request).

Without parallelizing that, the user experience would be very bad as each http request will make the UI hang.

### Making Apps Faster
Operations that are currently executed sequentially can sometimes be parallelized which would lead to an app speedup.

Examples:
 * Services - An application which processes invoices need not process each invoice sequentially.
 * Computationally Intensive Apps - Some problems can be parallelized & their results can later be merged using divide-and-conquer techniques.
 * Data Crunchers - applications which have to make 100 http requests to aggregate a collection of data can be parallelized to bring down total latency

### Reaping the Benefits of Concurrency
Concurrency can be very useful but there are some challenges we have to deal with to utilize it effectively.

## The Perils of Concurrency
Oftentimes, tasks which can be parallelized will need the units of execution to combine their results. In order to do that, we need to be aware of synchronization constraints.

There are mainly three problems related to concurrency we need to deal with - starvation, deadlock & race conditions.
 * Starvation - thread doesn't finish execution as it is deprived from a resource it needs. E.g. Thread waits for user input but user never enters it & keeps the thread alive indefinitely.
 * Deadlock - Two or more threads are waiting on each other for a shared resource
 * Race Condition - two threads compete to access the same resource

Example race condition:
```java
public class RaceCondition {
  private static boolean done;

  public static void main(final String[] args) throws InterruptedException {
    new Thread(() -> {
        int i = 0;
        while(!done) { i++; }
        System.out.println("Done!");
    }).start();

    System.out.println("OS: " + System.getProperty("os.name"));
    Thread.sleep(2000);
    done = true;
    System.out.println("flag done set to true");
  }
}
```

Output when run in client mode:
```
OS: Windows 7
flag done set to true
Done!
```

Output when run in server mode:
```
OS: Windows 7
flag done set to true
```

### Know your visibility: Understand the Memory Barrier 
The problem with the previous example is that the change we made to `done` may not be visible to other threads due to a memory barrier.

Memory barrier - copying of local/working memory to main memory.  
A change made by one thread is guaranteed to be visible by another if both the reading and writing threads cross the memory barrier.

The problem we saw can be fixed by making the `done` variable `volatile`. It warns the compiler that this variable can be changed behind the back of a thread and hence, it shouldn't be cached.
Note, however, that `volatile` doesn't make operations atomic out of the box.

Additionally, the variable can be guarded by a lock using the `synchronized` keyword which achieves the same result in this case.

### Avoid Shared Mutability
The consequences of forgetting to synchronize can be unpredictable and very hard to debug.

The core problem is that we're dealing with shared mutability. Every single access of shared mutable state in our java applications needs to be verified. Otherwise, our application is broken.

An efficient way to deal with this problem at its root is to avoid shared mutability altogether and instead rely on shared immutability.
Good alternative techniques for dealing with shared state will be explored throughout this book.
