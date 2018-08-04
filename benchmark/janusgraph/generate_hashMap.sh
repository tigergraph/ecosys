# this script use to load twitter vertex file and write hashmap (externalId : internalId) to disk
# CLASSPATH (need to modify): "$CLASSPATH:path/to/lib/*"
# path/to/twitterVertex/file (need to modify): e.g. /ebs/raw/twitter_rv/twitter_rv.net_unique_node
# path/to/twitterConf/file (need to modify): e.g. /ebs/install/janusgraph/conf/twitter_generate_hashmap.properties

export CLASSPATH="$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*"
java -Xmx96g generateVertexHashMap path/to/twitterVertex/file path/to/twitterConf/file 4000
