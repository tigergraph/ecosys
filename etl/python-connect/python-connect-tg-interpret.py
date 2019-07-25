#!/usr/bin/env python

# below we use REST endpoint interpreted_query to send a GSQL query to it.
import requests
import json

url = "http://localhost:14240/gsqlserver/interpreted_query"
payload = "INTERPRET QUERY (int a, int b) FOR GRAPH social { start = {person.*}; PRINT start; print a, b; }"
params = {"a": 10, "b": 4}
response = requests.request("POST", url, data=payload, params=params, auth=('tigergraph', 'tigergraph'))
todos = json.loads(response.text)
print (json.dumps(todos, indent=2))
