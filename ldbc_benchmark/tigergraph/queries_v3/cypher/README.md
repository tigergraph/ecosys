# Neo4j benchmark

The scripts are based for LDBC_SNB v0.4.0 data, and is modified from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/) and [previous work](https://github.com/zhuang29/graph_database_benchmark/tree/master/neo4j). The queries are directly copied from [ldbc/ldbc_snb_bi](https://github.com/ldbc/ldbc_snb_bi/tree/main/cypher/).

## Install Cypher
Download tarball of [Neo4j community edition](https://neo4j.com/download-center/#community). I downloaded into my laptop and then transfered the package to the server using `scp`.
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
## turn off neo4j password
```sh
neo4j stop
vi $NEO4J_HOME/conf/neo4j.conf
# comment out line 26 : dbms.security.auth_enabled=false
neo4j start
# wait some time
cypher-shell
# no password is required
```

## install python dependencies
```sh
pip3 install -U neo4j==4.2.1 python-dateutil
```


## Donwload LDBC SNB Data 
LDBC data are available for scale factor 1(https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download), 3(https://surfdrive.surf.nl/files/index.php/s/fY7YocVgsJhmqdT/download), 10(https://surfdrive.surf.nl/files/index.php/s/SY6lRzEzDvvESfJ/download), 30(https://surfdrive.surf.nl/files/index.php/s/dtkgN7ZDT37vOnm/download), 100(https://surfdrive.surf.nl/files/index.php/s/gxNeHFKWVwO0WRm/download). To download data of scale factor 1,

```sh
cd $HOME
yum install zstd
wget -O sf1-composite-projected-fk.tar.zst https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download 
zstd -d sf1-composite-projected-fk.tar.zst 
tar -xvf sf1-composite-projected-fk.tar
```

## load data into cypher
The script `load.sh` is modified from [ldbc_bi][https://github.com/ldbc/ldbc_snb_bi/blob/main/cypher/scripts/load-in-one-step.sh]. The script removes the header of csv files and load the header in `.headers/` and all the csv files in `initial_snapshot`. Since the data is modified, you may make a copy of the dataset before loading.
```sh
# Set the CSV_DIR to the direcotry of initial_snapshot
export CSV_DIR=$HOME/sf1/csv/bi/composite-projected-fk/
# Remove the header. Only needed for the first time loading !!!
for t in static dynamic ; do
    for d in $(ls $CSV_DIR/initial_snapshot/$t); do
        for f in $(ls $CSV_DIR/initial_snapshot/$t/$d/*.csv); do
            tail -n +2 $f > $f.tmp && mv $f.tmp $f 
            #echo $f
        done
    done
done 
echo "Done remove the header of csv files"

# load the data
sh load.sh 
```


### load data into cypher (Old)
This old version concatenate all csv files into one CSV and then replace the header and load.
```sh
export POSTFIX=.csv
export NEO4J_CONTAINER_NAME="neo-snb"
# directory of initial_snapshot
export RAW_DIR=$HOME/initial_snapshot
# we concatenate the csv data and store to $CSV_DIR 
export CSV_DIR=$HOME/ldbc_data 
# Supposing the data is in ~/initial_snapshot, go to home and run load.sh
cdir=$(pwd)
sh load-old.sh 
```


## install additional algorithm packages
Other than the basic neo4j package, the BI 10 query uses `apoc.path.subgraphNodes`, which requires [APOC](https://neo4j.com/labs/apoc/4.1/installation/) package. 
BI 19 uses `gds.shortestPath.dijkstra.stream`, which requires [graph data science (GDS)](https://neo4j.com/docs/graph-data-science/current/installation/) package. 

Download [APOC][https://github.com/neo4j-contrib/neo4j-apoc-procedures/releases/download/4.2.0.4/apoc-4.2.0.4-core.jar] and [GDS](https://s3-eu-west-1.amazonaws.com/com.neo4j.graphalgorithms.dist/graph-data-science/neo4j-graph-data-science-1.6.0-standalone.zip). 

After moving them to `$NEO4J_HOME/plugins`, then add the following text to `$NEO4J_HOME/conf/neo4j.conf`. We also changed the maximum memory size for queries.
```
dbms.memory.heap.initial_size=31g
dbms.memory.heap.max_size=31g
dbms.memory.pagecache.size=194000m
dbms.security.procedures.unrestricted=gds.*,apoc.*
dbms.security.procedures.whitelist=gds.*,apoc.coll.*,apoc.load.*
```

Restart neo4j and go into cypher shell
```sh
neo4j restart
#wait some time
cypher-shell
# in cypher shell 
# type `RETURN gds.version();` to show  gds version (the ending semicolon is required)
# type `CALL gds.list();` to install the package
```


## run queries
Currently parameter directory is hard coded in the file.
```sh
cypher-shell < indices.cypher
python3 bi.py
```
This will generate results in `./result/` and time elapsed in `./elasped_time/` folder


