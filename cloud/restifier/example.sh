echo get user profile
curl --user tigergraph:tigergraph localhost:8080/api/user/profile

echo create user
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/user/create \
    --data '{"username": "test", "password": "test"}'

echo grant superuser
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/user/grant \
    --data '{"users": ["test"], "role": "superuser"}'

echo revoke superuser
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/user/revoke \
    --data '{"users": ["test"], "role": "superuser"}'

echo create vertex
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"command": "CREATE VERTEX person (PRIMARY_ID name STRING, name STRING, age INT, gender STRING, state STRING)"}'

echo create edge
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"command": "CREATE UNDIRECTED EDGE friendship (FROM person, TO person, connect_day DATETIME)"}'

echo create graph
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"command": "CREATE GRAPH social (person, friendship)"}'

echo grant superuser
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/user/grant \
    --data '{"users": ["test"], "role": "queryreader", "graph": "social"}'

echo revoke superuser
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/user/revoke \
    --data '{"users": ["test"], "role": "queryreader", "graph": "social"}'

echo create query
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"graph": "social", "command":"CREATE QUERY hello(VERTEX<person> p) FOR GRAPH social{\n  Start = {p};\n  Result = SELECT tgt\n           FROM Start:s-(friendship:e) ->person:tgt;\n  PRINT Result;\n}\n"}'

echo install query
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"graph": "social", "command":"install query hello"}'

echo drop query
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"graph": "social", "command":"drop query hello"}'

echo drop graph
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"command": "drop GRAPH social"}'

echo drop edge
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"command": "drop EDGE friendship"}'

echo drop vertex
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/gsqlcmd \
    --data '{"command": "drop VERTEX person"}'

echo drop user
curl --user tigergraph:tigergraph \
    -H "content-type: application/json" \
    -X POST localhost:8080/api/user/drop \
    --data '{"username": "test"}'

