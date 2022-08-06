# Maintainability
The majority of the cost of software is spent in maintaining it after its initial development:
 * Fixing bugs
 * Adding new features
 * Operating it

We should aspire our systems to be easy to modify, extend and operate so that they're easy to maintain.

How to do that?
 * Good testing is a minimal requirement to be able to extend a system while ensuring it doesn't break.
 * Once the change is merged into the repo, it should be rolled out into production without that affecting the application's availability.
 * Operators need to be able to monitor the system's health, investigate degradations and restore it when it gets into a bad state.
 * This requires altering the system's state without touching the code - via a configuration change or a feature flag.

Historically, developers, testers and operators were different teams. Nowadays, developers do it all.

This part is all about best practices in testing & operating large scale distributed systems.

# Testing
The longer it takes to detect a bug, the more expensive it is to fix it.

How does testing help?
 * A software test verifies that some part of the application works correctly, catching bugs early.
 * The real benefit, though, is that tests allow you to alter the system's behavior with high confidence that you're not breaking the expected behavior.
 * Tests are also an always up-to-date documentation of the codebase.
 * Finally, they improve the public interface of a system as developers are forced to look at the system from a client's perspective.

Testing is not a silver bullet, though, as you can't predict all the states an application can get into. You can only predict those behaviors developers can predict.
Oftentimes, complex behaviors which only occur in production are not captured by tests.

Regardless, tests do a good job of validating expected behaviors, although they don't guarantee your code is bug-free.

If you want to be confident in your application's behavior, you'll need to add tests for it.

## Scope
Scope defines the code path under test aka system under test (SUT).

This designates whether a test is unit, integration or end to end.

A unit test validates the behavior of a single component such as a class.
A good unit test is relatively static & only changes when the behavior of the component changes.

To achieve that, a unit test should:
 * work with the public interface of the component only.
 * test for state changes in the SUT vs testing for a predetermined sequence of actions.
 * test for behaviors - ie, how SUT reacts to a given input given it is in a specific state.

An integration test has a larger scope than a unit test as it verifies the correct integration with an external component.

Integration tests have different meanings in difference contexts. Martin Fowler defines them as:
 * A narrow integration test only exercises the code paths of a service, communicating with an external dependency.
 * A broad integration test exercises code paths across multiple services. We will refer to those as end-to-end tests.

An end-to-end test validates behavior that spans multiple services, ie a user-facing scenario.
These tests usually run in a shared environment (eg staging or prod). They should not impact other users or tests using the same environment.

Because of the larger scope, they are slower & more prone to intermittent failures.

These can also be painful to maintain - when such a test fails, it's not obvious which particular component failed.
But they're a necessary evil as they validate user-facing behavior which is harder to validate with tests with a smaller scope.

To minimize end-to-end tests, you can frame them as a user journey test - they validate multi-step interactions of a user with a system vs. testing an individual action.

As the scope of a test increases, it becomes more brittle, slow and costly. Intermittently failing tests are nearly as bad as no tests at all since developers quickly learn to ignore them due to the noise.

A good trade-off is to have a lot of unit tests, a smaller fraction of integration tests and a few end to end tests:
![test-pyramid](images/test-pyramid.png)

## Size
The size of the tests determines how much computing resources (ie nodes) the test needs to run. This largely depends on how realistic the environment where the test runs is.

Scope and size are usually correlated, but they are distinct concepts.

How to differentiate tests in terms of size:
 * A small test runs in a single process, without performing any I/O - it's very fast, deterministic & probability of intermittent failures is low.
 * An intermediate test runs on a single node and performs local I/O - this leaves more room for delay & intermittent failures.
 * A large test requires multiple nodes to run - leading to more non-determinism and delays.

The larger the test is, the longer it takes to run & the flakier it becomes. Hence, we should write the smallest possible test for a given behavior.

A technique to avoid increasing the size of a test is using test doubles:
 * A fake is a lightweight implementation of the external dependency that behaves similarly to it. Eg an in-memory version of a database.
 * A stub is a function which always returns the same value regardless of input.
 * A mock has expectations on how it should be called and it's used to test interactions between objects.

The problem with test doubles is that they don't behave like the real system, hence the confidence we have with them is lower.
That's why - when the real implementation is fast, deterministic & has few dependencies, we can use it directly instead.
When that is not an option, we can use a fake of the real implementation, which is maintained by the same developers.

Stubbing and mocking are last-resort options as they offer the lowest degree of confidence.

For integration tests, using [contract tests](https://martinfowler.com/bliki/ContractTest.html) is a good compromise.
A contract test defines the request for an external dependency with expected result. The test uses this contract to mock the dependency.

But the dependency also validates its system acts in the way the contract expects it to.

## Practical considerations
Testing requires trade-offs, similar to everything else.

Let's imagine we want to test the behavior of a specific API endpoint of a service with:
 * A data store
 * An internal service owned by another team
 * A third-party API used for billing
![sample-system-for-testing](images/sample-system-for-testing.png)

As previously discussed, we should attempt to write the smallest possible test for the scope we want while minimizing the use of test doubles.

Our decisions:
 * Assuming our endpoint doesn't use the internal service, we can use a mock in its place.
 * If the data store has an in-memory version, we can use it to avoid making network calls.
 * We can't call the third-party billing API directly as that entails making actual transactions, but the API might have a testing environment we can use instead.

A more nuanced example - testing that we can safely purge the data belonging to a particular user across the entire system as that's mandated by GDPR.
Failing to comply can lead to fines up to 20mil EUR or 4% annual turnover.

The impact of this functionality silently breaking is high, hence we should maximize our confidence that the functionality works correctly - we'll need to setup an end to end test, that periodically runs in production and uses live services.

## Formal verification
Software tests are not the only way to capture bugs early.

Writing a high-level description of how the system behaves (ie specification) allows subtle bugs & architecture shortcomings to be detected before writing any code.
A specification helps us reason about the behaviors of our system. It's also a documentation and guide for the ones which implement the system.

A specification can range from a one-pager to a formal mathematical description that a computer can check.
Writing one doesn't entail describing every corner of the system in detail - we only want to specify those paths that are most likely to contain errors, which are hard to detect via tests.

TLA+ is a well-known formal specification language. One can use it to describe the behavior of a system via a set of states & changes across them.
Microsoft & Amazon use it to describe their most complex distributed systems, such as S3 and CosmosDB.

The goal of using such a tool is to verify the safety & liveness of an application:
 * Safety - something is true for all behaviors of the system (invariant).
 * Liveness - something eventually happens.

TLA+ is very useful for systems running at huge scale as those will eventually run into states, which humans can't imagine.

Example - we have a key-value store X and we want to migrate to key-value store Y.

How to implement that:
 * Service writes to both X and Y (dual writes), while reading from X.
 * One-off batch process backfills Y with the data from X.
 * Application switches to read/write exclusively from Y.

Approach seems reasonable but it doesn't guarantee that both stores will eventually end up in the same state:
 * In case write to A succeeds, but write to B fails, the data stores are not in the same state.
 * Using an atomic write resolves the liveness issue, but two service instances writing at the same time can lead to writes not coming in the same order.
![out-of-order-writes](images/out-of-order-writes.png)

 * Finally, using a message channel between application instances and the database can serialize the writes and guarantee global order.

Regardless of the specific problem, the point is that using a formal verification system such as TLA+ can help us catch these kinds of errors before any code is written.

# Continuous delivery and deployment
