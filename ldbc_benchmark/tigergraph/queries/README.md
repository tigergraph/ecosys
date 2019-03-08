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
* QUERY (Optional): If you want to run a single QUERY instead of all 46 queries, you can pass a type and number of specific query: IS_[1..7], IC_[1..14], BI_[1..25]. e.g. IS_2, Ic_12, bi_22.
* DEBUG (Optional): If you want to check out HTTP request/response, pass this flag then driver will print them out.
* SEED (Optional): If you just need some seed(s) to test, put the number of SEEDs you want to populate. It will print person.id and message.id and terminates without running the driver.