# Jenkov Concurrency Tutorial
> [Tutorial link](http://tutorials.jenkov.com/java-concurrency/index.html)

## Introduction
### What is Multithreading
Multithreading == multiple threads of execution inside a single program.
A multithreaded application is as if you have multiple CPUs executing your code at once.

However, threads != CPUs. Usually, a single CPU will share its execution time with multiple threads.
![CPUs with multiple threads](images/cpus-multiple-threads.png)

### Why Multithreading?
#### Better utilization of a single CPU
e.g. whilst one thread is waiting for input from a network, another one can run during this time.

#### Better utilization of multiple CPUs
If your computer has multiple CPUs then you'd need to use threads to fully utilize them.

#### Better UX in terms of responsiveness
A blocking operation (e.g. downloading a photo) in a UI should not cause the whole UI to stall. This can be achieved by having the UI be handled on a separate thread.

#### Better UX in terms of fairness
If one client's request takes too much time, the rest of the clients' requests should proceed.

### Multithreading vs. Multitasking
In the past, computers with a single CPU were able to execute only one program at a time.
This changed with the advent of multitasking.

#### Multitasking
Multitasking enables you to execute multiple programs at a time with a single CPU.
It was achieved by allowing programs to multiplex their CPU time - ie your foreground movie is running, but a background downloading program runs for a little from time to time.

#### Multithreading
Multithreading == multiple threads of execution inside the same program.
Multitasking == multiple programs running at the same time with the same CPU.

### Multithreading is hard
Multithreaded programs give rise to new kinds of errors not seen in single-threaded ones.
This stems from the fact that multiple threads are accessing the same shared memory, possibly, across different CPUs.

## Multithreading benefits
### Better CPU Utilization
Example sequential processing of files:
```
  5 seconds reading file A
  2 seconds processing file A
  5 seconds reading file B
  2 seconds processing file B
-----------------------
 14 seconds total
```

Example parallel processing of files (using the same CPU). Achieved by using the CPU for reading the second file while its idle:
```
  5 seconds reading file A
  5 seconds reading file B + 2 seconds processing file A
  2 seconds processing file B
-----------------------
 12 seconds total
```

### Simple Program Design
Sometimes, writing multithreaded code can result in simpler design as each processing unit is independent than the rest, hence, can be easier to read.

### More Responsive Programs
When multiple requests come to a server and the former requests take a long time to process, latter requests will have to wait regardless of processing time.

By using multithreading, one can achieve a much more responsive server.

Example without multithreading:
```
  while(server is active){
    listen for request
    process request
  }
```

Example with multithreading:
```
  while(server is active){
    listen for request
    hand request to worker thread
  }
```

## Multithreading Costs
To use multithreading in an application, the benefits gained should outweigh the costs.

Here are some of the costs of multithreading.
### More complex design
Some parts might be simpler by using multithreading, others though, might be more complex.

E.g. code involving shared data which needs to be synchronized.

### Context Switching Overhead
When a CPU needs to switch from executing one thread to another, there is some overhead incurred due to having to save the state of the thread in memory.

When you switch between threads too often, the time taken to switch threads might be more than the actual processing time.

### Increased Resource Consumption
A thread needs some resources in order to run. Hence, creating too much threads can consume a lot of memory/OS resources.

## Concurrency Models
Concurrency model == mechanism by which threads collaborate.

### Shared State vs Separate State
An important aspect of concurrency models is whether the state is shared or not.

Shared state -> can lead to race conditions and deadlocks.
![shared state](images/shared-state.png)

Separate state -> threads use their own state which is shared via immutable objects or copies
![separate state](images/separate-state.png)

### Parallel Workers
![parallel workers](images/parallel-workers.png)

This is the most commonly used concurrency model in java application (implemented via the Executors framework).

Advantages - easy to understand & parallelize further
Disadvantages:
 * Uses shared state & it had all its disadvantages
 * Stateless workers - using shared state means that you need to re-read it every time you need it to avoid using a stale copy. This can be slow at times
 * Job ordering is nondeterministic - you can't guarantee the order of execution for the jobs

### Assembly Line (aka event-driven)
![assembly line](images/assembly-line.png)

These systems are usually designed to use non-blocking IO and the non-blocking IO is the boundary between the workers.
![assembly line io](images/assembly-line-io.png)

Example implementation - in NodeJS with promises. Other implementations - Akka, Vert.x

Advantages:
 * No shared state - no need to worry about thread-safety
 * Stateful workers - since data is confined to workers, they can be stateful, ie keep their own data in-memory at all times
 * Better hardware conformity (aka mechanical sympathy) - single-threaded code plays nicer with hardware as you can use more optimized data structures and algorithms. You can also cache data.
 * Job ordering is possible - you can even log all events in a system & recreate the system's state from the logs later

Disadvantages:
 * May lead to complicated code
   * implementing callback hell
   * execution flow spread across several classes

### Actors vs. channels
Both use the "assembly line" concurrency model.

In the actor model, each worker is called an actor and they can send messages to one another.
![actor model](images/actor-model.png)

In the channels model, each worker subscribes to messages on a channel which other workers can send messages to.
![channels model](images/channels-model.png)

In the author's (and my) opinion, the channels model is more flexible as workers are decoupled from one another by leveraging channels.

### Functional Parallelism
Another popular concurrency model is functional parallelism.

Its idea is that a program execution can be broken down to the composition of individual functions 
which communicate by copying inputs/outputs (or using immutable objects) among them but never sharing them.

Function calls can be parallelized on multiple CPUs.

The caveat with this approach is knowing which functional calls to parallelize.
Some might be too small to be worth the hassle.

Example implementation is the java stream API.

### Which concurrency model is best?
If your tasks are naturally parallel & independent, the parallel workers concurrency model might be well suited for the job.
If the tasks need to share some state, the assembly line concurrency model fits better.

## Same-Threading
Concurrency model where a single-threaded system scales to N single-threaded systems.

### Why Single-threaded systems?
They are much simpler to deal with than multi-threaded systems due to the lack of shared state. Additionally, they can use non-thread safe data structures which utilize the CPU much more efficiently.

The downside is that a single-threaded system cannot fully utilize the CPU cores available on a machine:
![Single-threaded system](images/single-threaded-system.png)

### Same-threading == single-threading scaled out
![Same Threading](images/same-threading.png)

Multi-threaded system == a lot of threads which share state.
Same-threaded system == a lot of threads not sharing any state.
![Multi-threading vs. same-threading](images/multi-vs-same-threading.png)

### Thread Communication
If two threads in a same-threaded system need to communicate, they can achieve it by sending messages.
Messages are arbitrary byte sequences where the receiving thread get a copy of the original message.

The communication can take place via queues, pipes, unix sockets, TCP sockets, etc.

## Concurrency vs. Parallelism
Concurrency != Parallelism although they look very similar at first glance.

Concurrent execution == program is making progress on more than one task simultaneously. Doesn't necessarily need parallel execution:
![Concurrency](images/concurrency].png)

