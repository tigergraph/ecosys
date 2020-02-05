javac -cp third_party/json-simple-1.1.1.jar com/tigergraph/connector/jdbc/*.java
jar cvf tgjdbc.jar com
rm -rf  com/tigergraph/connector/jdbc/*.class
