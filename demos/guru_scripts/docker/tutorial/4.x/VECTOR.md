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
- [Support](#support) 
  

# Setup Environment 

Follow [Docker setup ](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up your docker Environment.

[Go back to top](#top)

# Setup Schema 
Copy [ddl.gsql](./vector/ddl.gsql) to your container. 
Next, run the following in your container's bash command line. 
```
gsql ddl.gsql
```

[Go back to top](#top)

# Load Data 

You can choose one of the following methods. 

- Load sample data from our publicly accessible s3 bucket 
  
  Copy [load.gsql](./vector/load.gsql) to your container. 
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
    - [account_emb.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/account_emb.csv)
    - [phone_emb.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/data/phone_emb.csv)

  - Copy [load2.gsql](./script/load2.gsql) to your container. Modify the script with your local file path. Next, run the following in your container's bash command line. 
    ```
       gsql load2.gsql
    ``` 
    or in GSQL Shell editor, copy the content of [load2.gsql](./script/load2.gsql), and paste in GSQL shell editor to run.
    
[Go back to top](#top)

# Query Examples 

In GSQL, each query block (SELECT-FROM-WHERE) can be used to generate a vertex set or a table. 

- SELECT A Vertex Set Style: if a query block generates a vertex set, we can store the vertex set in a variable, and use the vertex set variable to drive subsequent query blocks composition via pattern matching or set operation.


## Node Pattern
### SELECT A Vertex Set Style 
Copy [q1a.gsql](./vector/q1a.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q1a () SYNTAX v3 {

  // select from a node pattern-- symbolized by (),
  //":Account" is the label of the vertex type Account, "a" is a binding variable to the matched node. 
  // v is a vertex set variable, holding the selected vertex set
  v = SELECT a
      FROM (a:Account);

  // output vertex set variable v in JSON format with embedding
  PRINT v WITH_EMBEDDING;

  //we can use vertex set variable in the subsequent query block's node pattern.
  //v is placed in the node pattern vertex label position. The result is re-assigned to v. 
  v = SELECT a
      FROM (a:v)
      WHERE a.name == "Scott";

  // output vertex set variable v in JSON format with embedding
  PRINT v WITH_EMBEDDING;
}

# Compile and install the query as a stored procedure using gpr mode
install query -single q1a

# run the compiled query
run query q1a()
```

[Go back to top](#top)

## Edge Pattern 
### SELECT A Vertex Set Style 
Copy [q2a.gsql](./vector/q2a.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q2a (string accntName) SYNTAX v3 {

  //Declare a local sum accumulator to add values. Each vertex has its own accumulator of the declared type
  //The vertex instance is selected based on the FROM clause pattern.
  SumAccum<int> @totalTransfer = 0;
  //Declare a global list accumulator to store query embedding value.
  ListAccum<float> @@query_vector;

  // match an edge pattern-- symbolized by ()-[]->(), where () is node, -[]-> is a directed edge
  // "v" is a vertex set variable holding the selected vertex set.
  // {name: acctName} is a JSON style filter. It's equivalent to "a.name == acctName"
  // ":transfer" is the label of the edge type "transfer". "e" is the alias of the matched edge.
  v = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->(b:Account)
      //for each matched edge, accumulate e.amount into the local accumulator of b.
      ACCUM  b.@totalTransfer += e.amount;

  // fetch the query embedding from the query vertex to the ListAccum
  w = SELECT a FROM (a:Account {name: accntName}) POST-ACCUM @@query_vector += a.emb1;

  // get Top 2 vectors having least distance to the query vector
  r = TopKVectorSearch({Account.emb1}, @@query_vector, 2, {filter: v});

  //output each v and their static attribute and embedding value
  PRINT r WITH_EMBEDDING;

}

# Compile and install the query as a stored procedure using gpr mode
install query -single q2a

# run the compiled query
run query q2a("Scott")
```

## Path Pattern 

### SELECT A Vertex Set Style: Fixed Length vs. Variable Length Path Pattern
Copy [q3a.gsql](./vector/q3a.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE QUERY q3a (datetime low, datetime high, string accntName) SYNTAX v3 {

  //Define a custom tuple to store the vertex and its distance to the query vector
  TYPEDEF TUPLE <VERTEX s, FLOAT distance > DIST;
  //Declare a global heap accumulator to store the top 3 values
  HeapAccum<DIST>(3, distance ASC) @@result;
  //Declare a global list accumulator to store query embedding value.
  ListAccum<float> @@query_vector;

  // get query vector
  q = SELECT a FROM (a:Account {name: accntName}) POST-ACCUM @@query_vector += a.emb1;

  // a path pattern in ascii art () -[]->()-[]->()
  r = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->()-[e2:transfer]->(b:Account)
      WHERE e.date >= low AND e.date <= high and e.amount >500 and e2.amount>500
      ACCUM @@result += DIST(b, gds.vector.distance(b, @@query_vector, "COSINE"));

  // print the top 3 vertices
  PRINT @@result;

  // clean the heap accummulator
  @@result.clear();

  // below we use variable length path.
  // *1.. means 1 to more steps of the edge type "transfer"
  // select the reachable end point and bind it to vertex alias "b"
  r = SELECT b
      FROM (a:Account {name: accntName})-[:transfer*1..]->(b:Account)
      ACCUM @@result += DIST(b, gds.vector.consine_distance(b, query_vector));

  // print the top 3 vertices
  PRINT @@result;
}

# Compile and install the query as a stored procedure
install query q3a

# run the compiled query
run query q3a("2024-01-01", "2024-12-31", "Scott")
```

[Go back to top](#top)

## Pattern Summary

[Go back to top](#top)

# Advanced Topics

[Go back to top](#top)

# Support
If you like the tutorial and want to explore more, join the GSQL developer community at

https://community.tigergraph.com/

Or, study our product document at

https://docs.tigergraph.com/gsql-ref/current/intro/

[Go back to top](#top)
