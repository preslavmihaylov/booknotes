# Chapter 14 - Building custom synchronizers
When building a state-dependent class, you need to have certain preconditions met before performing an operation.
E.g. the `FutureTask` needs to complete before you can `get` it.

Such classes should most often be built on top of already existing synchronizers from the java standard library.
In some advanced cases, where you can't find what you need in the standard library, you might need to build your own ones.

# Managing state dependence
In a single-threaded program, the only option when a precondition fails is to return an error.
In a multi-threaded one, preconditions that aren't met can change due to the actions of another thread.

Hence, a precondition that fails might be coded to block, rather than fail in such an environment. Otherwise, usage of the class might be clunky & error-prone.

One way to implement such behavior is to go throgh the painful route of using standard means of synchronization.

In that case, the code would generally look like this:
```
acquire lock on object state
while (precondition does not hold) {
    release lock
    wait until precondition might hold
    optionally fail if interrupted or timeout expires
    reacquire lock
}

perform action
release lock
```

Throughout this chapter, we'll explore alternative ways to achieve this by implementing several versions of a blocking queue.

## Example: propagating precondition failures to callers
One way to support blocking behavior is to propagate this logic to the callers.

A sample implementation of a bounded buffer which fails when preconditions aren't met:
```java
@ThreadSafe
public class GrumpyBoundedBuffer<V> extends BaseBoundedBuffer<V> {
    public GrumpyBoundedBuffer(int size) { super(size); }

    public synchronized void put(V v) throws BufferFullException {
        if (isFull())
            throw new BufferFullException();

        doPut(v);
    }

    public synchronized V take() throws BufferEmptyException {
        if (isEmpty())
            throw new BufferEmptyException();

        return doTake();
    }
}
```

Having this requires the callers to implement retry logic like so:
```java
while (true) {
    try {
        V item = buffer.take();
        // use item
        break;
    } catch (BufferEmptyException e) {
        Thread.sleep(SLEEP_GRANULARITY);
    }
}
```

## Example: crude blocking by polling & sleeping
An alternative is to encapsulate the blocking logic into the bounded queue by using simple means of polling & sleeping:
```java
@ThreadSafe
public class SleepyBoundedBuffer<V> extends BaseBoundedBuffer<V> {
    public SleepyBoundedBuffer(int size) { super(size); }

    public void put(V v) throws InterruptedException {
        while (true) {
            synchronized (this) {
                if (!isFull()) {
                    doPut(v);
                    return;
                }
            }
            Thread.sleep(SLEEP_GRANULARITY);
        }
    }

    public V take() throws InterruptedException {
        while (true) {
            synchronized (this) {
                if (!isEmpty())
                    return doTake();
            }
            Thread.sleep(SLEEP_GRANULARITY);
        }
    }
}
```

This simplifies the usage of bounded buffer in comparison to the previous version - a step in the right direction.

However, the implementation is still fairly painful to make right as choosing the right sleep granularity is hard.
A way to make the current thread sleep but ensuring that it is awaken promptly once the precondition is met is using condition queues.

## Condition queues to the rescue
Condition queues are like the "toast is ready" signal on your toaster.
They are called so as they queue up threads waiting for a condition to become true.

**In order to use a condition queue on object X, you must hold object X's intrinsic lock.**

When you call `Object.wait`, the lock you're holding is atomically released & reaquired once the thread is woken up.

Example implementation of bounded buffer using condition queues:
```java
@ThreadSafe
public class BoundedBuffer<V> extends BaseBoundedBuffer<V> {
    // CONDITION PREDICATE: not-full (!isFull())
    // CONDITION PREDICATE: not-empty (!isEmpty())
    public BoundedBuffer(int size) { super(size); }

    // BLOCKS-UNTIL: not-full
    public synchronized void put(V v) throws InterruptedException {
        while (isFull())
            wait();

        doPut(v);
        notifyAll();
    }
    // BLOCKS-UNTIL: not-empty
    public synchronized V take() throws InterruptedException {
        while (isEmpty())
            wait();

        V v = doTake();
        notifyAll();

        return v;
    }
}
```

Note that condition queues don't do anything more than you can do with sleeping & polling.
They are just a performance optimization.

This implementation of a bounded buffer is good enough for production use. All it needs is a time-based version of the put and take operations. `Object.wait` have support for this.

# Using condition queues
Although condition queues are easy to use, they are also easy to use incorrectly.
There are a lot of rules one has to consider in order to use them properly.

## The condition predicate
To use condition queues correctly, you must first identify the condition predicate you are blocking on.

For example, to `take` an element from a buffer, the buffer should **not be empty**.
To `put` an element, on the other hand, the buffer should **not be full**.

Understanding the condition predicate is important as you have to already hold the lock guarding the state variables associated with a condition predicate before calling `wait`.

## Waking up too soon
If `wait` returns that doesn't mean the condition it guards is now true. You must always check the condition after returning from wait & return to waiting or fail if it doesn't hold.

