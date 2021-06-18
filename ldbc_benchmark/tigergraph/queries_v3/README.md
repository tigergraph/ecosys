# LDBC SNB GSQL Benchmark

## Table of Contents
* [Overview](#Overview)
* [Pre-requisite](#Pre-requisite)
* [Download](#Donwload-LDBC-SNB-Data)
* [Load-data](#Load-data)
* [Run](#run)

## Overview
This follows [LDBC Social Network Benchmark v.0.4.0](https://github.com/ldbc/ldbc_snb_docs). BI 4 and 17 are modified based on discussion with Garbor. 
```
BI4: Find the most popular *Forums* by *Country*, where the popularity of a *Forum*
  is measured by the number of members that *Forum* has from a given *Country*.
  Calculate the top 100 most popular *Forums*.
  If a *Forum* is popular in multiple countries, it should only included in the top 100 once.
  In case of a tie, the *Forum*(s) with the smaller id value(s) should be selected.
```
* [`quereis`](./queries) directory contains the BI queries in GSQL.
* [`driver.py`](./driver.py) is the utility script to do anything related to the benchmark in this implementation.
* [`schema.gsql`](./schema.gsql) defines the LDBC SNB schema and loading job in GSQL.
* [`cypher`](./cypher) directory contains benchmark using Neo4j.

Related links
* [JIRA page](https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/2352251355/LDBC-SNB)
* [LDBC_BI by Garbor](https://github.com/ldbc/ldbc_snb_bi) contains the benchmark using Cypher and postgres
* [Google Sheet](https://docs.google.com/spreadsheets/d/1NVdrOQtYBZl3g2B_jxYozo2pV-8B0Zzf50XDVw0JzTg/edit?ts=60b84592#gid=1034343597) contains internal benchmark results

## Pre-requisite 
* `TigerGraph` (at least 3.1.0) must be installed. I used 3.2.0. 
* `Python` (at least 3.6) must be installed to use the driver script. 
* Python library `requests` is required.

```sh
sudo yum install wget git tar python3 sshpass zstd 
python3 -m pip install requests
```
If zstd is not available. Download and compile the source from their github.
```sh
git clone https://github.com/facebook/zstd
cd zstd 
make && sudo make install
cd ..
```

Install tigergraph-3.1.3 Or find the latest build from (http://192.168.11.192/download.html). 
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

## Donwload LDBC SNB Data 
LDBC data are available for scale factor [1](https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download), [3](https://surfdrive.surf.nl/files/index.php/s/fY7YocVgsJhmqdT/download), [10](https://surfdrive.surf.nl/files/index.php/s/SY6lRzEzDvvESfJ/download), [30](https://surfdrive.surf.nl/files/index.php/s/dtkgN7ZDT37vOnm/download), [100](https://surfdrive.surf.nl/files/index.php/s/gxNeHFKWVwO0WRm/download). To download data of scale factor 1,

```sh
wget -O sf1-composite-projected-fk.tar.zst https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download 
zstd -d sf1-composite-projected-fk.tar.zst 
tar -xvf sf1-composite-projected-fk.tar
```

Prepare the data. We arrange the directory struture so that the data can be loaded.
```sh
mv sf1/csv/bi/composite-projected-fk/initial_snapshot .
cd initial_snapshot
mv static/* dynamic/* .
rm -r static dynamic/
# remove the empty files, these files can cause loading to fail
for f in *; do rm $f/_SUCCESS; done
```

## Load data
Checkout ldbc branch of the current repository
```sh
git clone --branch ldbc https://github.com/tigergraph/ecosys.git
cd ecosys/ldbc_benchmark/tigergraph/queries_v3
```
BI 15 requires user defined function bigint_to_string in `ExprFunctions.hpp `.
```sh
cp ExprFunctions.hpp $(gadmin config get System.AppRoot)/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
```
Load schema, data, and query. Usage of `driver.py` can be `-h` option. For example, to check how to load the data use `./driver.py load data -h`. The data directory should contain 31 folders in name of the vertex and edge type names. The CSV files inside these folders are loaded. 
```sh
./driver.py load schema
./driver.py load data ~/initial_snapshot 
./driver.py load query
```
THis is equivalent to
```sh
./driver.py load all ~/initial_snapshot
```

The directory can include the machine. If you want to load data that is distributed on all the machines, use
```sh
./driver.py load data ALL:~/initial_snapshot 
``` 

## Run
Usage of `./driver.py` can be found using `./driver.py run -h`. The basic usage is `./driver.py run -q [queries] -n [number of runs]`. For large scale factors, you may need to configure time out to allow query to run in longer time using `gadmin config group timeout`.
```sh
# Query bi19 is expensive, we recomment to run without bi19 for 3 times
./driver.py run -q not:19 -n 3
# To run all the queries
./driver.py run 
```

## Considerations in writing queries
There are many ways to write the query and here what we present is the one with the best performance. 
I may create a folder to discuss. The query is usually faster if:
* if you know the degree of the edges, and use SumAccum to store the information instead of SetAccum or MapAccum.
* if you start from a smaller vertex set 

## refreshes


