
# Introduction <a name="top"></a>

This OpenCypher tutorial provides a hands-on introduction to new users. The software program is the TigerGraph comprehensive environment for designing graph schemas, loading and managing data to build a graph, and querying the graph to perform data analysis

OpenCypher syntax emphasizes ASCII art in its syntax.

A more exhaustive description of functionality and behavior of OpenCypher is available from the [OpenCypher Language Reference](https://opencypher.org/).

To follow this tutorial, install the TigerGraph Docker image (configured with 8 CPUs and 20 GB of RAM or at minimum 4 CPUs and 16 GB of RAM) or set up a Linux instance with Bash access. Download our free [Community Edition](https://dl.tigergraph.com/) to get started.


# Table of Contents

- [Sample Graph](#sample-graph-for-tutorial)
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
  - [CRUD Statements](#crud-statements)
 - [Support](#support)
 - [Contact](#contact)

---
# Sample Graph For Tutorial
This graph is a simplifed version of a real-world financial transaction graph. There are 5 _Account_ vertices, with 8 _transfer_ edges between Accounts. An account may be associated with a _City_ and a _Phone_.
The use case is to analyze which other accounts are connected to 'blocked' accounts.

![Financial Graph](./pictures/FinancialGraph.jpg)

# Setup Environment 

If you have your own machine (including Windows and Mac laptops), the easiest way to run TigerGraph is to install it as a Docker image. Download [Community Edition Docker Image](https://dl.tigergraph.com/). Follow the [Docker setup instructions](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to  set up the environment on your machine.

**Note**: TigerGraph does not currently support the ARM architecture and relies on Rosetta to emulate x86 instructions. For production environments, we recommend using an x86-based system.
For optimal performance, configure your Docker environment with **8 CPUs and 20+ GB** of memory. If your laptop has limited resources, the minimum recommended configuration is **4 CPUs and 16 GB** of memory.

After installing TigerGraph, the `gadmin` command-line tool is automatically included, enabling you to easily start or stop services directly from your bash terminal.
```python
   docker load -i ./tigergraph-4.2.0-alpha-community-docker-image.tar.gz # the xxx.gz file name are what you have downloaded. Change the gz file name depending on what you have downloaded
   docker images #find image id
   docker run -d -p 14240:14240 --name mySandbox imageId #start a container, name it “mySandbox” using the image id you see from previous command
   docker exec -it mySandbox /bin/bash #start a shell on this container. 
   gadmin start all  #start all tigergraph component services
   gadmin status #should see all services are up.
```

For the impatient, load the sample data from the tutorial/gsql folder and run your first query.
```python
   cd tutorial/gsql/   
   gsql 00_schema.gsql  #setup sample schema in catalog
   gsql 01_load.gsql    #load sample data 
   gsql    #launch gsql shell
   GSQL> use graph financialGraph  #enter sample graph
   GSQL> ls #see the catalog content
   GSQL> select a from (a:Account)  #query Account vertex
   GSQL> select s, e, t from (s:Account)-[e:transfer]->(t:Account) limit 2 #query edge
   GSQL> select count(*) from (s:Account)  #query Account node count
   GSQL> select s, t, sum(e.amount) as transfer_amt  from (s:Account)-[e:transfer]->(t:Account)  # query s->t transfer ammount
   GSQL> exit #quit the gsql shell   
```

You can also access the GraphStudio visual IDE directly through your browser:
```python
   http://localhost:14240/
```

A login page will automatically open. Use the default credentials: user is `tigergraph`, password is `tigergraph`. 
Once logged in, click the GraphStudio icon. Assuming you've set up the tutorial schema and loaded the data, navigate by selecting `Global View`, then choose `financialGraph` from the pop up menu. Click Explore Graph to start interacting with your data visually.

To further explore the features of GraphStudio, you can view these concise introductory [videos](https://www.youtube.com/watch?v=29PCZEhyx8M&list=PLq4l3NnrSRp7RfZqrtsievDjpSV8lHhe-), and [product manual](https://docs.tigergraph.com/gui/4.2/intro/). 

The following command is good for operation.

```python
#To stop the server, you can use
 gadmin stop all
#Check `gadmin status` to verify if the gsql service is running, then use the following command to reset (clear) the database.
 gsql 'drop all'
```

**Note that**, our fully managed service -- [TigerGraph Savanna](https://savanna.tgcloud.io/) is entirely GUI-based and does not provide access to a bash shell. To execute the GSQL examples in this tutorial, simply copy the query into the Savanna GSQL editor and click Run.

Additionally, all Cypher examples referenced in this tutorial can be found in your TigerGraph tutorials/cypher folder.

[Go back to top](#top)

---
# Setup Schema 
We use an artificial financial schema and dataset as a running example to demonstrate the usability of graph searches. The figure above provides a visualization of all the graph data in the database.

Copy [00_schema.gsql](./gsql/00_schema.gsql) to your container. 
Next, run the following in your container's bash command line. 
```
gsql 00_schema.gsql
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
     gsql 01_load.gsql
  ```
  or in GSQL Shell editor, copy the content of [01_load.gsql](./gsql/01_load.gsql), and paste it into the GSQL shell editor to run.
  
- Load from local file in your container
  - Copy the following data files to your container.
    - [account.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/account.csv)
    - [phone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/phone.csv)
    - [city.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/city.csv)
    - [hasPhone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/hasPhone.csv)
    - [locate.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/locate.csv)
    - [transfer.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/transfer.csv)

  - Copy [25_load2.gsql](./gsql/25_load2.gsql) to your container. Modify the script with your local file path. Next, run the following in your container's bash command line. 
    ```
       gsql 25_load2.gsql
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

We will use examples to illustrate Cypher syntax. In TigerGraph, each Cypher query is installed as a stored procedure using a code generation technique for optimal performance, enabling repeated execution by its query name.

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
The result is shown in [c1.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/cypher/c1.out) under `/home/tigergraph/tutorial/cypher/c1.out`

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
The result is shown in [c2.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/cypher/c2.out) under `/home/tigergraph/tutorial/cypher/c2.out`

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
The result is shown in [c3.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/cypher/c3.out) under `/home/tigergraph/tutorial/cypher/c3.out`

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
The result is shown in [c4.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c4.out) under `/home/tigergraph/tutorial/cypher/c4.out`    

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

---

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

The result is shown in [c6.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c6.out) under `/home/tigergraph/tutorial/cypher/c6.out`   

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

The result is shown in [c7.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c7.out) under `/home/tigergraph/tutorial/cypher/c7.out`   

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

The result is shown in [c8.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c8.out) under `/home/tigergraph/tutorial/cypher/c8.out`   

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

The result is shown in [c21.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c21.out) under `/home/tigergraph/tutorial/cypher/c21.out`   

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

The result is shown in [c9.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c9.out) under `/home/tigergraph/tutorial/cypher/c9.out`   

[Go back to top](#top)

---

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

The result is shown in [c10.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c10.out) under `/home/tigergraph/tutorial/cypher/c10.out`  

[Go back to top](#top)

---

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

The result is shown in [c11.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c11.out) under `/home/tigergraph/tutorial/cypher/c11.out`  

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

The result is shown in [c12.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c12.out) under `/home/tigergraph/tutorial/cypher/c12.out`  

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

The result is shown in [c13.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c13.out) under `/home/tigergraph/tutorial/cypher/c13.out`  

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

The result is shown in [c14.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c14.out) under `/home/tigergraph/tutorial/cypher/c14.out`  

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
The result is shown in [c15.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c15.out) under `/home/tigergraph/tutorial/cypher/c15.out`

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

The result is shown in [c16.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c16.out) under `/home/tigergraph/tutorial/cypher/c16.out`

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

The result is shown in [c17.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c17.out) under `/home/tigergraph/tutorial/cypher/c17.out`

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

The result is shown in [c18.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c18.out) under `/home/tigergraph/tutorial/cypher/c18.out`

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

The result is shown in [c19.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c19.out) under `/home/tigergraph/tutorial/cypher/c19.out`

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

The result is shown in [c20.out](https://github.com/tigergraph/ecosys/blob/master/tutorials/cypher/c20.out) under `/home/tigergraph/tutorial/cypher/c20.out`

[Go back to top](#top)

---

## Other Expression Functions
There are many expression functions openCypher supports. Please refer to [openCypher functions](https://docs.tigergraph.com/gsql-ref/4.1/opencypher-in-gsql/opencypher-in-gsql) 

[Go back to top](#top)

---

## CRUD Statements

OpenCypher offers comprehensive support for performing Data Modification (Create, Update, Delete) operations on graph data. It provides an intuitive syntax to handle node and relationship manipulation, including their attributes.

### Insert Data

The `CREATE` statement in OpenCypher is used to add new nodes or relationships to the graph. If the specified node or relationship doesn't exist, it will be created. If it does exist, it will be replaced with the new data.

#### Insert Node

The following query creates a new `Account` node with properties `name` and `isBlocked`:

```python
CREATE OR REPLACE OPENCYPHER QUERY insertVertex(STRING name, BOOL isBlocked){
  CREATE (p:Account {name: $name, isBlocked: $isBlocked})
}

# This will create an `Account` node with `name="Abby"` and `isBlocked=true`.
interpret query insertVertex("Abby", true)
```

#### Insert Relationship

The following query creates a `transfer` edge between two `Account` nodes with properties `date` and `amount`

```python
CREATE OR REPLACE OPENCYPHER QUERY insertEdge(VERTEX<Account> s, VERTEX<Account> t, DATETIME dt, UINT amt){
  CREATE (s) -[:transfer {date: $dt, amount: $amt}]-> (t)
}

# Create two `transfer` relationships from "Abby" to "Ed"
interpret query insertEdge("Abby", "Ed", "2025-01-01", 100)
interpret query insertEdge("Abby", "Ed", "2025-01-09", 200)
```

You can use the `SELECT` statement to check if the insertion was successful.

```python
GSQL > select e from (s:Account {name: "Abby"}) -[e:transfer]-> (t:Account {name: "Ed"})
{
  "version": {
    "edition": "enterprise",
    "api": "v2",
    "schema": 0
  },
  "error": false,
  "message": "",
  "results": [
    {
      "Result_Table": [
        {
          "e": {
            "e_type": "transfer",
            "from_id": "Abby",
            "from_type": "Account",
            "to_id": "Ed",
            "to_type": "Account",
            "directed": true,
            "discriminator": "2025-01-01 00:00:00",
            "attributes": {
              "date": "2025-01-01 00:00:00",
              "amount": 100
            }
          }
        },
        {
          "e": {
            "e_type": "transfer",
            "from_id": "Abby",
            "from_type": "Account",
            "to_id": "Ed",
            "to_type": "Account",
            "directed": true,
            "discriminator": "2025-01-09 00:00:00",
            "attributes": {
              "date": "2025-01-09 00:00:00",
              "amount": 200
            }
          }
        }
      ]
    }
  ]
}
```

---

### Delete Data

The `DELETE` statement in OpenCypher is used to **remove nodes and relationships** from the graph. When deleting a node, all its associated relationships will also be deleted.

#### Delete a single node

When you delete a node, if it has relationships, all of its relationships will also be deleted.

```python
CREATE OR REPLACE OPENCYPHER QUERY deleteOneVertex(STRING name="Abby"){
  MATCH (s:Account {name: $name})
  DELETE s
}

# delete "Abby"
interpret query deleteOneVertex("Abby")
```

You can use the `SELECT` statement to check if the deletion was successful.

```python
GSQL > select s from (s:Account) where s.name="Abby"
{
  "version": {
    "edition": "enterprise",
    "api": "v2",
    "schema": 0
  },
  "error": false,
  "message": "",
  "results": [
    {
      "Result_Vertex_Set": []
    }
  ]
}
```

#### Delete all nodes of the specified type

You can delete all nodes of a particular label type.

**Single Label Type**

```python
### single type
CREATE OR REPLACE OPENCYPHER QUERY deleteAllVertexWithType01(){
  MATCH (s:Account)
  DELETE s
}

# Delete all nodes with the label `Account`
interpret query deleteAllVertexWithType01()
```

**Multiple Label Types**

```python
### multiple types
CREATE OR REPLACE OPENCYPHER QUERY deleteVertexWithType02(){
  MATCH (s:Account:Phone)
  DELETE s
}

# Delete all nodes with the label `Account` or `Phone`
interpret query deleteVertexWithType02()
```

#### Delete all nodes

This query deletes all nodes in the graph.

```python
CREATE OR REPLACE OPENCYPHER QUERY deleteAllVertex(){
  MATCH (s:_)
  DELETE s
}

interpret query deleteAllVertex()
```

You can use the `SELECT` statement to check if the deletion was successful.

```python
GSQL > select count(*) from (s:_)
{
  "version": {
    "edition": "enterprise",
    "api": "v2",
    "schema": 0
  },
  "error": false,
  "message": "",
  "results": [
    {
      "Result_Table": {
        "count_lparen_1_rparen_": 0
      }
    }
  ]
}
```


#### Delete relationships

You can delete relationships based on specific conditions.

**Delete `transfer` Relationships with a Date Filter**

This query deletes all `transfer` relationships where the date is earlier than the specified filter date.

```python
CREATE OR REPLACE OPENCYPHER QUERY deleteEdge(STRING name="Abby", DATETIME filterDate="2024-02-01"){
  MATCH (s:Account {name: $name}) -[e:transfer] -> (t:Account)
  WHERE e.date < $filterDate
  DELETE e
}

interpret query deleteEdge()
```

**Delete all outgoing edges of a specific account**

```python
//default parameter is "Abby"
CREATE OR REPLACE OPENCYPHER QUERY deleteAllEdge(STRING name="Abby"){
  MATCH (s:Account {name: $name}) -[e] -> ()
  DELETE e
}

# Delete all outgoing relationships from the node with the name "Abby"
interpret query deleteAllEdge()
```

---

### Update Data

Updating data in OpenCypher allows you to modify node and relationship attributes. The primary mechanism for updating attributes is the `SET` clause, which is used to assign or change the properties of nodes or relationships.

#### Update vertex attributes

You can update the attributes of a node. In this example, the `isBlocked` attribute of the `Account` node is set to `false` for a given account name.

```python
CREATE OR REPLACE OPENCYPHER QUERY updateAccountAttr(STRING name="Abby"){
  MATCH (s:Account {name: $name})
  SET s.isBlocked = false
}

# Update the `isBlocked` attribute of the `Account` node with name "Abby" to false
interpret query updateAccountAttr()
```

#### Update edge attributes

You can also update the attributes of a relationship. In this example, the `amount` attribute of a `transfer` relationship is updated for a specified account, as long as the target account is not blocked.

```python
CREATE OR REPLACE OPENCYPHER QUERY updateTransferAmt(STRING startAcct="Jenny", UINT newAmt=100){
  MATCH (s:Account {name: $startAcct})- [e:transfer]-> (t)
  WHERE NOT t.isBlocked
  SET e.amount = $newAmt
}

interpret query updateTransferAmt(_, 300)
```

You can use the `SELECT` statement to check if the update was successful.

```python
GSQL > select e from (s:Account {name: "Jenny"}) - [e:transfer]-> (t)
{
  "version": {
    "edition": "enterprise",
    "api": "v2",
    "schema": 0
  },
  "error": false,
  "message": "",
  "results": [
    {
      "Result_Table": [
        {
          "e": {
            "e_type": "transfer",
            "from_id": "Jenny",
            "from_type": "Account",
            "to_id": "Scott",
            "to_type": "Account",
            "directed": true,
            "discriminator": "2024-04-04 00:00:00",
            "attributes": {
              "date": "2024-04-04 00:00:00",
              "amount": 300
            }
          }
        }
      ]
    }
  ]
}
```


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

Connect with the author on LinkedIn: [Mingxi Wu](https://www.linkedin.com/in/mingxi-wu-a1704817/) 

[Go back to top](#top)


