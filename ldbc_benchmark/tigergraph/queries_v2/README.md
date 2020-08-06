# LDBC SNB Benchmark v2 syntax - scale factor (SF) 100 and 10000
# Table of Content
1. [Overview](#Overview)
2. [Directory structure](#Directory structure)
3. [How to run Benchmarks](#How to run Benchmarks)

## Overview
LDBC specification:
http://ldbc.github.io/ldbc_snb_docs/ldbc-snb-specification.pdf

Benchmark results: 
https://docs.google.com/spreadsheets/d/1TiFh4q_7W2g0392w5V-0hNxb0gQlXqP6k6wxiaxpCsw/edit#gid=0 

## [Directory structure]
```
queries_v2
¦   gsql_batch.sh    # for running gsql files in batch
¦   compare_result.py # for parsing and compare results
¦
+---queries_v1/queries
¦   ¦   is[1-7].gsql         # all IS queries are non-distributed query
¦   ¦   ic[2-4,6-14].gsql    # some IC queries are distributed
¦   ¦   bi[1-25].gsql        # all BI queries are distributed query, otherwise too slow
¦   ¦
¦   +---SF100/SF10000  # Schema for SF 100 and 10000 are a little different.
¦       ¦   ic1.gsql   # Compared to SF100, SF10000 removes email and language in Person vertex
¦       ¦   ic5.gsql   # Compared to SF100, SF10000 changes joinDate to creationDate in WORK_AT edge 
¦
+---result             # Gold answer from previous run
¦       SF10000
¦       SF100
+---seed               # seed for queries
    ¦   seed_SF10000.txt 
    ¦   seed_SF100.txt

Comment on queries: 
1. ORDER BY cannot be used for distributed query and there are some bugs for distribtued query. Some queries are not written in distributed query due to bugs. 
2. IS and IC queries usually start from a single vertex. Long linear queries are used here, but some queries are not very efficient. BI are expensive and usually divided into short-path queries. 
3. Due to bugs on to_vertex_set, IS4-7 give empty results if distributed query is used. Due to bug on listAccum<VERTEX>, ic14 and bi25 cannot be installed in distributed query. 
4. query_v1/bi22 didn't use v1 version because it takes too long (~40min). Due to bugs in distributed query, my validation on bi22 does not go through. The result for bi22 may be wrong.
```

## [How to run Benchmarks]
### install tigergraph
Tigergraph Installation and Loading for SF10000 on 24 CentOS machines: 
https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/1347289765/LDBC+Social+Network+Benchmark+6T+local+machine

Tigergraph Installation and Loading for SF100 on AWS r4.8xlarge: 
https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/802619473/LDBC+Social+Network+Benchmark

### Clone the repository 
```bash
git clone git@github.com:tigergraph/ecosys.git
cd ecosys
git checkout ldbc
cd ldbc_benchmark/tigergraph/queries_v2
```

### Run queries
bi25 and ic14 requires user defined function, which is included in ExprFunctions.hpp. Copy the file to tigergraph package.
```bash
#get the root directory of tigergraph
ROOTDIR=$(gadmin config get System.AppRoot)

# Copy paste the user defined function to the tigergraph directory
cp ExprFunctions.hpp $ROOTDIR/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
```

gsql_batch.sh is the script to parse gsql files in batch. The script stores query names in environment variable $query_list and loads three functions: install, drop and run to install, drop and run those queries. For help info: run "gsql_batch.sh -h"
```bash
#parse v2 queries for SF 10000
source gsql_batch.sh queries/*.gsql queries/SF10000/*.gsql
# The query list is printed out in the end. If any query fails, you can remove the failed queries and reassign value to $query_list, for example, query_list=ic1,ic2,ic3

#install the queries in $query_list
install

#run each query in query_list for 3 times. Output results in log/ and time info in err/
run 

#if you want to run in background 
( trap "true" HUP ; run ) > nohup.out 2>/dev/null </dev/null & disown
```

### Compare Results
To compare results with results in results/SF10000. Use 'python3 compare_result.py -h' for help mesage 
```bash
#If you don't have python3, For CentOS 
yum install -y python3-devel

# Show the running time of my queries and parse the results to parsed_result/.
python3 compare_result.py 

# If you also want to compare the results with the benchmark results of SF10000
python3 compare_result.py -c result/SF10000
```

Output shows the difference of the results, and the smallest time of three runs. The script reads results in log/, takes the fields of interest, sorts them and dumps parsed results to parsed_result/. You can use 'diff' command or text compare tools on the parsed results and see how the results are different between your runs and gold answer in result/SF10000. The script can read old json format ('../queries_pattern_match/result/') when option '--old' is turned on.

Example output of 'python3 compare_result.py -c result/SF10000':
```
ic1:PASS
time:1.88s
ic2:PASS
time:2.51s
ic3:PASS
time:8.35s
ic4:PASS
time:2.48s
ic5:PASS
time:12.6s
ic6:PASS
time:4.48s
ic7:PASS
time:2.56s
ic8:PASS
time:2.47s
ic9:PASS
time:5.92s
ic10:PASS
time:3.24s
ic11:PASS
time:4.91s
ic12:PASS
time:5.69s
ic13:PASS
time:1.7s
ic14:PASS
time:1.29s
is1:PASS
time:1.12s
is2:PASS
time:1.06s
is3:PASS
time:1.13s
is4:PASS
time:1.05s
is5:PASS
time:1.1s
is6:PASS
time:0.87s
is7:PASS
time:0.94s
bi1:PASS
time:7.13s
bi2:PASS
time:18.89s
bi3:PASS
time:7.9s
bi4:PASS
time:16.26s
bi5:PASS
time:22.66s
bi6:PASS
time:172.71s
bi7:PASS
time:582.86s
bi8:PASS
time:21.58s
bi9:PASS
time:29.91s
bi10:PASS
time:112.27s
bi11:PASS
time:1178.61s
bi12:PASS
time:4.93s
bi13:PASS
time:23.18s
bi14:PASS
time:121.55s
bi15:PASS
time:3.75s
bi16:PASS
time:469.56s
bi17:PASS
time:21.83s
bi18:PASS
time:232.44s
bi19:PASS
time:311.85s
bi20:PASS
time:377.36s
bi21:PASS
time:10.01s
bi22:PASS
time:2283.67s
bi23:PASS
time:14.46s
bi24:PASS
time:123.85s
bi25:PASS
time:60.39s
```

PASS indicate the results are the same as the target gold answer. 

