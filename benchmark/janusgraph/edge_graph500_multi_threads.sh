# this script use to retrieve vertex hashmap then load graph500 edge file
# CLASSPATH (need to modify): e.g. "$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*"
# path/to/graph500/edge/file (need to modify): e.g. /ebs/raw/graph500-22/graph500-22
# path/to/conf/file (need to modify): e.g. /ebs/install/janusgraph/conf/graph500-janusgraph-cassandra.properties
# path/to/graph500/hashmap/file (need to modify): e.g./ebs/raw/graph500/hashMap

export CLASSPATH="$CLASSPATH:/path/to/lib/*"
echo "load graph500 edge file" 
java -Xmx96g multiThreadEdgeImporter path/to/graph500/edge/file path/to/conf/file 4000 path/to/graph500/hashmap/file 
