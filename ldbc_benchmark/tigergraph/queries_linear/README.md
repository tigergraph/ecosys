# Linear Query Benchmark For LDBC SNB - scale factor (SF) 100 and 10000
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

Schema for SF 100 and 10000 are a little different. Compared to SF100, SF10000 removes email and language in Person vertex, and replaces joinDate in WorkAt with creationDate. ic1 and ic5 are different for SF 100 and 10000. These queries are located in different subfolder in queries/SF100 and queries/SF10000. 

Use main.sh to parse the queries. The script also loads three functions: install, drop and run 

Usage for main.sh: 

**main.sh -h** for help message. 

**main.sh [queries] [scale_factor]**
* queries, queries to parse, default is '*.gsql' (quotation mark is needed)
* scale factor, default is 10000

to parse ic queries for SF 10000: source main.sh 'ic*'

to parse all queries for SF 100: source main.sh '*.gsql' 100

```bash
# to parse all queries for SF 10000. 
source main.sh

# Neglect this step if no query fails.
# The query list is printed out in the end of main.sh output. 
# If any query fails, remove the failed query from the query_list 
query_list=ic1,ic2,ic3,...

#install the queries in query_list
install

#You can change seed, which is the input for 'run commmand'. default value set by main.sh is seed/seed_SF10000.txt
seed=seed/seed_SF10000.txt

#run each query in query_list for 3 times. results in log/ and time info is in err/
run
```
The log of queries are stored in folder log/ and time information is in err/ 
For example, if you want to run query ic4-10, you can parse all ic queries using 'source main.sh ic*', then 'export query_list=ic4,ic5,ic6,ic7,ic8,ic9,ic10; install; run'. 

##Comment on the queries
is and ic queries usually start from a single vertex. Long linear queries are used here, but some queries are not very efficient. 
bi queries usually start from a goup of vertices and are expensive. Long path queries are sometimes divided into short-path queries to reduce the running time. I have submitted tickets for improving the performance of long-path queries. Hopefully some long-path query can be used in the future. 

Some query cannot be written in distributed query due to some bugs. Due to bugs on to_vertex_set, is4-7 give empty results if distributed query is used. Due to bug on listAccum<VERTEX>, ic14 and bi25 cannot be installed. Other queries are written in distributed query. 


## Compare results
To compare results with the old one
```bash
# Show the running time of my queries and parse the results.
python3 compare_result.py 

# If you also want to compare the results with the benchmark results of SF10000
python3 compare_result.py -c SF10000
```
Usage for compare_result.py:

**python3 compare_result.py [-q/--query query] [-c/--compare result_folder] [--old] [--log log] [--err err]**
* -q --query (default all): which query to run, candidates are
  * 'all': all queries
  * 'ic': all ic queries
  * 'ic1,ic2': ic1 and ic2
* -c --compare result to compare (default None): directory containing the target results.
  * to compare against previous SF10000 results for BI queries: python3 compare_result.py -q bi -c result/SF10000/  
* --old: include this if the target results is in old format. old  results are ../queries_pattern_match/result/SF-100/. 
  * to compare with old results: 'python3 compare_result.py -c ../queries_pattern_match/result/SF-100/ --old'. Old results misinterprete some spaces into new lines, and may not be the same as the current runs. 
* --log log (defaul ./log) log of queries
* --err err (defaul ./err) time information of queries

Output shows the difference of the results, and the smallest time of three runs. The script also dumps current results (parsed from --log) to parsed_result/ and the target results (parsed from -c) to parsed_result0/. If the text files in these two folders are exactly the same, this means you got the same results from the current run. You can use 'diff' command or text compare tools to see how the results are different between new and old runs. Example output of 'python3 compare_result.py':
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
