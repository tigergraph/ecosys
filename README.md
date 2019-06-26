# ecosys
TigerGraph Ecosystem

Key Folders:
* etl - The Connector Ecosystem
  * dist_split - utility to split large data files for loading onto a distributed system
  * kafka-connect - kafka connector
  * spark_loading - This program uses Spark SQL to load parquet from Amazon S3 and store as CSV locally or in S3.
  * tg_connect - Contains two utilities to read csv files from Hadoop or records from a RDBMS and then posts to TigerGraph.
  * tg-java-driver - JDBC Type 4 Driver

* graph_algorithms - moved to https://github.com/tigergraph/gsql-graph-algorithms

* gurus_scripts - Data and GSQL scripts so users can replicate the demostrations in our Graph Gurus webinar series
  * comm_dect_demo
  * fraud_detection_demo
  * geospatial_search
  * loop_detection_demo
  * movie_recommendation
  * network_IT_resource
  * pagerank_demo
  * pattern_match
  * temporal_data
