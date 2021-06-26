# Neo4j Benchmark

## Table of Contents
* [Overview](#Overview)
* [Pre-requisite](#Pre-requisite)
* [Load-data](#Load-data)
* [Run](#run)

## Overview
The scripts are modified from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/) and [previous work](https://github.com/zhuang29/graph_database_benchmark/tree/master/neo4j). The queries are directly copied from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/) expect we add some conversion between date and datetime. 


## Pre-requisite
[Neo4j community edition v4.3.0](https://neo4j.com/download-center/#community) must be installed. I downloaded into my laptop and then transfered the package to the server using `scp`. After unpack the pacakge, we add the path to the `.bashrc` or `.profile` so that neo4j commands can be run directly from terminal. 
```sh
#neo4j require openjdk 11
sudo yum install java-11-openjdk-devel
tar -xf neo4j-community-4.3.0-unix.tar
echo 'export NEO4J_HOME='$(pwd)/neo4j-community-4.3.0 >> ~/.bashrc
echo 'export PATH=$PATH:$NEO4J_HOME/bin' >> ~/.bashrc
```
restart your terminal, turn off neo4j password and start neo4j
```sh
neo4j stop
vi $NEO4J_HOME/conf/neo4j.conf
# comment out line 27 : dbms.security.auth_enabled=false
neo4j start
# wait some time
cypher-shell
# to check that no password is required
```
install python dependencies
```sh
sudo pip3 install -U neo4j==4.2.1 python-dateutil
```

### Install additional algorithm packages
Other than the basic neo4j package, the BI 10 query uses `apoc.path.subgraphNodes`, which requires [APOC](https://neo4j.com/labs/apoc/4.1/installation/) package. 
BI 19 uses `gds.shortestPath.dijkstra.stream`, which requires [graph data science (GDS)](https://neo4j.com/docs/graph-data-science/current/installation/) package. 

Download [APOC](https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/4.3.0.0/apoc-4.3.0.0-all.jar) and [GDS](https://s3-eu-west-1.amazonaws.com/com.neo4j.graphalgorithms.dist/graph-data-science/neo4j-graph-data-science-1.6.0-standalone.zip). 

After copying them to `$NEO4J_HOME/plugins`, then add the following text to `$NEO4J_HOME/conf/neo4j.conf`. We also changed the maximum memory size for queries.

*Note: for Neo4j-4.2.x cypher-shell may not work if you install these packages first and then load data. But works if you load data first and then install the packages.*

```
dbms.memory.heap.initial_size=31g
dbms.memory.heap.max_size=31g
dbms.memory.pagecache.size=194000m
dbms.security.procedures.unrestricted=apoc.*,gds.*
dbms.security.procedures.whitelist=apoc.*,gds.*
```

Restart neo4j and go into cypher shell. If the packages are present, you can also check the versions by going to cypher shell and type `RETURN gds.version();` and `RETURN apoc.version();`.
```sh
neo4j restart
```

### Donwload LDBC SNB Data 
LDBC data are available for scale factor [1](https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download), [3](https://surfdrive.surf.nl/files/index.php/s/fY7YocVgsJhmqdT/download), [10](https://surfdrive.surf.nl/files/index.php/s/SY6lRzEzDvvESfJ/download), [30](https://surfdrive.surf.nl/files/index.php/s/dtkgN7ZDT37vOnm/download), [100](https://surfdrive.surf.nl/files/index.php/s/gxNeHFKWVwO0WRm/download). To download data of scale factor 1,

```sh
cd $HOME
yum install zstd
wget -O sf1-composite-projected-fk.tar.zst https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download 
zstd -d sf1-composite-projected-fk.tar.zst 
tar -xvf sf1-composite-projected-fk.tar
```

### load data into cypher
If your dataset has header, you need to first copy the data and remove the header.
```sh
cp -r sf1 sf1-without-header
find sf1-without-header -type f -name \*.csv -exec sed -i 1d '{}' \;
```

The current script `batches.py` can only read `inserts` and `deletes` folder in `$NEO4J_HOME/import/`. The script `load.sh` is modified from [ldbc_bi](https://github.com/ldbc/ldbc_snb_bi/blob/main/cypher/scripts/load-in-one-step.sh). 

```sh
cp -r sf1-without-header/csv/bi/composite-projected-fk/* $NEO4J_HOME/import/
# go back to the repository
cd $HOME/ecosys/ldbc_benchmark/tigergraph/queries_v3/cypher
# Set the CSV_DIR to the parent direcotry of initial_snapshot and load 
export CSV_DIR=$NEO4J_HOME/import
sh load.sh
# wait a minute for neo4j to start
```

## run queries
Currently parameter directory is hard coded in `bi.py`. The script `indices.cypher` create indices for data, which can improve the query speed but not affects the results. `indices.cypher` is required for insertion of edges, otherwise, the the query is extremely slow.
```sh
cypher-shell < indices.cypher
python3 bi.py -q all
```
The script`bi.py` can also specify which query to run, `./bi.py -h` for usage. `/bi.py -q 3` only runs bi3, `./bi.py -q not:19` runs all except bi19. The results are wrote to `./result` folder.

## Refreshes
The script `./batches.py` can do the insertion and deletion. Data path is hard coded in the cypher scripts right now. GSQL results need to be copied here because we use the parameters to run queries. You can skip running queries by specify reading frequency to 0 using `-r 0`.

```sh
cp -r ../results .
python3 batches.py $NEO4J_HOME/import -q not:17,19 #17 19 are too expensive for cypher
```

## Uninstall Neo4j

Sometimes Neo4j cypher-shell may not be stopped. Use the following command to find the process id in the last column as `pid/java`. And kill the process.
```sh
netstat -nlp | grep 7687
kill pid
```

## Run in background
```sh
nohup python3 -u ./driver.py all ~/sf1/csv/bi/composite-projected-fk/ > foo.out 2>&1 < /dev/null &  
```
