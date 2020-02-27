#!/bin/bash

############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################

### change to raw data file folder
export LDBC_SNB_DATA_DIR=/home/tigergraph/ldbc_snb_data/social_network/
### somehow LDBC SNB datagen doesn't get any benefit from multithreads. fix it to the single file for each vertex/edge
export LDBC_SNB_DATA_POSTFIX=_0_0.csv

# define schema and loading job
gadmin start
gsql setup_schema.gsql

# load data into TigerGraph
./load_data.sh
