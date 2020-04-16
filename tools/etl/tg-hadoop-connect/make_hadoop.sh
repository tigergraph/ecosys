# make sure: copy over json-simple-1.1.1.jar in third-parth to all hadoop classpath in all data nodes
javac -cp $(hadoop classpath) com/tigergraph/connector/hadoop/*.java
jar cvf tghadoop.jar com
rm -rf  com/tigergraph/connector/hadoop/*.class
