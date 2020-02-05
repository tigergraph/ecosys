# LDBC SNB data loading scripts for Neo4j

## Prerequisites

* Install Neo4j. Scripts are tested with Neo4j 3.5.1 Community edition.
* Generate LDBC SNB data.

## Run scripts

### Modify env_vars.sh

Once you are done with data generation, modify environment variables in [env_vars.sh](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/neo4j/load_scripts/env_vars.sh).
Change LDBC_SNB_DATA_DIR and NEO4J_DB_NAME properly.

### Load data and create indexes

Now you can load data into Neo4j:

```
./one_step_load.sh
```

Once the loading is done, it will print out the total time spent and size of loaded data in Bytes. This is however without indexes. At the end of [one_step_load.sh](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/neo4j/load_scripts/one_step_load.sh), it will call another cypher script to create indexes. If you see an authentication error, please check the password for db user neo4j and run:

```
cat create_indexes.cql | [NEO4J_HOME]/bin/cypher-shell -u neo4j -p [YOUR_PASSWORD]
```

Now you can try to check the time spent by running a python script:

```
python3 time_index.py
```

This will print out time spent to create indexes if the process is done. If not, it will show how many indexes are created so far instead, so you can keep trying to run this script until you the time.
