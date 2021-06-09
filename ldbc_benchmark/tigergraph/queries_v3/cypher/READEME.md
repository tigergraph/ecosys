# Neo4j benchmark

The scripts are based for LDBC_SNB v0.4.0 data, and is modified from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/) and [previous work](https://github.com/zhuang29/graph_database_benchmark/tree/master/neo4j). The queries are directly copied from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/).

## Install Cypher
Download tarball of [Neo4j community edition](https://neo4j.com/download-center/#community)
```sh
tar -xf neo4j-community-4.2.7-unix.tar
cat "export $NEO4J_HOME=$(pwd)/neo4j-community-4.2.7" >> ~/.bashrc
cat "export $PATH=$PATH:$NEO4J_HOME/bin" >> ~/.bashrc
```
restart your terminal
```sh
#start neo4j
$NEO4J_HOME/bin/neo4j console
```


## To load data into cypher 
```sh
# load environmental-variables.sh
source ./environmental-variables.sh

# Supposing the data is in ~/initial_snapshot, go to home and run load.sh
cdir=$(pwd)
cd ~
sh $cdir/load.sh 
```

## install python driver
sudo python3 -m pip install neo4j

## turn off neo4j password
```sh
neo4j stop
vi $NEO4J_HOME/conf/neo4j.conf
# comment out line 26 : dbms.security.auth_enabled=false
neo4j start
cypher-shell
# no password is required
```

 




