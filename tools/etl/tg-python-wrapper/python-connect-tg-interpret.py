#!/usr/bin/env python

# below we use REST endpoint interpreted_query to send a GSQL query to it.
import requests
import json

#your TigerGraph host machine URL
url = "http://localhost:14240/gsqlserver/interpreted_query"
#your GSQL query
payload = "INTERPRET QUERY (int a=10, int b=3) FOR GRAPH social { start = {person.*}; PRINT start; PRINT a, b; }"
#http request  with user/password, if you changed the default one, please replace them below.
response = requests.request("GET", url, data=payload, auth=('tigergraph', 'tigergraph'))
#convert query result into JSON object
todos = json.loads(response.text)
#print JSON in indent 2 format
print (json.dumps(todos, indent=2))
