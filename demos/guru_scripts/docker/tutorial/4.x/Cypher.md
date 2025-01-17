# Sample Graph To Start With <a name="top"></a>
![Financial Graph](./FinancialGraph.jpg)

# Content
This GSQL tutorial contains 
- [Setup Environment](#setup-Environment)
- [Setup Schema (model)](#setup-schema)
- [Load Data](#load-data)
- [Query Examples](#query-examples)
  - [Node Pattern](#node-pattern)
  - [Edge Pattern](#edge-pattern)
  - [Path Pattern](#path-pattern)
  - [Pattern Summary](#pattern-summary)
- [Advanced Topics](#advanced-topics)
  - [Accumulators](#accumulators)
    - [Accumulator Operators](#accumulator-operators)
    - [Global vs Vertex Attached Accumulator](#global-vs-vertex-attached-accumulator)
    - [ACCUM vs POST-ACCUM](#accum-vs-post-accum)
    - [Edge Attached Accumulator](#edge-attached-accumulator)
  - [Vertex Set Variables And Accumulator As Composition Tools](#vertex-set-variables-and-accumulator-as-composition-tools)
    - [Using Vertex Set Variables](#using-vertex-set-variables)
    - [Using Accumulators](#using-accumulators)
  - [Flow Control](#flow-control)
    - [IF Statement](#if-statement)
    - [WHILE Statement](#while-statement)
    - [FOREACH Statement](#foreach-statement)
    - [CONTINUE and BREAK Statement](#continue-and-break-statement)
    - [CASE WHEN Statement](#case-when-statement)
  - [Vertex Set Operators](#vertex-set-operators)
    - [Union](#union)
    - [Intersect](#intersect)
    - [Minus](#minus)
  - [Query Tuning](#query-tuning) 
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
