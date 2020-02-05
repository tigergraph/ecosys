This program reads a csv file from Hadoop and then posts to TigerGraph.

PreRequisite
============
1) TigerGraph has created the schema and the loading jobs
2) upload third_party/json-simple-1.1.1.jar to all Hadoop data nodes, under Hadoop path
3) For each loading job X, create an ETL config_X.json (see example test/config_hadoop.json)
4) Make sure TG configuration is correct


Run a loading job X:
====================
>> copy over config_x.json to /tmp/tg_demo.json, on all the data nodes 
>> ./etl_hadoop2tg.sh <source_file> <log_path>
e.g.
/user/data/soc_csv/bank_transfer_records.csv /user/data/soc_csv/bank_transfer_records.out


Tip 1: if the loading takes time, use "nohup" to run the loading even the session logout
 >> nohup ./etl_hadoop2tg.sh /user/data/soc_csv/bank_transfer_records.csv /user/data/soc_csv/bank_transfer_records.out
Tip 2: remove the log from hadoop before the run
>> hadoop fs -rm -r <log_path>


===================================================================
Advanced Usage
===================================================================
>> How to modify the source Java code, which are all under com.tigergraph.connector.hadoop
HadoopTGWriter.java	Hdfs2Tg.java		TGHadoopConfig.java

 - Hdfs2Tg
    main class, read in config, run mapper function and send data to TigerGraph
 - TGHadoopConfig
    read in the config file
 - HadoopTGWriter:
    buffer a batch of records, and use REST API post to TG

>> How to build
   ./make_hadoop.sh

>> How to write a config file for loading job X
First, make sure the config file is a valid JSON. Now look at this example (as under test/):
{
	"TG_URL": "http://192.168.10.113:9000",
	"TG_GRAPH": "gsql_demo",
	"TG_LOADJOB": "load_social1_social_users_csv",
	"TG_SEPARATOR_ASCII": 44,
	"TG_SEPARATOR_URL": ",",
	"TG_EOL_ASCII": 10,
	"TG_EOL_URL": "%0A",
	"TG_BATCH_SIZE": 100
}
Explanation:
- TG_URL
- TG_GRAPH: graph name
- TG_LOADJOB: the online loading job X's name. Must be recreated first
- TG_SEPARATOR_ASCII: this is the integer value of the separator char (https://www.asciitable.com/)
  TG_SEPARATOR_URL: this is same char which appears in URL (https://www.w3schools.com/tags/ref_urlencode.asp)
- TG_EOL_ASCII: end of line as integer (https://www.asciitable.com/)
  TG_EOL_URL: end of line in URL (https://www.w3schools.com/tags/ref_urlencode.asp)
- TG_BATCH_SIZE: this is the batch size when to post to TigerGraph, e.g., how many records in one post.
  Normally the larger, the better loading speed. But TigerGraph has the binary size limit on a post message,
  so choose this value based on the record size. Normally 100 to 500 is good.

===================================================================
License and contributing
===================================================================
All this code is in the public domain. The first version was created by TigerGraph.
If you'd like to contribute something, we would love to add your name to this list.
