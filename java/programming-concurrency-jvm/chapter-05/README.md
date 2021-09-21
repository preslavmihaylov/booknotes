# Chapter 5: Taming Shared Mutability
The goal of this chapter is to cover how to deal with legacy code which uses shared mutability.

  - [Shared Mutability != public](#shared-mutability--public)
  - [Spotting Concurrency Issues](#spotting-concurrency-issues)
  - [Preserve Invariant](#preserve-invariant)
  - [Mind your resources](#mind-your-resources)
  - [Ensure Visibility](#ensure-visibility)
  - [Enhance Concurrency](#enhance-concurrency)
  - [Ensure Atomicity](#ensure-atomicity)
  - [Recap](#recap)

## Shared Mutability != public
Just because all your fields are private doesn't mean they're not shared and mutable.

Shared variable -> accessed by more than one thread for read or write.

Regardless of access privileges, ensure that a variable passed to other methods as a parameter is thread-safe.
We must assume that the methods we call will access the passed instance in more than one thread.

Additionally, don't let any non-thread-safe reference escape from a class.

## Spotting Concurrency Issues
Example non-thread safe code with a lot of issues:
```java
public class EnergySource {
  private final long MAXLEVEL = 100;
  private long level = MAXLEVEL;
  private boolean keepRunning = true;

  public EnergySource() { 
    new Thread(new Runnable() {
      public void run() { replenish(); }
    }).start();
  }

  public long getUnitsAvailable() { return level; }

  public boolean useEnergy(final long units) {
    if (units > 0 && level >= units) {
      level -= units;
      return true;
    }
    return false;
  }

  public void stopEnergySource() { keepRunning = false; }

  private void replenish() {
    while(keepRunning) {
      if (level < MAXLEVEL) level++;

      try { Thread.sleep(1000); } catch(InterruptedException ex) {}
    }
  }
}
```

Problems with this code:
 *  The `replenish()` method is wasting a thread as it's busy waiting.
 * interrupted exception is ignored. It shouldn't be as this prevents thread cancellation
 * keepRunning is changed in a separate thread & lacks visibility guarantees
 * `if (level < MAXLEVEL) level++` is a race condition
 * Thread is started from inside the constructor. This should never be done
 * useEnergy has a race condition - `level >= units -> level -= units`

## Preserve Invariant
Rule of thumb - never starts thread from object constructor.

Why? - the `start()` method of a thread causes a memory barrier which exposes the partially consructed object to other threads. Also, the started thread has access to the partially constructed object instance.

How to handle this?

One way is to expose a separete method which clients need to call after creating the object. But that leads to other problems:
 * What if the client forgets to call that method which starts the threads & starts using the object's other methods which rely on that method being called
 * What if a client calls the thread-starting code more than once on the same instance

A better alternative is to create a static factory method which creates the object and invokes the thread-starting code. That way, the burden of knowing how to invoke methods is alleviated from the client and there are no thread-safety violations:
```java
  //Fixing constructor...other issues pending
  private EnergySource() {}

  private void init() {
    new Thread(new Runnable() {
      public void run() { replenish(); }
    }).start();        
  }

  public static EnergySource create() {
    return new EnergySource().init();
  }
```

## Mind your resources
The `replenish()` method inefficiently deals with threads. Instead of reusing threads, different threads are spawned per object instance.
Additionally, the method, albeit simple, is busy waiting inside the thread its invoked in, which clogs the application's parallelism potential.

Instead, to achieve the same effect of executing a piece of code at scheduled intervals, we can leverage a `ScheduledExecutorService` which reuses threads for multiple object instances, but also makes the code simpler:
```java
public class EnergySource {
  private final long MAXLEVEL = 100;
  private long level = MAXLEVEL;
  private static final ScheduledExecutorService replenishTimer =
    Executors.newScheduledThreadPool(10);
  private ScheduledFuture<?> replenishTask;

  private EnergySource() {}
  
  private void init() {   
    replenishTask = replenishTimer.scheduleAtFixedRate(new Runnable() {
      public void run() { replenish(); }
    }, 0, 1, TimeUnit.SECONDS);
  }
  
  public static EnergySource create() {
    final EnergySource energySource = new EnergySource();
    energySource.init();
    return energySource;
  }

  public long getUnitsAvailable() { return level; }

  public boolean useEnergy(final long units) {
    if (units > 0 && level >= units) {
      level -= units;
      return true;
    }
    return false;
  }

  public void stopEnergySource() { replenishTask.cancel(false); }

  private void replenish() { if (level < MAXLEVEL) level++; }
}
```

One additional concern is dealing with executor service shutdown. If that is not done, then the JVM can't shutdown our application.

There are two ways to handle this:
 * Provide an explicit `shutdown()` method which clients have to invoke at the end of the application
 * Make all threads created by the executor service daemon threads, which don't block JVM shutdown:
```java
private static final ScheduledExecutorService replenishTimer =
  Executors.newScheduledThreadPool(10,
      new java.util.concurrent.ThreadFactory() {
        public Thread newThread(Runnable runnable) {
        
          Thread thread = new Thread(runnable);
          thread.setDaemon(true);
          return thread;
        }
      });
```

## Ensure Visibility
Multiple threads can concurrently access the `level` variable. This can lead to race conditions as well as visibility issues.

This problem can be addressed by using the `synchronized` keyword to both guard a block of code from being executed by multiple threads at the same time while also ensuring visibility.

The simplest way to apply this in our example class is to mark all methods as `synchronized`.
```java
  //Ensure visibility...other issues pending
  //...
  public synchronized long getUnitsAvailable() { return level; }

  public synchronized boolean useEnergy(final long units) {
    if (units > 0 && level >= units) {
      level -= units;
      return true;
    }
    return false;
  }

  public synchronized void stopEnergySource() { 
    replenishTask.cancel(false); 
  }

  private synchronized void replenish() { if (level < MAXLEVEL) level++; }
```

This solves the race conditions and visibility issues but leads to performance degradation due to excessive locking.

## Enhance Concurrency
In the previous section, we made sure our code is thread-safe but that was at the expense of high concurrency.

We can improve our concurrency a bit while preserving thread-safety by locking in a more granular approach using `AtomicLong` for the level variable:
```java
public class EnergySource {
  private final long MAXLEVEL = 100;
  private final AtomicLong level = new AtomicLong(MAXLEVEL);
  private static final ScheduledExecutorService replenishTimer =
    Executors.newScheduledThreadPool(10);
  private ScheduledFuture<?> replenishTask;

  private EnergySource() {}
  
  private void init() {   
    replenishTask = replenishTimer.scheduleAtFixedRate(new Runnable() {
      public void run() { replenish(); }
    }, 0, 1, TimeUnit.SECONDS);
  }

  public static EnergySource create() {
    final EnergySource energySource = new EnergySource();
    energySource.init();
    return energySource;
  }

  public long getUnitsAvailable() { return level.get(); }

  public boolean useEnergy(final long units) {
    final long currentLevel = level.get();
    if (units > 0 && currentLevel >= units) {
      return level.compareAndSet(currentLevel, currentLevel - units);
    }
    return false;
  }

  public synchronized void stopEnergySource() { 
    replenishTask.cancel(false); 
  }

  private void replenish() { 
    if (level.get() < MAXLEVEL) level.incrementAndGet(); 
  }
}
```

The only method which is still `synchronized` is `stopEnergySource()` but that one is called very infrequently to be worth synchronizing with a more granular approach.

## Ensure Atomicity
Although we achieved thread-safety at the variable level, it is not guaranteed when multiple variables are part of the same invariant.

In the final modification, there's also a new variable called `usage` which needs to track the number of times `useEnergy()` is invoked and the level is decremented.

The solution is to use explicit synchronization but instead of using `synchronized`, we use `ReentrantReadWriteLock` which allows multiple readers and single writer:
```java
public class EnergySource {
  private final long MAXLEVEL = 100;
  private long level = MAXLEVEL;
  private long usage = 0;
  private final ReadWriteLock monitor = new ReentrantReadWriteLock();
  private static final ScheduledExecutorService replenishTimer =
    Executors.newScheduledThreadPool(10);
  private ScheduledFuture<?> replenishTask;

  private EnergySource() {}
  
  private void init() {   
    replenishTask = replenishTimer.scheduleAtFixedRate(new Runnable() {
      public void run() { replenish(); }
    }, 0, 1, TimeUnit.SECONDS);
  }

  public static EnergySource create() {
    final EnergySource energySource = new EnergySource();
    energySource.init();
    return energySource;
  }

  public long getUnitsAvailable() { 
    monitor.readLock().lock();
    try {
      return level;       
    } finally {
      monitor.readLock().unlock();
    }
  }
  
  public long getUsageCount() { 
    monitor.readLock().lock();
    try {
      return usage;
    } finally {
      monitor.readLock().unlock();
    }
  }
  
  public boolean useEnergy(final long units) {
    monitor.writeLock().lock();
    try {
      if (units > 0 && level >= units) {
        level -= units;
        usage++;
        return true;
      } else {
        return false;
      }
    } finally {
      monitor.writeLock().unlock();
    }
  }

  public synchronized void stopEnergySource() { 
    replenishTask.cancel(false); 
  }

  private void replenish() { 
    monitor.writeLock().lock();
    try {
      if (level < MAXLEVEL) { level++;  }
    } finally {
      monitor.writeLock().unlock();
    }
  }
}
```

## Recap
Working with shared mutable state is hard and error-prone which is why it is best to be avoided.

Some rules of thumb to have in mind though:
 * Don't create threads from constructors. Use static factories instead
 * Don't create arbitrary threads. Use thread pools via the executor service
 * Ensure visibility to shared mutable variables
 * Ensure decent concurrency while achieving thread-safety by adjusting the granularity of locking
 * Atomically synchronize access to related fields
