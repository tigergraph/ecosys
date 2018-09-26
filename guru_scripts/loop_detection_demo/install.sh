#!bin/bash
set -e
echo "Start download data from service.tigergraph.com"
echo
cd ./loading_scripts/
wget http://service.tigergraph.com/download/webinar/transactions.tar.gz
tar -xzf transactions.tar.gz
echo "Start GSQL system"
echo
gadmin start

echo "Start creating schema and loading"
echo
gdev=$(grep gdev ~/.gsql/gsql.cfg | cut -d " " -f 2)
cp -r ./TokenBank $gdev/gdk/gsql/src/
gsql schema.gsql
gsql loader.gsql

echo "Start installing queries"
echo
cd ../queries
bash install.sh
cd -

echo
echo "Finished, please run your queries."
echo
