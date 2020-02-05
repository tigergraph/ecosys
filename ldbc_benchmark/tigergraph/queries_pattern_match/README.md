# LDBC SNB queries and driver for TigerGraph Pattern-match Syntax

Here you can find LDBC SNB queries written in GSQL pattern-match syntax and Python driver to run all queries for TigerGraph.

## Prerequisites

Please make sure that you already installed TigerGraph, loaded all LDBC SNB data into it, and logged in as the user can run TigerGraph.
Then you type the following commands Tto install a helper function and all GSQL queries:

```
git clone git@github.com:tigergraph/ecosys.git
cd ecosys
git checkout ldbc
cd ldbc_benchmark/tigergraph/queries
./install_queries.sh
```

This will install a [user-defined function](https://docs.tigergraph.com/dev/gsql-ref/querying/operators-functions-and-expressions#user-defined-functions), helper queries, all LDBC SNB queries.

## Run driver

### Dependencies

The python driver requires some libraries, so install those before you run the driver:

```
sudo apt install libcurl4-openssl-dev libssl-dev
pip3 install tornado pycurl --user
```

### Usage

#### Make a directory to store the results:

```
mkdir res
```

#### For interpret mode:

```
python3 driver_interpret.py [-h] [-p PATH] [-n NUM] [-q QUERY] [-d DEBUG] [-s SEED]
```
You can run a query in interpret mode directly with the above driver script. To run the queries with a given seed, 3 scripts for different dataset (SF-1, SF-10, SF-100) are provided here: run_sf1_interpret.sh, run_sf10_interpret.sh and run_sf100_interpret.sh. You can run them with command like:
```
./run_sf1_interpret.sh
```
And the result and running time can be found under ./res/

#### For compile mode:

First, you have to install all the queries with command:
```
./install_compile.sh
```
Then you can run a query with command:
```
python3 driver_compile.py [-h] [-p PATH] [-n NUM] [-q QUERY] [-d DEBUG] [-s SEED]
```
To run the queries with a given seed, 3 scripts for different dataset (SF-1, SF-10, SF-100) are provided here: run_sf1_compile.sh, run_sf10_compile.sh and run_sf100_compile.sh. You can run them with command like:
```
./run_sf1_compile.sh
```
And the result and running time can be found under ./res/
