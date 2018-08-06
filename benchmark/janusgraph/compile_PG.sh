# this script use to compile pageRank codes in /PG folder
# LIBPATH (need to modify): "/path/to/lib/"

export LIBPATH="/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/"

echo "compile JanusPG"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar PG/*.java
