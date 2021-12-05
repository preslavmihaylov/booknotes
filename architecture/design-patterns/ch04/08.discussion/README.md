# Structural Patterns Discussion
This chapter focuses on outlining the differences between all structural patterns.
Most of them use similar mechanisms but have a different intent.

## Adapter vs. Bridge
They both have common attributes. Both forward requests to an object with a different interface.

The key difference is that Adapter focuses on resolving the incompatibility between two interfaces, while Bridge focuses on letting an abstraction differ from its (potentially multiple) implementation(s).

An Adapter is often used after classes are implement to resolve incompatibilities. Bridge is more of an upfront design to accommodate future implementations.

## Note about Facade
Facade might be thought of as an adapter to multiple adaptees. However, the difference is that adapter conforms to an existing interface, while Facade implements a new one.

## Composite vs. Decorator vs. Proxy
Composite and Decorator have similar object structures as they both rely on recursive object composition.

The difference, again, is in the intent.

Decorator lets you add responsibilities to an object without subclassing. Composite allows hierarchical structures to be treated uniformly.

These two patterns can be used in conjunction - Composite to represent an object hierarchy and Decorator to add additional responsibilities to leaf nodes in the hierarchy.

Decorator is also very similar to Proxy. The difference is, again, in the intent.

Decorator adds responsibilities to an existing object, while Proxy manages the lifecycle of an object. In addition to this, Proxy is usually not designed for recursive composition.
