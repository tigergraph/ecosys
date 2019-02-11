# LDBC SNB data loading scripts for Neo4j

## Prerequisites

* Install Neo4j. Scripts are tested with Neo4j 3.5.1 Community edition.
* Generate LDBC SNB data.

## Run scripts

### Modify env_vars.sh

Once you are done with data generation, modify environment variables in [env_vars.sh](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/neo4j/load_scripts/env_vars.sh).
Change LDBC_SNB_DATA_DIR and NEO4J_DB_NAME properly.

### Modify one_step_load.sh

* skip_preprocess: Neo4j requires pre-processing of data and this is done by [preprocess.sh](https://github.com/tigergraph/ecosys/blob/ldbc/ldbc_benchmark/neo4j/load_scripts/preprocess.sh): replacing the header for each file and capitalize some node name. Replacing a header can take huge amount of time if the file is big. Since this is one-time processing, you can skip pre-processing by changing the value of skip_preprocess to 1 second from the first line of this file.

* change_passwd.cql: If you are running the scripts for the first time, your db user neo4j should be the default password. However, you need to change the default password to create indexes, so it calls this cypher script to change the password. If you already changed your password, You can simply comment out the second from the last line of this file, and change -p flag in the last line accordingly.

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
