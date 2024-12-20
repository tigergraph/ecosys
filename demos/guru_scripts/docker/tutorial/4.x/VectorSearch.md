# Native Vector Support in TigerGraph
TigerGraph offers native vector support, making it easier to perform vector searches on graph patterns. This feature combines the strengths of graph and vector databases, enabling powerful data analysis and seamless query integration.

# Sample Graph To Start With <a name="top"></a>
![Financial Graph](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/FinancialGraph.jpg)

# Content
This GSQL tutorial contains 
- [Setup Environment](#setup-environment)
- [Setup Schema (model)](#setup-schema)
- [Load Data](#load-data)
- [Install GDS Functions](#install-gds-functions)
- [Vector Search Functions](#vector-search-functions)
  - [vectorSearch Function](#vectorsearch-function)
  - [Vector Built-in Functions](#vector-built-in-functions) 
- [Query Examples](#query-examples)
  - [Vector Search](#vector-search)
  - [Range Vector Search](#range-vector-search)
  - [Filtered Vector Search](#filtered-vector-search)
  - [Vector Search on Graph Patterns](#vector-search-on-graph-patterns)
  - [Vector Similarity Join on Graph Patterns](#vector-similarity-join-on-graph-patterns)
    
# Setup Environment 

Follow [Docker setup ](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up your docker Environment.

> **_Note:_** For vector feature preview, please pull `tigergraph/tigergraph:4.2.0-preview` docker images instead. For example:
> ```
> docker run -d -p 14240:14240 --name tigergraph --ulimit nofile=1000000:1000000 -t tigergraph/tigergraph:4.2.0-preview
> ```
> Please remember to apply your TigerGraph license key to the container:
> ```
> docker exec -it tigergraph /bin/bash
> gadmin license set <license_key>
> gadmin config apply -y
> gadmin start all
> ```

[Go back to top](#top)

# Setup Schema 
Copy [ddl.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/vector/ddl.gsql) to your container. 
Next, run the following in your container's bash command line. 
```
gsql ddl.gsql
```

[Go back to top](#top)

# Load Data 

You can choose one of the following methods. 

- Load sample data from our publicly accessible s3 bucket 
  
  Copy [load.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/vector/load.gsql) to your container. 
  Next, run the following in your container's bash command line. 
  ```
  gsql load.gsql
  ```
  or in GSQL Shell editor, copy the content of [load.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/script/load.gsql), and paste it into the GSQL shell editor to run.
  
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

  - Copy [load2.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/script/load2.gsql) to your container. Modify the script with your local file path. Next, run the following in your container's bash command line. 
    ```
    gsql load2.gsql
    ``` 
    or in GSQL Shell editor, copy the content of [load2.gsql](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/script/load2.gsql), and paste in GSQL shell editor to run.
    
[Go back to top](#top)

# Install GDS functions
GDS functions to be used in the queries need to be installed in advance

```python
import package gds
install function gds.**
show package gds.vector
```
[Go back to top](#top)
# Vector Search Functions
## vectorSearch Function
### Syntax
```
result = vectorSearch(VectorAttributes, QueryEmbedding, K, optionalParam)
```
### Function name 
In GSQL, we support top-k vector search via the function `vectorSearch()`, which will return the top k most similar vectors to an input `QueryEmbedding`. 
The result will be assigned to a vertex set varialbe, which can be used by subsequent GSQL query block. E.g., `result` will hold the top-k most similar vertices based on their embedding distance to the query embedding. 
### Parameter
|Parameter	|Description
|-------|--------
|`VectorAttributes`	|A set of vector attributes we will search, the items should be in format **VertexType.VectorName**. E.g., `{Account.eb1, Phone.eb1}`.
|`QueryEmbedding`	|The query embedding constant to search the top K most similar vectors.
|`K`	|The top k cutoff--where K most similar vectors will be returned.
|`optionalParam` | A map of optional params, including vertex candidate set, EF-- the exploration factor in HNSW algorithm, and a global MapAccum storing top-k (vertex, distance score) pairs. E.g., `{candidate_set: vset1, ef: 20, distance_map: @@distmap}`.

[Go back to top](#top)
## Vector Built-in Functions
In order to support vector type computation, GSQL provides a list of built-in vector functions. 

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

# Query Examples
## Vector Search
Do a top-k vector search on a given vertex type's vector attribute. 

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
install query -single q1

#run the query
run query q1([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557])
```

[Go back to top](#top)

## Range Vector Search
Do a range vector search with a given query embedding and a distance threshold. 

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q2 (LIST<float> query_vector, double threshold) SYNTAX v3 {

  v = SELECT a
      FROM (a:Account)
      WHERE gds.vector.distance(a.emb1, query_vector, "COSINE") < threshold;

  print v WITH VECTOR;
}

#compile and install the query as a stored procedure
install query -single q2

#run the query
run query q2([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557], 0.394)
```

[Go back to top](#top)
## Filtered Vector Search
Do a GSQL query block to select a vertex candidate set, then do vector top-k search on the candidate set. 

```python
#enter the graph
USE GRAPH financialGraph

CREATE OR REPLACE QUERY q3 (LIST<float> query_vector, int k) SYNTAX v3 {
  MapAccum<Vertex, Float> @@distances;
  //select candidate for vector search
  c = SELECT a
      FROM (a:Account)
      WHERE a.name in ("Scott", "Paul", "Steven");

  v = vectorSearch({Account.emb1}, query_vector, k, {candidate_set: c, distance_map: @@distances});

  print v WITH VECTOR;
  print @@distances;

}

#compile and install the query as a stored procedure
install query -single q3

#run the query
run query q3([-0.017733968794345856, -0.01019224338233471, -0.016571875661611557], 2)
```

[Go back to top](#top)
## Vector Search on Graph Patterns


[Go back to top](#top)
## Vector Similarity Join on Graph Patterns


[Go back to top](#top)


