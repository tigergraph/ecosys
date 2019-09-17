This program reads records from a RDBMS and then posts to TigerGraph.

PreRequisite
============
1) TigerGraph has created the schema and the loading jobs
2) Download the correct jdbc driver to third_party dir. Currently mySQL jdbc driver is included
3) For each loading job X, create an ETL config_X.json (see example test/config_jdbc2tg.json)
4) Make sure the JDBC configuration and TG configuration are correct


Run a loading job X:
====================
 >> ./etl_tgjdbc.sh config_X.json


Tip 1: if the loading takes time, use "nohup" to run the loading even the session logout
 >> nohup ./etl_tgjdbc.sh config_X.json >load_x.out
Tip 2: you may specify the database password in command line, which will override the one on config
>> ./etl_tgjdbc.sh config_X.json  mydbpassword


Advanced Usage
==============
>> How to modify the source Java code, which are all under com.tigergraph.connector.jdbc
 - TGJDBCSelectRunner
    main class, read in config, select from RDBMS as a ResultSet, and post to TigerGraph
 - TGJdbcConfig
    read in the config file
 - TGJdbcReader
    connect to RDBMS, run the query, and convert each record to string
 - JdbcTGWriter
    buffer a batch of records, and use REST API post to TG

>> How to build
   ./make_jdbc.sh

>> How to write a config file for loading job X
First, make sure the config file is a valid JSON. Now look at this example (as under test/):
{
	"JDBC_DRIVER": "com.mysql.cj.jdbc.Driver",
	"JDBC_URL": "jdbc:mysql://192.168.10.113:3306/tg_src_db?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC&useSSL=false",
	"DB_USER": "root",
	"DB_PASSWORD": "tigergraph",
	"DB_QUERY": "select * from SocialUser",
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
- JDBC_DRIVER: make sure this match the RDBMS and the downloaded driver
- JDBC_URL: refer the JDBC driver document. Here the database name is included.
- DB_USER
- DB_PASSWORD: this can be overridden by the command line
- DB_QUERY: the select statement. If the query is complicated, suggest to create a view in RDBMS first.

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


License and contributing
========================
All this code is in the public domain. The first version was created by TigerGraph.
If you'd like to contribute something, we would love to add your name to this list.
