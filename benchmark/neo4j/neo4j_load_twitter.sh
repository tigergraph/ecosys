############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the 
# acknowledgement to TigerGraph.
# Author: Mingxi Wu mingxi.wu@tigergraph.com
############################################################

# This command will invoke neo4j-import to 
# do bulk load of twitter data 

if [ ! -d "$1" ]; then
  echo "Input is not a valid path."
  echo "Usage: ./neo4j_load_twitter.sh neo4j_home_folder data_folder"
  echo "E.g.  ./neo4j_load_twitter.sh  /ebs/install/neo4j/neo4j-community-3.4.4 /ebs/data/twitter"
  exit 1
fi

rm -rf $1/data/databases/twitter_rv.db

$1/bin/neo4j-import --into $1/data/databases/twitter.db --id-type string --nodes:MyNode "$2/twitter_rv-node-header.txt,$2/twitter_rv.net_unique_node" --skip-duplicate-nodes --delimiter "\t" --relationships:MyEdge "$2/twitter_rv-edge-header.txt,$2/twitter_rv.net"