Parallel execution == Program is making progress on more than one task in parallel.
![Parallel execution](images/parallel-execution.png)

Parallel concurrent execution == N tasks on M CPUs where N > M.
![Parallel Concurrent Execution](images/parallel-concurrent-execution.png)

Parallelism == big task is split into subtasks which are executed concurrently and/or in parallel:
![Parallelism](images/parallelism.png)

### Concurrency/Parallelism combos
 * Concurrent, not parallel - e.g. working on task #1 while task #2 is waiting for file IO
 * Parallel, not concurrent - e.g. program works on only one task which is split in subtasks & they are executed in parallel
 * Not concurrent, not parallel - e.g. traditional CLI applications
 * Concurrent, Parallel - e.g. web server handling multiple requests in parallel and some of them are multiplexed on the same CPU while waiting for database operation to complete.Q

## Single-threaded concurrency
Making progress on more than one task on a single thread.

### Still new ground
Popular libraries, technologies still use this concurrency model. Netty, Vert.x, Undertow and NodeJS use this model.

The model is centered around an event loop - a thread running in a loop, waiting for events in the system. When an event is invoked, your code, subscribed to the event gets executed.

### Classic multi-threaded concurrency
Typically assign one task per thread, but there are nuances, e.g. thread pools.

Advantages:
 * Relatively easy to distribute the work load across multiple threads

