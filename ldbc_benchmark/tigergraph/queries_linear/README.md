# Linear query benchmark on ldbc_snb SF100 and SF1000
## Overview
Benchmark results: https://docs.google.com/spreadsheets/d/1TiFh4q_7W2g0392w5V-0hNxb0gQlXqP6k6wxiaxpCsw/edit#gid=0
Install tigergraph and load data for SF1000 on 24 machines: https://graphsql.atlassian.net/wiki/spaces/GRAP/pages/1347289765/LDBC+Social+Network+Benchmark+6T+local+machine

## Prerequisites
Please make sure that you already installed TigerGraph, loaded all LDBC SNB data into it, and logged in as the user can run TigerGraph. Then you type the following commands to install a helper function and all GSQL queries:
```bash
git clone git@github.com:tigergraph/ecosys.git
cd ecosys
git checkout ldbc
cd ldbc_benchmark/tigergraph/queries_linear
```

## Run queries
bi25 and ic14 requires user defined function, which is included in ExprFunctions.hpp. Copy the file to tigergraph package.
```bash
ROOTDIR=$(gadmin config get System.AppRoot)
cp ExprFunctions.hpp $ROOTDIR/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
```

To parse, install and run the queries. 
```bash
# parse all queries. If you only want to parse ic queries, run 'source main.sh ic*'
# This script also load function 'install, drop, run' 
source main.sh

# Neglect this step if no query fails.
# The query list is printed out in the end of terminal. 
# If any query fails, remove the failed query from the query_list 
query_list=ic1,ic2,ic3,...

#set seed, default seed/seed_SF10000.txt
seed=seed/seed_SF10000.txt

#install the queries in query_list
install

#run each query in query_list for 3 times
run
```


The log of queries are stored in folder log/ and time information is in err/ 
For example, if you want to run query ic4-10, you can parse all ic queries using 'source main.sh ic*', then 'export query_list=ic4,ic5,ic6,ic7,ic8,ic9,ic10; install; run'. 
Right now for SF100, bi19 cannot work. 

## Compare results
To compare results with the old one
```bash
python3 compare_result.py [-q/--query query] [-r/--result result] [--old] [--log log] [--err err]

# default is set for SF100.
python3 compare_result.py 
```
* options
  * -q query (default all): which query to run, candidates are
    * 'all' all queries
    * 'ic' all ic queries
    * 'ic1,ic2' ic1 and ic2
  * -r result (default SF100): directory containing the target results.
  * --old: include this if the target results is in old format. old  result are in ecosys/ldbc_benchmark/tigergraph/queries_pattern_match/result/SF-100/. To compare with old results 'python3 compare_result.py -r ../queries_pattern_match/result/SF-100/ --old'. Some queries may not pass 
  * --log log (defaul log) log of queries
  * --err err (defaul err) time information of queries


* Output shows the difference of the results, and the smallest time of three runs. The script also dumps current results (parsed from --log) to result/ and the target results (parsed from -r) to result0/. The text files in the two folders should be exactly the same. You can use 'diff' command or text compare tools to see how the results are different between new and old runs.
```
ic1:Fail: number of rows 7 != 5
time:5.53s
ic2:PASS
time:0.85s
ic3:PASS
time:2.23s
ic4:PASS
time:1.31s
ic5:PASS
time:2.99s
ic6:PASS
time:4.7s
ic7:PASS
time:0.76s
ic8:PASS
time:0.72s
ic9:PASS
time:6.58s
ic10:PASS
time:0.95s
ic11:PASS
time:0.82s
ic12:PASS
time:0.97s
ic13:PASS
time:0.71s
ic14:No benchmark for ic14
time:0.82s
is1:PASS
time:0.71s
is2:PASS
time:0.73s
is3:PASS
time:2.81s
is4:PASS
time:0.71s
is5:PASS
time:0.72s
is6:PASS
time:0.72s
is7:PASS
time:0.72s
bi1:PASS
time:19.16s
bi2:PASS
time:4.15s
bi3:PASS
time:24.9s
bi4:PASS
time:32.21s
bi5:PASS
time:4.21s
bi6:PASS
time:30.95s
bi7:No benchmark for bi7
time:140.43s
bi8:PASS
time:81.44s
bi9:PASS
time:1.24s
bi10:PASS
time:1.52s
bi11:PASS
time:17.62s
bi12:PASS
time:302.49s
bi13:No benchmark for bi13
time:29.68s
bi14:PASS
time:27.43s
bi15:PASS
time:0.89s
bi16:No benchmark for bi16
time:82.74s
bi17:No benchmark for bi17
time:1.48s
bi18:No benchmark for bi18
time:82.1s
bi19:No benchmark for bi19
No result for bi19
bi20:No benchmark for bi20
time:43.55s
bi21:Fail: (0,0) 105512 != 24189256211383
time:9.05s
bi22:PASS
time:111.59s
bi23:PASS
time:5.85s
bi24:PASS
time:107.94s
bi25:No benchmark for bi25
time:1.68s
```
Old results of ic1 and bi21 are wrong.  bi19 exceeds memroy limit and cannot be tested.