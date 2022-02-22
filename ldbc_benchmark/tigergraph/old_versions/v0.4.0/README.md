# LDBC SNB GSQL Implementation

## Table of Contents
* [Overview](#overview)
* [Setup](#setup)
* [Run](#run)

## Overview

This follows [LDBC Social Network Benchmark v.0.4.0-SNAPSHOT](https://github.com/ldbc/ldbc_snb_docs), retrieved on 2021-02-19.
The version of TigerGraph is 3.1.0.
The `workloads` directory contains the GSQL queries for the given workload.
[`driver.py`](./driver.py) is the utility script to do anything related to the benchmark in this implementation.
[`schema.gsql`](./schema.gsql) defines the LDBC SNB schema in GSQL.
It also defines a loading job for the data format `csv-composite-merged-fk`.

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
./driver.py run <parameters_dir> [workload...]
```
