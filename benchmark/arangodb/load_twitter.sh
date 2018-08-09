#!/bin/sh
echo "Load dataset: twitter"

echo "Create database: twitter"
arangosh --server.username "root" --server.password "root" --javascript.execute-string "print(db._createDatabase('twitter'))"

echo "Load vertex collection ..."
time arangoimp --file twitter_rv.net_unique_node --collection vertex --create-collection true --type tsv --server.password "root" --server.database "twitter" --threads 16

echo "Load edge collection ..."
# timeouts set in seconds
time arangoimp --file twitter_rv.net --collection edge --create-collection true --type tsv --create-collection-type edge --from-collection-prefix vertex --to-collection-prefix vertex --server.password "root" --server.database "twitter" --server.connection-timeout 86400 --server.request-timeout 86400 --threads 16

echo "Load complete!"