Disadvantages:
 * Hard to maintain & less performant when shared data gets involved.

### Single-threaded concurrency
You implement your task switching.

Benefits:
 * No thread visibility or race condition problems
 * control over task switching - you can decide how big of a chunk to work on before task switching
 * control over task prioritization

In the author's opinion, there are few benefits of this concurrency model over the classic multi-threaded approach. Javascript uses this model because it is a single-threaded language & it's the only option we've got.

### Challenges
 * Implementation required - you need to learn how to implement this model & actually implement it
 * Blocking operations have to be avoid or completed in background threads
 * Only uses a single CPU

### Single-threaded implementation - via thread loop
an infinite for loop is waiting for tasks to complete & completes them if any are present.
This thread can also be paused for several milliseconds if no work is estimated to come to avoid wasting CPU time in a tight loop.
![thread loop](images/thread-loop.png)

Agent - a component which does some work & is invoked by the thread loop.
Thread loops invoke agents:
![Thread loop with agent](images/thread-loop-agent.png)

## Creating and starting Java threads
Thread == like a virtual CPU which executes your code

### Creating threads
```java
new Thread().start();
```

This example doesn't provide the thread any code to execute, therefore it will stop after it's started.

Ways to provide code for threads to execute:
 * Subclassing:
```java
  public class MyThread extends Thread {

    public void run(){
       System.out.println("MyThread running");
    }
  }
```

 * Via Runnable:
```java
  new Thread(() -> { System.out.println("Lambda Runnable running"); }).start();
```

Common pitfall, avoid invoking `run()` over `start()`. That method executes the runnable in the current thread, rather than the new one.

Specifying the name of the thread:
```java
Thread thread = new Thread("New Thread") {
  public void run(){
    System.out.println("run by: " + getName());
  }
};


thread.start();
System.out.println(thread.getName());
```

Getting the current thread object:
```java
Thread thread = Thread.currentThread();
```

To stop a thread, the implementation should allow it. Example:
```java
public class MyRunnable implements Runnable {

    private boolean doStop = false;

    public synchronized void doStop() {
        this.doStop = true;
    }

    private synchronized boolean keepRunning() {
        return this.doStop == false;
    }

    @Override
    public void run() {
        while(keepRunning()) {
            // keep doing what this thread should do.
            System.out.println("Running");

            try {
                Thread.sleep(3L * 1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
```

## Race Conditions & Critical Sections
A race condition can occur in a critical section where the result of execution depends on the sequence of concurrently executing operations.

### Types of race conditions
 * Read-modify-write - read a value, modify it and write it back.
```java
  public class Counter {

     protected long count = 0;

     public void add(long value){
         this.count = this.count + value;
     }
  }
```

This can produce wrong results if executed by multiple threads simultaneously.

 * Check-then-act check a condition and act on it
```java
public class CheckThenActExample {

    public void checkThenAct(Map<String, String> sharedMap) {
        if(sharedMap.containsKey("key")){
            String val = sharedMap.remove("key");
            if(val == null) {
                System.out.println("Value for 'key' was null");
            }
        } else {
            sharedMap.put("key", "value");
        }
    }
}
```

In this situation, it is possible that multiple threads try to remove the same value, but only one of them will get a value back.

### Preventing race conditions
To prevent race conditions, you must ensure that critical sections are executed as an atomic instruction.

This can be achieved, typically, by using the `synchronized` keyword in Java.

You might also want to improve critical section throughput.
This can be achieved by splitting a big critical section into smaller ones.

Example:
```java
public class TwoSums {
    
    private int sum1 = 0;
    private int sum2 = 0;
    
    public void add(int val1, int val2){
        synchronized(this){
            this.sum1 += val1;   
            this.sum2 += val2;
        }
    }
}
```

