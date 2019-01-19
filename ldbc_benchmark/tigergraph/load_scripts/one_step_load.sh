#!/bin/bash

############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################

# change to your raw data file
export LDBC_DATA_DIR=/home/ubuntu/ldbc_snb_data/social_network
# somehow LDBC SNB datagen doesn't get any benefit from multithreads. fix it to the single file for each vertex/edge
export LDBC_DATA_POSTFIX=_0_0.csv

echo "----- setup_schema.gsql BEGIN"
gsql setup_schema.gsql
echo "----- setup_schema.gsql END"
echo "----- load_data.sh BEGIN"
bash load_data.sh
echo "----- load_data.sh END"
