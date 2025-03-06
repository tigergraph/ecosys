# TigerGraphX Quick Start: Using TigerGraph for Graph and Vector Database

In this quick start guide, we will work with the following graph:

![Financial Graph](https://github.com/tigergraph/ecosys/blob/master/tutorials/pictures/FinancialGraph.jpg)

## Installation Guide

Follow this guide to install and set up **TigerGraphX** in your environment.

### Requirements

This project requires **Python 3.10, 3.11 or 3.12** and **TigerGraph 4.2**. Ensure you meet the following prerequisites before proceeding:

#### **1. Python**
- Please ensure Python 3.10, 3.11 or 3.12 is installed on your system.
- You can download and install it from the [official Python website](https://www.python.org/downloads/).

#### **2. TigerGraph**

TigerGraph 4.2 is required for this project and can be set up in one of the following ways:

- **TigerGraph DB**: Install and configure a local instance of TigerGraph.
- **TigerGraph Cloud**: Use a cloud-hosted instance of TigerGraph.
- **TigerGraph Docker**: Use a Docker container to run TigerGraph. 

  ##### **Docker Setup Guide**

  Follow the [Docker setup guide](https://github.com/tigergraph/ecosys/blob/master/demos/guru_scripts/docker/README.md) to set up your Docker environment.

  > **_Note:_** For vector feature preview, please pull the `tigergraph/tigergraph:4.2.0-preview` Docker image instead. Here's how you can do it:
  >
  > ```bash
  > docker run -d -p 14240:14240 --name tigergraph --ulimit nofile=1000000:1000000 -t tigergraph/tigergraph:4.2.0-preview
  > ```
  >
  > After setting up the Docker container, remember to apply your TigerGraph license key to the instance. You can obtain a free developer license [here](https://dl.tigergraph.com/). Once you have your license key, follow these steps:
  >
  > ```bash
  > docker exec -it tigergraph /bin/bash
  > gadmin license set <license_key>
  > gadmin config apply -y
  > gadmin start all
  > ```

### Installation Steps

The simplest way to get started with **TigerGraphX** is by installing it directly from PyPI. Using a virtual environment is recommended to ensure a clean and isolated setup.

To install TigerGraphX, run:
```bash
pip install tigergraphx
```

This allows you to quickly start using the library without needing the source code.

#### **Verify Installation**

After installing, verify that TigerGraphX is installed correctly by running:
```bash
python -c 'import tigergraphx; print("TigerGraphX installed successfully!")'
```

If the installation was successful, you will see:
```
TigerGraphX installed successfully!
```

This ensures that the library is properly installed and ready for use.

## Create a Graph
### Define the TigerGraph Connection Configuration
Since our data is stored in a TigerGraph instance—whether on-premise or in the cloud—we need to configure the connection settings. The recommended approach is to use environment variables, such as setting them with the `export` command in the shell. Here, to illustrate the demo, we configure them within Python using the `os.environ` method.


```python
>>> import os
>>> os.environ["TG_HOST"] = "http://127.0.0.1"
>>> os.environ["TG_USERNAME"] = "tigergraph"
>>> os.environ["TG_PASSWORD"] = "tigergraph"
```

### Define a Graph Schema
TigerGraph is a schema-based database, which requires defining a schema to structure your graph. This schema specifies the graph name, nodes (vertices), edges (relationships), and their respective attributes.

In this example, we create a graph called "FinancialGraph" with three node types: "Account," "City," and "Phone," and three edge types: "transfer," "hasPhone," and "isLocatedIn."

- **Nodes:**
  - **Account**: Primary key `name`, attributes `name` (string) and `isBlocked` (boolean), vector attribute `emb1` (3).
  - **City**: Primary key `name`, attribute `name` (string).
  - **Phone**: Primary key `number`, attributes `number` (string) and `isBlocked` (boolean), vector attribute `emb1` (3).

- **Edges:**
  - **transfer**: Directed multi-edge between "Account" nodes, identified by `date` (datetime) as the unique identifier for each edge between a pair of source and target nodes. The edge has an attribute `amount` (integer).
  - **hasPhone**: Undirected edge between "Account" and "Phone" nodes.
  - **isLocatedIn**: Directed edge between "Account" and "City" nodes.

This schema defines the structure of the "FinancialGraph" with nodes and edges and their respective attributes.


```python
>>> graph_schema = {
...     "graph_name": "FinancialGraph",
...     "nodes": {
...         "Account": {
...             "primary_key": "name",
...             "attributes": {
...                 "name": "STRING",
...                 "isBlocked": "BOOL",
...             },
...             "vector_attributes": {"emb1": 3},
...         },
...         "City": {
...             "primary_key": "name",
...             "attributes": {
...                 "name": "STRING",
...             },
...         },
...         "Phone": {
...             "primary_key": "number",
...             "attributes": {
...                 "number": "STRING",
...                 "isBlocked": "BOOL",
...             },
...             "vector_attributes": {"emb1": 3},
...         },
...     },
...     "edges": {
...         "transfer": {
...             "is_directed_edge": True,
...             "from_node_type": "Account",
...             "to_node_type": "Account",
...             "discriminator": "date",
...             "attributes": {
...                 "date": "DATETIME",
...                 "amount": "INT",
...             },
...         },
...         "hasPhone": {
...             "is_directed_edge": False,
...             "from_node_type": "Account",
...             "to_node_type": "Phone",
...         },
...         "isLocatedIn": {
...             "is_directed_edge": True,
...             "from_node_type": "Account",
...             "to_node_type": "City",
...         },
...     },
... }
```

### Create a Graph
Running the following command will create a graph using the user-defined schema if it does not already exist. If the graph exists, the command will return the existing graph. To overwrite the existing graph, set the drop_existing_graph parameter to True. Note that creating the graph may take several seconds.


```python
>>> from tigergraphx import Graph
>>> G = Graph(graph_schema)
```

    2025-02-28 14:17:12,187 - tigergraphx.core.managers.schema_manager - INFO - Creating schema for graph: FinancialGraph...
    2025-02-28 14:17:15,857 - tigergraphx.core.managers.schema_manager - INFO - Graph schema created successfully.
    2025-02-28 14:17:15,857 - tigergraphx.core.managers.schema_manager - INFO - Adding vector attribute(s) for graph: FinancialGraph...
    2025-02-28 14:18:31,787 - tigergraphx.core.managers.schema_manager - INFO - Vector attribute(s) added successfully.


### Retrieve a Graph and Print Its Schema
Once a graph has been created in TigerGraph, you can retrieve it without manually defining the schema using the `Graph.from_db` method, which requires only the graph name:


```python
>>> G = Graph.from_db("FinancialGraph")
```

Now, let's print the schema of the graph in a well-formatted manner:


```python
>>> import json
>>> schema = G.get_schema()
>>> print(json.dumps(schema, indent=4, default=str))
```

    {
        "graph_name": "FinancialGraph",
        "nodes": {
            "Account": {
                "primary_key": "name",
                "attributes": {
                    "name": {
                        "data_type": "DataType.STRING",
                        "default_value": null
                    },
                    "isBlocked": {
                        "data_type": "DataType.BOOL",
                        "default_value": null
                    }
                },
                "vector_attributes": {
                    "emb1": {
                        "dimension": 3,
                        "index_type": "HNSW",
                        "data_type": "FLOAT",
                        "metric": "COSINE"
                    }
                }
            },
            "City": {
                "primary_key": "name",
                "attributes": {
                    "name": {
                        "data_type": "DataType.STRING",
                        "default_value": null
                    }
                },
                "vector_attributes": {}
            },
            "Phone": {
                "primary_key": "number",
                "attributes": {
                    "number": {
                        "data_type": "DataType.STRING",
                        "default_value": null
                    },
                    "isBlocked": {
                        "data_type": "DataType.BOOL",
                        "default_value": null
                    }
                },
                "vector_attributes": {
                    "emb1": {
                        "dimension": 3,
                        "index_type": "HNSW",
                        "data_type": "FLOAT",
                        "metric": "COSINE"
                    }
                }
            }
        },
        "edges": {
            "transfer": {
                "is_directed_edge": true,
                "from_node_type": "Account",
                "to_node_type": "Account",
                "discriminator": "{'date'}",
                "attributes": {
                    "date": {
                        "data_type": "DataType.DATETIME",
                        "default_value": null
                    },
                    "amount": {
                        "data_type": "DataType.INT",
                        "default_value": null
                    }
                }
            },
            "hasPhone": {
                "is_directed_edge": false,
                "from_node_type": "Account",
                "to_node_type": "Phone",
                "discriminator": "set()",
                "attributes": {}
            },
            "isLocatedIn": {
                "is_directed_edge": true,
                "from_node_type": "Account",
                "to_node_type": "City",
                "discriminator": "set()",
                "attributes": {}
            }
        }
    }


## Add Nodes and Edges
### Add Nodes
The following code adds three types of nodes to the graph:

- **Account Nodes:**  
  Each account node is identified by a name and includes two attributes:  
  - `isBlocked`: A boolean indicating if the account is blocked.  
  - `emb1`: A three-dimensional embedding vector.

- **Phone Nodes:**  
  Each phone node is identified by a phone number and has the same attributes as account nodes (`isBlocked` and `emb1`).

- **City Nodes:**  
  Each city node is identified by its name. No additional attributes are required.

For each node type, the code prints the number of nodes inserted.


```python
>>> nodes_for_adding = [
...     ("Scott", {"isBlocked": False, "emb1": [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557]}),
...     ("Jenny", {"isBlocked": False, "emb1": [-0.019265105947852135, 0.0004929182468913496, 0.006711316294968128]}),
...     ("Steven", {"isBlocked": True, "emb1": [-0.01505514420568943, -0.016819344833493233, -0.0221870020031929]}),
...     ("Paul", {"isBlocked": False, "emb1": [0.0011193430982530117, -0.001038988004438579, -0.017158523201942444]}),
...     ("Ed", {"isBlocked": False, "emb1": [-0.003692442551255226, 0.010494389571249485, -0.004631792660802603]}),
... ]
>>> print("Number of Account Nodes Inserted:", G.add_nodes_from(nodes_for_adding, node_type="Account"))

>>> nodes_for_adding = [
...     ("718-245-5888", {"isBlocked": False, "emb1": [0.0023173028603196144, 0.018836047500371933, 0.03107452765107155]}),
...     ("650-658-9867", {"isBlocked": True, "emb1": [0.01969221793115139, 0.018642477691173553, 0.05322211980819702]}),
...     ("352-871-8978", {"isBlocked": False, "emb1": [-0.003442931454628706, 0.016562696546316147, 0.012876809574663639]}),
... ]
>>> print("Number of Phone Nodes Inserted:", G.add_nodes_from(nodes_for_adding, node_type="Phone"))

>>> nodes_for_adding = ["New York", "Gainesville", "San Francisco"]
>>> print("Number of City Nodes Inserted:", G.add_nodes_from(nodes_for_adding, node_type="City"))
```

    Number of Account Nodes Inserted: 5
    Number of Phone Nodes Inserted: 3
    Number of City Nodes Inserted: 3


### Add Edges
Next, we add relationships between nodes by creating edges:

- **hasPhone Edges:**  
  These edges connect account nodes to phone nodes. Each tuple specifies an edge from an account to a phone number.

- **isLocatedIn Edges:**  
  These edges connect account nodes to city nodes. Each tuple specifies an edge from an account to a city.

- **transfer Edges:**  
  These edges connect one account node to another, representing a transfer relationship. Each edge tuple includes additional attributes:  
  - `date`: The date of the transfer.  
  - `amount`: The amount transferred.

For each relationship type, the code prints the number of edges inserted.


```python
>>> ebunch_to_add = [
...     ("Scott", "718-245-5888"),
...     ("Jenny", "718-245-5888"),
...     ("Jenny", "650-658-9867"),
...     ("Paul", "650-658-9867"),
...     ("Ed", "352-871-8978"),
... ]
>>> print("Number of hasPhone Edges Inserted:", G.add_edges_from(ebunch_to_add, "Account", "hasPhone", "Phone"))

>>> ebunch_to_add = [
...     ("Scott", "New York"),
...     ("Jenny", "San Francisco"),
...     ("Steven", "San Francisco"),
...     ("Paul", "Gainesville"),
...     ("Ed", "Gainesville"),
... ]
>>> print("Number of isLocatedIn Edges Inserted:", G.add_edges_from(ebunch_to_add, "Account", "isLocatedIn", "City"))

>>> ebunch_to_add = [
...     ("Scott", "Ed", {"date": "2024-01-04", "amount": 20000}),
...     ("Scott", "Ed", {"date": "2024-02-01", "amount": 800}),
...     ("Scott", "Ed", {"date": "2024-02-14", "amount": 500}),
...     ("Jenny", "Scott", {"date": "2024-04-04", "amount": 1000}),
...     ("Paul", "Jenny", {"date": "2024-02-01", "amount": 653}),
...     ("Steven", "Jenny", {"date": "2024-05-01", "amount": 8560}),
...     ("Ed", "Paul", {"date": "2024-01-04", "amount": 1500}),
...     ("Paul", "Steven", {"date": "2023-05-09", "amount": 20000}),
... ]
>>> print("Number of transfer Edges Inserted:", G.add_edges_from(ebunch_to_add, "Account", "transfer", "Account"))
```

    Number of hasPhone Edges Inserted: 5
    Number of isLocatedIn Edges Inserted: 5
    Number of transfer Edges Inserted: 8


For larger datasets, consider using [load_data](../../reference/01_core/graph/#tigergraphx.core.Graph.load_data) for more efficient handling of large-scale data.

## Exploring Nodes and Edges in the Graph
### Display the Number of Nodes and Edges
You can verify that the data has been inserted into the graph by running the following commands. For this example, the graph contains 11 nodes and 18 edges.


```python
>>> print(G.number_of_nodes())
```

    11



```python
>>> print(G.number_of_edges())
```

    18


### Check if Nodes and Edges Exist
To confirm the presence of specific nodes and edges, you can use the following checks:


```python
>>> print(G.has_node("Scott", "Account"))
```

    True



```python
>>> print(G.has_node("Ed", "Account"))
```

    True



```python
>>> print(G.has_edge("Scott", "Ed", src_node_type="Account", edge_type="transfer", tgt_node_type="Account"))
```

    True


### Display Node and Edge Attributes
#### Node Attributes
To view all attributes of a specific node, you can use:


```python
>>> print(G.nodes[("Account", "Scott")])
```

    {'name': 'Scott', 'isBlocked': False}


To retrieve a particular attribute, you can run:


```python
>>> print(G.nodes[("Account", "Scott")]["isBlocked"])
```

    False


#### Edge Attributes
You can fetch and display all attributes associated with the source-to-target relationship for a specific edge type. If multiple edges exist between the start node and the target node, the result will be a dictionary where each key represents the edge's discriminator, and each value is a dictionary of attribute name-value pairs. If there is only one edge, the result will be a single dictionary of attribute name-value pairs.


```python
>>> edges = G.get_edge_data("Scott", "Ed", src_node_type="Account", edge_type="transfer", tgt_node_type="Account")
>>> for key, value in edges.items():
...     print(key, value)
```

    2024-02-14 00:00:00 {'date': '2024-02-14 00:00:00', 'amount': 500}
    2024-01-04 00:00:00 {'date': '2024-01-04 00:00:00', 'amount': 20000}
    2024-02-01 00:00:00 {'date': '2024-02-01 00:00:00', 'amount': 800}


### Display Node's Vector Attributes
To retrieve a vector attribute for a single node:


```python
>>> vector = G.fetch_node(
...     node_id="Scott",
...     node_type="Account",
...     vector_attribute_name="emb1",
... )
>>> print(vector)
```

    [-0.01773397, -0.01019224, -0.01657188]


For multiple nodes:


```python
>>> vectors = G.fetch_nodes(
...     node_ids=["Scott", "Jenny"],
...     node_type="Account",
...     vector_attribute_name="emb1",
... )
>>> for vector in vectors.items():
...     print(vector)
```

    ('Scott', [-0.01773397, -0.01019224, -0.01657188])
    ('Jenny', [-0.01926511, 0.0004929182, 0.006711317])


### Filter the Nodes
To retrieve nodes that match a specific condition, return only selected attributes, and limit the results:


```python
>>> df = G.get_nodes(
...     node_type="Account",
...     node_alias="s", # "s" is the default value, so you can remove this line
...     filter_expression="s.isBlocked == False",
...     return_attributes=["name", "isBlocked"],
...     limit=2
... )
>>> print(df)
```

        name  isBlocked
    0   Paul      False
    1  Scott      False


### Display the Degree of Nodes
To check the degree (number of connections) of a specific node:


```python
>>> print(G.degree("Ed", "Account"))
```

    6


To filter by edge type:


```python
>>> print(G.degree("Ed", "Account", edge_types="transfer"))
```

    1



```python
>>> print(G.degree("Ed", "Account", edge_types="reverse_transfer"))
```

    3



```python
>>> print(G.degree("Ed", "Account", edge_types="hasPhone"))
```

    1



```python
>>> print(G.degree("Ed", "Account", edge_types="isLocatedIn"))
```

    1


## Graph Traversal
### Retrieve a Node’s Neighbors
Retrieve the first two “Account” nodes that Paul has transferred to, applying a filter to edges where the `amount` is greater than 653. Return the target node’s “name” and “isBlocked” attributes:


```python
>>> df = G.get_neighbors(
...     start_nodes="Paul",
...     start_node_type="Account",
...     start_node_alias="s", # "s" is the default value, so you can remove this line
...     edge_types="transfer",
...     edge_alias="e", # "e" is the default value, so you can remove this line
...     target_node_types="Account",
...     target_node_alias="t", # "t" is the default value, so you can remove this line
...     filter_expression="e.amount > 653",
...     return_attributes=["name", "isBlocked"],
...     limit=2,
... )
>>> print(df)
```

         name  isBlocked
    0  Steven       True


### Breadth-First Search
Below is an example of multi-hop neighbor traversal:


```python
>>> df = G.bfs(
...     start_nodes=["Paul"], 
...     node_type="Account", 
...     edge_types=["transfer", "reverse_transfer"], 
...     max_hops=2
... )
>>> print(df)
```

        name  isBlocked
    1  Scott      False


## Perform Vector Search

### Top-k Vector Search on a Given Vertex Type's Vector Attribute
To find the top 3 most similar accounts to "Scott" based on the embedding, we use the following code. As expected, "Scott" will appear in the list with a distance of 0.


```python
>>> results = G.search(
...     data=[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557],
...     vector_attribute_name="emb1",
...     node_type="Account",
...     limit=2
... )
>>> for result in results:
...     print(result)
```

    {'id': 'Scott', 'distance': 0, 'name': 'Scott', 'isBlocked': False}
    {'id': 'Steven', 'distance': 0.0325563, 'name': 'Steven', 'isBlocked': True}


### Top-k Vector Search on a Set of Vertex Types' Vector Attributes
The code below performs a multi-vector attribute search on "Account" and "Phone" node types using two vector attributes (emb1). It retrieves the top 5 similar nodes and fetches the isBlocked attribute for each result.


```python
>>> results = G.search_multi_vector_attributes(
...     data=[-0.003442931454628706, 0.016562696546316147, 0.012876809574663639],
...     vector_attribute_names=["emb1", "emb1"],
...     node_types=["Account", "Phone"],
...     limit=5,
...     return_attributes_list=[["isBlocked"], ["isBlocked"]]
... )
>>> for result in results:
...     print(result)
```

    {'id': '352-871-8978', 'distance': 1.788139e-07, 'isBlocked': False}
    {'id': '718-245-5888', 'distance': 0.09038806, 'isBlocked': False}
    {'id': '650-658-9867', 'distance': 0.2705743, 'isBlocked': True}
    {'id': 'Ed', 'distance': 0.5047379, 'isBlocked': False}
    {'id': 'Jenny', 'distance': 0.6291004, 'isBlocked': False}


### Top-k Vector Search Using a Vertex Embedding as the Query Vector
This code performs a top-k vector search for similar nodes to a specified node "Scott". It searches within the "Account" node type using the "emb1" embedding attribute and retrieves the top 2 similar node.


```python
>>> G.search_top_k_similar_nodes(
...     node_id="Scott",
...     vector_attribute_name="emb1",
...     node_type="Account",
...     limit=2
... )
```




    [{'id': 'Paul', 'distance': 0.3933879, 'name': 'Paul', 'isBlocked': False},
     {'id': 'Steven', 'distance': 0.0325563, 'name': 'Steven', 'isBlocked': True}]



### Top-k Vector Search with Specified Candidates
This code performs a top-2 vector search on the "Account" node type using the "emb1" embedding attribute. It limits the search to the specified candidate nodes: "Jenny", "Steven", and "Ed".


```python
>>> results = G.search(
...     data=[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557],
...     vector_attribute_name="emb1",
...     node_type="Account",
...     limit=2,
...     candidate_ids=["Jenny", "Steven", "Ed"]
... )
>>> for result in results:
...     print(result)
```

    {'id': 'Steven', 'distance': 0.0325563, 'name': 'Steven', 'isBlocked': True}
    {'id': 'Jenny', 'distance': 0.5804119, 'name': 'Jenny', 'isBlocked': False}


## Filtered Vector Search
Let's first retrieves all "Account" nodes where the isBlocked attribute is False and returns their name attributes in a Pandas DataFrame.


```python
>>> nodes_df = G.get_nodes(
...     node_type="Account",
...     node_alias="s", # The alias "s" is used in filter_expression. You can remove this line since the default node alias is "s"
...     filter_expression='s.isBlocked == False AND s.name != "Ed"',
...     return_attributes=["name"],
... )
>>> print(nodes_df)
```

        name
    0  Scott
    1   Paul
    2  Jenny


Then convert the name column of the retrieved DataFrame into a set of candidate IDs and performs a top-2 vector search on the "Account" node type using the "emb1" embedding attribute, restricted to the specified candidate IDs.


```python
>>> candidate_ids = set(nodes_df['name'])
... results = G.search(
...     data=[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557],
...     vector_attribute_name="emb1",
...     node_type="Account",
...     limit=2,
...     candidate_ids=candidate_ids
... )
>>> for result in results:
...     print(result)
```

    {'id': 'Paul', 'distance': 0.393388, 'name': 'Paul', 'isBlocked': False}
    {'id': 'Scott', 'distance': 0, 'name': 'Scott', 'isBlocked': False}


## Clear and Drop a Graph

### Clear the Graph
To clear the data in the graph without dropping it, use the following code:


```python
>>> print(G.clear())
```

    True


Afterwards, you can confirm that there are no nodes in the graph by checking:


```python
>>> print(G.number_of_nodes())
```

    0


### Drop the Graph
To clear the data and completely remove the graph—including schema, loading jobs, and queries—use the following code:


```python
>>> G.drop_graph()
```

    2025-02-28 14:22:33,656 - tigergraphx.core.managers.schema_manager - INFO - Dropping graph: FinancialGraph...
    2025-02-28 14:22:36,658 - tigergraphx.core.managers.schema_manager - INFO - Graph dropped successfully.

---

Start unlocking the power of graphs with **TigerGraphX** today!

---
# Contact
To contact us for commercial support and purchase, please email us at [info@tigergraph.com](mailto:info@tigergraph.com)

[Go back to top](#top)

