# this script use to run weakly connected component
# CLASSPATH (need to modify): "$CLASSPATH:path/to/lib/*"
# path/to/config/file (need to modify): e.g. /ebs/install/janusgraph/conf/graph500-janusgraph-cassandra.properties

export CLASSPATH="$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*:."
echo "WCC"
java WCC.JanusWCC path/to/config/file 
echo "================"
