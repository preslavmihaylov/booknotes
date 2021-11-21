# Creational Patterns Discussion

If you want to allow a system to vary the concrete objects it needs to instantiate, use a creational pattern:
 * Factory Method - simple method for instantiating concrete object(s), which can also be subclassed
 * Abstract Factory - a class for instantiating common product families using multiple factory methods
 * Prototype - a class for instantiating a type of product by copying its internals. Avoids creating too many subclasses
 * Builder - a class for instantiating a concrete object using a gradual instantiation approach versus the one-step approach all other patterns offer
 * Singleton - a class for providing a single global instance of a given class

Most often, applications start with the use of factory methods. If the needs become more complicated in the future, they branch out to either using Abstract Factory or Prototype.
 * Abstract Factory - set of factory methods for a common product family at the cost of many subclasses.
 * Prototype - allows concrete products to specify how they're to be created without the need for a parallel factories class hierarchy.
   * The downside is that all products need to abide by a common interface in order to use multiple different products by a single client 
   * Abstract Factory, on the other hand, allows a client to have access to different product types with different interfaces

If the creation process is more complicated, Builder can be used to make it more iterative.

Finally, Singleton can be used as a supplement to most other patterns to prevent e.g. the `AbstractFactory` from being instantiated more than once.
