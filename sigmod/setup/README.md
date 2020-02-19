Data Sets
==========
https://s3-us-west-1.amazonaws.com/tigergraph-benchmark-dataset/LDBC/SF-10/ldbc_snb_data-sf10.tar.gz

https://s3-us-west-1.amazonaws.com/tigergraph-benchmark-dataset/LDBC/SF-100/ldbc_snb_data-sf100.tar.gz

https://s3-us-west-1.amazonaws.com/tigergraph-benchmark-dataset/LDBC/SF-1000/ldbc_snb_data-sf1000.tar.gz

To setup tigergraph
====================
Download latest Tigergraph from https://www.tigergraph.com/developer/
Set timeout value to 6000s by following 
https://github.com/tigergraph/ecosys/blob/benchmark/benchmark/tigergraph/README


To setup neo4j
===============
Enterprise 4.0 verison
https://neo4j.com/download-neo4j-now/

Modify neo4j config
vim /data/neo4j-enterprise-4.0.0/conf/neo4j.conf
modify the following entries
dbms.security.auth_enabled=false
dbms.memory.heap.initial_size=100g
dbms.memory.heap.max_size=100g
dbms.memory.pagecache.size=194000m

dbms.transaction.timeout=9000s

dbms.active_database=ldbc10.db

- Modify env_var.sh to point your neo4j installation point. 
- run source env_var.sh
- run one_step_load.sh