This can be written (correctly) like so:
```java
public class TwoSums {
    
    private int sum1 = 0;
    private int sum2 = 0;

    private Integer sum1Lock = new Integer(1);
    private Integer sum2Lock = new Integer(2);

    public void add(int val1, int val2){
        synchronized(this.sum1Lock){
            this.sum1 += val1;   
        }
        synchronized(this.sum2Lock){
            this.sum2 += val2;
        }
    }
}
```

Note that this is a contrived example just to illustrate the concept. Critical sections for such small instructions are unnecessary.

## Thread Safety & Shared Resources
Code that is safe to call by multiple threads is thread-safe.
Code is thread safe -> no race conditions.

Local variables are thread-safe as they are stored inside the thread's own memory.

Thread-safe method example:
```java
public void someMethod(){

  long threadSafeInt = 0;

  threadSafeInt++;
}
```

Object member variables are not thread-safe by default if multiple threads share the same object instance.

Example:
```java
NotThreadSafe sharedInstance = new NotThreadSafe();

new Thread(new MyRunnable(sharedInstance)).start();
new Thread(new MyRunnable(sharedInstance)).start();

public class MyRunnable implements Runnable{
  NotThreadSafe instance = null;

  public MyRunnable(NotThreadSafe instance){
    this.instance = instance;
  }

  public void run(){
    this.instance.add("some text");
  }
}
```

If the instance is not shared, though, this code is thread-safe:
```java
new Thread(new MyRunnable(new NotThreadSafe())).start();
new Thread(new MyRunnable(new NotThreadSafe())).start();
```

### Thread Control Escape Rule
```
If a resource is created, used and disposed within
the control of the same thread,
and never escapes the control of this thread,
the use of that resource is thread safe.
```

A resource can be anything - object, database, file, etc.

If the objects you're using are thread-safe but the underlying resource (e.g. database) is accessed concurrently, this might not be thread-safe.
Example:
```
check if record X exists
if not, insert record X
```

Example going wrong:
```
Thread 1 checks if record X exists. Result = no
Thread 2 checks if record X exists. Result = no
Thread 1 inserts record X
Thread 2 inserts record X
```

## Thread Safety & Immutability
Race conditions occur only if multiple threads are accessing the same resource and writing to it.
If they only read it, it is thread-safe.

If we make the shared objects immutable, they are thread-safe. Example:
```java
public class ImmutableValue{

  private int value = 0;

  public ImmutableValue(int value){
    this.value = value;
  }

  public int getValue(){
    return this.value;
  }
}
```

To execute values on the immutable object, a new object is to be returned:
```java
public class ImmutableValue{

  private int value = 0;

  public ImmutableValue(int value){
    this.value = value;
  }

  public int getValue(){
    return this.value;
  }

  
  public ImmutableValue add(int valueToAdd){
    return new ImmutableValue(this.value + valueToAdd);
  }
}
```

The reference, though, is not thread-safe.

Example:
```java
public class Calculator{
  private ImmutableValue currentValue = null;

  public ImmutableValue getValue(){
    return currentValue;
  }

  public void setValue(ImmutableValue newValue){
    this.currentValue = newValue;
  }

  public void add(int newValue){
    this.currentValue = this.currentValue.add(newValue);
  }
}
```

In the above case, the object is immutable & thread-safe but its reference is not and needs to be synchronized.

## Java Memory Model
The JMM specifies how the Java VM works with the main memory.

Internal JMM:
![Java Memory Model](images/java-memory-model.png)

How the JMM maps to the hardware memory model:
![JMM to Hardware MM mapping](images/jmm-to-hardware-mapping.png)

When objects are stored in various different memory locations in a computer, certain problems may occur.

### Visibility
If two threads are sharing an object, without proper synchronization updates to the object by one thread might not be visible to another.

Typical explanation - the change from one thread is not flushed to main memory for it to be observed by another, running on a different CPU:
![Visibility problem](images/visibility-problem.png)

One way to solve this is to use the `volatile` keyword.

