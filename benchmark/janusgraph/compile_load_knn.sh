# This script use to compile java codes for data loading, and K-neighborhoood
# LIBPATH (need to modify): "path/to/lib/"

export LIBPATH="/ebs/install/janusgraph/janusgraph-0.2.1-hadoop2/lib/"

echo "compile multiThreadVertexImporter.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar multiThreadVertexImporter.java

echo "compile multiThreadEdgeImporter.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar multiThreadEdgeImporter.java

echo "compile singleThreadVertexImporter.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar singleThreadVertexImporter.java

echo "compile singleThreadEdgeImporter.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar singleThreadEdgeImporter.java

echo "compile singleThreadGraph500Importer.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar singleThreadGraph500Importer.java

echo "compile JanusKNeighbor.java"
javac -cp ${LIBPATH}gremlin-core-3.2.9.jar:${LIBPATH}commons-configuration-1.10.jar:${LIBPATH}janusgraph-core-0.2.1.jar:${LIBPATH}javapoet-1.8.0.jar JanusKNeighbor.java

