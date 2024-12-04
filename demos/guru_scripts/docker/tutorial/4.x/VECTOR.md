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
  - [TopKVectorSearch Summary](#topkvectorsearch-summary)
  - [GDS Functions Summary](#gds-functions-summary)
- [Advanced Topics](#advanced-topics)
  - [Schema Change](#schema-change)
  - [Vector Data Loading](#vector-data-loading)
  - [Python Integration](#python-integration)
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

### Vertex As Parameter
Copy [q1b.gsql](./vector/q1b.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q1b (VERTEX<Account> name) SYNTAX v3 {
  // Define a vextex set from the parameter
  v = {name};
  // output vertex set variable v in JSON format with embedding
  print v WITH_EMBEDDING;
}

#compile and install the query as a stored procedure
install query -single q1b

#run the query
run query q1b("Scott")
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

  // fetch the query vector from the query vertex to the ListAccum
  q = SELECT a FROM (a:Account {name: accntName}) POST-ACCUM @@query_vector += a.emb1;

  // get Top 2 vectors having least distance to the query vector
  r = TopKVectorSearch({Account.emb1}, @@query_vector, 2, {filter: v});

  //output each r with their static attribute and embedding value
  PRINT r WITH_EMBEDDING;

}

# Compile and install the query as a stored procedure using gpr mode
install query -single q2a

# run the compiled query
run query q2a("Scott")
```

### Query Vector from Parameter
Copy [q2b.gsql](./vector/q2b.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q2b (string accntName, list<double> query_vector) SYNTAX v3 {

  //Declare a local sum accumulator to add values. Each vertex has its own accumulator of the declared type
  //The vertex instance is selected based on the FROM clause pattern.
  SumAccum<int> @totalTransfer = 0;

  // match an edge pattern-- symbolized by ()-[]->(), where () is node, -[]-> is a directed edge
  // "v" is a vertex set variable holding the selected vertex set.
  // {name: acctName} is a JSON style filter. It's equivalent to "a.name == acctName"
  // ":transfer" is the label of the edge type "transfer". "e" is the alias of the matched edge.
  v = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->(b:Account)
      ACCUM  b.@totalTransfer += e.amount;

  // get Top 2 vectors having least distance to the query vector provided
  r = TopKVectorSearch({Account.emb1}, query_vector, 2, {filter: v});

  //output each v and their static attribute and embedding value
  PRINT r WITH_EMBEDDING;
}

#compile and install the query as a stored procedure
install query -single q2b

#run the query
run query q2b("Scott", [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```

## Path Pattern 

### Install GDS functions
GDS functions to be used in the queries need to be installed in advance

```python
import package gds
install function gds.**
```

### SELECT A Vertex Set Style: Fixed Length vs. Variable Length Path Pattern
Copy [q3a.gsql](./vector/q3a.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q3a (datetime low, datetime high, string accntName) SYNTAX v3 {

  // Define a custom tuple to store the vertex and its distance to the query vector
  TYPEDEF TUPLE <VERTEX s, FLOAT distance > DIST;
  // Declare a global heap accumulator to store the top 3 values
  HeapAccum<DIST>(3, distance ASC) @@result;
  // Declare a global list accumulator to store query embedding value.
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

### SELECT INTO A Table Style: Group By On A Path Table

If you're familiar with SQL, treat the matched path as a table -- table(a, e, b, e2, c) or unfold their attributes into table(a.attr1, a.attr2..., e.attr1, e.attr2...,b.attr1, b.attr2...). You can group by and aggregate on its columns, just like in SQL. Use `SELECT expr1, expr2..` as usual, with the extension "SELECT a", "SELECT e", "SELECT b" etc. as selecting the graph element.

Copy [q3b.gsql](./vector/q3b.gsql) to your container.

```python
USE GRAPH financialGraph

// create a query
CREATE OR REPLACE QUERY q3b (datetime low, datetime high, string acctName) SYNTAX v3 {

   // a path pattern in ascii art () -[]->()-[]->()
   // think the FROM clause is a matched table with columns (a, e, b, e2, c)
   // you can use SQL syntax to group by on the matched table
   // Below query find 2-hop reachable account c from a, and group by the path a, b, c
   // find out how much each hop's total transfer amount within the given distance range..
   SELECT a, b, c, sum(DISTINCT e.amount) AS hop_1_sum,  sum(DISTINCT e2.amount) AS hop_2_sum INTO T1
   FROM (a:Account)-[e:transfer]->(b)-[e2:transfer]->(c:Account)
   WHERE e.date >= low AND e.date <= high AND gds.vector.distance(b.emb1, c.emb1, "COSINE") < 1.0
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
   FROM (a:Account {name: acctName})-[:transfer*1..]->(b:Account)
   WHERE gds.vector.distance(b.emb1, a.emb1, "COSINE") < 1.0
   GROUP BY a, b;

   PRINT T2;

}

# Compile and install the query as a stored procedure
install query -single q3b

# run the compiled query
run query q3b("2024-01-01", "2024-12-31", "Scott")
```

[Go back to top](#top)

## TopKVectorSearch Summary

### Syntax
```
TopKVectorSearch(EmbeddingAttributes, EmbeddingConstant, K, optionalParam)
```

### Parameter
|Parameter	|Description
|-------|--------
|`EmbeddingAttributes`	|A set of embedding attributes we will search, the items should be in format **VertexType.EmbeddingName**, for example { v1.eb1, v2.eb2}.
|`EmbeddingConstant`	|The embedding constant to search the top K vectors that are most similar to it.
|`K`	|The number of the results to be given.
|`optionalParam` |Optional, a map of params, including vertex filter and EF overriding, for example {filter: vset1, ef: 20}.

### Return
Will return a vertex set

## GDS Functions Summary
### Table of supported GDS vector functions

| Function | Parameter | Description |
|------------|---------|--------------|
|gds.vector.cosine_distance |`list<double> list1, list<double> list2` |Calculates the cosine distance between two vectors represented as lists of doubles.
|gds.vector.dimension_count |`list<double> list1` |Returns the number of dimensions (elements) in a given vector, represented as a list of double values.
|gds.vector.distance |`list<double> list1, list<double> list2, string metric` |Calculates the distance between two vectors represented as lists of double values, based on a specified distance metric.
|gds.vector.elements_sum |`list<double> list1` |Calculates the sum of all elements in a vector, represented as a list of double values.
|gds.vector.ip_distance |`list<double> list1, list<double> list2` |Calculates the inner product (dot product) between two vectors represented as lists of double values.
|gds.vector.kth_element |`list<double> list1, int index` |Retrieves the k-th element from a vector, represented as a list of double values.
|gds.vector.l2_distance |`list<double> list1, list<double> list2` |Calculates the Euclidean distance between two vectors represented as lists of double values.
|gds.vector.norm |`list<double> list1, string metric` |Computes the norm (magnitude) of a vector based on a specified metric.

[Go back to top](#top)

# Advanced Topics

## Schema Change

## Vector Data Loading

## Python Integration
TigerGraph's Python integration is done via pyTigerGraph mainly using the following functions:

|Function	|Description
|-------|--------
|`TigerGraphConnection()`	|Construct a connection to TigerGraph database
|`gsql()`	|Run gsql command same as in a gsql console
|`runLoadingJobWithFile()`	|Load data to TigerGraph database using a text file as Data Source
|`runLoadingJobWithDataFrame()`	|Load data to TigerGraph database using a pandas.DataFrame as Data Source
|`runLoadingJobWithData()`	|Load data to TigerGraph database using a string variable as Data Source
|`runInstalledQuery()`	|Run an installed query via RESTPP endpoint

For more details, please refer to the [pyTigerGraph Doc](https://docs.tigergraph.com/pytigergraph/1.8/intro/).

### Manage TigerGraph Connections
Below example connects to a TigerGraph server with host as localhost and port as 14240 and disconnects from it.

#### Connect to a TigerGraph server
Construct a TigerGraph connection. 

```python
# Establish a connection to the TigerGraph database
import pyTigerGraph as tg
conn = tg.TigerGraphConnection(
    host="http://127.0.0.1",
    restppPort="14240",
    graphname="financialGraph",
    username="tigergraph",
    password="tigergraph"
)
```

#### Parameter
|Parameter	|Description
|-------|--------
|`host`	|IP address of the TigerGraph server.
|`restppPort`	|REST port of the TigerGraph server.
|`graphname`	|Graph name to be used for the schema.
|`username` |User name to connect to the TigerGraph server.
|`password` |Password to connect to the TigerGraph server.

#### Return
A TigerGraph connection created by the passed parameters.

#### Raises
* **TigerGraphException**: In case on invalid URL scheme.

### Create Schema
Schema creation in Python needs to be done by running a gsql command via the pyTigerGraph.gsql() function.

```python
# Create a vector with 1024 dimension in TigerGraph database
# Ensure to connect to TigerGraph server before any operations.
result = conn.gsql("""
    USE GLOBAL
    CREATE VERTEX Account(
        name STRING PRIMARY KEY, 
        isBlocked BOOL
    ) WITH VECTOR ATTRIBUTE emb1(
        DIMENSION = 3
    )
    CREATE GRAPH financialGraph(*)
""")
print(result)
```

### Load Data
Once a schema is created in TigerGraph database, a corresponding Loading Job needs to be created in order to define the data format and mapping to the schema. Given that the embedding data is usually separated by comma, it is recommended to use `|` as the separator for both of the data file and loading job. For example:
```
Scott|n|-0.017733968794345856, -0.01019224338233471, -0.016571875661611557
```

#### Create Loading Job

```python
# Create a loading job for the vector schema in TigerGraph database
# Ensure to connect to TigerGraph server before any operations.
result = conn.gsql("""
    CREATE LOADING JOB l1 {
        DEFINE FILENAME file1;
        LOAD file1 TO VERTEX Account VALUES ($0, $1) USING SEPARATOR="|";
        LOAD file1 TO EMBEDDING ATTRIBUTE emb1 ON VERTEX Account VALUES ($0, SPLIT($2, ",")) USING SEPARATOR="|";
    }
""")
print(result)
```

In case the vector data contains square brackets, the loading job should be revised to handle the extra brackets accordingly.

Data:
```python
Scott|n|[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557]
```

Loading job:
```python
        LOAD file1 TO EMBEDDING ATTRIBUTE emb1 ON VERTEX Account VALUES ($0, SPLIT(gsql_replace(gsql_replace($2,"[",""),"]",""),",")) USING SEPARATOR="|";
```

For more details about loading jobs, please refer to https://docs.tigergraph.com/gsql-ref/4.1/ddl-and-loading/loading-jobs.

#### Load From DataFrame
```python
# Generate and load data from pandas.DataFrame
# Ensure to connect to TigerGraph server before any operations.
import pandas as pd

embeddings = OpenAIEmbeddings()

text_data = {
    "sentences": [
        "Scott",
        "Jenny"
    ]
}

df = pd.DataFrame(text_data)
df['embedding'] = df['sentences'].apply(lambda t: embeddings.embed_query(t))
df['embedding'] = df['embedding'].apply(lambda x: ",".join(str(y) for y in x))
df['sentences'] = df['sentences'].apply(lambda x: x.replace("\n", "\\n"))

cols=["sentences", "embedding"]
result = conn.runLoadingJobWithDataFrame(df, "file1", "l1", "|", columns=cols)
print(result)
```

#### Load From Data File
```python
datafile = "openai_embedding.csv"
result = conn.runLoadingJobWithFile(datafile, "file1", "l1", "|")
print(result)
```

### Run a Query

A query accessing vector data needs to be created and installed in order to be called from gsql console or via RESTPP endpoint.

#### GSQL Console
```python
# Run a query to get the Top 3 vectors similar to the query vector
# Ensure to connect to TigerGraph server before any operations.
query = "Scott"
embeddings = OpenAIEmbeddings()
query_embedding = embeddings.embed_query(query)

result = conn.gsql(f"""
run query top3_vector({query_embeddings})
""")
print(result)
```

#### RESTPP endpoint
```python
# Run a RESTPP call to get the Top 3 vectors similar to the query vector
# Ensure to connect to TigerGraph server before any operations.
query = "Scott"
embeddings = OpenAIEmbeddings()
query_embedding = embeddings.embed_query(query)
result = conn.runInstalledQuery(
    "q2b",
    "accntName=Scott&query_vector="+"&query_vector=".join(str(y) for y in query_embedding),
    timeout=864000
)
print(result)
```

[Go back to top](#top)

# Support
If you like the tutorial and want to explore more, join the GSQL developer community at

https://community.tigergraph.com/

Or, study our product document at

https://docs.tigergraph.com/gsql-ref/current/intro/

[Go back to top](#top)
