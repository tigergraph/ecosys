import os
import json
from pyTigerGraph import TigerGraphConnection

def create_graph(conn: TigerGraphConnection):
    conn.gsql(f"""CREATE GRAPH {conn.graphname}()""")
    conn.ai.initializeSupportAI()

def load_data(conn: TigerGraphConnection):
    load_job = """CREATE LOADING JOB load_documents_content_json_first_load {
        DEFINE FILENAME DocumentContent;
        LOAD DocumentContent TO VERTEX Document VALUES($"url", gsql_current_time_epoch(0), _, _) USING JSON_FILE="true";
        LOAD DocumentContent TO VERTEX Content VALUES($"url", $"content", gsql_current_time_epoch(0)) USING JSON_FILE="true";
        LOAD DocumentContent TO EDGE HAS_CONTENT VALUES($"url" Document, $"url" Content) USING JSON_FILE="true";
    }"""
    
    conn.gsql(f"USE GRAPH {conn.graphname}\n{load_job}")
    conn.runLoadingJobWithFile("./data/pytg_current.jsonl", "DocumentContent", "load_documents_content_json_first_load")

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
    conn.graphname = "SupportAIDemo"

    # And then add CoPilot's address to the connection. This address
    # is the host's address where the CoPilot container is running.
    conn.ai.configureCoPilotHost(f"{conn.host}:8000")

    create_graph(conn)
    load_data(conn)
    update_graphrag(conn)
