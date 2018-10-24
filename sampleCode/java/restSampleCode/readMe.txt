1. Pre-request
	Please follow gsql101 Section 1 to 3, now you should have a graph named "social" with 7 vertices and 7 edges

2. Use DDL upsert VERTEX
	Upsert a vertex: "Amanda,Amanda,20,female,ca"

	Curl command looks like : 
		curl -X POST "localhost:9000/ddl/social?tag=postVertex&filename=postVertexFileName&sep=,&eol=\n" -d "Amanda,Amanda,20,female,ca"
	
	Run following command in linux shell
		gsql postVertex.gsql
	
		// comment out line 48, 49 in PostDDLSampleCode.java
		// uncomment line 45, 46 in PostDDLSampleCode.java

		javac -cp json.jar PostDDLSampleCode.java 
		java -cp "json.jar:." PostDDLSampleCode vertex

	Verify: in gsql shell
		use graph social
		SELECT * FROM person WHERE primary_id=="Amanda"

3. Use DDL upsert edge
	Upsert an edge: "Amanda,Leo,2011-08-08"
	Curl command looks like : 
		curl -X POST "localhost:9000/ddl/social?tag=postEdge&filename=postEdgeFileName&sep=,&eol=\n" -d "Amanda,Leo,2011-08-08"

	Run following command in linux shell
		gsql postEdge.gsql

		// comment out line 45, 46 in PostDDLSampleCode.java
		// uncomment line 48, 49 in PostDDLSampleCode.java

		javac -cp json.jar PostDDLSampleCode.java 
		java -cp "json.jar:." PostDDLSampleCode Edge

	Verify in gsql shell:
		use graph social
		SELECT * FROM person-(friendship)->person WHERE from_id =="Amanda"

4. DELETE edge
	Delete edge:  with starting vertex "Amanda"
	Curl command looks like : 
		curl -X DELETE "http://localhost:9000/graph/social/edges/person/Amanda/friendship"

	Run following command in linux shell
		// comment out line 42 in DeleteSampleCode.java
		// uncomment line 41 in DeleteSampleCode.java

		javac -cp json.jar DeleteSampleCode.java
		java -cp "json.jar:." DeleteSampleCode

	Verify in gsql shell:
		use graph social
		SELECT * FROM person-(friendship)->person WHERE from_id =="Amanda"

5. DELETE vertex
	Delete vetex: "Amanda"
	Curl command looks like : 
		curl -X DELETE "http://localhost:9000/graph/social/vertices/person/Amanda"

	Run following command in linux shell
		// comment out line 41 in DeleteSampleCode.java
		// uncomment line 42 in DeleteSampleCode.java

		javac -cp json.jar DeleteSampleCode.java
		java -cp "json.jar:.” DeleteSampleCode

	Verify in gsql shell:
		use graph social
		SELECT * FROM person WHERE primary_id=="Amanda"
