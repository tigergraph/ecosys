
# Introduction <a name="top"></a>

This OpenCypher tutorial provides a hands-on introduction to new users. The software program is the TigerGraph comprehensive environment for designing graph schemas, loading and managing data to build a graph, and querying the graph to perform data analysis

OpenCypher syntax emphasizes ASCII art in its syntax.

A more exhaustive description of functionality and behavior of OpenCypher is available from the [OpenCypher Language Reference](https://opencypher.org/).


# Sample Graph To Start With <a name="top"></a>
![Financial Graph](./FinancialGraph.jpg)

# Table of Contents
This GSQL tutorial contains 
- [Setup Environment](#setup-Environment)
- [Setup Schema (model)](#setup-schema)
- [Load Data](#load-data)
- [Cypher Syntax Overview](#cypher-syntax-overview)
- [Query Examples](#query-examples)
  - [Node Pattern](#node-pattern)
  - [Edge Pattern](#edge-pattern)
  - [Path Pattern](#path-pattern)
  - [Optional Match](#optional-match)
  - [With Clause](#with-clause)
  - [Sorting and Limiting Results](#sorting-and-limiting-results)
  - [Working With List](#working-with-list)
  - [Combining MATCH Pattern Results](#combining-match-pattern-results)
  - [Conditional Logic](#conditional-logic)
  - [Aggregate Functions](#aggregate-functions)
  - [Other Expression Functions](#other-expression-functions)
 - [Support](#support)
 - [Contact](#contact)
  

# Setup Environment 

Follow [Docker setup ](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up your docker Environment.

[Go back to top](#top)

---
# Setup Schema 
We use an artificial financial schema and dataset as a running example to demonstrate the usability of graph searches. The figure above provides a visualization of all the graph data in the database.

Copy [00_schema.gsql](./gsql/00_schema.gsql) to your container. 
Next, run the following in your container's bash command line. 
```
gsql schema.gsql
```
As seen below, the declarative DDL create vertex and edge types. Vertex type requires a `PRIMARY KEY`. Edge types requires a `FROM` and `TO` vertex types as the key. We allow edges of the same type share endpoints. In such case, a `DISCRIMINATOR` attribute is needed to differentiate edges sharing the same endpoints. `REVERSE_EDGE` specifies a twin edge type excep the direction is reversed. 

```python
//install gds functions
import package gds
install function gds.**

//create vertex types
CREATE VERTEX Account ( name STRING PRIMARY KEY, isBlocked BOOL)
CREATE VERTEX City ( name STRING PRIMARY KEY)
CREATE VERTEX Phone (number STRING PRIMARY KEY, isBlocked BOOL)

//create edge types
CREATE DIRECTED EDGE transfer (FROM Account, TO Account, DISCRIMINATOR(date DATETIME), amount UINT) WITH REVERSE_EDGE="transfer_reverse"
CREATE UNDIRECTED EDGE hasPhone (FROM Account, TO Phone)
CREATE DIRECTED EDGE isLocatedIn (FROM Account, TO City)

//create graph; * means include all graph element types in the graph.
CREATE GRAPH financialGraph (*)
```

[Go back to top](#top)

---

# Load Data 

You can choose one of the following methods. 

- Load sample data from our publicly accessible s3 bucket 
  
  Copy [01_load.gsql](./gsql/01_load.gsql) to your container. 
  Next, run the following in your container's bash command line. 
  ```
     gsql load.gsql
  ```
  or in GSQL Shell editor, copy the content of [01_load.gsql](./gsql/01_load.gsql), and paste it into the GSQL shell editor to run.
  
- Load from local file in your container
  - Copy the following data files to your container.
    - [account.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/account.csv)
    - [phone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/phone.csv)
    - [city.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/city.csv)
    - [hasPhone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/hasPhone.csv)
    - [locate.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/locate.csv)
    - [transfer.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/transfer.csv)

  - Copy [25_load.gsql](./gsql/25_load.gsql) to your container. Modify the script with your local file path. Next, run the following in your container's bash command line. 
    ```
       gsql load2.gsql
    ``` 
    or in GSQL Shell editor, copy the content of [25_load.gsql](./script/25_load.gsql), and paste in GSQL shell editor to run.

    The declarative loading script is self-explanatory. You define the filename alias for each data source, and use the the LOAD statement to map the data source to the target schema elements-- vertex types, edge types, and vector attributes.
    ```python
    USE GRAPH financialGraph

    DROP JOB load_local_file

    //load from local file
    CREATE LOADING JOB load_local_file  {
      // define the location of the source files; each file path is assigned a filename variable.  
      DEFINE FILENAME account="/home/tigergraph/data/account.csv";
      DEFINE FILENAME phone="/home/tigergraph/data/phone.csv";
      DEFINE FILENAME city="/home/tigergraph/data/city.csv";
      DEFINE FILENAME hasPhone="/home/tigergraph/data/hasPhone.csv";
      DEFINE FILENAME locatedIn="/home/tigergraph/data/locate.csv";
      DEFINE FILENAME transferdata="/home/tigergraph/data/transfer.csv";
      //define the mapping from the source file to the target graph element type. The mapping is specified by VALUES clause. 
      LOAD account TO VERTEX Account VALUES ($"name", gsql_to_bool(gsql_trim($"isBlocked"))) USING header="true", separator=",";
      LOAD phone TO VERTEX Phone VALUES ($"number", gsql_to_bool(gsql_trim($"isBlocked"))) USING header="true", separator=",";
      LOAD city TO VERTEX City VALUES ($"name") USING header="true", separator=",";
      LOAD hasPhone TO Edge hasPhone VALUES ($"accnt", gsql_trim($"phone")) USING header="true", separator=",";
      LOAD locatedIn TO Edge isLocatedIn VALUES ($"accnt", gsql_trim($"city")) USING header="true", separator=",";
      LOAD transferdata TO Edge transfer VALUES ($"src", $"tgt", $"date", $"amount") USING header="true", separator=",";
    }

    run loading job load_local_file
    ```
    
[Go back to top](#top)

---

# Cypher Syntax Overview

OpenCypher is a declarative query language designed for interacting with graph databases. It enables the retrieval and manipulation of nodes, relationships, and their properties.

The core syntax of openCypher follows the MATCH-WHERE-RETURN pattern.

- `MATCH` is used to specify graph patterns in an intuitive ASCII-art style, such as `()-[]->()-[]->()`. Here, `()` represents nodes, and `-[]->` represents relationships. By alternating nodes and relationships, users can define linear paths or complex patterns within the graph schema.
- The results of a `MATCH` operation are stored in an implicit working table, where the columns correspond to the aliases of graph elements (nodes or relationships) in the declared pattern. These columns can then be referenced in subsequent clauses, including MATCH, OPTIONAL MATCH, WITH, or RETURN. Each subsequent clause can transform the invisible working table by projecting aways columns, adding new columns, and rows. 

In the next section, we will explore Cypher syntax in detail through practical examples.

# Query Examples 

In OpenCypher, the main statement is a pattern match statement in the form of MATCH-WHERE-RETURN. Each MATCH statement will create or update an invisible working table. The working table consists all the alias (vertex/edge) and columns specified in the current and previous MATCH statements. Other statement will also work on the working table to drive the final result.

We will use examples to illustrate cypher syntax. 

---

## Node Pattern
### MATCH A Vertex Set 
Copy [c1.cypher](./cypher/c1.cypher) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c1() {
  // MATCH a node pattern-- symbolized by (),
  //":Account" is the label of the vertex type Account, "a" is a binding variable to the matched node. 
  // return will print out all the bound Account vertices in JSON format.
  MATCH (a:Account)
  RETURN a
}

# To run the query, we need to install it first.
# Compile and install the query as a stored procedure
install query c1

# run the compiled query
run query c1()
```
The result is shown in [c1.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c1.out) under `/home/tigergraph/tutorial/4.x/cypher/c1.out`

[Go back to top](#top)

### MATCH A Vertex Set With Filter
Copy [c2.cypher](./cypher/c2.cypher) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c2() {
  // MATCH a node pattern-- symbolized by (),
  //":Account" is the label of the vertex type Account, "a" is a binding variable to the matched node. 
  // WHERE clause specify a boolean condition to filter the matched Accounts. 
  // return will print out all the bound Account vertices in JSOn format.
  MATCH (a:Account)
  WHERE a.name = "Scott"
  RETURN a
}

# To run the query, we need to install it first.
# Compile and install the query as a stored procedure
install query c2

# run the compiled query
run query c2()
```
The result is shown in [c2.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c2.out) under `/home/tigergraph/tutorial/4.x/cypher/c2.out`

[Go back to top](#top)

---

## Edge Pattern 
### MATCH 1-hop Edge Pattern
Copy [c3.cypher](./cypher/c3.cypher) to your container. 

```python
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE OPENCYPHER QUERY c3(string accntName) {

    // match an edge pattern-- symbolized by ()-[]->(), where () is node, -[]-> is a directed edge
    // In cypher, we use $param to denote the binding literal
    // {name: $acctName} is a JSON style filter. It's equivalent to "a.name = $acctName".
    // ":transfer" is the label of the edge type "transfer". "e" is the alias of the matched edge.
    MATCH (a:Account {name: $accntName})-[e:transfer]->(b:Account)
    RETURN b, sum(e.amount) AS totalTransfer

}

# compile and install the query as a stored procedure
install query c3

# run the compiled query
run query c3("Scott")
```
The result is shown in [c3.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c3.out) under `/home/tigergraph/tutorial/4.x/cypher/c3.out`

Copy [c4.cypher](./cypher/c4.cypher) to your container. 

```python
USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c4() {

  //think the MATCH clause is a matched table with columns (a, e, b)
  //you can use SQL syntax to group by the source and target account, and sum the total transfer amount
  MATCH (a:Account)-[e:transfer]->(b:Account)
  RETURN a, b, sum(e.amount) AS transfer_total

}

#compile and install the query as a stored procedure
install query c4

#run the query
run query c4()
```
The result is shown in [c4.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c4.out) under `/home/tigergraph/tutorial/4.x/cypher/c4.out`    

[Go back to top](#top)

---

## Path Pattern 

### Fixed Length Path Pattern
Copy [c5.cypher](./cypher/c5.cypher) to your container. 

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c5(datetime low, datetime high, string accntName) {

  // a path pattern in ascii art () -[]->()-[]->()
  MATCH (a:Account {name: $accntName})-[e:transfer]->()-[e2:transfer]->(b:Account)
  WHERE e.date >= $low AND e.date <= $high and e.amount >500 and e2.amount>500
  RETURN b.isBlocked, b.name    
 
}

#compile and install the query as a stored procedure
install query c5

#run the query
run query c5("2024-01-01", "2024-12-31", "Scott")
```
[Go back to top](#top)

### Variable Length Path Pattern
Copy [c6.cypher](./cypher/c6.cypher) to your container. 

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c6 (string accntName) {

  // a path pattern in ascii art () -[]->()-[]->()
  MATCH (a:Account {name: $accntName})-[:transfer*1..]->(b:Account)
  RETURN a, b     

}

#compile and install the query as a stored procedure
install query c6

#run the query
run query c6("Scott")
```

The result is shown in [c6.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c6.out) under `/home/tigergraph/tutorial/4.x/cypher/c6.out`   

Copy [c7.cypher](./cypher/c7.cypher) to your container. 

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c7(datetime low, datetime high, string accntName) {

   // below we use variable length path.
   // *1.. means 1 to more steps of the edge type "transfer"
   // select the reachable end point and bind it to vertex alias "b"
   // note:
   // 1. the path has "shortest path" semantics. If you have a path that is longer than the shortest,
   // we only count the shortest. E.g., scott to scott shortest path length is 4. Any path greater than 4 will
   // not be matched.
   // 2. we can not put an alias to bind the edge in the the variable length part -[:transfer*1..]->, but
   // we can bind the end points (a) and (b) in the variable length path, and group by on them.
   MATCH (a:Account {name: $accntName})-[:transfer*1..]->(b:Account)
   RETURN a, b, count(*) AS path_cnt 
}

install query c7

run query c7("2024-01-01", "2024-12-31", "Scott")
```

The result is shown in [c7.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c7.out) under `/home/tigergraph/tutorial/4.x/cypher/c7.out`   

[Go back to top](#top)

### Sum Distinct On 1-hop Within A Path
Path pattern has multiple hops. To sum each hop's edge attributes, we need `DISTINCT` keyword. 
Copy [c8.cypher](./cypher/c8.cypher) to your container. 

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c8 (datetime low, datetime high) {

   // a path pattern in ascii art () -[]->()-[]->()
   // think the FROM clause is a matched table with columns (a, e, b, e2, c)
   // you can use SQL syntax to group by on the matched table
   // Below query find 2-hop reachable account c from a, and group by the path a, b, c
   // find out how much each hop's total transfer amount.
   MATCH (a:Account)-[e:transfer]->(b)-[e2:transfer]->(c:Account)
   WHERE e.date >= $low AND e.date <= $high
   RETURN a, b, c, sum(DISTINCT e.amount) AS hop_1_sum,  sum(DISTINCT e2.amount) AS hop_2_sum   
   
}

#compile and install the query as a stored procedure
install query c8

#run the query
run query c8("2024-01-01", "2024-12-31")
```

The result is shown in [c8.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c8.out) under `/home/tigergraph/tutorial/4.x/cypher/c8.out`   

[Go back to top](#top)

---

## Optional Match
`OPTIONAL MATCH` matches patterns against your graph, just like MATCH does. The difference is that if no matches are found, OPTIONAL MATCH will use a null for missing parts of the pattern.

In query c21, we first match `Account` whose name is $accntName. Next, we find if the matched `Account` satisfies the `OPTIONAL MATCH` clause. If not, we pad `null` on the `MATCH` clause produced match table row. If yes, we pad the `OPTIONAL MATCH` table to the `MATCH` clause matched row. 

Copy [c21.cypher](./cypher/c21.cypher) to your container. 

```python
use graph financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c21(String accntName){
  MATCH (srcAccount:Account {name: $accntName})
  OPTIONAL MATCH (srcAccount)- [e:transfer]-> (tgtAccount:Account)
  WHERE srcAccount.isBlocked
  RETURN srcAccount, tgtAccount
}

install query c21
run query c21("Jenny")
```

The result is shown in [c21.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c21.out) under `/home/tigergraph/tutorial/4.x/cypher/c21.out`   

---

## With Clause

The WITH clause in Cypher is used to chain parts of a query, pass intermediate results to the next part, or perform transformations like aggregation. It acts as a way to manage query scope and handle intermediate data without exposing everything to the final result.

### Key Uses:
- **Filter intermediate results**: Apply conditions on data before proceeding.
- **Aggregation**: Perform calculations and pass the results further.
- **Variable scope management**: Avoid cluttering query scope by controlling what gets passed forward.

### Filter intermediate result

In the example below, the `WITH a` passes the filtered `Account` (names starting with "J") to the next part of the query.
The `RETURN a.name` outputs the names.

Copy [c9.cypher](./cypher/c9.cypher) to your container. 

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c9() {

  MATCH (a:Account)
  WHERE a.name STARTS WITH "J"
  WITH a
  RETURN a.name
}

install query c9

run query c9()
```

The result is shown in [c9.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c9.out) under `/home/tigergraph/tutorial/4.x/cypher/c9.out`   

[Go back to top](#top)

### Aggregation
In c10 query below, the `WITH a.isBlocked AS Blocked, COUNT(a) AS blocked_count` groups data by `isBlocked` and calculates the count of Account.
`RETURN Blocked, blocked_count` outputs the aggregated results.

Copy [c10.cypher](./cypher/c10.cypher) to your container. 

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c10() {

  MATCH (a:Account)
  WITH a.isBlocked AS Blocked, COUNT(a) AS blocked_count
  RETURN Blocked, blocked_count

}

install query c10

run query c10()
```

The result is shown in [c10.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c10.out) under `/home/tigergraph/tutorial/4.x/cypher/c10.out`  

[Go back to top](#top)

### Variable scope management
In query c11 below, the ` WITH a.name AS name` narrows the scope to only the name property.
`WHERE name STARTS WITH "J"` filters names starting with 'J'. `RETURN name` outputs the filtered names.

Copy [c11.cypher](./cypher/c11.cypher) to your container. 

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE OPENCYPHER QUERY c11() {

   MATCH (a:Account)
   WITH a.name AS name
   WHERE name STARTS WITH "J"
   RETURN name
}

install query c11

run query c11()
```

The result is shown in [c11.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c11.out) under `/home/tigergraph/tutorial/4.x/cypher/c11.out`  

[Go back to top](#top)

---

## Sorting and Limiting Results
`ORDER BY` is a sub-clause following `RETURN` or `WITH`, and it specifies that the output should be sorted and how. `SKIP` defines from which record to start including the records in the output. `LIMIT` constrains the number of records in the output.

In query c12 below, the sorting (`ORDER BY`), skipping (`SKIP`), and limiting (`LIMIT`) operations occur after the WITH clause, which means they are applied to the intermediate results.

The query first aggregates data by counting `tgt2` for each `srcAccountName` using `WITH`. Next, `ORDER BY` sorts the results by `tgt2Cnt` (descending) and `srcAccountName` (descending). `SKIP 1` skips the first record from the sorted intermediate result set. `LIMIT 3` restricts the output to the next 3 records.

Copy [c12.cypher](./cypher/c12.cypher) to your container. 

```python
USE GRAPH financialGraph  

CREATE OR REPLACE OPENCYPHER QUERY c12(){  
  MATCH (src)-[e:transfer]-> (tgt1)  
  MATCH (tgt1)-[e:transfer]-> (tgt2)  
  WITH src.name AS srcAccountName, COUNT(tgt2) AS tgt2Cnt  
  ORDER BY tgt2Cnt DESC, srcAccountName DESC  
  SKIP 1  
  LIMIT 3  
  RETURN srcAccountName, tgt2Cnt  
} 

install query c12

run query c12()
```

The result is shown in [c12.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c12.out) under `/home/tigergraph/tutorial/4.x/cypher/c12.out`  

[Go back to top](#top)

In query c13 below, the sorting (`ORDER BY`), skipping (`SKIP`), and limiting (`LIMIT`) operations are applied after the `RETURN` clause, meaning they act on the final result set. The final result is the same as c12, but the order of operations is different.

Copy [c13.cypher](./cypher/c13.cypher) to your container. 

```python
USE GRAPH financialGraph  
CREATE OR REPLACE OPENCYPHER QUERY c13(){  
  MATCH (src)-[e:transfer]-> (tgt1)  
  MATCH (tgt1)-[e:transfer]-> (tgt2)  
  WITH src.name AS srcAccountName, COUNT(tgt2) AS tgt2Cnt   
  RETURN srcAccountName, tgt2Cnt  
  ORDER BY tgt2Cnt DESC, srcAccountName DESC  
  SKIP 1  
  LIMIT 3 
} 

install query c13

run query c13()
```

The result is shown in [c13.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c13.out) under `/home/tigergraph/tutorial/4.x/cypher/c13.out`  

[Go back to top](#top)

---

## Working With List

### UNWIND Clause
In Cypher, the `UNWIND` clause is used to transform a list into individual rows. It's helpful when you have a list of values and want to treat each value as a separate row in your query.

In query c14 below, the `UNWIND` is used to expand existing rows with each element of the list `[1, 2, 3]`. 
In each expanded row, the variable `x` will hold each element of the list, allowing you to perform further operations on it in the subsequent parts of the query.

Copy [c14.cypher](./cypher/c14.cypher) to your container. 

```python
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c14(){
   MATCH (src)-[e:transfer]-> (tgt1)
   WHERE src.name in ["Jenny", "Paul"]
   UNWIND [1, 2, 3] AS x //the "Jenny" row will be expanded to [Jenny, 1], [Jenny,2], [Jenny, 3]. Same fashion applies to the "Paul" row.
   WITH src AS srcAccount, e.amount * x AS res
   RETURN srcAccount, res
}

install query c14

run query c14()
```

The result is shown in [c14.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c14.out) under `/home/tigergraph/tutorial/4.x/cypher/c14.out`  

[Go back to top](#top)

---

### COLLECT Function
In Cypher, the `collect()` function is used to aggregate values into a list. It is often used in conjunction with `RETURN` or `WITH` to group and organize data into collections.

In query c15() below, `MATCH (src)-[e:transfer]->(tgt)` finds all 1-hop transfers started with "Jenny" or "Paul". `COLLECT(e.amount)` gathers all the `e.amount` into a single list, grouped by `srcAccount`. `RETURN` outputs the `amounts` list per `srcAccount`.

Copy [c15.cypher](./cypher/c15.cypher) to your container.

```python
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c15(){
   MATCH (src)-[e:transfer]->(tgt)
   WHERE src.name in ["Jenny", "Paul"]
   WITH src AS srcAccount, COLLECT(e.amount) AS amounts
   RETURN srcAccount, amounts
}

install query c15

run query c15()
```
The result is shown in [c15.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c15.out) under `/home/tigergraph/tutorial/4.x/cypher/c15.out`

[Go back to top](#top)

---

### Using WITH and COLLECT()
You can use `UNWIND` to decompose a list column produced by `COLLECT()`.

In query c16() below, `MATCH (src)-[e:transfer]->(tgt)` finds all 1-hop transfers started with "Jenny" or "Paul". `COLLECT(e.amount)` gathers all the `e.amount` into a single list, grouped by `srcAccount`. `UNWIND` expand the list, and append each element on the list's belonging row.  `WITH` will double the amount column. `RETURN` output the `UNWIND` result.

Copy [c16.cypher](./cypher/c16.cypher) to your container.

```python
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c16(){
   MATCH (src)-[e:transfer]->(tgt)
   WHERE src.name in ["Jenny", "Paul"]
   WITH src AS srcAccount, COLLECT(e.amount) AS amounts //collect will create ammounts list for each srcAccount
   UNWIND amounts as amount //for each source account row, inflate the row to a list of rows with each element in the amounts list
   WITH srcAccount, amount*2 AS doubleAmount
   RETURN srcAccount, doubleAmount
}

install query c16
run query c16()
```

The result is shown in [c16.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c16.out) under `/home/tigergraph/tutorial/4.x/cypher/c16.out`

[Go back to top](#top)

---

## Combining MATCH Pattern Results

Each `MATCH` clause will create a match table. You can use `UNION` and `UNION ALL` to combine schema compatiable match tables.

### UNION
In query c17() below, ` MATCH (s:Account {name: "Paul"})` finds "Paul". `MATCH (s:Account) WHERE s.isBlocked` finds all blocked accounts. `UNION` will combine these two with duplicates removed.

Copy [c17.cypher](./cypher/c17.cypher) to your container.

```python
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c17(){
  MATCH (s:Account {name: "Paul"})
  RETURN s AS srcAccount
  UNION
  MATCH (s:Account)
  WHERE s.isBlocked
  RETURN s AS srcAccount
}

install query c17
```

The result is shown in [c17.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c17.out) under `/home/tigergraph/tutorial/4.x/cypher/c17.out`

[Go back to top](#top)

---

### UNION ALL
In query c18() below, ` MATCH (s:Account {name: "Steven"})` finds "Steven". `MATCH (s:Account) WHERE s.isBlocked` finds all blocked accounts--"Steven". `UNION ALL` will combine these two, keeping the duplicates.

Copy [c18.cypher](./cypher/c18.cypher) to your container.

```python
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c18(){
  MATCH (s:Account {name: "Steven"})
  RETURN s AS srcAccount
  UNION ALL
  MATCH (s:Account)
  WHERE s.isBlocked
  RETURN s AS srcAccount
}

install query c18
run query c18()
```

The result is shown in [c18.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c18.out) under `/home/tigergraph/tutorial/4.x/cypher/c18.out`

[Go back to top](#top)

---

## Conditional Logic
### CASE Expression
The CASE expression in OpenCypher allows you to implement conditional logic within a query, enabling dynamic result customization based on specific conditions.

***Syntax***
```python
CASE
  WHEN <condition1> THEN <result1>
  WHEN <condition2> THEN <result2>
  ...
  ELSE <default_result>
END
```

In query c19() below, `CASE WHEN` will produce 0 for blcoked account, and 1 for non-blocked account.

Copy [c19.cypher](./cypher/c19.cypher) to your container.

```python
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c19(){
  MATCH (s:Account {name: "Steven"})- [:transfer]-> (t)
  WITH
    s.name AS srcAccount,
    t.name AS tgtAccount,
    CASE
       WHEN s.isBlocked = true THEN 0
       ELSE 1
    END AS tgt
  RETURN srcAccount, SUM(tgt) as tgtCnt
}

install query c19
run query c19()
```

The result is shown in [c19.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c19.out) under `/home/tigergraph/tutorial/4.x/cypher/c19.out`

[Go back to top](#top)

---

## Aggregate Functions
Aggregation functions in OpenCypher allow you to perform calculations over a set of values, summarizing or transforming the data into a single result. These functions are typically used in combination with the `WITH` or `RETURN` clauses to compute aggregate values based on certain criteria. In `WITH` and `RETURN`, other non-aggregate expressions are used form groups of the matched rows. 

### Common Aggregation Functions:
- ***COUNT()***: Counts the number of items in a given set. e.g. COUNT(*), COUNT(1), COUNT(DISTINCT columnName).
- ***SUM()***: Computes the sum of numeric values. It is often used to calculate totals, such as the total amount transferred.
- ***AVG()***: Calculates the average of numeric values.
- ***MIN()***: Finds the smallest value in a set. Often used to determine the minimum amount or value.
- ***MAX()***: Finds the largest value in a set. This is useful for identifying the highest value.
- ***COLLECT()***: Aggregates values into a list. Can be used to collect nodes or relationships into a list for further processing.
- ***STDEV()***: Computes the standard deviation of values.
- ***STDEVP()***: Computes the population standard deviation of values.

In query c20 below, we group by `src.name` and aggregate on other matched attributes in the matched table. 

Copy [c20.cypher](./cypher/c20.cypher) to your container.

```python
USE GRAPH financialGraph
CREATE OR REPLACE OPENCYPHER QUERY c20(){
  MATCH (src)-[e:transfer]->(tgt)
  WITH src.name AS srcAccount, 
       COUNT(DISTINCT tgt) AS transferCount, 
       SUM(e.amount) AS totalAmount,
       STDEV(e.amount) AS stdevAmmount
  RETURN srcAccount, transferCount, totalAmount, stdevAmmount
}

INSTALL query c20

run query c20()
```

The result is shown in [c20.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c20.out) under `/home/tigergraph/tutorial/4.x/cypher/c20.out`

[Go back to top](#top)

---

## Other Expression Functions
There are many expression functions openCypher supports. Please refer to [openCypher functions](https://docs.tigergraph.com/gsql-ref/4.1/opencypher-in-gsql/opencypher-in-gsql) 

[Go back to top](#top)

---

# Support 
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://community.tigergraph.com/

Or, study our product document at

https://docs.tigergraph.com/gsql-ref/current/intro/

[Go back to top](#top)

---

# Contact
To contact us for commercial support and purchase, please email us at [info@tigergraph.com](mailto:info@tigergraph.com)

[Go back to top](#top)


