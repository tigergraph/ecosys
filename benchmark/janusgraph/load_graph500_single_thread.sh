# this script use to load graph500 data
# CLASSPATH (need to modify): e.g. "$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*"
# path/to/graph500/rawData (need to modify): e.g. /ebs/raw/graph500-22/graph500-22
# path/to/configuration/file (need to modify): e.g. /ebs/install/janusgraph/conf/graph500-janusgraph-cassandra.properties

export CLASSPATH="$CLASSPATH:path/to/lib/*"
java -Xmx96g singleThreadGraph500Importer path/to/graph500/rawData path/to/configuration/file 4000

