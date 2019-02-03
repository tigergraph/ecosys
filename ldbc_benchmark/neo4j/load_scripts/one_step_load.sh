#!/bin/bash
. ./env_vars.sh

### if preprocessing is already done, you can skip it (0:run; 1:skip)
skip_preprocess=0

# stop neo4j
$NEO4J_HOME/bin/neo4j stop

# change active db
sed -i -E "s/(#*)(dbms.active_database=)(.+)/\2$NEO4J_DB_NAME/" $NEO4J_HOME/conf/neo4j.conf

# remove old data
rm -rf $NEO4J_DB_DIR/$NEO4J_DB_NAME

# preprocess raw data
if [ $skip_preprocess -eq 0 ] 
then
  ./preprocess.sh
fi

# clear debug.log
if [ -f $NEO4J_HOME/logs/debug.log ]; then
  cat $NEO4J_HOME/logs/debug.log >> $NEO4J_HOME/logs/debug.log.old
  > $NEO4J_HOME/logs/debug.log
fi

# load data
./load_data.sh

# print loaded data size (w/o indexes)
set -x
du -bc $NEO4J_HOME/data/databases/$NEO4J_DB_NAME/ | grep total
set +x

# restart neo4j
$NEO4J_HOME/bin/neo4j start

# wait until neo4j fully restarts
is_restarted=""
while [ "$is_restarted" = "" ] 
do
  sleep 1
  is_restarted=$(tail -n 2 $NEO4J_HOME/logs/neo4j.log | grep 'INFO  Started.')
done

# to create index, you have to change the default password for db user neo4j
cat change_passwd.cql | $NEO4J_HOME/bin/cypher-shell -u neo4j -p neo4j > /dev/null

# create indexes
cat create_indexes.cql | $NEO4J_HOME/bin/cypher-shell -u neo4j -p tigergraph --non-interactive > /dev/null
