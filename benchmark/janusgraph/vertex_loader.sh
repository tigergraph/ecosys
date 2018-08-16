# this script use to load graph500/twitter vertex file and write hashmap (externalId : internalId) to disk
# CLASSPATH (need to modify): "$CLASSPATH:path/to/lib/*"
# vertexImporter/class (need to modify): e.g multiThreadVertexImporter
# path/to/Vertex/file (need to modify): e.g. /ebs/raw/twitter_rv/twitter_rv.net_unique_node
# path/to/Conf/file (need to modify): e.g. /ebs/install/janusgraph/conf/twitter_generate_hashmap.properties

export CLASSPATH="$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*"
java -Xmx96g vertexImporter/class path/to/Vertex/file path/to/Conf/file 4000
