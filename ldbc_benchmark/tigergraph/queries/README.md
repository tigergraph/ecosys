# LDBC SNB queries and driver for TigerGraph

## Prerequisites

You first need to copy and paste the contents in [ExprFunctions.hpp](https://github.com/tigergraph/ecosys/tree/ldbc/ldbc_benchmark/tigergraph/queries/helper/ExprFunctions.hpp) to <tigergraph.root.dir>/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp.
You can find more detail about user-defined functions [here](https://docs.tigergraph.com/dev/gsql-ref/querying/operators-functions-and-expressions#user-defined-functions).

Then you need to install all queries:

```
./install_queries.sh
```

This will install LDBC SNB queries and helper queries.

## Run driver

### Dependencies

The python driver requires some libraries, so install those before you run the driver:

```
sudo apt install libcurl4-openssl-dev libssl-dev
pip3 install tornado pycurl --user
```

Once you done with this, you can now run the driver:

```
python3 driver.py --path [PATH_TO_SEEDS] --num [MAX_NUM_SEEDS] --query [QUERY_TYPE] --debug [0|1] --seed [NUM_SEEDS]
```

* PATH_TO_SEEDS (Optional): Full path to the directory of pre-generated seeds. Those seeds of txt files are under substitution_parameters/ in the directory where data generated. It is by default "/home/tigergraph/ldbc_snb_data/substitution_parameters/" and you can skip passing this argument by directly modifying DEFAULT_PATH_TO_SEEDS in [driver.py](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/tigergraph/queries/driver.py).
* MAX_NUM_SEEDS (Optional): Number of seeds to run queries. It will read up to MAX_NUM_SEEDS seeds from the files. It is by default 100 and you can skip passing this argument by directly modifying DEFAULT_MAX_NUM_SEEDS in [driver.py](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/tigergraph/queries/driver.py).
* QUERY_TYPE (Optional): If you want to run a single query instead of all queries, you can pass a type and number of specific query: IS[1..7], IC[1..14], BI[1..25]. e.g. IS2, Ic12, bi22.
* DEBUG (Optional): If you want to check out HTTP request and response, put 1 for this parameter.
* NUM_SEEDS (Optional): If you just want to have some seed(s) to test, put the number of seeds you want to populate. It will give you person.id and message.id.