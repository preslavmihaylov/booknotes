# Chapter 02 - Thread Safety

Concurrency is more about achieving thread-safety, than it is about creating & managing threads. Those are mechanisms, but at its core, concurrency aims to encapsulate shared mutable state from uncontrolled concurrent access.

Shared mutable state - properties of a class and/or properties of a dependent class (usually part of the class' properties). Any piece of data which can affect the class' externally visible behavior.

When should you synchronize? - when a shared, mutable variable is accessed by more than one thread **and** at least one of them modifies it.

Synchronization in Java is usually achieved by using the `synchronized` keyword, but it can also be achieved via the `volatile` keyword, atomic variables & explicit locks.

How to fix synchronization:
 * Don't share the variable between threads
 * Make the variable immutable
 * Use appropriate synchronization upon access
 
Good object-oriented practices (encapsulation) can help you achieve thread-safety more easily.

# What is thread-safety?
> A class is thread-safe when it continues to behave correctly when accessed from multiple threads.

Thread-safe classes encapsulate synchronization so that clients don't have to.

## Example - a stateless servlet
```java
@ThreadSafe
public class StatelessFactorizer implements Servlet {
  public void service(ServletRequest req, ServletResponse resp) {
    BigInteger i = extractFromRequest(req);
    BigInteger[] factors = factor(i);
    encodeIntoResponse(resp, factors);
  }
}
```

This file doesn't have any synchronization but it is thread-safe as it's a stateless class. All stateless classes are thread-safe.

It is only when servlets want to remember things from one request to another that the thread safety requirements become an issue.

# Atomicity
If we want to add a hit counter to the previous example, we could do it like this (not thread-safe!):
```java
@NotThreadSafe
public class UnsafeCountingFactorizer implements Servlet {
  private long count = 0;
  
  public long getCount() { return count; }
  
  public void service(ServletRequest req, ServletResponse resp) {
    BigInteger i = extractFromRequest(req);
    BigInteger[] factors = factor(i);
    
    ++count;
    encodeIntoResponse(resp, factors);  
  }
}
```

This class is not thread-safe as there is a state variable (count), which is accessed by multiple threads without any synchronization.
In the example above, it might appear that `++count` is an atomic operation but it isn't as its executing three different commands under the hood.

Several threads accessing that can lead to [this situation](https://github.com/preslavmihaylov/booknotes/blob/master/java/java-concurrency-in-practice/chapter-01/README.md#risks-of-threads).

## Race conditions
`UnsafeCountingFactorizer` has several race conditions.

The most common type of race condition is `check-then-act`. It happens when a given state-changing operation (the `act`) is executed if a given condition is met (the `check`).

Example:
```java
if (count < 10) {
  count = count + 1;
}
```

If the above example is executed by several threads, the count might receive a value greater than `10` which would violate the code snippet's invariant.

A common check-then-act race condition is lazy initialization:
```java
@NotThreadSafe
public class LazyInitRace {
  private ExpensiveObject instance = null;
  
  public ExpensiveObject getInstance() {
    if (instance == null)
      instance = new ExpensiveObject();
    return instance;
  }
}
```

Another common type of race condition is `read-modify-write`. The counting example has that race condition as the `++count` operation reads the value & then modifies it without any synchronization.

## Compound actions
To avoid race conditions, there must be a way to ensure that all dependent operations (e.g. `check-then-act`) are executed atomically.

An atomic compound action is one which, from the perspective of thread A, is either executed all the way or not executed at all.

Example of using atomic variables for fixing the `UnsafeCountingFactorizer` example:
```java
@ThreadSafe
public class UnsafeCountingFactorizer implements Servlet {
  private final AtomicLong count = new AtomicLong(0);
  
  public long getCount() { return count; }
  
  public void service(ServletRequest req, ServletResponse resp) {
    BigInteger i = extractFromRequest(req);
    BigInteger[] factors = factor(i);
    
    count.incrementAndGet();
    encodeIntoResponse(resp, factors);  
  }
}
```
