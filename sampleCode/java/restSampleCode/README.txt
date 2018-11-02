1. Pre-request
==============
  Please follow gsql 101 Section 1 to 3, now you should have a graph named social with 7 vertices and 7 edges

2. Use DDL upsert VERTEX
========================
  Upsert vertices: "Amanda,Amanda,20,female,ca" and “Leo,Leo,23,male,ca” to graph named "social".
     
  Step 1. Create and install a loading job to setup a loading tag. 
    Run the following command in linux shell
      gsql postVertex.gsql

  Step 2. Use curl command or Java program to post data to the tag via a REST endpoint. Note that filename below specifies the filename variable “postVertexFileName”, so that we know which loading statement will be used.
                
    Method 1 - Inline method to supply the data
    --------
      Curl command looks like:
        curl -X POST "localhost:9000/ddl/social?tag=postVertex&filename=postVertexFileName&sep=,&eol=;" -d "Amanda,Amanda,20,female,ca;Leo,Leo,23,male,ca" | jq .

      Or run Java example:
        // comment out line 69 in PostDDLSampleCode.java
        // uncomment line 68 in PostDDLSampleCode.java
        javac -cp json.jar PostDDLSampleCode.java 
        java -cp "json.jar:." PostDDLSampleCode vertex social postVertex postVertexFileName , \;
               
    Method 2 - store input data into a separate file 
    --------
      Curl command looks like: 
        curl -X POST --data-binary @vertexData.csv "localhost:9000/ddl/social?tag=postVertex&filename=postVertexFileName&sep=,&eol=\n" | jq .
                  
      Run Java example in linux shell:
        javac -cp json.jar PostDDLSampleCode.java
        java -cp "json.jar:." PostDDLSampleCode vertex social postVertex postVertexFileName , \\n ./vertexData.csv  

      Verify: in gsql shell
	use graph social
	SELECT * FROM person WHERE primary_id=="Amanda"
        SELECT * FROM person WHERE primary_id=="Leo"

3. Use DDL upsert edge
======================
  Upsert edges: "Amanda,Leo,2011-08-08" and "Amanda,Tom,2016-03-05" to graph named "social".
    
  Step 1. Create and install a loading job to setup a loading tag.

    Run the following command in linux shell
      gsql postEdge.gsql

  Step 2. Use curl command or Java program to post data to the tag via a REST endpoint. Note that filename below specifies the filename variable “postEdgeFileName”, so that we know which loading statement will be used.

    Method 1 - Inline method to supply the data
    --------
      Curl command looks like:
        curl -X POST "localhost:9000/ddl/social?tag=postEdge&filename=postEdgeFileName&sep=,&eol=;" -d "Amanda,Leo,2011-08-08;Amanda,Tom,2016-03-05"  | jq .

      Run Java example in linux shell:
        // comment out line 68 in PostDDLSampleCode.java
        // uncomment line 69 in PostDDLSampleCode.java
        javac -cp json.jar PostDDLSampleCode.java
        java -cp "json.jar:." PostDDLSampleCode edge social postEdge postEdgeFileName , \;

    Method 2 - store input data into a separate file
    --------
      Run Curl command: 
        curl -X POST --data-binary @edgeData.csv "localhost:9000/ddl/social?tag=postEdge&filename=postEdgeFileName&sep=,&eol=\n" | jq .
 
      Run Java example in linux shell:
        javac -cp json.jar PostDDLSampleCode.java 
        java -cp "json.jar:." PostDDLSampleCode edge social postEdge postEdgeFileName , \\n ./edgeData.csv

    Verify in gsql shell:
      use graph social
      SELECT * FROM person-(friendship)->person WHERE from_id =="Amanda"

4. DELETE edge
==============
  Delete edge:  with starting vertex is Amanda
    Curl command looks like : 
      curl -X GET "http://localhost:9000/graph/social/edges/person/Amanda/friendship"

    Run Java example in linux shell:
      // comment out line 42 in DeleteSampleCode.java
      // uncomment line 41 in DeleteSampleCode.java
      javac -cp json.jar DeleteSampleCode.java
      java -cp "json.jar:." DeleteSampleCode

    Verify in gsql shell:
      use graph social
      SELECT * FROM person-(friendship)->person WHERE from_id =="Amanda"

5. DELETE vertex
================
  Delete vetex: Amanda
    Curl command looks like: 
      curl -X GET "http://localhost:9000/graph/social/vertices/person/Amanda"

    Run Java example in linux shell:
      // comment out line 41 in DeleteSampleCode.java
      // uncomment line 42 in DeleteSampleCode.java
      javac -cp json.jar DeleteSampleCode.java
      java -cp "json.jar:.” DeleteSampleCode

    Verify in gsql shell:
      use graph social
      SELECT * FROM person WHERE primary_id=="Amanda"

