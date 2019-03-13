# LDBC SNB queries and driver for TigerGraph

## Prerequisites

You first need to copy and paste the contents in [ExprFunctions.hpp](https://github.com/tigergraph/ecosys/tree/ldbc/ldbc_benchmark/tigergraph/queries/helper/ExprFunctions.hpp) to <tigergraph.root.dir>/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp.
You can find more detail about user-defined functions [here](https://docs.tigergraph.com/dev/gsql-ref/querying/operators-functions-and-expressions#user-defined-functions).

Then you need to install all queries:

```
./install_queries.sh
```

This will install all LDBC SNB queries and helper queries.

## Run driver

### Dependencies

The python driver requires some libraries, so install those before you run the driver:

```
sudo apt install libcurl4-openssl-dev libssl-dev
pip3 install tornado pycurl --user
```

Once you done with this, you can now run the driver:

```
python3 driver.py [-h] [-p PATH] [-n NUM] [-q QUERY] [-d [DEBUG]] [--seed SEED]
```

* PATH (Optional): Full PATH to the directory of pre-generated seeds. Those seeds of txt files are under substitution_parameters/ in the directory where data generated. It is by default "/home/tigergraph/ldbc_snb_data/substitution_parameters/" and you can skip passing this argument by directly modifying DEFAULT_PATH_TO_SEEDS in [driver.py](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/tigergraph/queries/driver.py).
* NUM (Optional): Max NUMber of seeds to run queries. It will read up to MAX_NUM_SEEDS seeds from the files. It is by default 100 and you can skip passing this argument by directly modifying DEFAULT_MAX_NUM_SEEDS in [driver.py](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/tigergraph/queries/driver.py).
* QUERY (Optional): If you want to run a single QUERY or all queries in a workload instead of all 46 queries, you can pass a type and/or number of specific query: IS[_1..7], IC[_1..14], BI[_1..25]. 
  * to run a specific query: IS_2, Ic_12, bi_22
  * to run all queries in a specific workload: is, iC, BI
* DEBUG (Optional): If you want to print out HTTP request/response and/or response time for each HTTP request, pass this flag with one of the following values:
  * 1: print HTTP request/response and response time for each HTTP request
  * 2: print only response time for each HTTP request
* SEED (Optional): You can run a query QUERY with a speicfic parameter SEED for NUM number of times by passing a pipe-separated and quoted string here, i.e. you can simply copy and paste a single line from a text file under substitution_parameters/ directory and enclose it with quotation marks. Note that the order of parameter in the string matters, and SEED will be ignored unless you provide a specific query QUERY to run.