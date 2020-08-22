# Chapter 11 - Performance and Scalability
The rationale for using concurrency is often because one wants to achieve more performance.

This chapter focuses on analyzing, monitoring and improving the performance of multi-threaded programs.
Often times, however, performance improvements come at the cost of complexity and/or safety risks.

Safety should not be compromised in favor of performance.

# Thinking about performance
When we think about performance, we usually mean extracting more throughput with fewer resources.

Performance is often bound by a certain resource (bottleneck). E.g. database calls, I/O, CPU, etc.

Additionally, using multiple threads has some performance costs in comparison to single-threaded programs due to context switching, locking, etc.
In some cases, a multi-threaded program could perform worse than a single-threaded equivalent.

By using concurrency to exploit performance we aim for two things:
 * Utilize the available resources more effectively
 * Leverage the existing resources more effectively (e.g. keeping the CPUs as busy as possible)

## Performance vs. Scalability
Performance can be measured in multiple ways:
 * service time
 * latency,
 * throughput
 * efficiency
 * capacity
 * scalability

The latter two properties are measures of "how much" work can be done. The first ones are measures of "how fast" work can be done.
> Scalability == the extent to which capacity is improved when additional resources are added

Designing and tuning for performance is very different from tuning for scalability.

In the former case, the goal is to do the same amount of work with less effort.
In the later case, the goal is to parallelize the work so that capacity is improved when additional resources are added.

Often times, these two goals are at odds with each other. Sometimes, degrading performance can lead to improved scalability.

A monolithic, coupled application with an MVC architecture will yield better performance for a single request than a multitier architecture, split across several systems (micro services).

For most web applications, scalability is often more important than performance. That is what this chapter focuses on.

## Evaluating performance tradeoffs
Nearly all engineering decisions are tradeoffs.

Often times, one has to make traceoffs based on limited information. E.g. quicksort is better for large datasets, but bubblesort is best for small ones.
One has to know in advance the amount of data in order to process it most effectively.

This is what most often leads to premature optimizations - making trade offs with limited requirements.
It is best to optimize for correctness first & only improve performance after it is proven to be insufficient.

> First make it right, then make it fast - if it is not fast enough already

Applying performance optimizations often lead to higher complexity & less readability. This inherently increases maintenance cost.

Before making a performance optimization to make a routine faster, think about:
 * What does "fast" mean in this case?
 * Under what conditions will this actually be faster? Light vs. heavy load. Small vs. big datasets. Support answer with measurements.
 * How often are these conditions met? Is it an edge-case or a typical use-case?
 * What are the costs of applying this performance optimization?

The quest for performance is the single greatest source of concurrency bugs. It has lead to countless bad idioms for the sake of performance.

E.g. the double-check idiom over plain & simple synchronization.

Measure performance improvement before & after optimizations.

# Amdahl's law

