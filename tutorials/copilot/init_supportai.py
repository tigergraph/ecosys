import os
import json
from pyTigerGraph import TigerGraphConnection

def create_graph(conn: TigerGraphConnection):
    conn.gsql(f"""CREATE GRAPH {conn.graphname}()""")
    conn.ai.initializeSupportAI()

def load_data(conn: TigerGraphConnection):
    load_job = """CREATE LOADING JOB load_documents_content_as_json {
    DEFINE FILENAME DocumentContent;
    LOAD DocumentContent TO TEMP_TABLE tc (doc_id, doc_type, content) VALUES (flatten_json_array($0, $"doc_id", $"doc_type", $"content")) USING SEPARATOR="|||||||||||";

    LOAD TEMP_TABLE tc TO VERTEX Document VALUES($"doc_id", gsql_current_time_epoch(0), _, _);
    LOAD TEMP_TABLE tc TO VERTEX Content VALUES($"doc_id", $"doc_type", $"content", gsql_current_time_epoch(0));
    LOAD TEMP_TABLE tc TO EDGE HAS_CONTENT VALUES($"doc_id" Document, $"doc_id" Content);
    }"""
    
    conn.gsql(f"USE GRAPH {conn.graphname}\n{load_job}")
    conn.runLoadingJobWithFile("./data/tg_tutorials.jsonl", "DocumentContent", "load_documents_content_as_json", sep="|||||||||||||")

def update_graphrag(conn: TigerGraphConnection):
    conn.ai.forceConsistencyUpdate(method="graphrag")

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

    # And then add CoPilot's address to the connection. This address
    # is the host's address where the CoPilot container is running.
    conn.ai.configureCoPilotHost(f"{conn.host}:8000")

    create_graph(conn)
    load_data(conn)
    update_graphrag(conn)
