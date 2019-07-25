#!/usr/bin/env python

# below we use REST endpoint interpreted_query to send a GSQL query to it.
import requests
import json

#your TigerGraph host machine URL
url = "http://localhost:14240/gsqlserver/interpreted_query"
#your GSQL query
payload = "INTERPRET QUERY (int a, int b) FOR GRAPH social { start = {person.*}; PRINT start; PRINT a, b; }"
#query parameters
params = {"a": 10, "b": 4}
#http request  with user/password
response = requests.request("GET", url, data=payload, params=params, auth=('tigergraph', 'tigergraph'))
#convert query result into JSON object
todos = json.loads(response.text)
#print JSON in indent 2 format
print (json.dumps(todos, indent=2))
