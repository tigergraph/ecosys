Sample Graph To Start With 
==============================
![Financial Graph](./FinancialGraph.jpg)

Content
==============
This GSQL tutorial contains 

- Setup schema (model)
- Load data
- Query Examples


Setup Schema
===============
Copy the following DDL statment into a file. E.g., "ddl.gsql". 
```
CREATE VERTEX Account ( name STRING PRIMARY KEY, isBlocked BOOL)
CREATE VERTEX City ( name STRING PRIMARY KEY)
CREATE VERTEX Phone (nmuber STRING PRIMARY KEY, isBlocked BOOL)

CREATE DIRECTED EDGE transfer (FROM Account, TO Account, DISCRIMINATOR(date DATETIME), amount UINT) WITH REVERSE_EDGE="transfer_reverse"
CREATE UNDIRECTED EDGE hasPhone (FROM Account, TO Phone)
CREATE DIRECTED EDGE isLocatedIn (FROM Account, TO City)

CREATE GRAPH financialGraph (*)
```
In docker container, in bash command line, run the ddl script by typing
```
gsql ddl.gsql
```

Documents
==============
https://docs.tigergraph.com/start/gsql-101


Support
===============
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://community.tigergraph.com/


- 
