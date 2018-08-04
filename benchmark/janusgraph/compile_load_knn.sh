# This script use to compile java codes for data loading, and K-neighborhoood
# LIBPATH (need to modify): "path/to/lib/"

export LIBPATH="/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/"

echo "compile JanusImporter.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar JanusImporter.java

echo "compile JanusKNeighbor.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar JanusKNeighbor.java

echo "compile generateVertexHashMap.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar generateVertexHashMap.java

echo "compile JanusImporterHashmap.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar JanusImporterHashmap.java
