import os
import json
from pyTigerGraph import TigerGraphConnection

def create_graph(conn: TigerGraphConnection):
    conn.gsql(f"""CREATE GRAPH {conn.graphname}()""")
    conn.ai.initializeSupportAI()

def load_data(conn: TigerGraphConnection):
    res = conn.ai.createDocumentIngest(
        data_source="local",
        data_source_config={"data_path": "./data/tg_tutorials.jsonl"},
        loader_config={"doc_id_field": "doc_id", "content_field": "content", "doc_type": "markdown"},
        file_format="json",
    )
    conn.ai.runDocumentIngest(res["load_job_id"], res["data_source_id"], res["data_path"])

def update_graphrag(conn: TigerGraphConnection):
    conn.ai.forceConsistencyUpdate("graphrag")

if __name__ == "__main__":
    with open("./configs/db_config") as cfg:
        config = json.load(cfg)

    # We first create a connection to the database
    conn = TigerGraphConnection(
        host=config["hostname"],
        username=config["username"],
        password=config["password"],
        restppPort=config["restppPort"],
    )
    conn.graphname = "TigerGraphRAG"

    # And then add GraphRAG's address to the connection. This address
    # is the host's address where the GraphRAG container is running.
    conn.ai.configureGraphRAGHost(f"{conn.host}:8000")

    create_graph(conn)
    load_data(conn)
    update_graphrag(conn)
