
if [ "$1" = "graph500" ] &&  [ "$2" = "pagerank" ]
then
    echo "Running PageRank on graph500 ..."
    nohup arangosh --server.database "graph500" --server.username "root" --server.password "root" --javascript.execute pg_graph500.js
elif [ "$1" = "twitter" ] &&  [ "$2" = "pagerank" ]
then
    echo "Running PageRank on twitter ..."
    nohup arangosh --server.database "twitter" --server.username "root" --server.password "root" --javascript.execute pg_twitter.js
elif [ "$1" = "graph500" ] &&  [ "$2" = "wcc" ]
then
    echo "Running WCC on graph500 ..."
    nohup arangosh --server.database "graph500" --server.username "root" --server.password "root" --javascript.execute wcc_graph500.js
elif [ "$1" = "twitter" ] &&  [ "$2" = "wcc" ]
then
    echo "Running WCC on twitter ..."
    nohup arangosh --server.database "twitter" --server.username "root" --server.password "root" --javascript.execute wcc_twitter.js
else
   echo "Please provide graph name (graph500, twitter) AND  algorithm name (pagerank, wcc)"
fi
