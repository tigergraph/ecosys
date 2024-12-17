# Native Vector Support in TigerGraph

TigerGraph has unveiled native vector support, a revolutionary feature that significantly amplifies the capabilities of graph analytics by incorporating high-dimensional vector embeddings. This cutting-edge enhancement is tailored for contemporary AI and machine learning workflows, facilitating a seamless integration of structured graph data with vector embeddings, thus unlocking new levels of analytical power and efficiency.

# Sample Graph To Start With <a name="top"></a>
![Financial Graph](./FinancialGraph.jpg)

# Content
This GSQL tutorial contains 
- [Setup Environment](#setup-Environment)
- [Setup Schema (model)](#setup-schema)
- [Load Data](#load-data)
- [Install GDS Functions](#install-gds-functions)
- [Query Examples](#query-examples)
  - [Node Pattern](#node-pattern)
  - [Edge Pattern](#edge-pattern)
  - [Path Pattern](#path-pattern)
  - [vectorSearch Summary](#vectorsearch-summary)
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

# Install GDS functions
GDS functions to be used in the queries need to be installed in advance

```python
import package gds
install function gds.**
```

# Query Examples 

In GSQL, each query block (SELECT-FROM-WHERE) can be used to generate a vertex set or a table. 

- Vertex as Parameter: if a single or a set of vertex id is provided as a query parameter, it can be activatd as a vertex set and used in subsequent query blocks
- SELECT A Vertex Set Style: if a function or query block generates a vertex set, we can store the vertex set in a variable, and use the vertex set variable to drive subsequent query blocks composition via pattern matching or set operation.

### Vertex As Parameter
Copy [q1a.gsql](./vector/q1a.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q1a (VERTEX<Account> name, SET<VERTEX<Account>> slist, LIST<float> query_vector) SYNTAX v3 {
  // Define a vextex set from the vertex parameter
  v = {name};

  // output vertex set variable v in JSON format with embedding
  print v WITH VECTOR;

  // Define a vextex set from the vertex set parameter
  v = {slist};

  // Get the most similar vector from the list
  // The result is re-assigned to v. 
  v = vectorSearch({Account.emb1}, query_vector, 1, {candidate_set: v});

  // output vertex set variable v in JSON format with embedding
  print v WITH VECTOR;
}

#compile and install the query as a stored procedure
install query -single q1a

#run the query
run query q1a("Scott", ["Steven", "Jenny"], [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```

[Go back to top](#top)

## Node Pattern
### SELECT A Vertex Set Style 
Copy [q1b.gsql](./vector/q1b.gsql) to your container.

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q1b () SYNTAX v3 {

  ListAccum<float> @@query_vector;

  // select from a node pattern-- symbolized by (),
  //":Account" is the label of the vertex type Account, "a" is a binding variable to the matched node. 
  // v is a vertex set variable, holding the selected vertex set
  v = SELECT a
      FROM (a:Account)
      WHERE a.emb1.size() == 3 and a.name == "Scott";
      POST-ACCUM @@query_vector += a.emb1;

  // Get the top 3 similar vertices for Scott from all Account vertices
  // The result is re-assigned to v. 
  v = vectorSearch({Account.emb1}, @@query_vector, 3);

  // output vertex set variable v in JSON format with embedding
  PRINT v WITH VECTOR;
}

# Compile and install the query as a stored procedure using gpr mode
install query -single q1b

# run the compiled query
run query q1b()
```

## Edge Pattern 
### SELECT A Vertex Set Style 
Copy [q2a.gsql](./vector/q2a.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q2a (string accntName, list<double> query_vector) SYNTAX v3 {

  //Declare a global accumulator to store the distances of the result from vectorSearch
  MapAccum<Vertex, Float> @@distances;

  //Declare a local sum accumulator to add values. Each vertex has its own accumulator of the declared type
  //The vertex instance is selected based on the FROM clause pattern.
  SumAccum<int> @totalTransfer = 0;

  // match an edge pattern-- symbolized by ()-[]->(), where () is node, -[]-> is a directed edge
  // "v" is a vertex set variable holding the selected vertex set.
  // {name: acctName} is a JSON style filter. It's equivalent to "a.name == acctName"
  // ":transfer" is the label of the edge type "transfer". "e" is the alias of the matched edge.
  v = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->(b:Account)
      WHERE e.amount >= 1000;

  // get Top 3 vectors having least distance to the query vector provided
  r = vectorSearch({Account.emb1}, query_vector, 3, {candidate_set: v, distance_map: @@distances});

  //output each r and their static attribute and embedding value
  PRINT r WITH VECTOR;

  //print distance mapping
  PRINT @@distances;

  // get total amount for all transfers originated from the top 3 vertices
  v = SELECT b
      FROM (a:r)-[e:transfer]->(b:Account)
      ACCUM b.@totalTransfer += e.amount;

  // output each v and their static attributes
  PRINT v;
}

#compile and install the query as a stored procedure
install query -single q2a

#run the query
run query q2a("Scott", [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```

### Query Vector from Parameter
Copy [q2b.gsql](./vector/q2b.gsql) to your container. 

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q2b (string accntName) SYNTAX v3 {

  //Declare a global map accumulator to store vector distance values.
  MapAccum<Vertex<Account>, Float> @@similarity;

  //Declare a global list accumulator to store query embedding value.
  ListAccum<float> @@query_vector;

  //Declare a local sum accumulator to add values. Each vertex has its own accumulator of the declared type
  //The vertex instance is selected based on the FROM clause pattern.
  SumAccum<int> @totalTransfer = 0;

  // fetch the query vector from the query vertex to the ListAccum
  q = SELECT a FROM (a:Account {name: accntName}) POST-ACCUM @@query_vector += a.emb1;

  // match an edge pattern-- symbolized by ()-[]->(), where () is node, -[]-> is a directed edge
  // "v" is a vertex set variable holding the selected vertex set.
  // {name: acctName} is a JSON style filter. It's equivalent to "a.name == acctName"
  // ":transfer" is the label of the edge type "transfer". "e" is the alias of the matched edge.
  // get Top 3 vectors having least distance to the query vector
  v = SELECT b
      FROM (a:Account)-[e:transfer]->(b:Account)
      ACCUM  b.@totalTransfer += e.amount
      ORDER BY gds.vector.cosine_distance(b.emb1, @@query_vector)
      LIMIT 3;

  //output each v with their static attribute and embedding value
  PRINT v WITH VECTOR;

  // get the similarity values of the top 3 vectors from the previous query block
  r = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->(b:v)
      WHERE b.@totalTransfer > 3000
      ACCUM @@similarity += ( b -> 1 - gds.vector.cosine_distance(@@query_vector, b.emb1));

  //output similarity values calculated
  PRINT r[r.@totalTransfer], @@similarity;
}

# Compile and install the query as a stored procedure using gpr mode
install query -single q2b

# run the compiled query
run query q2b("Scott")
```

## Path Pattern 

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

  // Declare a local groupby accumulator to store vector distances.
  GroupByAccum<Vertex<Account> b, SetAccum<float> distance> @distances;

  // a path pattern in ascii art () -[]->()-[]->()
  r = SELECT b
      FROM (a:Account {name: accntName})-[e:transfer]->()-[e2:transfer]->(b:Account)
      WHERE e.date >= low AND e.date <= high and e.amount >500 and e2.amount>500
      ACCUM @@result += DIST(b, gds.vector.distance(a.emb1, b.emb1, "COSINE"));

  // print the top 3 vertices
  PRINT @@result;

  // below we use variable length path.
  // *1.. means 1 to more steps of the edge type "transfer"
  // select the reachable end point and bind it to vertex alias "b"
  r = SELECT b
      FROM (a:Account {name: accntName})-[:transfer*1..]->(b:Account)
      WHERE a.name != b.name
      ACCUM a.@distances += (b -> gds.vector.cosine_distance(a.emb1, b.emb1));

  // print eacho r with static attributes and distances
  PRINT r;
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
#enter the graph
USE GRAPH financialGraph

# create a query
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
   WHERE gds.vector.distance(b.emb1, a.emb1, "COSINE") > 0.8
   GROUP BY a, b;

   PRINT T2;
}

# Compile and install the query as a stored procedure
install query -single q3b

# run the compiled query
run query q3b("2024-01-01", "2024-12-31", "Scott")
```

[Go back to top](#top)

## vectorSearch Summary

### Syntax
```
vectorSearch(VectorAttributes, EmbeddingConstant, K, optionalParam)
```

### Parameter
|Parameter	|Description
|-------|--------
|`VectorAttributes`	|A set of vector attributes we will search, the items should be in format **VertexType.VectorName**, for example { v1.eb1, v2.eb2}.
|`EmbeddingConstant`	|The embedding constant to search the top K vectors that are most similar to it.
|`K`	|The number of the results to be given.
|`optionalParam` |Optional, a map of params, including vertex candidate set and EF overriding, for example {candidate_set: vset1, ef: 20}.

### Return
Will return a vertex set

## GDS Functions Summary
### Table of supported GDS vector functions

| Function | Parameter | Description |
|------------|---------|--------------|
|gds.vector.distance |`list<double> list1, list<double> list2, string metric` |Calculates the distance between two vectors represented as lists of double values, based on a specified distance metric.
|gds.vector.cosine_distance |`list<double> list1, list<double> list2` |Calculates the cosine distance between two vectors represented as lists of doubles.
|gds.vector.ip_distance |`list<double> list1, list<double> list2` |Calculates the inner product (dot product) between two vectors represented as lists of double values.
|gds.vector.l2_distance |`list<double> list1, list<double> list2` |Calculates the Euclidean distance between two vectors represented as lists of double values.
|gds.vector.norm |`list<double> list1, string metric` |Computes the norm (magnitude) of a vector based on a specified metric.
|gds.vector.dimension_count |`list<double> list1` |Returns the number of dimensions (elements) in a given vector, represented as a list of double values.
|gds.vector.elements_sum |`list<double> list1` |Calculates the sum of all elements in a vector, represented as a list of double values.
|gds.vector.kth_element |`list<double> list1, int index` |Retrieves the k-th element from a vector, represented as a list of double values.

[Go back to top](#top)

# Advanced Topics

## Schema Change

### Global Vertex and Edge
Global vertex/edge is the vertex/edge type created in global scope and shared with multiple graphs, which can only be modified from the global scope.

#### Add a Vector To Global Vertex

```python
# enter global
USE GLOBAL

# create a global schema change job to modify the global vertex
CREATE GLOBAL SCHEMA_CHANGE JOB add_emb2 {
  ALTER VERTEX Account ADD VECTOR ATTRIBUTE emb2(DIMENSION=3, METRIC="L2");
}

# run the global schema_change job
run global schema_change job add_emb2
```

#### Remove a Vector From Global Vertex

```python
# enter global
USE GLOBAL

# create a global schema change job to modify the global vertex
CREATE GLOBAL SCHEMA_CHANGE JOB drop_emb2 {
  ALTER VERTEX Account DROP VECTOR ATTRIBUTE ( emb2 );
}

# run the global schema_change job
run global schema_change job drop_emb2
```

### Local Graph and Local Vertex
Local graph contains its own vertex and edge types as well as data, which is invisible from other local graphs.

#### Create a Local Graph
```python
# enter global
USE GLOBAL

# create an empty local graph
CREATE GRAPH localGraph()
```

#### Create Local Vertex and Edge
```python
#enter local graph
USE GRAPH localGraph

# create a local schema change job to create local vertex with or without vector
CREATE SCHEMA_CHANGE JOB add_local_vertex FOR GRAPH localGraph {
  ADD VERTEX Account (name STRING PRIMARY KEY, isBlocked BOOL);
  ADD VERTEX Phone (number STRING PRIMARY KEY, isBlocked BOOL);
  ADD DIRECTED EDGE transfer (FROM Account, TO Account, DISCRIMINATOR(date DATETIME), amount UINT) WITH REVERSE_EDGE="transfer_reverse";
}
run schema_change job add_local_vertex
```

#### Add a Vector To Local Vertex
```python
#enter local graph
USE GRAPH localGraph

# create a local schema change job to modify the local vertex
CREATE SCHEMA_CHANGE JOB add_local_emb1 FOR GRAPH localGraph {
  ALTER VERTEX Account ADD VECTOR ATTRIBUTE emb1(DIMENSION=3, METRIC="COSINE");
  ALTER VERTEX Account ADD VECTOR ATTRIBUTE emb2(DIMENSION=10, METRIC="L2");
  ALTER VERTEX Phone ADD VECTOR ATTRIBUTE emb1(DIMENSION=3);
}

# run the local schema_change job
run schema_change job add_local_emb1
```

#### Remove a Vector From Local Vertex

```python
#enter local graph
USE GRAPH localGraph

# create a local schema change job to modify the global vertex
CREATE SCHEMA_CHANGE JOB drop_local_emb1 FOR GRAPH localGraph {
  ALTER VERTEX Account DROP VECTOR ATTRIBUTE ( emb1 );
}

# run the local schema_change job
run schema_change job drop_local_emb1
```

#### Remove Local Vertex and Edge
```python
#enter local graph
USE GRAPH localGraph

# create a local schema change job to drop local vertex with or without vector
CREATE SCHEMA_CHANGE JOB drop_local_vertex FOR GRAPH localGraph {
  DROP VERTEX Account, Phone;
  DROP EDGE transfer;
}
RUN SCHEMA_CHANGE JOB drop_local_vertex
```

#### Remove a Local Graph
Dropping a local graph will also drop all of its vertex, edge and data.
```python
# enter global
USE GLOBAL

# drop the whole local graph
DROP GRAPH localGraph CASCADE;
```

For more details, please visit https://docs.tigergraph.com/gsql-ref/4.1/ddl-and-loading/.

## Vector Data Loading

### File Loading
#### Identify Data Format
It is crucial to find the proper data format for embedding loading, mainly to identify the possible values of the primary key, text or binary contents, and the embedding values, in order to define appropriate headers, separator and end-of-line character to have the data parsed by the loading job correctly.
* Field Separator - If the content contains comma, it's recommended to use `|` instead.
* Newline Character - If the content contains newline character, it's recommended to escape it or define another end-of-line character.
* Header line - Headers can make the fields human-friendly, otherwise the fields will be referrd according to their positions.

Below is a typical data format for embedding values:
```python
id|name|isBlocked|embedding
1|Scott|n|-0.017733968794345856, -0.01019224338233471, -0.016571875661611557
```

#### Create Loading Job
```python
# enter graph
USE GRAPH embGraph

#create a loading job for the vetex and edge
CREATE LOADING JOB load_local_file FOR GRAPH embGraph {
 // define the location of the source files; each file path is assigned a filename variable.  
 DEFINE FILENAME file1="/home/tigergraph/data/account_emb.csv";

 //define the mapping from the source file to the target graph element type. The mapping is specified by VALUES clause. 
 LOAD file1 TO VERTEX Account VALUES ($"name", gsql_to_bool(gsql_trim($"isBlocked"))) USING header="true", separator=",";
 LOAD file1 TO VECTOR ATTRIBUTE emb1 ON VERTEX Account VALUES ($1, SPLIT($3, ",")) USING SEPARATOR="|", HEADER="true";
}
```

#### Run Loading Job Locally
If the source file location has been defined in the loading job directly, use the following command:
```python
USE GRAPH embGraph
run loading job load_local_file
```

It can also provide a file path in the command to override the file path defined inside the loading job:
```python
USE GRAPH embGraph
run loading job load_local_file using file1="/home/tigergraph/data/account_emb_no_header.csv", header="false"
```

#### Run Loading Job Remotely
TigerGraph also supports run a loading job remotely via DDL endpoint `POST /restpp/ddl/{graph_name}?tag={loading_job_name}&filename={file_variable_name}`.

For example:
```python
curl -X POST --data-binary @./account_emb.csv "http://localhost:14240/restpp/ddl/embGraph?tag=load_local_file&filename=file1&sep=|"
```

### RESTPP Loading
```python
curl -X POST "http://localhost:14240/restpp/graph/embGraph" -d '
{
  "vertices": {
    "Account": {
      "Scott": {
        "name": {
          "value": "Curry"
        },
        "isBlocked": {
          "value":  false
        },
        "emb1": {
          "value": [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557]
        }
      }
    }
  }
}
'
```

### Other Data Source
TigerGraph supports various ways to load data, including loading from cloud storage and parquet file format. 

Please refer to https://docs.tigergraph.com/tigergraph-server/4.1/data-loading/ for more details.

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
# Create a vector with 3 dimension in TigerGraph database
# Ensure to connect to TigerGraph server before any operations.
result = conn.gsql("""
    USE GLOBAL
    CREATE VERTEX Account(
        name STRING PRIMARY KEY, 
        isBlocked BOOL
    )
    CREATE GRAPH financialGraph(*)
    CREATE GLOBAL SCHEMA_CHANGE JOB fin_add_vector FOR GRAPH financialGraph {
        ALTER VERTEX Account ADD VECTOR ATTRIBUTE emb1(dimension=3);
    }
    RUN GLOBAL SCHEMA_CHANGE JOB fin_add_vector
""")
print(result)
```

### Load Data
Once a schema is created in TigerGraph database, a corresponding Loading Job needs to be created in order to define the data format and mapping to the schema. Given that the embedding data is usually separated by comma, it is recommended to use `|` as the separator for both of the data file and loading job. For example:
```
1|Scott|n|-0.017733968794345856, -0.01019224338233471, -0.016571875661611557
```

#### Create Loading Job

```python
# Create a loading job for the vector schema in TigerGraph database
# Ensure to connect to TigerGraph server before any operations.
result = conn.gsql("""
    CREATE LOADING JOB l1 {
        DEFINE FILENAME file1;
        LOAD file1 TO VERTEX Account VALUES ($0, $1) USING SEPARATOR="|";
        LOAD file1 TO VECTOR ATTRIBUTE emb1 ON VERTEX Account VALUES ($1, SPLIT($3, ",")) USING SEPARATOR="|";
    }
""")
print(result)
```

In case the vector data contains square brackets, the loading job should be revised to handle the extra brackets accordingly.

Data:
```python
1|Scott|n|[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557]
```

Loading job:
```python
LOAD file1 TO VECTR ATTRIBUTE emb1 ON VERTEX Account VALUES ($1, SPLIT(gsql_replace(gsql_replace($2,"[",""),"]",""),",")) USING SEPARATOR="|";
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
run query q2a("Scott", {query_embeddings})
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
    "q2a",
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
