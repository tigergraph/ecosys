import os
from pyTigerGraph import TigerGraphConnection

host = "http://localhost"

username = "tigergraph"
password = "tigergraph"

# We first create a connection to the database
conn = TigerGraphConnection(
    host=host,
    username=username, 
    password=password,
    restppPort=14240
    )

conn.graphname = "SupportAIDemo"

# And then add CoPilot's address to the connection. This address
# is the host's address where the CoPilot container is running.
conn.ai.configureCoPilotHost(f"{host}:8000")

query = "How do I get the vertex count from TigerGrpah using Python?"
print(f"""Fetching answer for question: {query}""")

resp = conn.ai.answerQuestion(
    query,
    method="hnswoverlap",
    method_parameters = {
        "indices": ["Document", "DocumentChunk", "Entity", "Relationship"],
        "top_k": 2,
        "num_hops": 2,
        "num_seen_min": 2,
        "verbose": True
    })

print(f"""\nAnswer using HNSW_Overlap:\n{resp["response"]}""")

resp = conn.ai.answerQuestion(
    query,
    method="graphrag",
    method_parameters={
        "community_level": 2,
        "combine": False,
        "top_k": 5,
        "verbose": True
    })

print(f"""\nAnswer using GraphRAG:\n{resp["response"]}""")
