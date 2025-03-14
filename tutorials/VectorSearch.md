# Native Vector Support in TigerGraph
TigerGraph offers native vector support, making it easier to perform vector searches on graph patterns. This feature combines the strengths of graph and vector databases, enabling powerful data analysis and seamless query integration. We believe agentic AI will benefit from this powerful combination!

# Sample Graph To Start With <a name="top"></a>
![Financial Graph](https://github.com/tigergraph/ecosys/blob/master/tutorials/pictures/FinancialGraph.jpg)

# Content
This GSQL tutorial contains 
- [Setup Environment](#setup-environment)
- [Setup Schema (model)](#setup-schema)
- [Load Data](#load-data)
- [Install GDS Functions](#install-gds-functions)
- [Vector Search Functions](#vector-search-functions)
  - [Vector Search Architecture](#vector-search-architecture)
  - [vectorSearch Function](#vectorsearch-function)
  - [Vector Built-in Functions](#vector-built-in-functions) 
- [Query Examples](#query-examples)
  - [Vector Search](#vector-search)
  - [Range Vector Search](#range-vector-search)
  - [Filtered Vector Search](#filtered-vector-search)
  - [Vector Search on Graph Patterns](#vector-search-on-graph-patterns)
  - [Vector Similarity Join on Graph Patterns](#vector-similarity-join-on-graph-patterns)
  - [Vector Search Driven Pattern Match](#vector-search-driven-pattern-match)
- [Essential Operations and Tools](#Essential-operations-and-tools)
  - [Global and Local Schema Change](#global-and-local-schema-change)
  - [Vector Data Loading](#vector-data-loading)
  - [Python Integration](#python-integration)
- [Vector Update](#vector-update)
- [Support](#support)
- [Reference](#reference)
- [Contact](#contact)
    
# Setup Environment 

If you have your own machine (including Windows and Mac laptops), the easiest way to run TigerGraph is to install it as a Docker image. Download [Community Edition Docker Image](https://dl.tigergraph.com/). Follow the [Docker setup instructions](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to  set up the environment on your machine.

After you installed TigerGraph, you can use gadmin tool to start/stop services under Bash shell.

```python
       docker load -i ./tigergraph-4.2.0-alpha-community-docker-image.tar.gz # the xxx.gz file name are what you have downloaded. Change the gz file name depending on what you have downloaded
       docker images #find image id
       docker run -d --name mySandbox imageId #start a container, name it “mySandbox” using the image id you see from previous command
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
The following command is good for operation.

```python
#To stop the server, you can use
 gadmin stop all
#Check `gadmin status` to verify if the gsql service is running, then use the following command to reset (clear) the database.
 gsql 'drop all'
```

**Note that**, our fully managed service -- [TigerGraph Savanna](https://savanna.tgcloud.io/) is entirely GUI-based, with no access to a bash shell. To run the GSQL examples in this tutorial, simply copy the GSQL query into the Savanna GSQL editor and click the RUN button.

[Go back to top](#top)

# Setup Schema 
We use an artificial financial schema and dataset as a running example to demonstrate the usability of hybrid vector and graph searches. The figure above provides a visualization of all the graph data in the database.

To augment the graph dataset with vector data, for each Account and Phone node, we generated a 3-dimensional random vector data. By default, the cosine metric is used to measure the distance between vectors.

Locate [00_ddl.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/00_ddl.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container. 
Next, run the following in your container's bash command line. 
```
gsql /home/tigergraph/tutorial/vector/00_ddl.gsql
```

As seen below, `Account` and `Phone` vertex types are extended with `emb1` vector attribute, which is 3-dimensional vector. By default, the `emb1` will use `consine` metric. An ANN search index will be automatically built and maintained as vector data is loaded and updated.  

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

//create vectors
CREATE GLOBAL SCHEMA_CHANGE JOB fin_add_vector {
  //add an embedding attribute "emb1" to vertex type "Account"
  ALTER VERTEX Account ADD VECTOR ATTRIBUTE emb1(dimension=3);
  ALTER VERTEX Phone ADD VECTOR ATTRIBUTE emb1(dimension=3);
}
run global schema_change job fin_add_vector

//create graph; * means include all graph element types in the graph.
CREATE GRAPH financialGraph (*)
```

[Go back to top](#top)

# Load Data 

You can choose one of the following methods. 

- Load sample data from our publicly accessible s3 bucket 
  
  Locate [01_load.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/01_load.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container. 
  Next, run the following in your container's bash command line. Wait 2 mintues as it's pulling data from s3. 

  ```
  gsql /home/tigergraph/tutorial/vector/01_load.gsql
  ```
  or in GSQL Shell editor, copy the content of [01_load.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/01_load.gsql), and paste it into the GSQL shell editor to run.
  
- Load from local file in your container
  - Locate the following data files under `/home/tigergraph/tutorial/data` or copy them to your container:
    - [account.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/account.csv)
    - [phone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/phone.csv)
    - [city.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/city.csv)
    - [hasPhone.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/hasPhone.csv)
    - [locate.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/locate.csv)
    - [transfer.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/transfer.csv)
    - [account_emb.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/account_emb.csv)
    - [phone_emb.csv](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/data/phone_emb.csv)

  - Locate [02_load2.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/02_load2.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container. Modify the script with your local file path if necessary. Next, run the following in your container's bash command line. 
    ```
    gsql /home/tigergraph/tutorial/vector/02_load2.gsql
    ``` 
    or in GSQL Shell editor, copy the content of [02_load2.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/02_load2.gsql), and paste in GSQL shell editor to run.
 
    The declarative loading script is self-explanatory. You define the filename alias for each data source, and use the the `LOAD` statement to map the data source to the target schema elements-- vertex types, edge types, and vector attributes. 

    ```python
    USE GRAPH financialGraph

    DROP JOB load_local_file

    //define a loading job to load from local file
    CREATE LOADING JOB load_local_file  {
      // define the location of the source files; each file path is assigned a filename variable.  
      DEFINE FILENAME account="/home/tigergraph/tutorial/data/account.csv";
      DEFINE FILENAME phone="/home/tigergraph/tutorial/data/phone.csv";
      DEFINE FILENAME city="/home/tigergraph/tutorial/data/city.csv";
      DEFINE FILENAME hasPhone="/home/tigergraph/tutorial/data/hasPhone.csv";
      DEFINE FILENAME locatedIn="/home/tigergraph/tutorial/data/locate.csv";
      DEFINE FILENAME transferdata="/home/tigergraph/tutorial/data/transfer.csv";
      DEFINE FILENAME accountEmb="/home/tigergraph/tutorial/data/account_emb.csv";
      DEFINE FILENAME phoneEmb="/home/tigergraph/tutorial/data/phone_emb.csv";
      //define the mapping from the source file to the target graph element type. The mapping is specified by VALUES clause. 
      LOAD account TO VERTEX Account VALUES ($"name", gsql_to_bool(gsql_trim($"isBlocked"))) USING header="true", separator=",";
      LOAD phone TO VERTEX Phone VALUES ($"number", gsql_to_bool(gsql_trim($"isBlocked"))) USING header="true", separator=",";
      LOAD city TO VERTEX City VALUES ($"name") USING header="true", separator=",";
      LOAD hasPhone TO Edge hasPhone VALUES ($"accnt", gsql_trim($"phone")) USING header="true", separator=",";
      LOAD locatedIn TO Edge isLocatedIn VALUES ($"accnt", gsql_trim($"city")) USING header="true", separator=",";
      LOAD transferdata TO Edge transfer VALUES ($"src", $"tgt", $"date", $"amount") USING header="true", separator=",";
      LOAD accountEmb TO VECTOR ATTRIBUTE emb1 ON VERTEX Account VALUES ($0, SPLIT($1, ",")) USING SEPARATOR="|", header="true";
      LOAD phoneEmb TO VECTOR ATTRIBUTE emb1 ON VERTEX Phone VALUES ($0, SPLIT($1, ",")) USING SEPARATOR="|", header="true";
    }

    run loading job load_local_file
    ```
    
[Go back to top](#top)

# Install GDS functions
GDS functions to be used in the queries need to be installed in advance

```python
gsql 
GSQL> import package gds
GSQL> install function gds.**
GSQL> show package gds.vector
```
[Go back to top](#top)
# Vector Search Functions
## Vector Search Architecture
TigerGraph supports both ANN vector search and exact vector search.

### Approximate Nearest Neighbors (ANN)
ANN is a technique for identifying points that are approximately closest to a query point in high-dimensional spaces. It offers significant improvements in speed and scalability compared to exact methods, with only a slight trade-off in accuracy.

TigerGraph enhances vertex capabilities by introducing support for vector attributes. When vector data is loaded as an attribute, the engine automatically indexes it to facilitate ANN searches. This indexing process leverages TigerGraph’s Massively Parallel Processing (MPP) architecture, enabling efficient parallel processing across multiple compute cores or machines. By default, the HNSW algorithm is used, with future releases planned to support additional indexing methods.

TigerGraph provides a user-friendly vectorSearch function for performing ANN searches within a GSQL query. This built-in function integrates seamlessly with other GSQL query blocks and accumulators, supporting both basic and advanced use cases. These include pure vector searches, filtered vector searches, and searches based on graph patterns.

### Exact Vector Search 
To support exact searches, TigerGraph includes a set of built-in vector functions. These functions allow users to perform operations on vector attributes, enabling advanced capabilities such as exact top-k vector searches, similarity joins on graph patterns, and innovative fusions of structured and unstructured data.


[Go back to top](#top)

## vectorSearch Function
### Syntax
```
//result is a vertex set variable, storing the top-k most similar vertices. 
result = vectorSearch(VectorAttributes, QueryVector, K, optionalParam)
```
### Function name and return type
In GSQL, we support top-k ANN (approximate nearest neighbor) vector search via the function `vectorSearch()`, which will return the top k most similar vectors to an input `QueryVector`.
The result will be assigned to a vertex set variable, which can be used by subsequent GSQL query blocks. E.g., `result` will hold the top-k most similar vertices based on their embedding distance to the query embedding.

### Parameter
|Parameter	|Description
|-------|--------
|`VectorAttributes`	|A set of vector attributes we will search, the items should be in format **VertexType.VectorName**. E.g., `{Account.eb1, Phone.eb1}`.
|`QueryVector`	|The query embedding constant to search the top K most similar vectors.
|`K`	|The top k cutoff--where K most similar vectors will be returned.
|`optionalParam` | A map of optional params, including vertex candidate set, EF-- the exploration factor in HNSW algorithm, and a global MapAccum storing top-k (vertex, distance score) pairs. E.g., `{candidate_set: vset1, ef: 20, distance_map: @@distmap}`.

[Go back to top](#top)
## Vector Built-in Functions
In order to support vector type computation, GSQL provides a list of built-in vector functions. You can see the function signatures by typing the following command in GSQL shell.


```python
GSQL> show package gds.vector
````
You will see
```
Packages "gds.vector":
  - Object:
    - Functions:
        - gds.vector.cosine_distance(list<double> list1, list<double> list2) RETURNS (float) (installed)
        - gds.vector.dimension_count(list<double> list1) RETURNS (int) (installed)
        - gds.vector.distance(list<double> list1, list<double> list2, string metric) RETURNS (float) (installed)
        - gds.vector.elements_sum(list<double> list1) RETURNS (float) (installed)
        - gds.vector.ip_distance(list<double> list1, list<double> list2) RETURNS (float) (installed)
        - gds.vector.kth_element(list<double> list1, int kth_index) RETURNS (float) (installed)
        - gds.vector.l2_distance(list<double> list1, list<double> list2) RETURNS (float) (installed)
        - gds.vector.norm(list<double> list1, string metric) RETURNS (float) (installed)
```

| Function | Parameter | Return Type | Description |
|------------|---------|--------------|--------------|
|gds.vector.distance |`list<double> list1, list<double> list2, string metric` |float|Calculates the distance between two vectors represented as lists of double values, based on a specified distance metric: "cosine", "l2", "ip".
|gds.vector.cosine_distance |`list<double> list1, list<double> list2` |float|Calculates the cosine distance between two vectors represented as lists of doubles.
|gds.vector.ip_distance |`list<double> list1, list<double> list2` |float|Calculates the inner product (dot product) between two vectors represented as lists of double values.
|gds.vector.l2_distance |`list<double> list1, list<double> list2` |float|Calculates the Euclidean distance between two vectors represented as lists of double values.
|gds.vector.norm |`list<double> list1, string metric` |float|Computes the norm (magnitude) of a vector based on a specified metric.
|gds.vector.dimension_count |`list<double> list1` |int|Returns the number of dimensions (elements) in a given vector, represented as a list of double values.
|gds.vector.elements_sum |`list<double> list1` |float|Calculates the sum of all elements in a vector, represented as a list of double values.
|gds.vector.kth_element |`list<double> list1, int index` |float|Retrieves the k-th element from a vector, represented as a list of double values.

You can also see these built-in function implementations, which is GSQL code. For example, if we want to see the `distance` function implementation, we can do
```python
GSQL>show function gds.vector.distance
```
[Go back to top](#top)

# Query Examples
## Vector Search
### Top-k vector search on a given vertex type's vector attribute. 

Locate [03_q1.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/03_q1.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/03_q1.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q1 (LIST<float> query_vector) SYNTAX v3 {
  MapAccum<Vertex, Float> @@distances;

  //find top-5 similar embeddings from Account's embedding attribute emb1, store the distance in @@distance
  v = vectorSearch({Account.emb1}, query_vector, 5, { distance_map: @@distances});

  print v WITH VECTOR; //show the embeddings
  print @@distances; //show the distance map
}

#compile and install the query as a stored procedure
install query q1

#run the query
run query q1([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```
The result is shown in [q1.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q1.out) under `/home/tigergraph/tutorial/vector/q1.out` 

You can also use POST method to call REST api to invoke the installed query. By default, the query will be located at URL "restpp/query/{graphName}/{queryName}". 
On the payload, you specify the parameter using "key:value" by escaping the quotes of the parameter name.

```python
curl -u "tigergraph:tigergraph" -H 'Content-Type: application/json' -X POST "http://127.0.0.1:14240/gsql/v1/queries/q1?graph=financialGraph" -d '{
  "parameters":{"query_vector":[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557]}}' | jq
```

### Top-k vector search on a set of vertex types' vector attributes. 

Locate [04_q1a.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/04_q1a.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/04_q1a.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q1a (LIST<float> query_vector) SYNTAX v3 {
  MapAccum<Vertex, Float> @@distances;
  //specify vector search on Account and Phone's emb1 attribute. 
  v = vectorSearch({Account.emb1, Phone.emb1}, query_vector, 8, { distance_map: @@distances});

  print v WITH VECTOR;
  print @@distances;
}

#compile and install the query as a stored procedure
install query q1a

#run the query
run query q1a ([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```
The result is shown in [q1a.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q1a.out) under `/home/tigergraph/tutorial/vector/q1a.out` 
### Top-k vector search using a vertex embedding as the query vector

Locate [05_q1b.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/05_q1b.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/05_q1b.gsql
```
```python
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q1b () SYNTAX v3 {
  //this global accumulator will be storing the query vector. 
  //You can retrieve an embedding attribute and accmulate it into a ListAccum<float>
  ListAccum<float> @@query_vector;
  MapAccum<Vertex, Float> @@distances;
 
 //find Scott's embedding, store it in @@query_vector
 s = SELECT a
     FROM (a:Account)
     WHERE a.name == "Scott"
     POST-ACCUM @@query_vector += a.emb1;

  //find top-5 similar to Scott's embedding from Account's embedding attribute emb1, store the distance in @@distance
  v = vectorSearch({Account.emb1}, @@query_vector, 5, { distance_map: @@distances});

  print v WITH VECTOR; //show the embeddings
  print @@distances; //show the distance map
}

#compile and install the query as a stored procedure
install query q1b

#run the query
run query q1b()
```
The result is shown in [q1b.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q1b.out) under `/home/tigergraph/tutorial/vector/q1b.out` 

### Top-k vector search from a vertex set parameter

Locate [06_q1c.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/06_q1c.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.

```
gsql /home/tigergraph/tutorial/vector/06_q1c.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q1c (VERTEX<Account> name, SET<VERTEX<Account>> slist, LIST<float> query_vector) SYNTAX v3 {
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
install query q1c

#run the query
run query q1c("Scott", ["Steven", "Jenny"], [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```
The result is shown in [q1c.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q1c.out) under `/home/tigergraph/tutorial/vector/q1c.out` 

[Go back to top](#top)

## Range Vector Search
Do a range vector search with a given query embedding and a distance threshold. 

Locate [07_q2.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/07_q2.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/07_q2.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q2 (LIST<float> query_vector, double threshold) SYNTAX v3 {
  //find Account whose emb1 distance to a query_vector is less than a threshold
  v = SELECT a
      FROM (a:Account)
      WHERE gds.vector.distance(a.emb1, query_vector, "COSINE") < threshold;

  print v WITH VECTOR;
}

#compile and install the query as a stored procedure
install query q2

#run the query
run query q2([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557], 0.394)
```
The result is shown in [q2.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q2.out) under `/home/tigergraph/tutorial/vector/q2.out` 

[Go back to top](#top)
## Filtered Vector Search
Do a GSQL query block to select a vertex candidate set, then do vector top-k search on the candidate set. 

Locate [08_q3.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/08_q3.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/08_q3.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q3 (LIST<float> query_vector, int k) SYNTAX v3 {
  MapAccum<Vertex, Float> @@distances;
  //select candidate for vector search
  c = SELECT a
      FROM (a:Account)
      WHERE a.name in ("Scott", "Paul", "Steven");
  //do top-k vector search within the vertex set "c", store the top-k distances to the distance_map
  v = vectorSearch({Account.emb1}, query_vector, k, {candidate_set: c, distance_map: @@distances});

  print v WITH VECTOR;
  print @@distances;

}

#compile and install the query as a stored procedure
install query q3

#run the query
run query q3([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557], 2)
```

The result is shown in [q3.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q3.out) under `/home/tigergraph/tutorial/vector/q3.out` 

You can also use POST method to call REST api to invoke the installed query. By default, the query will be located at URL "restpp/query/{graphName}/{queryName}". 
On the payload, you specify the parameter using "key:value" by escaping the quotes of the parameter name.
```python
curl -X POST "http://127.0.0.1:14240/restpp/query/financialGraph/q3" -d '{"query_vector":[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557], "k": 2}' | jq
```

[Go back to top](#top)
## Vector Search on Graph Patterns

### Approximate Nearest Neighbor (ANN) vector search on a graph pattern
Do a pattern match first to find candidate vertex set. Then, do a vector search. 

Locate [09_q4.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/09_q4.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/09_q4.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q4 (datetime low, datetime high, LIST<float> query_vector) SYNTAX v3 {


  MapAccum<Vertex, Float> @@distances1;
  MapAccum<Vertex, Float> @@distances2;

  // a path pattern in ascii art ()-[]->()-[]->()
  c1 = SELECT b
       FROM (a:Account {name: "Scott"})-[e:transfer]->()-[e2:transfer]->(b:Account)
       WHERE e.date >= low AND e.date <= high and e.amount >500 and e2.amount>500;

  //ANN search. Do top-k search on the vertex set "c1".
  v = vectorSearch({Account.emb1}, query_vector, 2, {candidate_set: c1, distance_map: @@distances1});

  PRINT v WITH VECTOR;
  PRINT @@distances1;

  // below we use variable length path.
  // *1.. means 1 to more steps of the edge type "transfer"
  // select the reachable end point and bind it to vertex alias "b"
  c2 = SELECT b
       FROM (a:Account {name: "Scott"})-[:transfer*1..]->(b:Account)
       WHERE a.name != b.name;
  //ANN search. Do top-k search on the vertex set "c2"
  v = vectorSearch({Account.emb1}, query_vector, 2, {candidate_set: c2, distance_map: @@distances2});

  PRINT v WITH VECTOR;
  PRINT @@distances2;

}

#compile and install the query as a stored procedure
install query q4

#run the query
run query q4("2024-01-01", "2024-12-31", [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```
The result is shown in [q4.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q4.out) under `/home/tigergraph/tutorial/vector/q4.out` 

### Exact vector search on a graph pattern 

Use `ORDER BY ASC` or `ORDER BY DESC` to do exact top-k vector search. This method is exepensive. 

Locate [10_q4a.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/10_q4a.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/10_q4a.gsql
```

```python
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q4a (LIST<float> query_vector) SYNTAX v3 {


  MapAccum<Vertex, Float> @@distances1;
  MapAccum<Vertex, Float> @@distances2;

  // do an exact top-k search on "b" using the ORDER BY clause with ASC keyword
  c1 = SELECT b
       FROM (a:Account)-[e:transfer]->(b:Account)
       ORDER BY gds.vector.cosine_distance(b.emb1, query_vector) ASC
       LIMIT 3;

  PRINT c1 WITH VECTOR;


  // an approximate top-k search on the Account vertex set
  v = vectorSearch({Account.emb1}, query_vector, 3, {distance_map: @@distances1});

  PRINT v WITH VECTOR;
  PRINT @@distances1;

  // below we use variable length path.
  // *1.. means 1 to more steps of the edge type "transfer"
  // select the reachable end point and bind it to vertex alias "b"
  // do an exact top-k reverse-search on "b" using the ORDER BY clause with DESC keyword
  c2 = SELECT b
       FROM (a:Account {name: "Scott"})-[:transfer*1..]->(b:Account)
       WHERE a.name != b.name
       ORDER BY gds.vector.cosine_distance(b.emb1, query_vector) DESC
       LIMIT 3;

  PRINT c2 WITH VECTOR;

  // an approximate top-k search on the Account vertex set
  v = vectorSearch({Account.emb1}, query_vector, 5, {distance_map: @@distances2});

  PRINT v WITH VECTOR;
  PRINT @@distances2;

}

#compile and install the query as a stored procedure
install query q4a

#run the query
run query q4a([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```
The result is shown in [q4a.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q4a.out) under `/home/tigergraph/tutorial/vector/q4a.out` 

[Go back to top](#top)
## Vector Similarity Join on Graph Patterns
### Top-K similarity join on graph patterns
Find most similar pairs from a graph pattern. Exhaustive search any two pairs specified by vertex alias from a given graph pattern. 

Locate [11_q5.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/11_q5.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/11_q5.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q5() SYNTAX v3 {

  //Define a custom tuple to store the vertex pairs and their distance
  TYPEDEF TUPLE <VERTEX s, VERTEX t, FLOAT distance> pair;

  //Declare a global heap accumulator to store the top 2 similar pairs
  HeapAccum<pair>(2, distance ASC) @@result;

  // a path pattern in ascii art () -[]->()-[]->().
  // for each (a,b) pair, we calculate their "CONSINE" distance. and store them in a Heap.
  // only the top-2 pair will be kept in the Heap
  v  = SELECT b
       FROM (a:Account)-[e:transfer]->()-[e2:transfer]->(b:Account)
       ACCUM @@result += pair(a, b, gds.vector.distance(a.emb1, b.emb1, "COSINE"));

  PRINT @@result;
}

#compile and install the query as a stored procedure
install query q5

#run the query
run query q5()
```
The result is shown in [q5.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q5.out) under `/home/tigergraph/tutorial/vector/q5.out`

### Range similarity join on graph patterns. 
Find similar pairs whose distance is less than a threshold from a graph pattern. Exhaustive search any two pairs specified by vertex alias from a given graph pattern. 

Locate [12_q5a.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/12_q5a.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/12_q5a.gsql
```
```python
#enter the graph
USE GRAPH financialGraph

# create a query
CREATE OR REPLACE QUERY q5a() SYNTAX v3 {

  //find close pairs that has distance less than 0.8
  SELECT a, b,  gds.vector.distance(b.emb1, a.emb1, "COSINE") AS dist INTO T
  FROM (a:Account)-[e:transfer]->()-[e2:transfer]->(b:Account)
  WHERE gds.vector.distance(a.emb1, b.emb1, "COSINE") < 0.8;

  PRINT T;
}

#compile and install the query as a stored procedure
install query q5a

#run the query
run query q5a()
```
The result is shown in [q5a.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q5a.out) under `/home/tigergraph/tutorial/vector/q5a.out` 
[Go back to top](#top)

## Vector Search Driven Pattern Match
Do vector search first, the result drive the next pattern match. 

Locate [13_q6.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/13_q6.gsql) under `/home/tigergraph/tutorial/vector` or copy it to your container.
Next, run the following in your container's bash command line.
```
gsql /home/tigergraph/tutorial/vector/13_q6.gsql
```

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q6 (LIST<float> query_vector) SYNTAX v3 {
  //find top-3 vectors from Account.emb1 that are closest to query_vector
  R = vectorSearch({Account.emb1}, query_vector, 3);

  PRINT R;

  //query composition via vector search result R
  V = SELECT b
      FROM (a:R)-[e:transfer]->()-[e2:transfer]->(b:Account);

  print V ;
}

#compile and install the query as a stored procedure
install query q6

#run the query
run query q6([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```
The result is shown in [q6.out](https://raw.githubusercontent.com/tigergraph/ecosys/master/tutorials/vector/q6.out) under `/home/tigergraph/tutorial/vector/q6.out` 
[Go back to top](#top)
# Essential Operations and Tools

## Global and Local Schema Change

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
  ALTER VERTEX Account DROP VECTOR ATTRIBUTE emb2;
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
  ALTER VERTEX Account DROP VECTOR ATTRIBUTE  emb1;
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
DROP GRAPH localGraph CASCADE
```

For more details, please visit [https://docs.tigergraph.com/gsql-ref/4.1/ddl-and-loading/](https://docs.tigergraph.com/gsql-ref/4.1/ddl-and-loading/).

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
USE GRAPH financialGraph

#create a loading job for the vetex and edge
CREATE LOADING JOB load_local_file FOR GRAPH financialGraph {
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
USE GRAPH financialGraph
run loading job load_local_file
```

It can also provide a file path in the command to override the file path defined inside the loading job:
```python
USE GRAPH financialGraph
run loading job load_local_file using file1="/home/tigergraph/data/account_emb_no_header.csv", header="false"
```

#### Run Loading Job Remotely
TigerGraph also supports run a loading job remotely via DDL endpoint `POST /restpp/ddl/{graph_name}?tag={loading_job_name}&filename={file_variable_name}`.

For example:
```python
curl -X POST --data-binary @./account_emb.csv "http://localhost:14240/restpp/ddl/financialGraph?tag=load_local_file&filename=file1&sep=|"
```

### RESTPP Loading
You can follow the official documentation on RESTPP loading https://docs.tigergraph.com/tigergraph-server/4.1/api/upsert-rest. 
Below is a simple example. 
```python
curl -X POST "http://localhost:14240/restpp/graph/financialGraph" -d '
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

Please refer to [https://docs.tigergraph.com/tigergraph-server/4.1/data-loading/](https://docs.tigergraph.com/tigergraph-server/4.1/data-loading/) for more details.

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
    CREATE GLOBAL SCHEMA_CHANGE JOB fin_add_vector {
        ALTER VERTEX Account ADD VECTOR ATTRIBUTE emb1(dimension=3);
    }
    RUN GLOBAL SCHEMA_CHANGE JOB fin_add_vector
    CREATE GRAPH financialGraph(*)
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
    CREATE LOADING JOB load_emb {
        DEFINE FILENAME file1;
        LOAD file1 TO VERTEX Account VALUES ($1, $2) USING SEPARATOR="|";
        LOAD file1 TO VECTOR ATTRIBUTE emb1 ON VERTEX Account VALUES ($1, SPLIT($3, ",")) USING SEPARATOR="|", HEADER="false";
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

For more details about loading jobs, please refer to [https://docs.tigergraph.com/gsql-ref/4.1/ddl-and-loading/loading-jobs/](https://docs.tigergraph.com/gsql-ref/4.1/ddl-and-loading/loading-jobs/).

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
result = conn.runLoadingJobWithDataFrame(df, "file1", "load_emb", "|", columns=cols)
print(result)
```

#### Load From Data File
```python
datafile = "openai_embedding.csv"
result = conn.runLoadingJobWithFile(datafile, "file1", "load_emb", "|")
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
run query q1({query_embeddings})
""")
print(result)
```

#### RESTPP endpoint
```python
# Run a RESTPP call to get the Top 3 vectors similar to the query vector
# List needs to be specified in format of <param=value1&param=value2...>
# Ensure to connect to TigerGraph server before any operations.
query = "Scott"
embeddings = OpenAIEmbeddings()
query_embedding = embeddings.embed_query(query)
result = conn.runInstalledQuery(
    "q1",
    "query_vector="+"&query_vector=".join(str(y) for y in query_embedding),
    timeout=864000
)
print(result)
```

[Go back to top](#top)

# Vector Update

## Delayed Update Visibility

Vector attributes are fully editable, allowing users to create, read, update, and delete them like any other vertex attribute. However, since they are indexed using HNSW for fast ANN search, updates may not be immediately visible until the index is rebuilt. To track the rebuild status, we provide a REST endpoint for real-time status check.

```python
/vector/status/{graph_name}/{vertex_type}/{vector_name}
```

**Example**

Check a vector attribute of vertex type `v1`.
```python
curl -X GET "http://localhost:14240/restpp/vector/status/g1/v1/embAttr1"

#sample output
{"version":{"edition":"enterprise","api":"v2","schema":0},"error":false,"message":"fetched status success","results":{"NeedRebuildServers":["GPE_1#1"]},"code":"REST-0000"}
```

Also, we can check by vertex type. 

```python
curl -X GET "http://localhost:14240/restpp/vector/status/g1/v1"

#sample output
{"version":{"edition":"enterprise","api":"v2","schema":0},"error":false,"message":"fetched status success","results":{"NeedRebuildServers":["GPE_1#1"]},"code":"REST-0000"}
```

We can add `verbose` flag, which will show all needed rebuild vector instances.

```python
curl -X GET "http://localhost:14240/restpp/vector/status/g1/v1/embAttr1?verbose=true"

#sample output
{"version":{"edition":"enterprise","api":"v2","schema":0},"error":false,"message":"fetched status success","results":{"NeedRebuildInstances({SEGID}_{VECTOR_ID})":{"GPE_1#1":[1_1]}},"code":"REST-0000"}
```

## Frequent Update Consume More Memory and Disk

Based on our experiments, a large volume of updates may lead to high memory and disk consumption. We observed that the index file size closely matches memory usage, while the main file size grows as expected, accumulating all update records.

# Support
If you like the tutorial and want to explore more, join the GSQL developer community at

[https://community.tigergraph.com/](https://community.tigergraph.com/)

[Go back to top](#top)

# Reference
[TigerVector: Supporting Vector Search in Graph Databases for Advanced RAGs](https://arxiv.org/abs/2501.11216), to appear in [SIGMOD 2025 proceedings](https://2025.sigmod.org/).

For GSQL quick start, please refer to [GSQL Tutorial](https://github.com/tigergraph/ecosys/blob/master/tutorials/README.md)

[Go back to top](#top)

# Contact
To contact us for commercial support and purchase, please email us at [info@tigergraph.com](mailto:info@tigergraph.com)

[Go back to top](#top)

