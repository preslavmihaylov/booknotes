# Introduction
Designing object-oriented software is hard:
 * Factor objects into classes with the right granularity
 * Design should be specific for the problem but general enough to address future problems & requirements.
 * Avoid redesign or at least minimize it.

Yet, there are some noticeable patterns in experienced OO designers which help them make good designs - they reuse solutions that have worked well for them in the past, instead of starting from first principles
These patterns are solutions to common problems, which makes OO software more flexible, elegant & reusable.

Purpose of the book - record OO design experience in the form of design patterns.

## What Is a Design Pattern?
> Each pattern describes a problem which occurs over and over again in our environment, and then describes the core of the solution to that problem, in such a way that you can use this solution a million times over, without ever doing it the same way twice"

A pattern has four elements:
 * Name - a handle to quickly describe a design problem in a few words.
 * Problem - when to apply the pattern.
 * Solution - The elements which make up the design.
 * Consequences - results & trade-offs of applying the pattern

> A design pattern names, abstracts, and identifies the key aspects of a common design structure that make it useful for creating a reusable object-oriented design.

## Describing Design Patterns
This section covers how the patterns in the book are structured & the goal behind every part of the structure:
 * Pattern Name and Classification - Convey the essence of the pattern succinctly
 * Intent - answers "What does the pattern do? What's its rationale? What particular problem does it address?
 * Also Known As - other well-known names for the pattern if any
 * Motivation - An example scenario where the pattern can be put to use
 * Applicability - When can the pattern be applied?
 * Structure - A graphical representation of the classes in the pattern
 * Participants - Classes participating in the pattern & their responsibilities
 * Collaborations - How the participants collaborate to carry out their responsibilities
 * Consequences - What are the trade-offs and results of using the pattern
 * Implementation - Pitfalls, hints or techniques to be aware of while implementing the pattern
 * Sample Code - example implementation
 * Known Uses - Examples of the pattern found in real systems
 * Related Patterns - Other closely related design patterns

## The Catalog of Design Patterns
This section briefly covers the design patterns explored in the book & what they solve in a nutshell:
 * [Abstract Factory](./../ch03/01.abstract-factory) - Create related or dependent objects through a common interface without specifying the concrete type.
 * [Adapter](./../ch04/01.adapter) - Convert an incompatible interface to a compatible one, which the client expects.
 * [Bridge](./../ch04/02.bridge) - Decouple abstraction from implementation so that they can vary independently.
 * [Builder](./../ch03/02.builder) - Separate object construction from representation so that construction can support multiple representations.
 * [Chain of Responsibility](./../ch05/01.chain-of-responsibility) - Avoid coupling the sender of a request to its receiver by creating a chain of receivers.
 * [Command](./../ch05/02.command) - Encapsulate a request as an object, letting you support request history, undo/redo, logging, etc.
 * [Composite](./../ch04/03.composite) - Compose objects into tree structures to represent recursive hierarchies.
 * [Decorator](./../ch04/04.decorator) - Attach responsibilities to an object dynamically.
 * [Facade](./../ch04/05.facade) - Provide a unified interface to a set of interfaces in a subsystem.
 * [Factory Method](./../ch03/03.factory-method) - Define an interface for creating an object, but let subclasses decide the specific instance.
 * [Flyweight](./../ch04/06.flyweight) - Use sharing to support large numbers of small objects efficiently.
 * [Interpreter](./../ch05/03.interpreter) - Define a grammar for a language as well as an interpreter for that grammar.
 * [Iterator](./../ch05/04.iterator) - Provide a way to access elements of a collection without exposing internal representation.
 * [Mediator](./../ch05/05.mediator) - Define an object which encapsulates how a set of objects interact.
 * [Memento](./../ch05/06.memento) - Capture & externalize an object's internal state without violating encapsulation.
 * [Observer](./../ch05/07.observer) - Define a one-to-many dependency between objects (i.e. publish-subscribe model).
 * [Prototype](./../ch03/04.prototype) - Define a set of related objects via a common prototype. Derive new objects from this prototype.
 * [Proxy](./../ch04/07.proxy) - Control access to an object via a placeholder for it.
 * [Singleton](./../ch03/05.singleton) - Ensure a class has only one instance.
 * [State](./../ch05/08.state) - Alter object's behavior based on its internal state changes.
 * [Strategy](./../ch05/09.strategy) - Define a common interface for a family of algorithms & let them vary independently.
 * [Template Method](./../ch05/10.template-method) - Define a skeleton of an algorithm & defer some steps to subclasses.
 * [Visitor](./../ch05/11.visitor) - Represent an operation to be performed on a given object family.

## Organizing the Catalog
![Design Patterns Catalog](images/dp-catalog.png)

Interpreting the table:
 * Purpose - define what's a class' purpose.
    * Creational - concerned with object creation. 
    * Structural - concerned with object composition.
    * Behavioral - concerned with how objects behave & distribute responsibility.
 * Scope - whether a pattern concerns objects (dynamic, runtime) or classes (static, compile-time).

## How Design Patterns Solve Design Problems
How design patterns help you solve problems in your day to day.

### Finding Appropriate Objects
One way to decompose a system into a set of objects is to model the classes via entities found in the real world.

The problem with this approach is that the system will be able to represent today's reality but not tomorrow's. This trumps one of the core value propositions of software - being flexible to change.

Instead, throughout the lifecycle of a system, classes which don't have counterparties in the real-world will emerge - eg builders, composites, etc.

These classes help the system evolve to meet new requirements. Design patterns help you find less-obvious abstractions and how to implement them effectively.

### Determining Object Granularity
Objects can vary tremendously in size & number. Design patterns help you decide what should be in an object.

Different patterns provide ways to e.g. decompose an object into smaller parts (builder, visitor), handle large number of small objects (flyweight), represent a subsystem as an object (Facade).

### Specifying Object Interfaces
Design patterns help you specify object interfaces by identifying the key elements which go in an interface.

E.g. Memento specifies that an object should define two interfaces - public & restricted (which exposes internal details).
E.g. Decorator require that the interfaces of the decorators be identical to the decorated object.

### Interpreting Common UML Constructs
Defining a class - lines separate class name from operations from data.
![Class Definition](images/class-definition.png)

Dashed Line - class instantiates objects of another class:
![Class Instantiating Objects](images/uml-class-instantiating-objects.png)

Line with arrow in the middle - inheritance:
![Inheritance Example](images/uml-inheritance-example.png)

Abstract classes are written in italic. Dog-eared box represents example implementation in pseudocode:
![Abstract Class + example implementation](images/uml-abstract-class-example.png)

Mixin - provide optional interface of functionality through multiple inheritance:
![Mixin Example](images/uml-mixin-example.png)

### Specifying Object Implementation
Class inheritance == define object implementation in terms of another's implementation
Interface inheritance == define when an object can be used instead of another

Class inheritance is a mechanism for reusing functionality in a parent class. Additionally, it lets you define a family of objects with a common interface.

Common wisdom is to "program against an interface, not an implementation". I.e. use interfaces as much as you can.

However, you need to specify concrete implementations which implement the interfaces at some point in your program and certain design patterns help you encapsulate that process (abstract factory, factory method, etc).

### Putting Reuse Mechanisms to Work
