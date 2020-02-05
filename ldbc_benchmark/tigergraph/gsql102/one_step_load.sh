#!/bin/bash

###############################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for training purpose.
# Author: mingxi.wu@tigergraph.com
################################################

### change to raw data file folder
export LDBC_SNB_DATA_DIR=/home/tigergraph/ldbc_snb_data/social_network/

# define schema and loading job
gadmin start
gsql setup_schema.gsql

# load data into TigerGraph
./load_data.sh
