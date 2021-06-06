# LDBC SNB GSQL Benchmark

## Table of Contents
* [Overview](#overview)
* [Setup](#setup)
* [Run](#run)

## Overview

This follows [LDBC Social Network Benchmark v.0.4.0-SNAPSHOT](https://github.com/ldbc/ldbc_snb_docs), retrieved on 2021-02-19.
The version of TigerGraph is 3.1.0.
The `reads` directory contains the BI queries in GSQL.
[`driver.py`](./driver.py) is the utility script to do anything related to the benchmark in this implementation.
[`schema.gsql`](./schema.gsql) defines the LDBC SNB schema and loading job in GSQL.
For more details, refer to the JIRA page (https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/2352251355/LDBC-SNB)


## LDBC SNB Data 
LDBC data are available for scale factor 1(https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download), 3(https://surfdrive.surf.nl/files/index.php/s/fY7YocVgsJhmqdT/download), 10(https://surfdrive.surf.nl/files/index.php/s/SY6lRzEzDvvESfJ/download), 30(https://surfdrive.surf.nl/files/index.php/s/dtkgN7ZDT37vOnm/download), 100(https://surfdrive.surf.nl/files/index.php/s/gxNeHFKWVwO0WRm/download). To download data of scale factor 1,

```sh
yum install zstd
wget -O sf1-composite-projected-fk.tar.zst https://surfdrive.surf.nl/files/index.php/s/xM6ujh448lnJxXX/download 
zstd -d sf1-composite-projected-fk.tar.zst 
tar -xvf sf1-composite-projected-fk.tar
```


## Checkout Current repository

```sh
git clone https://github.com/tigergraph/ecosys.git
2cd ecosys
3git checkout ldbc
4cd ecosys/ldbc_benchmark/tigergraph/queries_v3
```

## Setup
TigerGraph must be installed and running.
Python (at least 3.6) must be installed to use the driver script.
The driver script also uses `requests` library.
Install `requests` by running:
```sh
python3 -m pip install requests
```

Run
```sh
gadmin status
```
to check if TigerGraph is running or not.

Run
```sh
gadmin start all
```
to start TigerGraph.

Run
```sh
./driver.py load schema
./driver.py load query
./driver.py load data <data_dir>
# Or
./driver.py load all <data_dir>
```
to load the schema, the queries from the workloads, and the data.
You can also specify the workloads from which to install the queries.

## Run

Run
```sh
./driver.py run <parameters_dir> [list of workload]
```
