#!/bin/bash

############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################

#change to your raw data file
export RAW_DATA_PATH=/home/ubuntu/data/social_network/
# numThreads specified in ldbc_snb_datagen/params.ini
export TOTAL_FILE_NUMBER=6
# this is the final destination to store your processed raw data 
# TGT_DATA_ROOT_FOLDER must be different with RAW_DATA_PATH
export TGT_DATA_ROOT_FOLDER=/home/ubuntu/data/raw_data

bash pre_process_data.sh
echo "------------------------------completed pre-processing raw data----------------------------"
bash move_data.sh
echo "------------------------------completed moving raw data to new destionation----------------"
gsql setup_schema.gsql
echo "------------------------------completed setup schema---------------------------------------"
bash load_data.sh
echo "------------------------------completed loading data---------------------------------------"

