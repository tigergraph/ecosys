# this script use to run PageRank
# CLASSPATH (need to modify): e.g. "$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*" 
# path/to/conf/file (need to modify): e.g. /ebs/install/janusgraph/conf/graph500-janusgraph-cassandra.properties
# path/to/unique/id/file (need to modify): e.g. /ebs/raw/graph500-22/graph500_janusgraph_ids

export CLASSPATH="$CLASSPATH:/path/to/lib/*:."
echo "PageRank"
java PG.JanusPageRank path/to/conf/file path/to/unique/id/file
echo "===================="
