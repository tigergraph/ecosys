# TigerGraphX Quick Start: Using TigerGraph for Graph and Vector Database

In this quick start guide, we will work with the following graph:

![Financial Graph](https://raw.githubusercontent.com/tigergraph/ecosys/master/demos/guru_scripts/docker/tutorial/4.x/FinancialGraph.jpg)

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
graph_schema = {
    "graph_name": "FinancialGraph",
    "nodes": {
        "Account": {
            "primary_key": "name",
            "attributes": {
                "name": "STRING",
                "isBlocked": "BOOL",
            },
            "vector_attributes": {"emb1": 3},
        },
        "City": {
            "primary_key": "name",
            "attributes": {
                "name": "STRING",
            },
        },
        "Phone": {
            "primary_key": "number",
            "attributes": {
                "number": "STRING",
                "isBlocked": "BOOL",
            },
            "vector_attributes": {"emb1": 3},
        },
    },
    "edges": {
        "transfer": {
            "is_directed_edge": True,
            "from_node_type": "Account",
            "to_node_type": "Account",
            "discriminator": "date",
            "attributes": {
                "date": "DATETIME",
                "amount": "INT",
            },
        },
        "hasPhone": {
            "is_directed_edge": False,
            "from_node_type": "Account",
            "to_node_type": "Phone",
        },
        "isLocatedIn": {
            "is_directed_edge": True,
            "from_node_type": "Account",
            "to_node_type": "City",
        },
    },
}
```

### Define the TigerGraph Connection Configuration
In addition to defining the schema, a connection configuration is necessary to establish communication with the TigerGraph server.


```python
connection = {
    "host": "http://127.0.0.1",
    "username": "tigergraph",
    "password": "tigergraph",
}
```

### Create a Graph
Running the following command will create a graph using the user-defined schema if it does not already exist. If the graph exists, the command will return the existing graph. To overwrite the existing graph, set the drop_existing_graph parameter to True. Note that creating the graph may take several seconds.


```python
from tigergraphx import Graph
G = Graph(graph_schema, connection)
```

    2025-01-08 22:18:40,473 - tigergraphx.core.graph.base_graph - INFO - Creating schema for graph FinancialGraph...
    2025-01-08 22:18:40,503 - tigergraphx.core.graph.base_graph - INFO - Schema created successfully.


## Add Nodes and Edges
*Note*: This example demonstrates how to easily add nodes and edges using the API. However, adding nodes and edges individually may not be efficient for large-scale operations. For better performance when loading data into TigerGraph, it is recommended to use a loading job. Nonetheless, these examples are ideal for quickly getting started.
### Add Nodes


```python
nodes_for_adding = [
    ("Scott", {"isBlocked": False, "emb1": [-0.017733968794345856, -0.01019224338233471, -0.016571875661611557]}),
    ("Jenny", {"isBlocked": False, "emb1": [-0.019265105947852135, 0.0004929182468913496, 0.006711316294968128]}),
    ("Steven", {"isBlocked": True, "emb1": [-0.01505514420568943, -0.016819344833493233, -0.0221870020031929]}),
    ("Paul", {"isBlocked": False, "emb1": [0.0011193430982530117, -0.001038988004438579, -0.017158523201942444]}),
    ("Ed", {"isBlocked": False, "emb1": [-0.003692442551255226, 0.010494389571249485, -0.004631792660802603]}),
]
print("Number of Account Nodes Inserted:", G.add_nodes_from(nodes_for_adding, node_type="Account"))

nodes_for_adding = [
    ("718-245-5888", {"isBlocked": False, "emb1": [0.0023173028603196144, 0.018836047500371933, 0.03107452765107155]}),
    ("650-658-9867", {"isBlocked": True, "emb1": [0.01969221793115139, 0.018642477691173553, 0.05322211980819702]}),
    ("352-871-8978", {"isBlocked": False, "emb1": [-0.003442931454628706, 0.016562696546316147, 0.012876809574663639]}),
]
print("Number of Phone Nodes Inserted:", G.add_nodes_from(nodes_for_adding, node_type="Phone"))

nodes_for_adding = ["New York", "Gainesville", "San Francisco"]
print("Number of City Nodes Inserted:", G.add_nodes_from(nodes_for_adding, node_type="City"))
```

    Number of Account Nodes Inserted: 5
    Number of Phone Nodes Inserted: 3
    Number of City Nodes Inserted: 3


### Add Edges


```python
ebunch_to_add = [
    ("Scott", "718-245-5888"),
    ("Jenny", "718-245-5888"),
    ("Jenny", "650-658-9867"),
    ("Paul", "650-658-9867"),
    ("Ed", "352-871-8978"),
]
print("Number of hasPhone Edges Inserted:", G.add_edges_from(ebunch_to_add, "Account", "hasPhone", "Phone"))

ebunch_to_add = [
    ("Scott", "New York"),
    ("Jenny", "San Francisco"),
    ("Steven", "San Francisco"),
    ("Paul", "Gainesville"),
    ("Ed", "Gainesville"),
]
print("Number of isLocatedIn Edges Inserted:", G.add_edges_from(ebunch_to_add, "Account", "isLocatedIn", "City"))

ebunch_to_add = [
    ("Scott", "Ed", {"date": "2024-01-04", "amount": 20000}),
    ("Scott", "Ed", {"date": "2024-02-01", "amount": 800}),
    ("Scott", "Ed", {"date": "2024-02-14", "amount": 500}),
    ("Jenny", "Scott", {"date": "2024-04-04", "amount": 1000}),
    ("Paul", "Jenny", {"date": "2024-02-01", "amount": 653}),
    ("Steven", "Jenny", {"date": "2024-05-01", "amount": 8560}),
    ("Ed", "Paul", {"date": "2024-01-04", "amount": 1500}),
    ("Paul", "Steven", {"date": "2023-05-09", "amount": 20000}),
]
print("Number of transfer Edges Inserted:", G.add_edges_from(ebunch_to_add, "Account", "transfer", "Account"))
```

    Number of hasPhone Edges Inserted: 5
    Number of isLocatedIn Edges Inserted: 5
    Number of transfer Edges Inserted: 8


### Display the Number of Nodes
Next, let's verify that the data has been inserted into the graph by using the following command. As expected, the number of nodes is 5.


```python
print(G.number_of_nodes())
```

    11


## Perform Vector Search

### Top-k Vector Search on a Given Vertex Type's Vector Attribute
To find the top 3 most similar accounts to "Scott" based on the embedding, we use the following code. As expected, "Scott" will appear in the list with a distance of 0.


```python
results = G.search(
    data=[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557],
    vector_attribute_name="emb1",
    node_type="Account",
    limit=3
)
for result in results:
    print(result)
```

    {'id': 'Paul', 'distance': 0.393388, 'name': 'Paul', 'isBlocked': False}
    {'id': 'Scott', 'distance': 0, 'name': 'Scott', 'isBlocked': False}
    {'id': 'Steven', 'distance': 0.0325563, 'name': 'Steven', 'isBlocked': True}


After performing the vector search, the following code retrieves the detailed embeddings of the top-k nodes identified in the search. This is achieved by using their IDs and the specified vector attribute. The results are then printed for each node.


```python
node_ids = {item['id'] for item in results}
nodes = G.fetch_nodes(node_ids, vector_attribute_name="emb1", node_type="Account")
for node in nodes.items():
    print(node)
```

    ('Paul', [0.001119343, -0.001038988, -0.01715852])
    ('Scott', [-0.01773397, -0.01019224, -0.01657188])
    ('Steven', [-0.01505514, -0.01681934, -0.022187])


### Top-k Vector Search on a Set of Vertex Types' Vector Attributes
The code below performs a multi-vector attribute search on "Account" and "Phone" node types using two vector attributes (emb1). It retrieves the top 5 similar nodes and fetches the isBlocked attribute for each result.


```python
results = G.search_multi_vector_attributes(
    data=[-0.003442931454628706, 0.016562696546316147, 0.012876809574663639],
    vector_attribute_names=["emb1", "emb1"],
    node_types=["Account", "Phone"],
    limit=5,
    return_attributes_list=[["isBlocked"], ["isBlocked"]]
)
for result in results:
    print(result)
```

    {'id': '352-871-8978', 'distance': 1.788139e-07, 'isBlocked': False}
    {'id': '718-245-5888', 'distance': 0.09038806, 'isBlocked': False}
    {'id': '650-658-9867', 'distance': 0.2705743, 'isBlocked': True}
    {'id': 'Ed', 'distance': 0.5047379, 'isBlocked': False}
    {'id': 'Jenny', 'distance': 0.6291004, 'isBlocked': False}


### Top-k Vector Search Using a Vertex Embedding as the Query Vector
This code performs a top-k vector search for similar nodes to a specified node "Scott". It searches within the "Account" node type using the "emb1" embedding attribute and retrieves the top 2 similar node.


```python
G.search_top_k_similar_nodes(
    node_id="Scott",
    vector_attribute_name="emb1",
    node_type="Account",
    limit=2
)
```

    [{'id': 'Paul', 'distance': 0.3933879, 'name': 'Paul', 'isBlocked': False},
     {'id': 'Steven', 'distance': 0.0325563, 'name': 'Steven', 'isBlocked': True}]



### Top-k Vector Search with Specified Candidates
This code performs a top-2 vector search on the "Account" node type using the "emb1" embedding attribute. It limits the search to the specified candidate nodes: "Jenny", "Steven", and "Ed".


```python
results = G.search(
    data=[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557],
    vector_attribute_name="emb1",
    node_type="Account",
    limit=2,
    candidate_ids=["Jenny", "Steven", "Ed"]
)
for result in results:
    print(result)
```

    {'id': 'Steven', 'distance': 0.0325563, 'name': 'Steven', 'isBlocked': True}
    {'id': 'Jenny', 'distance': 0.5804119, 'name': 'Jenny', 'isBlocked': False}


## Filtered Vector Search
Let's first retrieves all "Account" nodes where the isBlocked attribute is False and returns their name attributes in a Pandas DataFrame.


```python
nodes_df = G.get_nodes(
    node_type="Account",
    node_alias="s", # The alias "s" is used in filter_expression. You can remove this line since the default node alias is "s"
    filter_expression='s.isBlocked == False AND s.name != "Ed"',
    return_attributes=["name"],
)
print(nodes_df)
```

        name
    0  Scott
    1   Paul
    2  Jenny


Then convert the name column of the retrieved DataFrame into a set of candidate IDs and performs a top-2 vector search on the "Account" node type using the "emb1" embedding attribute, restricted to the specified candidate IDs.


```python
candidate_ids = set(nodes_df['name'])
results = G.search(
    data=[-0.017733968794345856, -0.01019224338233471, -0.016571875661611557],
    vector_attribute_name="emb1",
    node_type="Account",
    limit=2,
    candidate_ids=candidate_ids
)
for result in results:
    print(result)
```

    {'id': 'Scott', 'distance': 0, 'name': 'Scott', 'isBlocked': False}
    {'id': 'Paul', 'distance': 0.393388, 'name': 'Paul', 'isBlocked': False}


## Clear and Drop a Graph

### Clear the Graph
To clear the data in the graph without dropping it, use the following code:


```python
print(G.clear())
```

    True


Afterwards, you can confirm that there are no nodes in the graph by checking:


```python
print(G.number_of_nodes())
```

    0


### Drop the Graph
To clear the data and completely remove the graph—including schema, loading jobs, and queries—use the following code:


```python
G.drop_graph()
```

---

Start unlocking the power of graphs with **TigerGraphX** today!
