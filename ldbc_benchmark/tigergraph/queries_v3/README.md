# TigerGraph LDBC SNB Benchmark

## Table of Contents
* [Overview](#Overview)
* [Pre-requisites](#Pre-requisite)
* [Download LDBC SNB data](#Donwload-LDBC-SNB-Data)
* [Load data](#Load-data)
* [Run Query and batch update](#run)
* [Usage of driver.py](#Usage-of-driver.py)
* [Considerations in writing gsql queries](#Considerations-in-writing-gsql-queries)
## Overview
The benchmark follows the speficiation in [LDBC Social Network Benchmark v.0.4.0](https://github.com/ldbc/ldbc_snb_docs). The repository contains the following folders
* [`queries`](./queries) BI GSQL queries.
* [`dml`](./dml) GSQL loading job and queries for inserting and deleting data.
* [`driver.py`](./driver.py) is the utility script to run the benchmark.
* [`schema.gsql`](./schema.gsql) defines the LDBC SNB schema and loading job
* [`cypher`](./cypher) the same benchmark using Neo4j.
* [`LDBC_10TB`](./LDBC_10TB) scripts for downloading and processing data of 10TB and 30TB.

Related links
* [LDBC_BI by Garbor](https://github.com/ldbc/ldbc_snb_bi) contains the benchmark using Cypher and postgres
* [(TierGraph internal) JIRA page](https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/2352251355/LDBC-SNB)
* [(TierGraph internal) results in Google Sheet](https://docs.google.com/spreadsheets/d/1NVdrOQtYBZl3g2B_jxYozo2pV-8B0Zzf50XDVw0JzTg/edit?ts=60b84592#gid=1034343597) contains internal benchmark results

## Pre-requisite 
* `TigerGraph` (at least 3.1.0, the lastest 3.2.0 is recommended) and its pre-requisites 
  * `sshpass` only required for the host
  * `net-tools`
* `Python` (at least 3.6) 
  * Python package `requests`

```sh
sudo yum install wget git tar python3-pip sshpass gcc
sudo python3 -m pip install requests
```
Install tigergraph-3.1.3. For internal test, you can find the latest build from (http://192.168.11.192/download.html). 
```sh
wget https://dl.tigergraph.com/enterprise-edition/tigergraph-3.1.3-offline.tar.gz
tar -xf tigergraph-3.1.3-offline
cd tigergraph-3.1.3-offline/
./install.sh
# following the instruction to install
su tigergraph
gadmin status
# check if TigerGraph is running or not.
```

## Config Tigergraph 
Enterprise edition also store a backup copy be default. We turn off this backup copy to have higher loading speed and less disk space.
To increase loading intensive, we can increase the number of loading handler based on the number of cpus in the machine. To see the cpu info, use `lscpu`. 
The timeout is only needed for large scale factors. It seems python requests are not constrained by the timeout, and timeout is not needed if you run only through python.
```sh
gadmin config entry GPE.BasicConfig.Env
# add MVExtraCopy=0; (default is 1) to turn off backup copy
# add ConcurrentRequest=128; (default is 16) may increase loading speed  

gadmin config group RESTPP-LOADER
# change FileLoader.Factory.HandlerCount from 4 to 40 or 80

gadmin config group timeout 
# Change FileLoader.Factory.DefaultQueryTimeoutSec: 16 -> 6000
# Change KafkaLoader.Factory.DefaultQueryTimeoutSec: 16 -> 6000
# Change RESTPP.Factory.DefaultQueryTimeoutSec: 16 -> 6000

gadmin config apply -y
gadmin restart all -y
```

## Donwload LDBC SNB Data 
For data larger than 1TB, refer to [LDBC_10TB](./LDBC_10TB). *For smaller dataset, we worked on the data with headers.* To download data of scale factor 1 (the link is no longer valid),

```sh
wget -O sf1-composite-projected-fk.tar.zst https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download 
zstd -d sf1-composite-projected-fk.tar.zst 
tar -xvf sf1-composite-projected-fk.tar
```

To install `zstd`, use `sudo yum install zstd`. If zstd is not available. Download and compile the source from their github.
```sh
git clone https://github.com/facebook/zstd
cd zstd 
make && sudo make install
cd ..
```

## Load data
Checkout ldbc branch of the current repository
```sh
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3
```
Load schema, data, and query. Usage of `driver.py` can be shown using `-h` option. For example, to check how to load the data use `./driver.py load data -h`. The data directory should contain 31 folders in name of the vertex and edge type names. The CSV files inside these folders are loaded. 
```sh
./driver.py load schema
./driver.py load data ~/sf1/sf1/csv/bi/composite-projected-fk/ --header
./driver.py load query
```
THis is equivalent to
```sh
./driver.py load all ~/initial_snapshot --header
```

The directory can include the machine. Default setting is `ANY`, which loads any data on each on the machine. If you want to load data on the node m1, use
```sh
./driver.py load data m1:~/initial_snapshot --header
``` 
After loading, you can checkout the number of vertices and edges using the following command. For SF1, there are 1116485 Comment vertices in the initial snapshot.
```sh
./driver.py stat
```

## Run query and batch updates
Usage of `./driver.py` can be found using `./driver.py run -h`. The basic usage is `./driver.py run -q [queries] -n [number of runs] -p [parameter file]`. The default parameter file is `auto`, which means if `param.json` file is not in the results folder, the driver will automatically generate the parameter file. An example paramter file for sf1 is in [./paramters/sf1.json](./paramters/sf1.json). you can use it by using `-p paramters/sf1.json`
```sh
./driver.py run  -n 3 
```

```sh
# Query bi19 is expensive, we recomment to run without bi19 for 3 times
./driver.py run -q not:19 -n 3
# To run all the queries
./driver.py run 
```
## Compare the results of initial state
The starting time of LDBC SNB graph is 2012-09-13. The documented GSQL results are in `results_sf[scale factor]/initial`. The documented  Cypher results are in `cypher/results_sf[scale factor]/initial`. To compare the results with the documented GSQL results.
```sh
 ./driver.py compare -s results -t results_sf1/initial
```
The script can be also used to compare the GSQL and cypher results. 
```sh
# this is also the default parameter setting
 ./driver.py compare -s results -t cypher/results 
```

## Refreshes with batch inserts and deletes
Then run the refresh workloads. The results and timelog are output to `results/`. 
```sh
./driver.py refresh ~/sf1/csv/bi/composite-projected-fk/ --header
```
After runnning neo4j benchmark, you can compare the results
```sh
# after running neo4j, compare thje
./driver.py compare 
```

## Run in background
The procedures above is equivalent to the following one command. 
For scale factors larger than 100, it usually takes many hours. 
I prefer to add nohup to allow the process continue after I log out. 
We also add sleep time equal to the running time between query runs because releasing memory also takes time. No sleep time can generate out-of-memory issue.
```sh
nohup python3 -u ./driver.py all ~/sf100/csv/bi/composite-projected-fk/ -s 1 --header > foo.out 2>&1 < /dev/null &  
```

# Usage of driver.py
./driver.py load all [dara_dir] will runs
* ./driver.py load query 
  *  gsql schema.gsql
* ./driver.py load query 
  * Copy paste user defined function `ExprFunctions.hpp` to tigergraph. 
  * `gsql [queries/*.gsql, delete/*.gsql, stat.gsql, parameter/gen_para.gsql]`
  * `gsql -g ldbc_snb 'install query [all queries]'` 
* ./driver.py load data [dara_dir]
  * remove _SUCCESS files in sf1, `find sf1 -name _SUCCESS -type f -delete`
  * `gsql -g ldbc_snb 'run loading job load_static using FILENAMES'`
  * `gsql -g ldbc_snb 'run loading job load_dynamic using FILENAMES'`



# Considerations in writing gsql queries
There are many ways to write the query and here what we present is the one with the best performance. 
I may create a folder to discuss. The query is usually faster if:
* if you know the degree of the edges, and use SumAccum to store the information instead of SetAccum or MapAccum.
* if you start from a smaller vertex set 

