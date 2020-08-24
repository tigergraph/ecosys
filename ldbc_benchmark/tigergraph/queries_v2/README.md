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
¦   +---SF10000        # Schema for SF <100 and 10000 are a little different.
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
1. The queries work for tigergraph 3.0 (build on 8/4/2020). Older 3.0 package may have bugs and generate wrong results for bi8 and bi22.
2. IS and IC queries usually start from a single vertex. Long linear queries are used here, but some queries are not very efficient. BI are expensive and written in a way that minimize the running time. 
3. ORDER BY cannot be used for distributed query and there are some bugs for distribtued query. Due to bugs on to_vertex_set, IS4-7 give empty results if distributed query is used. Due to bug on listAccum<VERTEX>, ic14 and bi25 cannot be installed in distributed query. We did not wirte is1-3 and ic1 into distributed query because there is no speed improvement. There are bugs when converting V1 is2,bi6,bi17 into distributed queries.
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
bi25 and ic14 requires user defined function bigint_to_string, which is included in ExprFunctions.hpp. Copy the file to tigergraph package.
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
# to parse queries for SF<100, source gsql_batch.sh queries/*.gsql queries/SF100/*.gsql
# The query list is printed out in the end. If any query fails, you can remove the failed queries and reassign value to $query_list, for example, query_list=ic1,ic2,ic3

#install the queries in $query_list
install

#run each query in query_list for 3 times. Output results in log/ and time and error info in err/
#  Usage: run <seed_file>. default seed_file is seed/seed_SF10000.txt
run 

#to run in background and direct output to nohup.out 
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

Output shows the difference of the results, and the median time of three runs. The script reads results in log/, takes the fields of interest, sorts them and dumps parsed results to parsed_result/. You can use 'diff' command or text compare tools on the parsed results and see how the results are different between your runs and gold answer in result/SF10000. The script can read old json format ('../queries_pattern_match/result/') when option '--old' is turned on.

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
...
```

PASS indicate the results are the same as the target gold answer. To extract the time information for copying into google sheet
```
python3 compare_result.py | awk -F":" '{print $3}' | tr -d 's'
```

