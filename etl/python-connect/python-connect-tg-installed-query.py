import requests
import json

#url of the REST end point of the installed query hello
# It is in this general format http://server_ip:9000/query/graphname/queryname
url ="http://localhost:9000/query/social/hello"
#query parameters
params = {"p": "Tom"}
#invoke the REST call
response = requests.request("GET", url,  params=params, auth=('tigergraph', 'tigergraph'))
#put the response in a JSON object
todos = json.loads(response.text)
#output the JSON
print (json.dumps(todos, indent=2))
