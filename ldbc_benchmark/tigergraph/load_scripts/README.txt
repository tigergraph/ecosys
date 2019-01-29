############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################

You first need to generate LDBC SNB data before run scripts here. Please follow the instructions here for more detail:
https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/802619473/LDBC+Social+Network+Benchmark

Once you are done with data generation, modify LDBC_SNB_DATA_DIR in one_step_load.sh to the directory of generated data.

Then simply do:
> ./one_step_load.sh

It'll first create the schema and the loading job in setup_schema.gsql, and load_data.sh will load all data into TigerGraph.
