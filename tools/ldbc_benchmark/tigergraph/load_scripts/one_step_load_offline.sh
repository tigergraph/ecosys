#!/bin/bash

### change to raw data file folder
export LDBC_SNB_DATA_DIR=/home/tigergraph/ldbc_snb_data/social_network/
### somehow LDBC SNB datagen doesn't get any benefit from multithreads. fix it to the single file for each vertex/edge
export LDBC_SNB_DATA_POSTFIX=_0_0.csv

# modify ~/.gium/loading.cfg for LDBC SNB graph
sed -i -E "s/(GRAPH_NAME=)(.+)/\1\"ldbc_snb\"/;s/(LOADING_JOBS=)(.+)/\1(\"load_ldbc_snb\")/" /home/tigergraph/.gium/loading.cfg

# modify setup_schema.gsql with pre-set path and install it
sed -E "s#(DEFINE FILENAME )([v_]*)([a-zA-Z_]*)(_file)(.+)#\1\2\3\4 = \"$LDBC_SNB_DATA_DIR\3$LDBC_SNB_DATA_POSTFIX\";#g" setup_schema.gsql > setup_schema_offline.gsql
gadmin start
gsql setup_schema_offline.gsql

# run gautoloading
chmod +x /home/tigergraph/.gium/gautoloading.sh
/home/tigergraph/.gium/gautoloading.sh