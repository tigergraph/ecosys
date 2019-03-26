# LDBC SNB queries and driver for TigerGraph

Here you can find all 46 LDBC SNB queries written in GSQL and Python driver to run all queries for TigerGraph.

## Prerequisites

Please make sure that you already installed TigerGraph, loaded all LDBC SNB data into it, and logged in as the user can run TigerGraph.
Then you type the following commands Tto install a helper function and all GSQL queries:

```
git clone git@github.com:tigergraph/ecosys.git
git checkout ldbc
cd ecosys/ldbc_benchmark/tigergraph/queries
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

```
python3 driver.py [-h] [-p PATH] [-n NUM] [-q QUERY] [-d DEBUG] [-s SEED]
```

* PATH (Optional): Full PATH to the directory of pre-generated seeds. Those seeds of txt files are under substitution_parameters/ in the directory where data generated. It is by default "/home/tigergraph/ldbc_snb_data/substitution_parameters/" and you can skip passing this argument by directly modifying DEFAULT_PATH_TO_SEEDS in [driver.py](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/tigergraph/queries/driver.py#L19).
* NUM (Optional): Max NUMber of seeds to run queries. It will read up to MAX_NUM_SEEDS seeds from the files. It is by default 100 and you can skip passing this argument by directly modifying DEFAULT_MAX_NUM_SEEDS in [driver.py](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/tigergraph/queries/driver.py#L20).
* QUERY (Optional): If you want to run a single QUERY or all queries in a workload instead of all 46 queries, you can pass a type and/or number of specific query: IS[_1..7], IC[_1..14], BI[_1..25]. 
  * run a specific query: IS_2, Ic_12, bi_22
  * run all queries in a specific workload: is, iC, BI
* DEBUG (Optional): If you want to print out HTTP request/response and/or response time for each HTTP request, pass this flag with one of the following values:
  * 1: print HTTP request/response and response time for each HTTP request
  * 2: print only response time for each HTTP request
* SEED (Optional): You can run a query QUERY with a speicfic parameter SEED for NUM number of times by passing a pipe-separated and quoted string here, i.e. you can simply copy and paste a single line from a text file under substitution_parameters/ directory and enclose it with quotation marks. Note that the order of parameter in the string matters, and SEED will be ignored unless you provide a specific query QUERY to run.