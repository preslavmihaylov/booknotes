# Chapter 10: Zen of Programming Concurrency
Concurrency is not about the methods we call but about the approaches we take and the libraries we opt to use.

## Exercise Your Options
Among the most impactful decisions when dealing with concurrency is how to deal with state:
 * Shared mutability
 * Isolated mutability
 * Immutability

Avoiding shared mutability will solve a lot of concurrency issues. Adapting to some of the other styles takes some effort but those efforts will soon pay back.

## Concurrency: Programmer's Guide
Upgrade to the modern Java concurrency API as a first step.

If you have to deal with shared mutable state:
 * Make sure you use the modern tools the Java SDK provides.
 * Make fields `final` wherever possible

If it's not possible to avoid mutability, at least transition to isolated mutability.
It's much easier to resolve concurrency issues by design than having to deal with them as they surface.

If you have frequent shared reads but not as frequent writes, the STM model can be quite useful.
Alternatively, use actors to achieve isolated mutability.

## Concurrency: Architect's Guide
Concurrency and performance are most likely issues you will have to deal with at some point.

It's much better to eliminate concurrency issues than having to deal with them.
You can achieve that by removing mutable state - design applications around immutable objects or at least isolated mutability.

To deal with performance issues coming from concurrent access, consider concurrent and persistent data structures.

Try to handle concurrency concerns with the language & concurrency model choice but make sure you're sold on this in order to convince your team as well.

## Choose Wisely
In the JVM world, we generally have three options:
 * synchronize-and-suffer
 * software transactional memory
 * Actor-based concurrency

No one option provides a perfect solution for all applications.

Modern JDK Concurrency API:
 * comes out of the box with the stdlib
 * Easy to create & manage pools of threads
 * Comes with a lot of concurrent data structures
 * The main drawback of this API is that to use it effectively, we need to be aware of synchronization issues and how to avoid them

Software Transactional Memory:
 * Lock-free programming as state is immutable and identities are mutated only within transactions
 * Drawback #1 - we must ensure that the state is immutable
 * Drawback #2 - we must ensure that code inside transactions is idempotent and has no side effects
 * Drawback #3 - not suitable for write-heavy applications

The Actor-Based Model
 * Removes synchronization issues by promoting isolated mutability
 * Drawback #1 - communication between actors has to happen in messages
   * hence, we need to avoid implementing "chatty" actors
