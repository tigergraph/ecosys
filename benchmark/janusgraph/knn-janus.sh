# this sript use to run k-hops query
# CLASSPATH (need to modify): "$CLASSPATH:path/to/lib/*"
# path/to/config/file (need to modify): e.g. /ebs/install/janusgraph/conf/graph500-janusgraph-cassandra.properties
# path/to/random/seed (need to modify): e.g. /ebs/benchmark/code/janusgraph/graph500-22-seed
# traversal_depth (need to modify): 1 for 1 step, 2 for 2 steps ..
# number_of_seed_to_test (need to modify): number of random seed you want to test, up to 300.

export CLASSPATH="$CLASSPATH:/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/*"
echo "knn"
java JanusKNeighbor path/to/config/file path/to/random/seed traversal_depth number_of_seed_to_test
echo "================="
