
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
- [Query Examples](#query-examples)
  - [Node Pattern](#node-pattern)
  - [Edge Pattern](#edge-pattern)
  - [Path Pattern](#path-pattern)
  - [With Clause](#with-clause)
 - [Support](#support) 
  

# Setup Environment 

Follow [Docker setup ](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up your docker Environment.

[Go back to top](#top)

# Setup Schema 
We use an artificial financial schema and dataset as a running example to demonstrate the usability of graph searches. The figure above provides a visualization of all the graph data in the database.

Copy [ddl.gsql](./script/ddl.gsql) to your container. 
Next, run the following in your container's bash command line. 
```
gsql ddl.gsql
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

# Load Data 

You can choose one of the following methods. 

- Load sample data from our publicly accessible s3 bucket 
  
  Copy [load.gsql](./script/load.gsql) to your container. 
  Next, run the following in your container's bash command line. 
  ```
     gsql load.gsql
  ```
  or in GSQL Shell editor, copy the content of [load.gsql](./script/load.gsql), and paste it into the GSQL shell editor to run.
  
- Load from local file in your container
  - Copy the following data files to your container.
    - [account.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/account.csv)
    - [phone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/phone.csv)
    - [city.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/city.csv)
    - [hasPhone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/hasPhone.csv)
    - [locate.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/locate.csv)
    - [transfer.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/transfer.csv)

  - Copy [load2.gsql](./script/load2.gsql) to your container. Modify the script with your local file path. Next, run the following in your container's bash command line. 
    ```
       gsql load2.gsql
    ``` 
    or in GSQL Shell editor, copy the content of [load2.gsql](./script/load2.gsql), and paste in GSQL shell editor to run.

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

# Query Examples 

In OpenCypher, the main statement is a pattern match statement in the form of MATCH-WHERE-RETURN. Each MATCH statement will create or update an invisible working table. The working table consists all the alias (vertex/edge) and columns specified in the current and previous MATCH statements. Other statement will also work on the working table to drive the final result.

We will use examples to illustrate cypher syntax. 

## Node Pattern
### MATCH A Vertex Set 
Copy [c1.cypher](./cypher/c1.cypher) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE OPENCYPHER QUERY c1() {
  // MATCH a node pattern-- symbolized by (),
  //":Account" is the label of the vertex type Account, "a" is a binding variable to the matched node. 
  // return will print out all the bound Account vertices in JSOn format.
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

## Edge Pattern 
### MATCH 1-hop Edge Pattern
Copy [c3.cypher](./cypher/c3.cypher) to your container. 

```python
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE OPENCYPHER QUERY c3(string acctName) {

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
CREATE OR REPLACE OPENCYPHER QUERY c8 (datetime low, datetime high, string accntName) {

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
run query c8("2024-01-01", "2024-12-31", "Scott")
```

The result is shown in [c8.out](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/tutorial/4.x/cypher/c8.out) under `/home/tigergraph/tutorial/4.x/cypher/c8.out`   

[Go back to top](#top)

## With Clause

The WITH clause in Cypher is used to chain parts of a query, pass intermediate results to the next part, or perform transformations like aggregation. It acts as a way to manage query scope and handle intermediate data without exposing everything to the final result.

### Key Uses:
- **Filter intermediate results**: Apply conditions on data before proceeding.
- **Aggregation**: Perform calculations and pass the results further.
- **Variable scope management**: Avoid cluttering query scope by controlling what gets passed forward.

### Filter intermediate result

In the exmaple below, the `WITH a` passes the filtered `Account` (names starting with "J") to the next part of the query.
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

# Support 
If you like the tutorial and want to explore more, join the GSQL developer community at 

https://community.tigergraph.com/

Or, study our product document at

https://docs.tigergraph.com/gsql-ref/current/intro/

[Go back to top](#top)

