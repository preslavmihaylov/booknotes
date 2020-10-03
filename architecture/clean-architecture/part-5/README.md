# Part 5 - Architecture

## What is architecture?

First of all, software architects are programmers, some of the best actually.  
They may not write as much code as everyone else does, but they do continue engaging in programming tasks.

The architecture of a system is its shape - the way the application is decomposed into components & the way those components interact with each other.

**The purpose of architecture is to make it easier to develop, maintain, deploy and operate the application. It is not to make the application work.**

### Development

A software system which is hard to develop is not likely to have a long and healthy lifetime.
Hence, the architecture of a system should make it easy to develop for the owning teams.

Very often, when a system is developed by a small team, the team decides to not invest in building proper interfaces and superstructures to avoid the impediment of one.
This is likely the reason why so many applications lack a good architecture these days.

### Deployment
To be effective, a software system has to be deployable.

Higher cost of deployment => the system is less useful.

This is often problematic in typical codebases as a deployment strategy is not considered in the early days of a project.
This can lead to e.g. someone making a "micro-service" architecture, but without a good means of deployment, it is hell to make all the services synchronize & communicate together.

### Operation
The impact of architecture on system operation tends to be lesser than the impact on the other paramenters.

Any operational problem can be tackled by throwing more hardware at a problem without that impacting the system's architecture.

Due to this, architectures that impede operation are less costly than architectures that impede developability and deployability.
The reason is that hardware is cheap, but people are expensive.

This is not to say that architecture which aids operation is not desirable. But it is to say that the cost equation leans more towards the other parameters.

### Maintenance
Of all aspects of a software system, maintenance is the most costly.

The never ending flow of new features and the inevitable trail of defects consumes vast amounts of man hours.A

The cost of maintenance is expressed in the time it takes to determine the best place for a new feature and the risk associated with adding it.

A good architecture greatly mitigates these costs. When components have stable interfaces and proper boundaries, ambiguity and risk will be reduced.

### Keeping options open

The goal of an architecture is to make a software system flexible.

That can be achieved by "keeping options open".

Every software can be decomposed into two distinct sections - business rules and details.

Examples of details are:
 * What database will be used
 * Is this a web server/GUI application/Sprint application, etc
 * Is it using a dependency injection framework

A good software architecture should defer making the decision whether any of these technologies will be used to as late as possible.

The longer you defer making any of these decisions, the more information you have whether you actually need them.

> A good architect maximizes the number of decisions not made

### Device independence
The author shares an example of not following this approach from the 1960s, when he wrote software which was heavily device specific.
Once they had to migrate the software to a different device, that operation was extremely difficult as all the code had to be rewritten for the new device.

In the end, the programming society has learnt its lesson by abstracting away the specific devices being used behind an operating system interface.

### Junk Mail
Here, the author shows a constrasting story to the previous one in which he worked for a company, which had to print junk mail on a particular medium.
At some point, they had to change the medium and since they were using the OS interface for interacting with the external device, they were able to migrate seamlessly.

And the value of "keeping options open" was enormous as the new medium saved the company a lot of time and money, and it was achieved with insignificant development effort.

### Physical addressing
The author goes on to share yet another story about how he worked on a project, which didn't follow the advice in this chapter and things weren't going well.
I've omitted this one for brevity.

## Independence