### Race conditions
If two threads are sharing an object and both of them are updating it, a race condition can occur:
![Race Condition](images/race-condition.png)

One way to solve this is to use the `synchronized` keyword.

## Java Happens Before guarantee
Happens before guarantees - a set of rules governing how the JMM is allowed to reorder instructions for performance gains.

### Instruction reordering
Modern CPUs can execute non-dependent instructions in parallel. Example:
```
a = b + c

d = e + f
```

Instruction reordering is allowed by the JMM as long as it doesn't change the semantics of the program.
However, when these instructions happen across multiple threads, the JMM doesn't guarantee the order of the instructions which can lead to problems.

The rootcause is that the dependent instructions are not visible to the second thread.

One way to guarantee visibility is to use the `volatile` keyword. It provides a guarantee that operations on the variable will be visible by all threads accessing it.

The reason is that the JMM provides a `happens-before` guarantee for volatile variables - ie, `writes to volatile variables happens-before subsequent reads`.
This imposes a restriction on the JMM to not do any wise things to reorder instructions.

As a side-effect, this can also lead to "synchronization piggybacking" where you can achieve synchronization on non-synchronized variables by piggybacking on the `happens-before` guarantee.

Example:
```
this.nonVolatileVarA = 34;
this.nonVolatileVarB = new String("Text");
this.volatileVarC    = 300;
```

In this example, all three variables are visible to other threads, despite the fact that only the third one is declared `volatile`.

There are similar read/write guarantees for the `synchronized` keyword I won't go through because it is not so important.

The TLDR is that `happens-before` guarantees are the assembly of java concurrency and you shouldn't rely too heavily on it.

## Java Synchronized block
When you use the `synchronized` keyword, you are locking a section of your code to be executed by only one thread at a time. The rest will have to wait for the first one to finish.

Example synchronized instance method:
```java
public class MyCounter {

  private int count = 0;

  public synchronized void add(int value){
      this.count += value;
  }
}
```

This method is synchronized on the instance of the object, not the class itself. In example, different instances can execute that method in parallel.

Additionally, this also means that only one synchronized method on a single instance can be executed at a time. Example:
```java
public class MyCounter {

  private int count = 0;

  public synchronized void add(int value){
      this.count += value;
  }
  public synchronized void subtract(int value){
      this.count -= value;
  }
}
```

In this case, only one thread can be executing either `add` or `subtract` at a time.

Static methods can be synchronized as well, but they are synchronized on the class itself, rather than the instance:
```java
public static MyStaticCounter{

  private static int count = 0;

  public static synchronized void add(int value){
      count += value;
  }
}
```

You can also synchronize specific blocks of a method as well:
```java
  public void add(int value){

    synchronized(this){
       this.count += value;   
    }
  }
```

The passed parameter to `synchronized` is the object being locked on. In this case, it is the class instance.

These two methods are equivalent in terms of synchronization:
```java
  public class MyClass {
  
    public synchronized void log1(String msg1, String msg2){
       log.writeln(msg1);
       log.writeln(msg2);
    }

  
    public void log2(String msg1, String msg2){
       synchronized(this){
          log.writeln(msg1);
          log.writeln(msg2);
       }
    }
  }
```

In most cases, it makes sense to synchronize on `this`. As a rule of thumb, don't synchronize on any primitive objects.

### Limitations
The `synchronzed` keyword allows only one thread to read or write at a time. There are alternatives for other use cases.

E.g. for multiple reading threads and one writing thread, use Read/Write locks.
E.g. for multiple writing threads, use a Semaphore.

Also note that there is some performance overhead when using `synchronized` due to lock contention.

As a caveat, the synchronized keyword allows reentrance. In example, you can invoke several `synchronized` blocks at the same time as long as they are synchronized on the same object.
Example:
```java
public class MyClass {
    
  List<String> elements = new ArrayList<String>();
    
  public void count() {
    if(elements.size() == 0) {
        return 0;
    }
    synchronized(this) {
       elements.remove();
       return 1 + count();  
    }
  }
}
```
