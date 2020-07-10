## ------- Run Built-in Queries -------

# Get vertex cardinality
echo "Get VERTEX cardinality"
echo "-----------------------------------------------------------------------------"
curl -X POST 'http://localhost:9000/builtins/social' -d  '{"function":"stat_vertex_number","type":"*"}'  | jq .
echo "============================================================================="


# Get edge cardinality
echo "Get EDGE cardinality"
echo "-----------------------------------------------------------------------------"
curl -X POST 'http://localhost:9000/builtins/social' -d  '{"function":"stat_edge_number","type":"*"}' | jq .
echo "============================================================================="


# Example. Find a person vertex whose primary_id is "Tom".
echo "Example. Find a person vertex whose primary_id is \"Tom\"."
echo "-----------------------------------------------------------------------------"
curl -X GET "http://localhost:9000/graph/social/vertices/person/Tom" | jq .
echo "============================================================================="


# Example. Find all friendship edges whose source vertex's primary_id is "Tom".
echo "Example. Find all friendship edges whose source vertex's primary_id is \"Tom\"."
echo "-----------------------------------------------------------------------------"
curl -X GET "http://localhost:9000/graph/social/edges/person/Tom/friendship/" | jq .
echo "============================================================================="