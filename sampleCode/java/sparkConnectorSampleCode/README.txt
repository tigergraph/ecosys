1. Setup
========
  1.1 Please follow GSQL 101 Section 1 to 3, now you should have a graph named "social" with 7 vertices and 7 edges. 

  1.2 [Linux Shell] You need to install OpenJdk 8, Scala(>= 2.11.7), Spark(>= 2.0.0) into all machines in cluster.
      -- Install Scala use one of the following command
           sudo apt-get install scala
           sudo yum install scala
    
      -- Install spark 2.2.1 use the following command
           wget https://archive.apache.org/dist/spark/spark-2.2.1/spark-2.2.1-bin-hadoop2.7.tgz
           tar xvzf spark-2.2.1-bin-hadoop2.7.tgz
           rm -rf spark-2.2.1-bin-hadoop2.7.tgz
           sudo mv spark-2.2.1-bin-hadoop2.7 /usr/local/spark
           sudo iptables -A INPUT -p tcp --dport 6066 -j ACCEPT
           sudo iptables -A INPUT -p tcp --dport 7077 -j ACCEPT
           sudo iptables -A INPUT -p tcp --dport 8080 -j ACCEPT
           sudo iptables -A INPUT -p tcp --dport 8081 -j ACCEPT
           sudo iptables-save
           echo 'export SPARK_HOME=/usr/local/spark' >> .bashrc
           echo 'export PATH=$PATH:$SPARK_HOME/bin' >> .bashrc
           source .bashrc
           spark-shell

  1.3 [Linux Shell] Set Spark Cluster with following command
      cd /usr/local/spark
      ./sbin/start-master.sh
      # get the master node url like "spark://ip-xxx-xxx-xxx-xxx:7077"
      ./sbin/start-slave.sh $master_url

2. Upsert(update or insert) vertex(vertices) with data stored in Spark to TigerGraph
=======
    Upsert vertices: "Amanda,Amanda,20,female,ca" and “Leo,Leo,23,male,ca” to graph named "social".

    Step 1. [Linux Shell] Create and install a loading job to setup a loading tag. 
      Run the following command
        gsql postVertex.gsql
   
    Step 2. [Linux Shell] Run Java program to post data (stored in Spark) to TigerGraph via a REST endpoint. Note that filename below specifies the filename variable “postVertexFileName”, so that we know which loading statement will be used.
      Compile and run Java example with following command
        # bash compileAndRun.sh vertex_or_edge graph_name loading_job_name file_name_variable file_separator end_of_line_charactor input_file_path master_url
        bash compileAndRun.sh vertex social postVertex postVertexFileName , \\n ./vertexData.csv spark://172.30.1.58:7077

    Step 3. [Option][GSQL Shell] verify upsert resulti with following gsql command
      use graph social
      SELECT * FROM person WHERE primary_id=="Amanda"
      SELECT * FROM person WHERE primary_id=="Leo"

3. Upsert(update or insert) edge(s) with data stored in Spark to TigerGraph
=======
    Upsert edges: "Amanda,Leo,2011-08-08" and "Amanda,Tom,2016-03-05" to graph named "social".

    Step 1. [Linux Shell] Create and install a loading job to setup a loading tag.
      Run the following command
        gsql postEdge.gsql

    Step 2. [Linux Shell] Run Java program to post data (stored in Spark) to TigerGraph via a REST endpoint. Note that filename below specifies the filename variable “postVertexFileName”, so that we know which loading statement will be used.
      Compile and run Java example with following command
        # bash compileAndRun.sh vertex_or_edge graph_name loading_job_name file_name_variable file_separator end_of_line_charactor input_file_path master_url
        bash compileAndRun.sh edge social postEdge postEdgeFileName , \\n ./edgeData.csv spark://172.30.1.58:7077

   Step 3. [Option][GSQL Shell] verify upsert result with following gsql command 
     use graph social
     SELECT * FROM person-(friendship)->person WHERE from_id =="Amanda" 
