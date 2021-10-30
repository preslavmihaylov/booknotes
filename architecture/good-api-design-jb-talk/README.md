# How To Design A Good API and Why it Matters
A talk given by Joshua Bloch at a Google Tech Talk - 24 Jan, 2007

[Original Talk](https://www.youtube.com/watch?v=aAb7hSCtvGw&ab_channel=GoogleTechTalks)  
[Slides](http://www.cs.bc.edu/~muller/teaching/cs102/s06/lib/pdf/api-design)

## Table of Contents
- [Why It Matters](#why-it-matters)
  - [Why is API Design important?](#why-is-api-design-important)
  - [Why is API Design important to you?](#why-is-api-design-important-to-you)
  - [The Characteristics of a good API](#the-characteristics-of-a-good-api)
- [The Process of API Design](#the-process-of-api-design)
  - [Gather requirements](#gather-requirements)
  - [Short Specification](#short-specification)
  - [Write to Your API Early and Often](#write-to-your-api-early-and-often)
  - [Writing to SPI is Even More Important](#writing-to-spi-is-even-more-important)
  - [Maintain Realistic Expectations](#maintain-realistic-expectations)
- [General Principles](#general-principles)
  - [API Should Do One Thing and Do It Well](#api-should-do-one-thing-and-do-it-well)
  - [API Should Be As Small As Possible But No Smaller](#api-should-be-as-small-as-possible-but-no-smaller)
  - [Implementation Should Not Impact API](#implementation-should-not-impact-api)
  - [Minimize Accessibility of Everything](#minimize-accessibility-of-everything)
  - [Names Matter](#names-matter)
  - [Documentation matters](#documentation-matters)
  - [Consider Performance Consequences of API Design Decisions](#consider-performance-consequences-of-api-design-decisions)
  - [API Must Coexist Peacefully with Platform](#api-must-coexist-peacefully-with-platform)
- [Class Design](#class-design)
  - [Minimize Mutability](#minimize-mutability)
  - [Subclass Only When It Makes Sense](#subclass-only-when-it-makes-sense)
  - [Design and Document for Inheritance or Else Prohibit it](#design-and-document-for-inheritance-or-else-prohibit-it)
- [Method Design](#method-design)
  - [Don't Make The Client Do Anything The Module Could Do](#dont-make-the-client-do-anything-the-module-could-do)
  - [Don't Violate The Principle of Least Astonishment](#dont-violate-the-principle-of-least-astonishment)
  - [Fail Fast - Report Errors as Soon as Possible](#fail-fast---report-errors-as-soon-as-possible)
  - [Provide Programmatic Access to All Data Available in String Form](#provide-programmatic-access-to-all-data-available-in-string-form)
  - [Overload With Care](#overload-with-care)
  - [Use Appropriate Parameter and Return Types](#use-appropriate-parameter-and-return-types)
  - [Use Consistent Parameter Ordering Across Methods](#use-consistent-parameter-ordering-across-methods)
  - [Avoid Long Parameter Lists](#avoid-long-parameter-lists)
  - [Avoid Return Values that Demand Exceptional Processing](#avoid-return-values-that-demand-exceptional-processing)
- [Exception Design](#exception-design)
  - [Throw Exceptions to Indicate Exceptional Conditions](#throw-exceptions-to-indicate-exceptional-conditions)
  - [Favor Unchecked Exceptions](#favor-unchecked-exceptions)
  - [Include Failure-Capture Information in Exceptions](#include-failure-capture-information-in-exceptions)

## Why It Matters
### Why is API Design important?
APIs are among a company's greatest assets:
 * Successful APIs capture customers
 * Customers invest heavily in an API - buying, writing & learning
   * Once a customer learns an API, they are reluctant to learn a different one as they're already used to the old one

But a bad API can be among a company's greatest liabilities:
 * It can lead to an unending stream of support calls
 * Can inhibit a team from moving forward & working on more impactful projects

APIs are also forever. You have one chance to build them right.

### Why is API Design important to you?
If you're programming, you're already an API designer. Good code is modular and each module has an API.

If a module is successful, it will be used and reused. Hence, you're no longer free to change it at will.

If you think in terms of APIs when you program, you'll write more high-quality code.

### The Characteristics of a good API
 * Easy to learn
 * Easy to use, even without docs
 * Hard to misuse - it should force you to do the right thing
 * Easy to read and maintain code that uses it
 * Sufficiently powerful to satisfy requirements - not powerful, but powerful enough to satisfy requirements
 * Easy to evolve - meets requirements, but can evolve to meet future requirements
 * Appropriate for your audience - what's a good API for analysts is not a good API for physicists

## The Process of API Design
### Gather requirements
Learn what the clients need from the API but be wary that they'll usually propose solutions, instead of specifying requirements.
You have to extract the true requirements in the form of use-cases - the problems that the API should be able to solve.

Use-cases are the benchmark against which you can measure any proposed solution.

Example:
 * What they say - we need data structures and RPCs with the version 2 attributes
 * What they mean - We need a new data format that accommodates evolution of attributes

Sometimes it can be easier and more rewarding to build a more generalized solution.
This doesn't mean you should over-engineer an API every time you get a new requirement. 
It means to be mindful about situations where building a generalized solution is easier, whilst also being more rewarding.

### Short Specification
Start with a one-pager specification. At this stage, agility trumps completeness.
If a spec is short, it's easy to modify. 

Share with as many people as possible, listen for their feedback & take it seriously.

Once you gain confidence, tinker with it as a client. 
This will give you a better idea of what it's like to use the API.
This doesn't mean implementing the API, but pretending it's implemented.

### Write to Your API Early and Often
 * Start before you've implemented the API to avoid throwing away the implementation afterwards.
 * Start before you've even specified it properly to avoid writing specs you'll throw away
 * Continue doing this process as you're fleshing it out
 * Code written at this stage continues living on as examples and unit tests - if you write the examples right, you've seeded the market with good uses of your API and vice versa

The speaker gave an example of broken Java examples in the first release of java which lead to thousands of broken concurrent programs that still exist.

> Those initial pieces of code that you write to any API are among the most important pieces of code that you'll ever write to it.

> You should spend 10 times more time on example code than you spend on production code.

### Writing to SPI is Even More Important
SPI (Service Provider Interface) == plug-in interface enabling multiple implementations

Write multiple plugins before release, at least three.

### Maintain Realistic Expectations
Most APIs have constraints - you can't please everyone.

Expect to make mistakes, which you'll fix after several years of real-world use. Your API will evolve.

## General Principles
### API Should Do One Thing and Do It Well
Functionality should be easy to explain. The routines should have good names. 
If it's hard to name one, it's generally a bad sign.

If you're doing too many things - consider splitting modules.
If you're exposing internal details via many modules - consider consolidating modules & hiding internals.

### API Should Be As Small As Possible But No Smaller
API should satisfy requirements.

> When in doubt, leave it out. You can always add but never remove.

Conceptual weight is more important than bulk - how many concepts do I need to learn about?

If an API uses existing interfaces, it keeps the conceptual weight small
> If you know how to use an ExecutorService, you already know how to use a RetryingExecutorService

You need to do a lot without learning a lot.

### Implementation Should Not Impact API
How you want to implement the API should not impact the APIs signature.

The implementation details confuse users and inhibit the freedom to change the implementation.
E.g. throwing SQL exceptions from your API. What if you want to change the SQL database later?

When documenting your API, don't specify implementation details - eg mentioning the hash function used for a `Set`.

### Minimize Accessibility of Everything
Make all members of a class as private as possible.

This minimizes coupling and maximizes information hiding.

### Names Matter
Names should be self-explanatory and consistent across the API and the platform around the API.

They should also be consistent - having "remove" and "delete" words inside the same API is confusing.

### Documentation matters
Achieving good reuse requires good design and good documentation. 
Even if a component is designed well, it won't be reused without good documentation.

Document religiously in a public API - every class, interface, method, parameter, exception.

### Consider Performance Consequences of API Design Decisions
Fortunately, good API design usually leads to opportunity for good performance.

Bad decisions can limit performance:
 * Making a type mutable - an instance needs to be allocated on every call
 * Constructor instead of static factory
 * Using implementation type instead of interface - if the implementation is not performant, you can't change that in the future

### API Must Coexist Peacefully with Platform
Obey standard naming conventions, mimic patterns in core API.
Know and avoid common API pitfalls - eg finalizers, public static final arrays.

Don't transliterate APIs - eg taking a C++ interface and mimic it in Java. You should think of how to "implement it the Java way".

## Class Design
### Minimize Mutability
Classes should be immutable unless there's a good reason to do otherwise:
 * Advantages - simple, thread-safe, reusable
 * Disadvantages - separate object for each value. In example, making a copy of a big object if you want to change a small part of it.

If mutable, keep state-space small, well defined - make it clear when it's legal to call a method.

### Subclass Only When It Makes Sense
A subclass should be substitutable for the base class (Liskov). Otherwise, use composition.

Public classes should not subclass other public classes for ease of implementation.

Bad example - `Stack extends Vector`
Good example - `Set extends Collection`

### Design and Document for Inheritance or Else Prohibit it
Inheritance violates encapsulation. Mere method invocation does not.

If a subclass overrides a method in a base class, you may have changed the behavior of other methods in the subclass because they were dispatching flow of control to the overriden method.
If in the future, the base class changes this dispatching, then that future version of the base class breaks all subclasses.

To avoid this, document exactly how every method uses every other method in the base class and don't change that in future versions.

Rule of thumb - make concrete classes final.

Good class - `AbstractSet`, `AbstractMap`

## Method Design
### Don't Make The Client Do Anything The Module Could Do
Reduce the need for boilerplate code in clients which wire up complicated code via multiple API calls to execute a single operation.

### Don't Violate The Principle of Least Astonishment
The user of an API should not be surprised by its behavior.

It's worth extra implementation effort and it's even worth reduced performance.

### Fail Fast - Report Errors as Soon as Possible
Doing it at compile time is best. At runtime, throw them during the first bad method invocation.

Bad example - an error is thrown not when the object is constructed, but when it's first used:
```java
// A Properties instance maps strings to strings
public class Properties extends Hashtable {
    public Object put(Object key, Object value);

    // Throws ClassCastException if this properties
    // contains any keys or values that are not strings
    public void save(OutputStream out, String comments);
}
```

### Provide Programmatic Access to All Data Available in String Form
Otherwise, the strings will be parsed & it turns the strings in to de facto API.

Good example - an exception which returns a stack trace should return it as an object not as a blob of string.

### Overload With Care
Avoid ambiguous overloadings - do different things when passed the same value.

Example:
```java
public TreeSet(Collection c); // Ignores order
public TreeSet(SortedSet s); // Respects order
```

A `SortedSet` is also a `Collection`. Which method will get invoked?

It's often better to just use a different name.

### Use Appropriate Parameter and Return Types
Interfaces over classes for input parameters.

Use most specific input parameter type.
Otherwise, you'll return an error at runtime instead of doing it at compile time

In example, don't accept `Collection` if you blow up unless you get a `List`.

Don't use strings if possible because you lose static typing.

### Use Consistent Parameter Ordering Across Methods
Very confusing for clients & is error-prone.

Bad example:
```cpp
#include <string.h>
char *strcpy (char *dest, char *src);
void bcopy (void *src, void *dst, int n);
```

### Avoid Long Parameter Lists
Three or fewer parameters is ideal. Especially avoid long lists of parameters with the same type.

How to tackle issue if you need more than three?
 * Break up method into multiple methods
 * Create helper class to hold parameters - ie the `Builder` pattern

### Avoid Return Values that Demand Exceptional Processing
In example, return empty collection instead of `null`, because you'll need to check it afterwards.

## Exception Design
### Throw Exceptions to Indicate Exceptional Conditions
In example, if you want to iterate a list and it has zero elements, don't throw an exception.

### Favor Unchecked Exceptions
Overuse of checked exceptions leads to boilerplate.

Use checked exceptions when the client needs to take a recovery action. Use unchecked exceptions when there's a programming error.

### Include Failure-Capture Information in Exceptions
Allows diagnosis and recovery. Do this for checked exceptions. For unchecked ones, a message is sufficient.
