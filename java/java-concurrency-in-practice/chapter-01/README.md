# Chapter 01 - Introduction

Concurrency is the easiest way to leverate multiprocessor systems to exploit better performance. They are also inescapable in Java nowadays. E.g. Every time you're making a Spring application, you are making a concurrent application.

However, it makes writing programs a lot harder as now, programming constructs are not accessed in a sequential fashion.

## Brief history of concurrency
Initially, computers were designed to execute one program at a time, in order.
This was often inefficient due to:
 * Resoure Utilization - another program can execute while you're waiting for network I/O
 * Fairness - Multiple users using the system should have the same share of system resources available to them
 * Convenience - It is often more desirable to have multiple programs, executing small tasks rather than one big program executing all tasks
 
Hence, operating systems were designed to handle multiple processes, running at the same time.
The way the processes synchronize is typically via one of several mechanisms:
 * Sockets
 * File handles
 * Shared memory
 * Signals
 
The way OS-es handled parallel processes is what inspired the use of threads in programs.   
A thread is an independent execution flow in a program, with its own stack, but sharing the same heap as other threads in the program.
When writing concurrent programs, one must be careful to synchronize access to heap memory, used by multiple threads.

## Benefits of threads
If used properly, threads can reduce the cost of ownership & development time.

Whenever you have naturally asynchronous tasks, it is often easier to model these with threads.  
For example, implementing GUI programs, which don't block when the user clicks a button is one which is easier to implement with threads, rather than sequentially.  

Additionally, threads are used to better utilize available resources & throughput in server applications.

They can be used to exploit multiple processors.  
In a multi-processor system, a single-threaded program is only leveraging 1/N CPU resources, where N = cpu count.  

Even in single-processor systems, multithreaded programs can be useful when e.g. you have to wait for file I/O & want to do another task in the meantime.  

A useful metaphor is reading the newspaper, while waiting for the water to boil, rather than waiting for the water to boil & then reading the newspaper.  

## Risks of threads
Since multithreaded programs are so mainstream now, concurrency is not an advanced programming topic, but a required one, nowadays.

### Safety hazards
How your class will be executed in a multithreaded system is unpredictable & can lead to safety hazards.  
Example - an `UnsafeSequence` class:
```
public class UnsafeSequence {
  private int value;
  
  public int getNext() {
    return value++;
  }
}
```

With unlucky timing, two threads executing `getNext()` at the same time might get the same exact number:
TODO: Add screenshot

This is an example of a race condition - the program's behavior depends on how the runtime interleaves thread access.  

Such shared memory access need to be synchronized & Java provides mechanisms to achieve that:
```
public class UnsafeSequence {
  GuardedBy("this") private int value;
  
  public synchronized int getNext() {
    return value++;
  }
}
```

### Liveness hazards
Liveness means "something good eventually happens". When there is a liveness problem, it often means that a program has reached a state from which it can't exit.  

An example of this in a single-threaded program is entering an infinite loop. In multi-threaded programs, there are additional liveness risks involved.  
Deadlocks are an example of a liveness hazard - thread A locks resource A' and waits for resource B'. thread B locks resource B' and waits for resource A'.

### Performance hazards
Threads bare all the performance risks of single-threaded programs, but introduce more.

An example of a performance hazard, caused by a multi-threaded program is using an excessive amount of threads. Using threads is associated to context switching, which is costly & causes performance issues if it occurs too often (as it is in the case of many threads).

Additionally, using synchronization mechanisms prevent the compiler from making certain optimizations.

### Threads are everywhere
Even if you never create a thread yourself, you are already writing multi-threaded programs if you're using Spring.  
All requests, coming from your endpoints are executed in different threads, meaning that your code needs to be thread-safe to handle large throughput.

Examples of frameworks & tools, demanding you write thread-safe code:
 * Timer
 * Swing
 * JSPs & Servlets
 * RMI