This can happen because `notifyAll` can be called when any state has changed, not only the state you are waiting on.
Additionally, `wait` can sometimes return for no particular reason.

Hence, the canonical form for using wait is:
```java
void stateDependentMethod() throws InterruptedException {
    // condition predicate must be guarded by lock
    synchronized(lock) {
        while (!conditionPredicate())
            lock.wait();
        // object is now in desired state
    }
}
```

## Missed signals
A missed signal is a liveness problem, similar to a deadlock.

It happens when thread A notifies of a particular state change, and thread B starts waiting for that notification afterwards.
Thread B will never receive the notification (unless someone invokes `notifyAll` again).

When you use the canonical form for waiting, this hazard cannot happen.

## Notification
When someone invokes `notify` or `notifyAll`, they should do it with the lock being held.
Additionally, threads invoking one of these methods should quickly exit in order to unblock the waiting threads (due to the lock being held).

In most circumstances, you should use `notifyAll`. `notify` can be used to optimize the algorithm but is very hard to get right.
`notify` wakes up a single thread, instead of waking up all threads waiting on a condition queue

If `BoundedBuffer` used `notify` instead of `notifyAll`, this hazard could occur:
 * Thread A waits on predicate A', thread B waits on predicate B'
 * Thread C invokes `notify` since predicate B' is met
 * The JVM wakes up thread A, whose predicate is not met and it goes back to sleep
 * Thread B, on the other hand, never gets the notification & could wait forever

In order to use `notify`, these preconditions need to be met:
 * A condition queue is used for a single condition predicate
 * A notification on the queue enables at most one thread to proceed

Since most classes don't meet these preconditions, the prevailing wisdom is to always use `notifyAll`.
This is inefficient but it is much easier to verify correctness this way.

Another possible performance optimization is using conditional notification - notifying only once when the condition is met:
```java
public synchronized void put(V v) throws InterruptedException {
    while (isFull())
        wait();

    boolean wasEmpty = isEmpty();
    doPut(v);
    if (wasEmpty)
        notifyAll();
}
```

This is more efficient but hard to get right and should be done only if it is really needed.

## Example: a gate class
Using condition queues, a recloseable gate can be written like so:
```java
@ThreadSafe
public class ThreadGate {
    // CONDITION-PREDICATE: opened-since(n) (isOpen || generation>n)
    @GuardedBy("this") private boolean isOpen;
    @GuardedBy("this") private int generation;

    public synchronized void close() {
        isOpen = false;
    }

    public synchronized void open() {
        ++generation;
        isOpen = true;
        notifyAll();
    }

    // BLOCKS-UNTIL: opened-since(generation on entry)
    public synchronized void await() throws InterruptedException {
        int arrivalGeneration = generation;
        while (!isOpen && arrivalGeneration == generation)
            wait();
    }
}
```

The more complicated condition using `generation` is done because if gates are opened and closed in quick succession, a gate might be reopened before the threads from the previous run are allowed to pass.

This is why custom synchronizers are hard to maintain - adding more state variables can complicate class design substantially.

## Subclass safety issues
When you have a class using a condition queue and you want ot support subclassing, the synchronization policy should be clearly documented, along with condition predicates, etc.
Additionally, the condition queue, lock & condition predicate variables should be available to the subclass for extension purposes.

Alternatively, make the class final & don't allow extension.
Otherwise, you'll have to stick to using `notifyAll`.

## Encapsulating condition queues
You shouldn't expose your condition queue to clients of a class. Otherwise, callers might be tempted to assume they understand your protocols for waiting and notification & `wait` on your condition queue.

If alien code mistakenly waits on your condition queue, that could subvert your notification protocol.

This advice, however, is not consistent with the most common way to use condition queues by relying on the intrinsic object lock.
Alternatively, you can lock on a private object explicitly. This, however, subverts client-side locking.

# Explicit condition objects
Just as explicit locks are a generalization of intrinsic locks, `Condition` is a generalization of intrinsic condition queues.

Intrinsic queues have several drawbacks:
 * Each intrinsic lock can have only one associated condition queue. This means that multople threads might wait on the same condition queue for different condition predicates
 * The most common pattern for using intrinsic queues involves making the queue publicly available.

If you want to have a concurrent object \w multiple condition predicates or exercise more control over the visibility of a condition queue, the explicit `Condition` object can help.

A `Condition` is associated with a single lock and is created by invoking `newCondition` on a given `Lock` instance. Unlike intrinsic condition queues, multiple explicit queues can be associated with the same lock.

Just as `Lock` offers a more rich feature set in comparison to intrinsic locks, `Condition` offers a richer feature set than intrinsic condition queues:
```java
public interface Condition {
    void await() throws InterruptedException;
    boolean await(long time, TimeUnit unit) throws InterruptedException;
    long awaitNanos(long nanosTimeout) throws InterruptedException;
    void awaitUninterruptibly();
    boolean awaitUntil(Date deadline) throws InterruptedException;
    void signal();
    void signalAll();
}
```

