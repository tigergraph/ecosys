# Neo4j Benchmark

## Table of Contents
* [Overview](#Overview)
* [Pre-requisite](#Pre-requisite)
* [Load-data](#Load-data)
* [Run](#run)

## Overview
The scripts are modified from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/) and [previous work](https://github.com/zhuang29/graph_database_benchmark/tree/master/neo4j). The queries are directly copied from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/) expect we add some conversion between date and datetime. 


## Pre-requisite
[Neo4j community edition v4.2.7](https://neo4j.com/download-center/#community) must be installed. I downloaded into my laptop and then transfered the package to the server using `scp`. After unpack the pacakge, we add the path to the `.bashrc` or `.profile` so that neo4j commands can be run directly from terminal. 
```sh
tar -xf neo4j-community-4.2.7-unix.tar
cat "export $NEO4J_HOME=$(pwd)/neo4j-community-4.2.7" >> ~/.bashrc
cat "export $PATH=$PATH:$NEO4J_HOME/bin" >> ~/.bashrc
```
restart your terminal, turn off neo4j password and start neo4j
```sh
neo4j stop
vi $NEO4J_HOME/conf/neo4j.conf
# comment out line 26 : dbms.security.auth_enabled=false
neo4j start
# wait some time
cypher-shell
# to check that no password is required
```
install python dependencies
```sh
pip3 install -U neo4j==4.2.1 python-dateutil
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
The script `load.sh` is modified from [ldbc_bi](https://github.com/ldbc/ldbc_snb_bi/blob/main/cypher/scripts/load-in-one-step.sh). The script removes the header of csv files and load the header in `./headers` and all the csv files in `initial_snapshot`. Since the data is modified, you may make a copy of the dataset before loading.
```sh
# Set the CSV_DIR to the parent direcotry of initial_snapshot 
export CSV_DIR=$HOME/sf1/csv/bi/composite-projected-fk/
# make a copy of the data
cp -r sf1 sf1-copy
# Remove the header of csv files. Only needed for the first time loading !!!
for t in static dynamic ; do
    for d in $(ls $CSV_DIR/initial_snapshot/$t); do
        for f in $(ls $CSV_DIR/initial_snapshot/$t/$d/*.csv); do
            tail -n +2 $f > $f.tmp && mv $f.tmp $f 
            #echo $f
        done
    done
done 
echo "Done remove the header of csv files"
# load data
sh load.sh
```

### Another method to load date (deprecated)
If you already followed the Tigergraph instruction, you should have initial_shot at the `$HOME` direcotry. 
```sh
export POSTFIX=.csv
# directory of initial_snapshot
export RAW_DIR=$HOME/initial_snapshot
# we concatenate the csv data and store to $CSV_DIR 
export CSV_DIR=$HOME/ldbc_data 
cd ecosys/ldbc_benchmark/tigergraph/queries_v3/cypher
sh load-old.sh 
```

### Install additional algorithm packages
Other than the basic neo4j package, the BI 10 query uses `apoc.path.subgraphNodes`, which requires [APOC](https://neo4j.com/labs/apoc/4.1/installation/) package. 
BI 19 uses `gds.shortestPath.dijkstra.stream`, which requires [graph data science (GDS)](https://neo4j.com/docs/graph-data-science/current/installation/) package. 

Download [APOC](https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/4.2.0.4/apoc-4.2.0.4-core.jar) and [GDS](https://s3-eu-west-1.amazonaws.com/com.neo4j.graphalgorithms.dist/graph-data-science/neo4j-graph-data-science-1.6.0-standalone.zip). 

After moving them to `$NEO4J_HOME/plugins`, then add the following text to `$NEO4J_HOME/conf/neo4j.conf`. We also changed the maximum memory size for queries.
```
dbms.memory.heap.initial_size=31g
dbms.memory.heap.max_size=31g
dbms.memory.pagecache.size=194000m
dbms.security.procedures.unrestricted=apoc.*,gds.*
dbms.security.procedures.whitelist=apoc.*,gds.*
```

Restart neo4j and go into cypher shell
```sh
neo4j restart
#wait some time
cypher-shell
# in cypher shell 
>> RETURN gds.version(); #to show  gds version (the ending semicolon is required)
>> RETURN apoc.version(); #to show  gds version (the ending semicolon is required)
>> CALL gds.list(); #to install the package
>> :exit
```


## run queries
Currently parameter directory is hard coded in the file.
```sh
cypher-shell < indices.cypher
python3 bi.py
```
The script`bi.py` can also specify which query to run, `./bi.py -h` for usage. `/bi.py -q 3` only runs bi3, `./bi.py -q not:19` runs all except bi19. The results are wrote to `./result` folder.


