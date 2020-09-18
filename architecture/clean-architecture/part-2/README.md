# Part 2 - Starting with the bricks: Programming Paradigms

Since programming was first conceived, there were numerous programming languages created. But there only three programming paradigms created.
It is unlikely that there will be more paradigms invented in the future.

Programming language == set of programming structures available in your toolbox.
Programming Paradigm == discipline on what programming structures to use and when to use them.

## Paradigm Overview
There are three programming paradigms

### Structured programming
Not the first one to be invented but the first one to be adopted.

Created by Edsger Dijkstra via his discipline on avoiding `goto` statements.

He replaced those jumps with the more familiar `if/then/else` and `do/while/until`.

> Structured programming imposes discipline on direct transfer of control

### Object-Oriented Programming
Invented two years earlier than structured programming.

It is based on the discovery that local function variables could live on the heap, rather than the stack & result in variables existing long after a function has returned.

These variables became known as instance variables, the function became known as a constructor and the nested functions became methods.
Via this route, it was later discovered that polymorphism was possible via the disciplined use of function pointers.

> Object-oriented programming imposes discipline on indirect transfer of control

### Functional Programming
The latest one to be adopted, but the first one to be discovered is functional programming.

It is based on lambda calculus & the notion of immutability - that variables do not change.
This effectively means that functional programming languages lack an assignment statement.

> Functional programming imposes discipline upon assignment

### Food for thought
Notice that all these paradigms don't add anything to the programmer's capabilities. They only **remove** capabilities from them.

Paradigms tell us what not to do, rather than telling us what to do.

All these paradigms were discovered in the 1950s' and 1960s'. There were no major paradigms added afterwards.

## Structured programming
The paradigm was discovered by Edsger Dijkstra by analysing existing programs in an attempt to establish mathematical proofs for programs.

What he discovered was that if a program uses simple constructs - sequence (e.g. `cmd1; cmd2;`), selection (e.g. `if/then/else`), iteration (e.g. `do/while`), such programs were provable.
However, if a program used `goto`s to do undisciplined jumps across function boundaries, these were very hard to prove.

Along the same time, some computer scientists proved that **all programs can be constructed from these simple structures**.

Hence, it was concluded that sticking to these three simple structures allowed one to construct any program he'd like, while also keeping his program mathematically provable.
This is how structured programming was born.

It all began with the publication of the renowned letter named "Go To statement considered harmful". 
Although some complained at the time & didn't support this idea, it eventually went through and modern programming languages don't have support for uncontrolled uses of `goto` at all.

### Functional Decomposition, formal proof & science
In the 1970s and 1980s, some disciplines such as structured analysis and structured design were invented, which allowed programmers to decompose large systems into small provable functions by decomposing the entire stack into modules -> functions -> statements, which were recursively provable.

However, the idea of mathematically proving a program is correct didn't get popularized & Dijkstra's idea slowly faded & died.
But there was another highly successful alternative - the scientific method.

Mathematics focuses on proving that a statement is true. Science focuses on proving that a statement is false.

For example, no one can prove that Newton's second law of motion `F = ma` is true, but many people can show that it is a very reliable approximation by demonstrating it is correct up to a lot of decimal places.

If a statement cannot be proved false after much effort it is deemed to be true enough for our purposes.
This is the discipline used in programming nowadays

### Tests
Tests are the practical implementation of the scientific method in programming.

We don't focus on proving that a program is correct (free of bugs). We focus on proving that a program is incorrect.
And if we fail to do so, than we deem the program sufficiently correct for our purposes.

However, these techniques can only be applied to provable programs - those which are free of `goto`s.
Only then can we apply functional decomposition to separate our program into small provable functions, for which we use tests to prove they are sufficiently correct.

Software architects also apply such restrictive disciplines, albeit at a much higher level, in order to structure programs into distinct modules, which can be separately tested & proved correct enough.
