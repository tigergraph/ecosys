# ecosys
TigerGraph Ecosystem

Key Folders:
* etl - The Connector Ecosystem
  * dist_split - utility to split large data files for loading onto a distributed system
  * tg-kafka-connect - kafka connector
  * tg-python-wrapper - Python wrapper with examples of calling TigerGraph queries
  * tg-s3-parquet-load - This program uses Spark SQL to load parquet from Amazon S3 and store as CSV locally or in S3
  * tg-rdbms-import - utility to read records from a RDBMS and then posts to TigerGraph
  * tg-hadoop-connect - utility to read csv files from Hadoop and then posts to TigerGraph
  * tg-jdbc-driver - JDBC Type 4 Driver for Spark, Python and Java

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
