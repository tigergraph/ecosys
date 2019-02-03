#!/bin/bash

### change to raw data file folder
export LDBC_SNB_DATA_DIR=/home/neo4j/ldbc_snb_data/social_network
### change db name if you want to keep old data
export NEO4J_DB_NAME=ldbc_snb.db

### somehow LDBC SNB datagen doesn't get any benefit from multithreads. fix it to the single file for each vertex/edge
export LDBC_SNB_DATA_POSTFIX=_0_0.csv
### environment variables for neo4j
export NEO4J_HOME=/home/neo4j/neo4j-community-3.5.1
export NEO4J_DB_DIR=$NEO4J_HOME/data/databases
