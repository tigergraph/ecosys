# Native Vector Support in TigerGraph
TigerGraph now offers native vector support, making it easier to perform vector searches on graph patterns. This feature combines the strengths of graph and vector databases, enabling powerful data analysis and seamless query integration.

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
|`VectorAttributes`	|A set of vector attributes we will search, the items should be in format **VertexType.VectorName**. E.g., `{Account.eb1, Phone.eb2}`.
|`QueryEmbedding`	|The query embedding constant to search the top K most similar vectors.
|`K`	|The top k cutoff--where K most similar vectors will be returned.
|`optionalParam` | A map of optional params, including vertex candidate set, EF-- the exploration factor in HNSW algorithm, and a global MapAccum storing top-k (vertex, distance score) pairs. E.g., `{candidate_set: vset1, ef: 20, distance_map: @@distmap}`.

[Go back to top](#top)
