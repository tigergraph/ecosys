echo "Load dataset: graph500"
 
echo "Create database: graph500"
arangosh --server.username "root" --server.password "root" --javascript.execute-string "print(db._createDatabase('graph500'))"

# Use --threads 16 for rocksdb storage engine
echo "Load vertex collection ..."
time arangoimp --file graph500-22_unique_node --collection vertex --create-collection true --type tsv --server.password "root" --server.database "graph500" --threads 16

# Use --threads 16 for rocksdb storage engine
echo "Load edge collection ..."
time arangoimp --file graph500-22 --collection edge --create-collection true --type tsv --create-collection-type edge --from-collection-prefix vertex --to-collection-prefix vertex --server.password "root" --server.database "graph500" --threads 16

echo "Load complete!"
