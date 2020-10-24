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


