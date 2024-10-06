# Chapter 04 - Composing Objects

- [Designing a thread-safe class](#designing-a-thread-safe-class)
  - [Gathering synchronization requirements](#gathering-synchronization-requirements)
  - [State-dependent operations](#state-dependent-operations)
  - [State ownership](#state-ownership)
- [Instance confinement](#instance-confinement)
  - [The Java monitor pattern](#the-java-monitor-pattern)
  - [Example: tracking fleet vehicles](#example-tracking-fleet-vehicles)
- [Delegating thread safety](#delegating-thread-safety)
  - [Independent state variables](#independent-state-variables)
  - [When delegation fails](#when-delegation-fails)
  - [Publishing underlying state variables](#publishing-underlying-state-variables)
  - [Example: vehicle tracker that publishes its state](#example-vehicle-tracker-that-publishes-its-state)
- [Adding functionality to existing thread-safe classes](#adding-functionality-to-existing-thread-safe-classes)
  - [Client-side locking](#client-side-locking)
  - [Composition](#composition)
- [Documenting synchronization policies](#documenting-synchronization-policies)

So far, we've covered the fundamentals of what thread-safety problems there are & the low-level mechanisms to deal with them.  

But it is infeasible to analyze every single memory access for thread-safety violations.  
Instead, this chapter focuses on learning how to take thread-safe components & compose them in a way that makes your own components thread-safe.

# Designing a thread-safe class

The design process for a thread-safe class includes three steps:
 * Identify the variables that form an object's state
 * Identify the invariants that constrain the state variables
 * Establish a synchronization policy to achieve thread-safety

If all fields are primitive, then all fields of a class form its state. Example:
```java
@ThreadSafe
public final class Counter {
    @GuardedBy("this") private long value = 0;
    public synchronized long getValue() {
        return value;
    }

    public synchronized long increment() {
        if (value == Long.MAX_VALUE)
            throw new IllegalStateException("counter overflow");
        return ++value;
    }
}
```

If there is a composed object, the state extends to the composed object's state as well. Example - a linked list's state encompasses the state of all its nodes' states.

Synchronization policy - the mechanism of choice for managing thread-safety. E.g. locking, immutability, thread-confinement, etc.

## Gathering synchronization requirements
When designing a class for thread-safety, one must first understand a class's invariants.  

An example is a `NumberRange` class with a lower and upper bound. An invariant of that class is that the lower bound cannot be greater than the upper bound.  
To ensure thread-safety, compound actions involving updating the bounds need to be made atomic.  

On the other hand, if there is a field whose value doesn't depend on its own state, synchronization can be relaxed to achieve better performance.  

But in sum, one must understand a class's invariants first, before designing it for thread-safety.

## State-dependent operations

Some methods have certain state-based preconditions. For example, to remove an element from a queue, the queue must be non-empty.  

In a single-threaded program, such an operation has no choice but to fail. In a concurrent program, you have the possibility to "wait" until the preconditions are met.  

This can be achieved using low-level mechanisms such as `wait` and `notify` but it is advised to rather rely on existing library classes such as `Semaphore` and `BlockingQueue`.

## State ownership

An object encapsulates the state it owns and owns the state it encapsulates. The object is responsible for managing the locking protocol to be used for the state it owns.  
For example, a `HashMap` owns the internal `Map.Entry` objects and is responsible for deciding the locking mechanism to be used.

# Instance confinement

If an object is not thread-safe, it can be encapsulated in a wrapper object & all code paths accessing the non-thread-safe object can be synchronized.  
This is referred to as instance confinement - all code paths to an encapsulated non-thread-safe instance are guarded by a lock.

An example of a use case is the `Collections.synchronizedX` classes, which wrap the underlying non-thread-safe collections using the Decorator pattern.

Example for instance confinement. Note that the `HashSet<Person>` is not thread-safe:
```java
@ThreadSafe
public class PersonSet {
    @GuardedBy("this")
    private final Set<Person> mySet = new HashSet<Person>();

    public synchronized void addPerson(Person p) {
        mySet.add(p);
    }

    public synchronized boolean containsPerson(Person p) {
        return mySet.contains(p);
    }
```

## The Java monitor pattern

This is a simple synchronization pattern - an object encapsulates all its state variables by using the object's intrinsic lock (i.e. `this`).

More sophisticated use-cases, where different state variables of an object form different invariants might demand a more sophisticated locking strategy by e.g. using several explicit locks:
```java
public class ThreadSafeClass {
	private Object lock1 = new Object();
	private Object lock2 = new Object();

	public void methodA() {
		synchronized(lock1) {
			//critical section A
		}
	}

	public void methodB() {
		synchronized(lock1) {
			//critical section B
		}
	}

	public void methodC() {
		synchronized(lock2) {
			//critical section C
		}
	}

	public void methodD() {
		synchronized(lock2) {
			//critical section D
		}
	}
}
```

In the example above, methods A and B are mutually-exclusive but a concurrently running thread can execute method C or D in parallel.

## Example: tracking fleet vehicles

A more sophisticated example of building a thread-safe class follows - a collection which maintains the locations of some vehicles (e.g. taxis).

Example for achieving thread-safety with the java monitor pattern:
```java
@ThreadSafe
public class MonitorVehicleTracker {
    @GuardedBy("this")
    private final Map<String, MutablePoint> locations;

    public MonitorVehicleTracker(Map<String, MutablePoint> locations) {
        this.locations = deepCopy(locations);
    }

    public synchronized Map<String, MutablePoint> getLocations() {
        return deepCopy(locations);
    }

    public synchronized MutablePoint getLocation(String id) {
        MutablePoint loc = locations.get(id);
        return loc == null ? null : new MutablePoint(loc);
    }

    public synchronized void setLocation(String id, int x, int y) {
        MutablePoint loc = locations.get(id);
        if (loc == null)
            throw new IllegalArgumentException("No such ID: " + id);

        loc.x = x;
        loc.y = y;
    }

    private static Map<String, MutablePoint> deepCopy(Map<String, MutablePoint> m) {
        Map<String, MutablePoint> result = new HashMap<String, MutablePoint>();
        for (String id : m.keySet())
            result.put(id, new MutablePoint(m.get(id)));

        return Collections.unmodifiableMap(result);
    }
}
```

Note that when returning the map of locations, a copy is made. This means that if some location changes afterwards, the returned collection will not update its values.  
Whether you want this behavior or not is based on requirements.

# Delegating thread safety
The java monitor pattern is useful when writing a class composed of non-thread-safe objects.  

If the class, however, is composed of thread-safe objects you might get away with not using any synchronization at all.  
However, in some cases, extra synchronization is needed even if all fields are thread-safe.

Example of delegating thread-safety & not using any explicit synchronization:
```java
@ThreadSafe
public class DelegatingVehicleTracker {
    private final ConcurrentMap<String, Point> locations;
    private final Map<String, Point> unmodifiableMap;

    public DelegatingVehicleTracker(Map<String, Point> points) {
        locations = new ConcurrentHashMap<String, Point>(points);
        unmodifiableMap = Collections.unmodifiableMap(locations);
    }

    public Map<String, Point> getLocations() {
        return unmodifiableMap;
    }

    public Point getLocation(String id) {
        return locations.get(id);
    }

    public void setLocation(String id, int x, int y) {
        if (locations.replace(id, new Point(x, y)) == null)
            throw new IllegalArgumentException("invalid vehicle name: " + id);
    }
}
```

Note that in the example above, the `Point` class is immutable. If it was mutable, that would not have been a thread-safe class.  

Furthermore, note that this implementation is slightly different from the previous one as the `getLocations` method returns a "live" view of the underlying collection.  

This, as already stated, is based on requirements.

## Independent state variables
The previous example delegates thread-safety to a single thread-safe object. There are no addition synchronization requirements if that is the case.  

If, however, there is more than one state variable, then thread-safety depends on whether the state variables are independent.

If they are independent, no additional locking is required as they can safely be accessed concurrently without mutual exclusion.

Example:
```java
public class VisualComponent {
    private final List<KeyListener> keyListeners = new CopyOnWriteArrayList<KeyListener>();
    private final List<MouseListener> mouseListeners = new CopyOnWriteArrayList<MouseListener>();

    public void addKeyListener(KeyListener listener) {
        keyListeners.add(listener);
    }

    public void addMouseListener(MouseListener listener) {
        mouseListeners.add(listener);
    }

    public void removeKeyListener(KeyListener listener) {
        keyListeners.remove(listener);
    }

    public void removeMouseListener(MouseListener listener) {
        mouseListeners.remove(listener);
    }
}
```

In the above example, `CopyOnWriteArrayList` is a thread-safe collection.

## When delegation fails
If state variables form an invariant (i.e. they are a compound action), then explicit synchronization is needed even if the classes are thread-safe:
```java
public class NumberRange {
    // INVARIANT: lower <= upper
    private final AtomicInteger lower = new AtomicInteger(0);
    private final AtomicInteger upper = new AtomicInteger(0);

    public void setLower(int i) {
        // Warning -- unsafe check-then-act
        if (i > upper.get())
            throw new IllegalArgumentException("can’t set lower to " + i + " > upper");

        lower.set(i);
    }

    public void setUpper(int i) {
        // Warning -- unsafe check-then-act
        if (i < lower.get())
            throw new IllegalArgumentException("can’t set upper to " + i + " < lower");
        upper.set(i);
    }

    public boolean isInRange(int i) {
        return (i >= lower.get() && i <= upper.get());
    }
}
```

## Publishing underlying state variables
Only if a class is thread-safe and doesn't participate in any class invariants can it be published outside its scope.

For example, a class that holds a `lastLoginDate` of a user can publish that variable as long as it's thread-safe & it isn't used in any other invariants (e.g. check-then-acts).
On the other hand, publishing the `value` in the `Counter` class is not a good idea as the class constrains the possible values to be non-negative.

Lastly, just because you can publish a variable doesn't mean it is a good idea as you might hinder future development & maintenance.

## Example: vehicle tracker that publishes its state
```java
@ThreadSafe
public class PublishingVehicleTracker {
    private final Map<String, SafePoint> locations;
    private final Map<String, SafePoint> unmodifiableMap;

    public PublishingVehicleTracker(Map<String, SafePoint> locations) {
        this.locations = new ConcurrentHashMap<String, SafePoint>(locations);
        this.unmodifiableMap = Collections.unmodifiableMap(this.locations);
    }

    public Map<String, SafePoint> getLocations() {
        return unmodifiableMap;
    }

    public SafePoint getLocation(String id) {
        return locations.get(id);
    }

    public void setLocation(String id, int x, int y) {
        if (!locations.containsKey(id))
            throw new IllegalArgumentException("invalid vehicle name: " + id);

        locations.get(id).set(x, y);
    }
}
```

In the example above, the `SafePoint` class is thread-safe, but mutable. 

Hence, clients can modify it at will when they receive it from `getLocation` or `getLocations`.  

This is possible only if the modification of the point isn't "interesting" to the `PublishingVehicleTracker` (e.g. this won't be OK if there was some hook to be executed when a point is modified).

And here is the thread-safe `SafePoint`:
```java
@ThreadSafe
public class SafePoint {
    @GuardedBy("this") private int x, y;

    private SafePoint(int[] a) { this(a[0], a[1]); }

    public SafePoint(SafePoint p) { this(p.get()); }

    public SafePoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public synchronized int[] get() {
        return new int[] { x, y };
    }

    public synchronized void set(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
```

# Adding functionality to existing thread-safe classes
It is advised to use an existing thread-safe class which supports all the operations you need.

But oftentimes, a class supports almost all the operations you need.

In that case, you need to extend it to support the additional operation.

For example, you might have a thread-safe list, for which you want to add the `putIfAbsent` method.  
The easiest way to achieve that is to modify the existing class.

But oftentimes, that is not possible as you don't own the source code.

In that case, you need to use different mechanisms to do it.

One of the simplest way to do it is via extending the class & synchronizing the new method using the class' intrinsic lock.  
```java
@ThreadSafe
public class BetterVector<E> extends Vector<E> {
    public synchronized boolean putIfAbsent(E x) {
        boolean absent = !contains(x);
        if (absent)
            add(x);

        return absent;
    }
}
```

This will work only if the class uses the intrinsic lock internally as well. Otherwise, the resulting class is not thread-safe.  

Be cautious, however, as if the internal class changes its synchronization policy in a new version, your class will no longer work.

## Client-side locking
If extension is not an option, you might want to provide the new method in a "helper" class.

This is a failed attempt to do so:
```java
@NotThreadSafe
public class ListHelper<E> {
    public List<E> list = Collections.synchronizedList(new ArrayList<E>());

    public synchronized boolean putIfAbsent(E x) {
        boolean absent = !list.contains(x);
        if (absent)
            list.add(x);

        return absent;
    }
}
```

This code doesn't work as the helper class doesn't use the same lock as the `synchronizedList`.  
This way, the `putIfAbsent` operation is not mutually exclusive to the rest of the list's operations.

The correct way to do so is using client-side locking - using the lock that the thread-safe class uses:
```java
@ThreadSafe
public class ListHelper<E> {
    public List<E> list = Collections.synchronizedList(new ArrayList<E>());

    public boolean putIfAbsent(E x) {
        synchronized (list) {
            boolean absent = !list.contains(x);
            if (absent)
                list.add(x);

            return absent;
        }
    }
}
```

Although this works, be wary with this technique as it relies on the underlying class sticking to its synchronization policy.  
If this changes in the future, your class will no longer be thread-safe.

Just as extension breaks encapsulation of implementation, client-side locking breaks encapsulation of synchronization policy.

## Composition
A more reliable way to add a new atomic method to a thread-safe class is by wrapping it into another class.  
This is the approach taken by `Collections.synchronizedList`.

This way, the wrapper class uses its own lock to manage synchronization & doesn't rely on the underlying class' locking strategy:
```java
@ThreadSafe
public class ImprovedList<T> implements List<T> {
    private final List<T> list;

    public ImprovedList(List<T> list) { this.list = list; }

    public synchronized boolean putIfAbsent(T x) {
        boolean contains = list.contains(x);
        if (contains)
            list.add(x);

        return !contains;
    }

    public synchronized void clear() { list.clear(); }
    // ... similarly delegate other List methods
}
```

# Documenting synchronization policies
Document a class's thread safety guarantees for its clients. Document it's synchronization policy for its maintainers