Example usage \w bounded buffer:
```java
@ThreadSafe
public class ConditionBoundedBuffer<T> {
    protected final Lock lock = new ReentrantLock();
    // CONDITION PREDICATE: notFull (count < items.length)
    private final Condition notFull = lock.newCondition();
    // CONDITION PREDICATE: notEmpty (count > 0)
    private final Condition notEmpty = lock.newCondition();

    @GuardedBy("lock")
    private final T[] items = (T[]) new Object[BUFFER_SIZE];
    @GuardedBy("lock") private int tail, head, count;

    // BLOCKS-UNTIL: notFull
    public void put(T x) throws InterruptedException {
        lock.lock();
        try {
            while (count == items.length)
                notFull.await();

            items[tail] = x;
            if (++tail == items.length)
                tail = 0;

            ++count;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    // BLOCKS-UNTIL: notEmpty
    public T take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0)
                notEmpty.await();

            T x = items[head];
            items[head] = null;
            if (++head == items.length)
                head = 0;

            --count;
            notFull.signal();
            return x;
        } finally {
            lock.unlock();
        }
    }
}
```

This is an example of using `signal` over `signalAll`.

Choose explicit `Condition` objects over intrinsic condition queues only if you need its advanced feature set.

# Anatomy of a synchronizer
Although most synchronizers can be implemented using means discussed in this chapter, most are actually implemented using a common base class called `AbstractQueuedSynchronizer`.
The reason is that this framework provides an easier means of managing condition queues and it is also more scalable and performant.

Most synchronizers in the standard library are built on top of this class.

# AbstractQueuedSynchronizer
The basic operations this class supports are a variant of `acquire` and `release` - acquire could block and release unblocks threads waiting in acquire.
For different synchronizers, what `acquire` and `release` mean are different stories.

Additionally, a synchronizer typically has a state. AQS supports this as it provides an integer which can be interpreted as state by any class using it.
For example, `Semaphore` would interpret it as the number of permits available, while `ReentrantLock` will use it as a binary option to represent open or closed.

## A simple latch
An example of a binary latch, implemented using AQS:
```java
@ThreadSafe
public class OneShotLatch {
    private final Sync sync = new Sync();

    public void signal() { sync.releaseShared(0); }
    public void await() throws InterruptedException {
        sync.acquireSharedInterruptibly(0);
    }

    private class Sync extends AbstractQueuedSynchronizer {
        protected int tryAcquireShared(int ignored) {
            // Succeed if latch is open (state == 1), else fail
            return (getState() == 1) ? 1 : -1;
        }
        protected boolean tryReleaseShared(int ignored) {
            setState(1); // Latch is now open
            return true; // Other threads may now be able to acquire
        }
    }
}
```

In the above scenario, `tryAcquireShared` indicates to the AQS what condition means that the threads should block, while `tryReleaseShared` sets the state to the correct value in order to unblock the other threads.

`acquireSharedInterruptibly` is like waiting for the condition to hold in a conditino queue and `releaseShared` invokes `tryReleaseShared` which unblocks the waiting threads.

`OneShotLatch` could have extended `AQS` rather than delegating to it, but that is not recommended (composition over inheritance). Neither of the standard library classes using AQS extend it directly.

# AQS in java.util.concurrent synchronizer classes
Without getting too deep into the details, we'll explore how the standard library classes use AQS.

## ReentrantLock
ReentrantLock uses AQS's state to represent the number of locks being held by a single owner (which is maintained by `ReentrantLock` separately):
```java
protected boolean tryAcquire(int ignored) {
    final Thread current = Thread.currentThread();
    int c = getState();
    if (c == 0) {
        if (compareAndSetState(0, 1)) {
            owner = current;
            return true;
        }
    } else if (current == owner) {
        setState(c+1);
        return true;
    }

    return false;
}
```

## Semaphone and CountDownLatch
Example usage of AQS in `Semaphore`:
```java
protected int tryAcquireShared(int acquires) {
    while (true) {
        int available = getState();
        int remaining = available - acquires;
        if (remaining < 0 || compareAndSetState(available, remaining))
            return remaining;
    }
}

protected boolean tryReleaseShared(int releases) {
    while (true) {
        int p = getState();
        if (compareAndSetState(p, p + releases))
            return true;
    }
}
```

The only fancy thing here is the usage of `compareAndSetState`. This is used for reasons explained in chapter 15.

`CountDownLatch` behaves in a similar way - it uses the state to hold the number of remaining permits. If it reaches 0, the latch is unblocked.

## FutureTask
`FutureTask` uses AQS to hold the current state of the task. This is represented by an enum, holding status running, completed or cancelled.
`Future.get` blocks until the AQS' state is not completed or cancelled.

## ReentrantReadWriteLock
This class also relies on AQS & represents its write-lock count by using the first 16 bits of AQS' state and its read-lock count by using the other 16 bits.

