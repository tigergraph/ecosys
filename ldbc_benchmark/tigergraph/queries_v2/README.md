# LDBC SNB Benchmark v2 syntax - scale factor (SF) 100 and 10000
# Table of Content
- [Overview](#overview)
- [Directory structure](#directory-structure)
- [How to run Benchmarks](#how-to-run-benchmarks)
  * [install tigergraph](#install-tigergraph)
  * [Clone the repository](#clone-the-repository)
  * [Run queries](#run-queries)
  * [Compare Results](#compare-results)

## Overview
LDBC specification:
http://ldbc.github.io/ldbc_snb_docs/ldbc-snb-specification.pdf

Benchmark results: 
https://docs.google.com/spreadsheets/d/1TiFh4q_7W2g0392w5V-0hNxb0gQlXqP6k6wxiaxpCsw/edit#gid=0 

## Directory structure 
```
queries_v2
¦   gsql_batch.sh          # for running gsql files in batch
¦   compare_result.py      # for parsing and compare results
¦
+---queries                # queries with linear per pattern
+---queries_v1             # queries based on old version
¦   ¦   is[1-7].gsql       # all IS queries are non-distributed query
¦   ¦   ic[2-4,6-14].gsql  # some IC queries are distributed
¦   ¦   bi[1-25].gsql      # all BI queries except BI25,BI17 are distributed query
¦   ¦
¦   +---SF100
¦   +---SF10000        # Schema for SF 100 and 10000 are a little different.
¦       ¦   ic1.gsql   # Compared to SF100, SF10000 removes email and language in Person vertex
¦       ¦   ic5.gsql   # Compared to SF100, SF10000 changes joinDate to creationDate in WORK_AT edge 
¦
+---result             # Parsed results from previous runs (validated)
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

## How to run Benchmarks
### install tigergraph 3.0 released version 
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
time:2.78s
ic2:PASS
time:2.61s
ic3:PASS
time:10.78s
ic4:PASS
time:2.72s
ic5:PASS
time:6.47s
ic6:PASS
time:9.35s
ic7:PASS
time:2.63s
ic8:PASS
time:4.37s
ic9:PASS
time:7.22s
ic10:PASS
time:3.18s
ic11:PASS
time:6.66s
ic12:PASS
time:3.05s
ic13:PASS
time:2.37s
ic14:PASS
time:1.27s
is1:PASS
time:1.03s
is2:PASS
time:1.52s
is3:PASS
time:0.96s
is4:PASS
time:1.12s
is5:PASS
time:1.03s
is6:PASS
time:1.76s
is7:PASS
time:1.15s
bi1:PASS
time:7.99s
bi2:PASS
time:88.05s
bi3:PASS
time:13.11s
bi4:PASS
time:12.94s
bi5:PASS
time:5.44s
bi6:PASS
time:5.8s
bi7:PASS
time:506.5s
bi8:PASS
time:21.91s
bi9:PASS
time:12.67s
bi10:PASS
time:38.49s
bi11:PASS
time:39.76s
bi12:PASS
time:6.4s
bi13:PASS
time:31.73s
bi14:PASS
time:412.11s
bi15:PASS
time:5.03s
bi16:PASS
time:194.13s
bi17:PASS
time:39.38s
bi18:PASS
time:34.67s
bi19:PASS
time:324.16s
bi20:PASS
time:454.12s
bi21:PASS
time:159.85s
bi22:PASS
time:37.64s
bi23:PASS
time:61.04s
bi24:PASS
time:75.94s
bi25:PASS
time:66.83s
```

PASS indicate the results are the same as the target gold answer. 

