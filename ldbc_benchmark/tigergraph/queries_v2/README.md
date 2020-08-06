# LDBC SNB Benchmark v2 syntax - scale factor (SF) 100 and 10000
## Overview
Benchmark results: 
https://docs.google.com/spreadsheets/d/1TiFh4q_7W2g0392w5V-0hNxb0gQlXqP6k6wxiaxpCsw/edit#gid=0 

## Setup
Tigergraph Installation and Loading for SF10000 on 24 CentOS machines: 
https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/1347289765/LDBC+Social+Network+Benchmark+6T+local+machine

Tigergraph Installation and Loading for SF100 on AWS r4.8xlarge: 
https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/802619473/LDBC+Social+Network+Benchmark

### clone the repository 
```bash
git clone git@github.com:tigergraph/ecosys.git
cd ecosys
git checkout ldbc
cd ldbc_benchmark/tigergraph/queries_linear
```

## Run queries
bi25 and ic14 requires user defined function, which is included in ExprFunctions.hpp. Copy the file to tigergraph package.
```bash
#get the root directory of tigergraph
ROOTDIR=$(gadmin config get System.AppRoot)

# Copy paste the user defined function to the tigergraph directory
cp ExprFunctions.hpp $ROOTDIR/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
```

Schema for SF 100 and 10000 are a little different. Compared to SF100, SF10000 removes email and language in Person vertex, and replaces joinDate in WorkAt with creationDate. ic1 and ic5 are different for SF 100 and 10000. These queries are located in different subfolder in queries/SF100 and queries/SF10000. There is the diretory structure

```
queries_v2
¦   gsql_batch.sh
¦   file001.txt    
¦
+---queries_v1/queries
¦   ¦   is[1-7].gsql
¦   ¦   ic[2-4,6-14].gsql
¦   ¦   bi[1-25].gsql
¦   ¦
¦   +---SF100/SF10000
¦       ¦   ic1.gsql
¦       ¦   ic5.gsql
+---result
¦       SF10000
¦       SF100
+---seed
    ¦   seed_SF10000.txt
    ¦   seed_SF100.txt
```

gsql_batch.sh is the script to parse many gsql files. The script stores query names in environment variable $query_list and also loads three functions: install, drop and run to install, drop and run those queries.

Usage for gsql_batch.sh: 
**gsql_batch.sh -h** for help message. 
**gsql_batch.sh [queries]**
* queries - queries to parse, default is *.gsql, 
* examples
* to parse v2 queries for SF 10000. 
** source gsql_batch.sh queries/*.gsql queries/SF10000/*.gsql
* to parse v1 queries for SF 10000.  
** source gsql_batch.sh queries_v1/*.gsql queries_v1/SF10000/*.gsql
* to parse v2 ic queries for SF 10000. 
** source gsql_batch.sh queries/ic*.gsql queries/SF10000/ic*.gsql

```bash
#parse v2 queries for SF 10000
source gsql_batch.sh queries/*.gsql queries/SF10000/*.gsql
# The query list is printed out in the end 
# If any query fails, remove the failed query from the $query_list 
query_list=ic1,ic2,ic3

#install the queries in $query_list
install

#run each query in query_list for 3 times. results in log/ and time info is in err/
run
#you can specify other seed file, if you want to run seed for SF100
# run seed/seed_SF10000.txt

```
The log of queries are stored in folder log/ and running time is in err/ 

##Comment on the queries
is and ic queries usually start from a single vertex. Long linear queries are used here, but some queries are not very efficient. 
bi queries usually start from a goup of vertices and are expensive. Long path queries are sometimes divided into short-path queries to reduce the running time. I have submitted tickets for improving the performance of long-path queries. Hopefully some long-path query can be used in the future. 

Some query cannot be written in distributed query due to some bugs. Due to bugs on to_vertex_set, is4-7 give empty results if distributed query is used. Due to bug on listAccum<VERTEX>, ic14 and bi25 cannot be installed. Other queries are written in distributed query. 


## Compare results
To compare results with the old one
```bash
#If you don't have python3, For CentOS 
yum install -y python3-devel

# Show the running time of my queries and parse the results to parsed_result/.
python3 compare_result.py 

# If you also want to compare the results with the benchmark results of SF10000
python3 compare_result.py -c result/SF10000
```
usage: compare_result.py [-h] [-q QUERIES] [-c COMPARE] [-l LOG] [-e ERR]
                         [-s SAVE] [-sc SAVE_COMPARE] [--old]

Parse the results to python str and compare to target result

optional arguments:
  -h, --help            show this help message and exit
  -q QUERIES, --queries QUERIES
                        queries to parse and compare (default: all). example:
                        -q ic1, -q ic
  -c COMPARE, --compare COMPARE
                        folder of target results to compare (default: None).
                        example: -c result/SF10000
  -l LOG, --log LOG     folder of the current results (default: log)
  -e ERR, --err ERR     folder of the running time (default: err)
  -s SAVE, --save SAVE  folder to save the parsed format of current results
                        (default: parsed_result)
  -sc SAVE_COMPARE, --save_compare SAVE_COMPARE
                        folder to save the parsed target results (default:
                        None)
  --old                 True if the target results to compare is in the old
                        JSON format



Output shows the difference of the results, and the smallest time of three runs. The script takes the field of interest, order them and dumps current results (parsed from --log) to parsed_result/. You can use 'diff' command or text compare tools on the parsed results and see how the results are different between your runs and gold answer in result/SF10000. Example output of 'python3 compare_result.py'. The script also support reading old json format ('../queries_pattern_match/result/') when option '--old' is turned on:
```
ic1:time:18.16s
ic2:time:2.66s
ic3:time:11.48s
ic4:time:2.88s
ic5:time:4.53s
ic6:time:9.53s
ic7:time:2.76s
ic8:time:4.39s
ic9:time:7.43s
ic10:time:10.42s
ic11:time:6.82s
ic12:time:13.32s
ic13:time:2.36s
ic14:time:1.23s
is1:time:1.66s
is2:time:5.91s
is3:time:1.53s
is4:time:0.97s
is5:time:0.79s
is6:time:1.44s
is7:time:0.86s
bi1:time:7.8s
bi2:time:87.04s
bi3:time:12.98s
bi4:time:13.25s
bi5:time:5.06s
bi6:time:6.27s
bi7:time:606.21s
bi8:time:25.5s
bi9:time:13.59s
bi10:time:20.09s
bi11:time:41.98s
bi12:time:4.9s
bi13:time:36.44s
bi14:time:407.28s
bi15:time:5.46s
bi16:time:828.71s
bi17:time:37.47s
bi18:time:34.9s
bi19:time:332.43s
bi20:time:108.46s
bi21:time:170.42s
bi22:time:278.4s
bi23:time:71.22s
bi24:time:78.09s
bi25:time:61.76s
```
