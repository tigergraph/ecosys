# this script use to retrieve vertex hashmap then load partitioned twitter edge files one by one 
# CLASSPATH (need to modify): e.g. "$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*"
# path/to/twtter/splited/data (need to modify): e.g. /ebs/raw/twitter_rv/splited/twitter_rv_0
# path/to/conf/file (need to modify): e.g. /ebs/install/janusgraph/conf/twitter_generate_hashmap.properties
# path/to/twiiter/hashmap/file (need to modify): e.g./ebs/raw/twitter_rv/twitter-hashMap

export CLASSPATH="$CLASSPATH:/path/to/lib/*"
echo "file: twitter_rv_0" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file 
echo "file: twitter_rv_1" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file 
echo "file: twitter_rv_2" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file 
echo "file: twitter_rv_3" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file 
echo "file: twitter_rv_4" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file
echo "file: twitter_rv_5" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file
echo "file: twitter_rv_6" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file
echo "file: twitter_rv_7" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file
echo "file: twitter_rv_8" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file
echo "file: twitter_rv_9" 
java -Xmx96g JanusImporterHashmap path/to/twtter/splited/data path/to/conf/file 4000 path/to/twiiter/hashmap/file
