#!/bin/bash
### change to raw data file folder
export LDBC_SNB_DATA_DIR=/data/ldbc_snb_data/social_network
### change db name if you want to keep old data
export NEO4J_DB_NAME=ldbc

### somehow LDBC SNB datagen doesn't get any benefit from multithreads. fix it to the single file for each vertex/edge
export LDBC_SNB_DATA_POSTFIX=_0_0.csv
### environment variables for neo4j
export NEO4J_HOME=/home/ubuntu/neo4j-enterprise-4.1.1
export NEO4J_DB_DIR=$NEO4J_HOME/data/databases

# stop neo4j
$NEO4J_HOME/bin/neo4j stop

# change active db and disable password
sed -i -E "s/(#*)(dbms.default_database=)(.+)/\2$NEO4J_DB_NAME/" $NEO4J_HOME/conf/neo4j.conf
sed -i -E "s/(#*)dbms.security.auth_enabled=false/dbms.security.auth_enabled=false/" $NEO4J_HOME/conf/neo4j.conf
# remove old data
rm -rf $NEO4J_DB_DIR/$NEO4J_DB_NAME

# pre-process raw data
num_headers=$(ls ${LDBC_SNB_DATA_DIR} | grep '_header.csv' | wc -l)
if [ $num_headers -eq 0 ]; then
  ./preprocess.sh
elif [ $num_headers -eq 31 ]; then
  echo "Found header files. Skipping pre-processing."
else 
  echo "You have wrong number of header files. Please check ${LDBC_SNB_DATA_DIR}/"
  exit 1
fi

# clear debug.log (for index timing)
if [ -f $NEO4J_HOME/logs/debug.log ]; then
  cat $NEO4J_HOME/logs/debug.log >> $NEO4J_HOME/logs/debug.log.old
  > $NEO4J_HOME/logs/debug.log
fi

# load data
./load_data_4.sh

# print loaded data size (w/o indexes)
set -x
du -bc $NEO4J_HOME/data/databases/$NEO4J_DB_NAME/ | grep total
set +x

# restart neo4j
$NEO4J_HOME/bin/neo4j start

# wait until neo4j fully restarts
printf "Waiting for neo4j... "
is_restarted=""
while [ "$is_restarted" = "" ] 
do
  sleep 1
  is_restarted=$(tail -n 2 $NEO4J_HOME/logs/neo4j.log | grep 'INFO  Started.')
done
echo "restarted"

# to create index, you have to change the default password for db user neo4j
# cat change_passwd.cql | $NEO4J_HOME/bin/cypher-shell -u neo4j -p neo4j > /dev/null

# create indexes
printf "Creating indexes... "
cat create_indexes.cql | $NEO4J_HOME/bin/cypher-shell --non-interactive > /dev/null
echo "now processing in backgound."
echo "Please run 'python3 time_index.py' to check the status."
