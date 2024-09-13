# Sample Graph To Start With <a name="top"></a>
![Financial Graph](./FinancialGraph.jpg)

# Content
This GSQL tutorial contains 
- [Setup Enviroment](#setup-enviroment)
- [Setup Schema (model)](#setup-schema)
- [Load Data](#load-data)
- [Query Examples](#query-examples)
  - [Node Pattern](#node-pattern)
  - [Edge Pattern](#edge-pattern)
  - [Path Pattern](#path-pattern)
- [Advanced Topics](#advanced-topics)
  - [Accumulators](#accumulators)
    - [Accumulator Operators](#accumulator-operators)
    - [Global vs Vertex Attached Accumulator](#global-vs-vertex-attached-accumulator)
    - [ACCUM vs POST-ACCUM](#accum-vs-post-accum)
  - [Vertex Set Varaibles And Accumulator As Composition Tools](#vertex-set-varaibles-and-accumulator-as-composition-tools)  
  

# Setup Enviroment 

Follow [Docker setup ](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to setup your docker enviroment.

[Go back to top](#top)

# Setup Schema 
Copy [ddl.gsql](./script/ddl.gsql) to your container. 
Next, run the following in your container's bash command line. 
```
gsql ddl.gsql
```

[Go back to top](#top)

# Load Data 
Copy [load.gsql](./script/load.gsql) to your container. 
Copy the following data files to your container.

- [account.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/account.csv)
- [city.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/city.csv)
- [phone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/phone.csv)
- [hasPhone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/hasPhone.csv)
- [locate.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/locate.csv)
- [transfer.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/transfer.csv)
  
Next, run the following in your container's bash command line. 
```
gsql load.gsql
```

[Go back to top](#top)

# Query Examples 

## Node Pattern
Copy [q1a.gsql](./script/q1a.gsql) to your container. 

```sql
#enter the graph
USE GRAPH financialGraph

//create a query
CREATE OR REPLACE QUERY q1a () SYNTAX v3 {

  // select from a node pattern-- symbolized by ()
  // v is a vertex set variable, holding the selected vertex set
  v = SELECT a
      FROM (a:Account);

  // output vertex set variable v in JSON format
  PRINT v;
}

#compile and install the query as a stored procedure
install query q1a

#run the query
run query q1a()
```

If you come from SQL world, think the matched node as a table with table(a) or table(a.attr1, a.attr2...) You can group by on the matched node table, just as you group by a table and aggregate in SQL. You can do SELECT expressions as usual, with the extension of SELECT a (which means select the graph element a)

Copy [q1b.gsql](./script/q1b.gsql) to your container. 

```sql
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q1b () SYNTAX v3 {
  //think the FROM clause as a table with column (a)
  // you can group by using a and its attribute. 
  SELECT a.isBlocked, count(*) INTO T
  FROM (a:Account)
  GROUP BY a.isBlocked;

  PRINT T;
}

#compile and install the query as a stored procedure
install query q1b

#run the query
run query q1b()
```

[Go back to top](#top)

## Edge Pattern 
Copy [q2a.gsql](./script/q2a.gsql) to your container. 

```sql

USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q2a (string accntName) SYNTAX v3 {

  // declare an local sum accumulator; you can keep adding values into it
  // "local accumulator" means each vertex will have an accumulator of
  // the declared type and can be accumulated into based on the 
  // FROM clause pattern.
  SumAccum<int> @totalTransfer = 0;

  // match an edge pattern-- symbolized by ()-[]->(), where () is node, -[]-> is an edge
  // v is a vertex set variable holding the selected vertex set
  v = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->(b:Account)
      //for each matched row, do the following accumulation
      ACCUM  b.@totalTransfer += e.amount;

  //output each v and their static attribute and runtime accumulators' state
  PRINT v;

}

#compile and install the query as a stored procedure
install query q2a

#run the query
run query q2a("Scott")
```

You can group by on the matched edge table, just as you group by a table and aggregate in SQL. 

Copy [q2b.gsql](./script/q2b.gsql) to your container. 

```sql
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q2b () SYNTAX v3 {

  //think the FROM clause is a matched table with columns (a, e, b)
  //you can use SQL syntax to group by the source and target account, and sum the total transfer amount
  SELECT a, b, sum(e.amount)  INTO T
  FROM (a:Account)-[e:transfer]->(b:Account)
  GROUP BY a, b;

  //output the table in JSON format
  PRINT T;

}

#compile and install the query as a stored procedure
install query q2b

#run the query
run query q2b()
```

[Go back to top](#top)

## Path Pattern 

### Fixed Length vs. Variable Length Path Pattern
Copy [q3a.gsql](./script/q3a.gsql) to your container. 

```sql
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE QUERY q3a (datetime low, datetime high, string accntName) SYNTAX v3 {

  // a path pattern in ascii art () -[]->()-[]->(), where alternating node() and edge -[]->
  R = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->()-[e2:transfer]->(b:Account)
      WHERE e.date >= low AND e.date <= high and e.amount >500 and e2.amount>500;

      PRINT R;

  // below we use variable length path.
  // *1.. means 1 to more steps of the edge type "transfer"
  // select the reachable end point and bind it to vertex alias "b"
  R = SELECT b
      FROM (a:Account {name: accntName})-[:transfer*1..]->(b:Account);

      PRINT R;

}

install query q3a

run query q3a("2024-01-01", "2024-12-31", "Scott")
```
You can group by on the matched path table, just as you group by a table and aggregate in SQL. 

### GroupBy on Path Table
Copy [q3b.gsql](./script/q3b.gsql) to your container. 

```sql
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE QUERY q3b (datetime low, datetime high, string accntName) SYNTAX v3 {

   // a path pattern in ascii art () -[]->()-[]->()
   // think the FROM clause is a matched table with columns (a, e, b, e2, c)
   // you can use SQL syntax to group by on the matched table
   // Below query find 2-hop reachable account c from a, and group by the path a, b, c
   // find out how much each hop's total transfer amount.
   SELECT a, b, c, sum(DISTINCT e.amount) AS hop_1_sum,  sum(DISTINCT e2.amount) AS hop_2_sum INTO T1
   FROM (a:Account)-[e:transfer]->(b)-[e2:transfer]->(c:Account)
   WHERE e.date >= low AND e.date <= high
   GROUP BY a, b, c;

   PRINT T1;

   /* below we use variable length path.
      *1.. means 1 to more steps of the edge type "transfer"
      select the reachable end point and bind it to vertex alias "b"
     note: 
      1. the path has "shortest path" semantics. If you have a path that is longer than the shortest,
      we only count the shortest. E.g., scott to scott shortest path length is 4. Any path greater than 4 will
      not be matched.
     2. we can not put an alias to bind the edge in the the variable length part -[:transfer*1..]->, but 
     we can bind the end points (a) and (b) in the variable length path, and group by on them.
   */
   SELECT a, b, count(*) AS path_cnt INTO T2
   FROM (a:Account {name: accntName})-[:transfer*1..]->(b:Account)
   GROUP BY a, b;

   PRINT T2;

}

install query q3b

run query q3b("2024-01-01", "2024-12-31", "Scott")
```

[Go back to top](#top)

# Advanced Topics
## Accumulators
GSQL is a Turing-complete graph database query language. One of its key advantages over other graph query languages is its support for accumulators, which can be either global or vertex local. An accumulator is a state variable in GSQL. Its state is mutable throughout the life cycle of a query.

### Accumulator Operators
An accumulator in GSQL supports two operators: assignment (=) and accumulation (+=).

- `=` operator: The assignment operator can be used to reset the state of an accumulator or its current value.

- `+=` operator: The accumulation operator can be used to add new values to the accumulator’s state. Depending on the type of accumulator, different accumulation semantics are applied.

```sql
USE GRAPH financialGraph

// "distributed" key word means this query can be run both on a single node or a cluster of nodes 
CREATE OR REPLACE DISTRIBUTED QUERY a1 (/* parameters */) SYNTAX v3 {

    SumAccum<INT> @@sum_accum = 0;
    MinAccum<INT> @@min_accum = 0;
    MaxAccum<INT> @@max_accum = 0;
    AvgAccum @@avg_accum;
    OrAccum @@or_accum = FALSE;
    AndAccum @@and_accum = TRUE;
    ListAccum<INT> @@list_accum;

    // @@sum_accum will be 3 when printed
    @@sum_accum +=1;
    @@sum_accum +=2;
    PRINT @@sum_accum;

    // @@min_accum will be 1 when printed
    @@min_accum +=1;
    @@min_accum +=2;
    PRINT @@min_accum;

    // @@max_accum will be 2 when printed
    @@max_accum +=1;
    @@max_accum +=2;
    PRINT @@max_accum;

    @@avg_accum +=1;
    @@avg_accum +=2;
    PRINT @@avg_accum;

    // @@or_accum will be TRUE when printed
    @@or_accum += TRUE;
    @@or_accum += FALSE;
    PRINT @@or_accum;

    // @@and_accum will be FALSE when printed
    @@and_accum += TRUE;
    @@and_accum += FALSE;
    PRINT @@and_accum;

    // @@list_accum will be [1,2,3,4] when printed
    @@list_accum += 1;
    @@list_accum += 2;
    @@list_accum += [3,4];
    PRINT @@list_accum;

}

//install the query
install query  a1

//run the query
run query a1()
```  
In the above example, six different accumulator variables (those with prefix @@) are declared, each with a unique type. Below we explain the semantics and usage of them.

- `SumAccum<INT>` allows user to keep adding INT values

- `MinAccum<INT>` keeps the smallest INT number it has seen. As the @@min_accum statements show, we accumulated 1 and 2 to the MinAccum accumulator, and end up with the value 0, as neither of 1 nor 2 is smaller than the initial state value 0.

- `MaxAccum<INT>` is the opposite of MinAccum. It returns the MAX INT value it has seen. The max_accum statements accumulate 1 and 2 into it, and end up with the value 2.

-  `AvgAccum` keeps the average value it has seen. It returns the AVG INT value it has seen. The avg_accum statements accumulate 1 and 2 into it, and end up with the value 1.5.

- `OrAccum` keeps OR-ing the internal boolean state variable with new boolean variables that accumulate to it. The initial default value is assigned FALSE. We accumulate TRUE and FALSE into it, and end up with the TRUE value.

- `AndAccum` is symmetric to OrAccum. Instead of using OR, it uses the AND accumulation semantics. We accumulate TRUE and FALSE into it, and end up with the FALSE value.

- `ListAccum<INT>` keeps appending new integer(s) into its internal list variable. We append 1, 2, and [3,4] to the accumulator, and end up with [1,2,3,4].

[Go back to top](#top)
### Global vs Vertex Attached Accumulator
At this point, we have seen that accumulators are special typed variables in GSQL. We are ready to explain their global and local scopes.

Global accumulators belong to the entire query. They can be updated anywhere within the query, whether inside or outside a query block. Local accumulators belong to each vertex. The term "local" indicates that they are local to the vertex element. These accumulators can only be updated when their owning vertex is accessible within a SELECT-FROM-WHERE-ACCUM query block. To differentiate them, we use specific prefixes in their identifiers when declaring them.

- `@@` is used for declaring global accumulator variables. It is always used stand-alone. E.g @@cnt +=1

- `@` is used for declaring local accumulator variables. It must be used with a vertex variable specified in the FROM clause in a query block. E.g. v.@cnt += 1 where v is a vertex variable specified in a FROM clause of a SELECT-FROM-WHERE query block.

```sql
USE GRAPH financialGraph

CREATE OR REPLACE DISTRIBUTED QUERY a2 (/* parameters */) SYNTAX V3 {

    SumAccum<INT> @cnt = 0; //local accumulator
    SumAccum<INT>  @@hasPhoneCnt = 0; //global accumulator

   // -[]- is an undirected edge.
   S = SELECT a
       FROM (a:Account) - [e:hasPhone] - (p:Phone)
       WHERE a.isBlocked == FALSE
       ACCUM  a.@cnt +=1,
              p.@cnt +=1,
              @@hasPhoneCnt +=1;

   PRINT S;
   PRINT @@hasPhoneCnt;

}

install query a2
run query a2()
```

In the above example:

- `@cnt` is a local accumulator. Once declared, each vertex variable x specified in a FROM clause can access it in the form x.@cnt. The local accumulator state is mutable by any query block.

- `@@hasPhoneCnt` is a global accumulator.

The ACCUM clause will execute its statements for each pattern matched in the FROM clause and evaluated as TRUE by the WHERE clause.

**Detailed Explanation:**
- The `FROM` clause identifies the edge patterns that match Account -[hasPhone]- Phone.

- The `WHERE` clause filters the edge patterns based on the Account.isBlocked attribute.

- The `ACCUM` clause will execute once for each matched pattern that passes the WHERE clause.

For each matching pattern that satisfies the WHERE clause, the following will occur:

- `a.@cnt += 1`
- `p.@cnt += 1`
- `@@hasPhoneCnt += 1`

The accumulator will accumulate based on the accumulator type.

[Go back to top](#top)

### ACCUM vs POST-ACCUM

#### ACCUM
Running example. 
```sql
USE GRAPH financialGraph

CREATE OR REPLACE DISTRIBUTED QUERY a2 () SYNTAX V3 {

    SumAccum<INT> @cnt = 0; //local accumulator
    SumAccum<INT>  @@hasPhoneCnt = 0; //global accumulator

   // -[]- is an undirected edge.
   S = SELECT a
       FROM (a:Account) - [e:hasPhone] - (p:Phone)
       WHERE a.isBlocked == FALSE
       ACCUM  a.@cnt +=1,
              p.@cnt +=1,
              @@hasPhoneCnt +=1;

   PRINT S;
   PRINT @@hasPhoneCnt;

}

install query a2
run query a2()
```
- `FROM-WHERE` Produces a Binding Table
  
We can think of the FROM and WHERE clauses specify a binding table, where the FROM clause specifies the pattern, and the WHERE clause does a post-filter of the matched pattern instances-- the result is a table, each row in the table is a pattern instance with the binding variables specified in the FROM clause as columns. In the above query a2 example, the FROM clause produces a result table (a, e, p) where “a” is the Account variable, “e” is the “hasPhone” variable, and “p” is the Phone variable.

- `ACCUM` Loops Each Row in the Binding Table

The `ACCUM` clause executes its statement(s) once for each row in the result table. The execution is done in a map-reduce fashion.

**Map-Reduce Interpretation:** The ACCUM clause uses snapshot semantics, executing in two phases:

- **Map Phase:** Each row in the binding table is processed in parallel, starting with the same accumulator snapshot as inputs. The snapshot of accumulator values is taken before the start of the ACCUM clause.

- **Reduce Phase:** These inputs are aggregated into their respective accumulators, creating a new snapshot of accumulator values.

The new snapshot is available for access after the ACCUM clause.


#### POST-ACCUM

The optional `POST-ACCUM` clause enables accumulation and other computations across the set of vertices produced by the `FROM-WHERE` binding table. `POST-ACCUM` can be used without `ACCUM`. If it is preceded by an `ACCUM` clause, then its statement can access the new snapshot value of accumulators computed by the `ACCUM` clause.

Running example. 

```sql
USE GRAPH financialGraph

CREATE OR REPLACE DISTRIBUTED QUERY a3 () SYNTAX V3 {

    SumAccum<INT> @cnt = 0; //local accumulator
    SumAccum<INT>  @@testCnt1 = 0; //global accumulator
    SumAccum<INT>  @@testCnt2 = 0; //global accumulator

   S = SELECT a
       FROM (a:Account) - [e:hasPhone] - (p:Phone)
       WHERE a.isBlocked == TRUE
       //a.@cnt snapshot value is 0
       ACCUM  a.@cnt +=1, //add 1 to a.@cnt
              @@testCnt1+= a.@cnt //access a.@cnt snapshot value 0
       POST-ACCUM (a) //loop vertex “a” set.
          @@testCnt2 += a.@cnt; //access a.@cnt new snapshot value 1


   PRINT @@testCnt1;
   PRINT @@testCnt2;
   PRINT S;

}

INSTALL QUERY a3

RUN QUERY a3()
```

- `POST-ACCUM` Loops A Vertex Set Selected From the Binding Table
  
The `POST-ACCUM` clause is designed to do some computation based on a selected vertex set from the binding table. It executes its statements(s) once for each distinct value of a referenced vertex column from the binding table. You can have multiple `POST-ACCUM` clauses. But each `POST-ACCUM` clause can only refer to one vertex variable defined in the `FROM` clause. In query a3, `POST-ACCUM (a)` means we project the vertex “a” column from the binding table, remove the duplicates, and loop through the resulting vertex set.

Another characteristic of the `POST-ACCUM` clause is that its statement(s) can access the aggregated accumulator value computed in the `ACCUM` clause.

In query a3, the `POST-ACCUM` statement will loop over the vertex set “a”, and its statement `@@testCnt2+=a.@cnt` will read the updated snapshot value of `@a.cnt`, which is 1.

```sql
USE GRAPH financialGraph

CREATE OR REPLACE DISTRIBUTED QUERY a4 () SYNTAX V3 {

     SumAccum<int> @@edgeCnt = 0;
     MaxAccum<int> @maxAmount = 0;
     MinAccum<int> @minAmount = 100000;

     MaxAccum<int> @@maxSenderAmount = 0;
     MinAccum<int> @@minReceiverAmount = 100000;
     SumAccum<int> @@bCnt = 0;
     SumAccum<int> @@aCnt = 0;

    S = SELECT b
        FROM (a:Account) - [e:transfer] - (b:Account)
        WHERE NOT a.isBlocked
        ACCUM  a.@maxAmount += e.amount, //sender max amount
               b.@minAmount += e.amount, //receiver min amount
                @@edgeCnt +=1
        POST-ACCUM (a) @@maxSenderAmount += a.@maxAmount
        POST-ACCUM (b) @@minReceiverAmount += b.@minAmount
        POST-ACCUM (a) @@aCnt +=1
        POST-ACCUM (b) @@bCnt +=1 ;

  PRINT @@maxSenderAmount,  @@minReceiverAmount;
  PRINT @@edgeCnt, @@aCnt, @@bCnt;

}

INSTALL QUERY a4


RUN QUERY a4()
```

When you reference a vertex alias in a `POST-ACCUM` statement, you bind that vertex alias to the `POST-ACCUM` clause implicitly. You can also explicitly bind a vertex alias with a `POST-ACCUM` clause by putting the vertex alias in parentheses immediately after the keyword `POST-ACCUM`. Each `POST-ACCUM` clause must be bound with one and only one vertex alias.

In query a4(), we have multiple `POST-ACCUM` clauses, each will be looping one selected vertex set.

- `POST-ACCUM (a) @@maxSenderAmount += a.@maxAmount`: In this statement, we loop through the vertex set "a", accessing the aggregate result value `a.@maxAmount` from the `ACCUM` clause. We can write the same statement by removing “(a)”: `POST-ACCUM @@maxSenderAmount += a.@maxAmount`. The compiler will infer the `POST-ACCUM` is looping “a”.

- `POST-ACCUM (b) @@minReceiverAmount += b.@minAmount`: In this statement, we loop through the vertex set “b”, accessing the aggregate result value `b.@minAmount`

- `POST-ACCUM (a) @@aCnt +=1`: In this statement, we loop through the vertex set “a”, for each distinct “a”, we increment `@@aCnt`.

- `POST-ACCUM (b) @@bCnt +=1`: in this statement, we loop through the vertex set “b”, for each distinct “b”, we increment `@@bCnt`.

Note that you can only access one vertex alias in a `POST-ACCUM`. Below example is not allowed, as it has two vertex alias (a, b) in `a.@maxAmount` and `b.@maxAmount`, respectively. 

```sql
POST-ACCUM @@maxSenderAmount += a.@maxAmount + b.@maxAmount;
```
[Go back to top](#top)

## Vertex Set Varaibles And Accumulator As Composition Tools

**Query Composition** means that one query block's computation result can be used as input to another query block. 

User can use two methods to achieve query composition. 

- (1) Using vertex set variable.

GSQL query consists of a sequence of query blocks. Each query block will produce a vertex set variable. In top-down syntax order, subsequent query block's `FROM` clause pattern can refer to prior query block's vertex set variable. Thus, achieving query block composition.  

High level, within the query body brackets, you can define a sequence of connected or unconnected query blocks to make up the query body. Below is the skeleton of a query body.

```sql
CREATE OR REPLACE DISTRIBUTED QUERY q3(/* parameters */) SYNTAX V3 {
    // Query body

    V1= Query_Block_1;


    V2= Query_Block_2;


    V3= Query_Block_3;

        .
        .
        .

    V_n = Query_Block_n;

    PRINT V_i;
}
```

A typical GSQL query follows a top-down sequence of query blocks. Each query block generates a vertex set, which can be used by subsequent query blocks to drive pattern matching. For example, 
the query a5 below achieve query composition via tgtAccnts vertex set variable, where the first SELECT query block compute this variable, and the second SELECT query block uses the variable in its `FROM` clause. 

```sql
USE GRAPH financialGraph

CREATE OR REPLACE DISTRIBUTED QUERY a5() SYNTAX V3 {

 SumAccum<int> @cnt = 0;

 //for each blocked account, find its 1-hop-neighbor who has not been blocked.
 tgtAccnts = SELECT y
             FROM (x:Account)- [e:transfer] -> (y:Account)
             WHERE x.isBlocked == TRUE AND y.isBlocked == FALSE
             ACCUM y.@cnt +=1;

 // tgtAccnts vertex set drive the below query block
 tgtPhones = SELECT z
             FROM (x:tgtAccnts)- [e:hasPhone] - (z:Phone)
             WHERE z.isBlocked
             ACCUM z.@cnt +=1;

  PRINT tgtPhones;
}

INSTALL QUERY a5

RUN QUERY a5()
```
- (2) Using accumulators.
 
Recall that vertex-attached accumulator can be accessed in a query block. Across query blocks, if the same vertex is accessed, it's vertex-attached accumulator (a.k.a local accumulator) can be treated as the runtime attribute of the vertex,
each query block will access the latest value of each vertex's local accumulator, thus achieving composition. 

Global variable maintains a global state, it can be accessed within query block, or at the same level as a query block. 
For example, in a6 query below, the first query block accumulate 1 into each `y`'s `@cnt` accumulator, and increment the global accumulator `@@cnt`. In the second query block's `WHERE` clause, we use the `@cnt` and `@@cnt` accumulator, thus achieving composition. 

```sql
"a5.gsql" 27L, 681C                                                                                             27,12         All
USE GRAPH financialGraph

CREATE OR REPLACE DISTRIBUTED QUERY a6() SYNTAX V3 {

 SumAccum<int> @cnt = 0;
 SumAccum<int> @@cnt = 0;

 //for each blocked account, find its 1-hop-neighbor who has not been blocked.
 tgtAccnts = SELECT y
             FROM (x:Account)- [e:transfer] -> (y:Account)
             WHERE x.isBlocked == TRUE AND y.isBlocked == FALSE
             ACCUM y.@cnt +=1, @@cnt +=1;

 // tgtAccnts vertex set drive the below query block
 tgtPhones = SELECT z
             FROM (x:tgtAccnts)- [e:hasPhone] - (z:Phone)
             WHERE z.isBlocked AND x.@cnt >1 AND @@cnt>0
             ACCUM z.@cnt +=1;

  PRINT tgtPhones;
}


INSTALL QUERY a6


RUN QUERY a6()
```

[Go back to top](#top)
# Support 
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://community.tigergraph.com/

Or, study our product document at

https://docs.tigergraph.com/gsql-ref/current/intro/

[Go back to top](#top)



