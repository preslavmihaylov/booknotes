# Part 6 - Details

## The database is a detail
From an architectural point of view, the database is a detail.
The data model is significant to the architecture, but the specific database being used to store it is a detail.

### Relational databases
In the 1970s, the relational model for storing data became popular for a good reason - it is elegant, disciplined and robust.

But no matter how brilliant it is, it is still a technology - a detail.

Relational tables might offer some convenience when retrieving/storing your data, but there isn't anything architecturally significant about arranging data into rows and tables.
The use cases of the system should not know anything about how the data is being stored.

#### Why are database systems so prevalent?
Because the main way to store data nowadays if via magnetic disks and storing data on magnetic disks is way slower than storing the data in memory.

Due to this limitation, some kind of data management system is needed in order to have some form of indexing, caching, optimized query schemes.
There are mainly two kinds of data management systems - file systems and RDBMS.

File systems are good at finding files based on their name/ID, but not good at searching for files based on contents.
RDBMS are good in searching through file contents, but not good at fetching the whole file.

Each of these has its use cases.

### What if there were no disk?
Disks used to be quite prevalent at the time, but they seem to be replaced by RAM nowadays - e.g. SSDs.

When the disks are gone, the databases we use today might become irrelevant.

### Details
In our software, we represent our data into data structures - lists, stacks, queues, etc.
In databases, we represent them as files/directories/tables/rows.

From an architectural point of view, it doesn't matter how the data structures are represented while residing in a database.

#### But what about performance?
Performance is an architectural concern, but in terms of fast read/write access, that can be addressed via the low-level data access mechanisms.
It shouldn't be an overall architectural concern.

### Anecdote
The author shares a story about working in a company which used random-access files for data storage as it made perfect sense for their use cases.

However, some marketing guy came and said they needed a RDBMS. The author fought that decision and eventually, the company did add an RDBMS.

The reason for that, was because the customers wanted a RDBMS even if they didn't need it.

What the author should have done, in that case, is to integrate an RDBMS via a low-level component so that they can easily replace it in the future once its realised it is not needed.

### Conclusion
The data model is architecturally significant. How that data model is being persisted is not.
The database is a detail.

## The Web is a detail
The author shares some stories of how the Web was thought to be a big think in the 1990s which changed everything.

It shouldn't have been a big deal. Our industry has seen multiple oscillations of such technologies which fundamentally moved the main horsepower between the server and the client.
Previously, it was thought the whole processing should be concentrated at the server. Nowadays, the clients tend to be more stuffed than the servers, etc etc.

### The endless pendulum
The oscillations between centralizing computer power and distributing will continue. It is a never ending cycle.

Those oscillations are short-term issues which should be encapsulated in our low-level components.
That way, we will be able to face them fearlessly as our projects mature.

At this point, the author shares a story about some personal finance software vendor, which originally had its application in a GUI.
But then, some marketing guy came and wanted it to be based in the Web. No one liked that among the customers, so they reverted back to having a GUI.

At that point, if the software architect had detached the UI from the business rules, they would have had (or did have) an easier time migrating between UIs based on the marketing department's fluctuations.

### The Upshot
The conclusion from all this is that the GUI is a detail, as well is the Web.

They are an IO device and we've learned long ago that we should write our software to be IO independent.
Hence, interactions with the UI should be encapsulated in low-level components.

However, the author also mentions that there are some specifics between the different UI mediums. The way a web applications interacts with a GUI is different from the way it interacts with the Web.

So, to some extent, the application is coupled to the specific UI being used.

However, the business rules and use cases can still be isolated from that. They can be assumed as some routines to which you pass some data and they return data back.

### Conclusion
This abstraction is not easy and it will take some time, but it is worth it and it is possible.

## Frameworks are details
There are many useful frameworks which have become quite popular as they are very useful.

However, frameworks are not architectures.

### Framework authors
Framework authors usually do their work for free which is admirable.

But they have their own best interests in mind, not yours as they don't know you.

Typically, frameworks are being made to serve the purposes of their authors' problems, not their users.
However, there is oftentimes an overlap between what the authors need and what their users need, which makes the frameworks popular.

### Asymmetric marriage
The relationship between users and framework authors is very asymmetrical.

Users marry the framework, but the authors don't marry the users.

This way, you take on all the risks and all the burdens. The authors take none.

### The risks
 * The architecture of the framework is often not very clean. Once your domain objects inherit from the framework objects, your whole application marries the framework.
 * The framework might help you with some early features of your application, but as it grows, it will probably outgrow the facilities of the framework
 * The framework might evolve in directions you don't find useful and helpful. You may even find old useful features disappearing.
 * A new and better framework might come along which you might want to switch to

### The solution
> Don't marry the framework!

Treat the framework as a detail that belongs to the outer circles of your architecture. Don't let it slip in the inner ones.

If the framework wants you to derive its base objects, don't do so. Derive proxies instead, which you keep in components that are plugins to your business rules.

For example, Spring is a good dependency injection framework. Don't use `@autowired` through your whole project.
Instead, isolate those usages in your main component.

### I now pronounce you...
There are some frameworks you simple must marry - the C++ STL framework, the standard libraries, etc.

That is normal, but it should be a decision. If you decide to marry a framework, you should know that you will be stuck with it forever.
Hence, only marry frameworks that you are certain will be stable dependencies.

### Conclusion
When faced with a framework, don't marry it right away. Consider putting the framework behind an architectural boundary.

Try to find a way to get the milk, without buying the cow.

