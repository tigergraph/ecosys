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

#Copy paste the user defined function to the tigergraph directory
cp ExprFunctions.hpp $ROOTDIR/dev/gdk/gsql/src/QueryUdf/ExprFunctions.hpp
```

Schema for SF 100 and 10000 are a little different. SF 10000 removes email and language in 
```bash
#If you benchmark on SF100, you need to change creationDate to joinDate in ic5.gsql
sed -i 's/creationDate/joinDate/g' ic5.gsql
```

To parse, install and run the queries. 
```bash
# parse all queries. 
# If you only want to parse ic queries, run 'source main.sh ic*'
# If you only want to parse all queries for SF 100, run 'source main.sh * 100'
# This script also load function 'install, drop, run' 
source main.sh

# Neglect this step if no query fails.
# The query list is printed out in the end of terminal. 
# If any query fails, remove the failed query from the query_list 
query_list=ic1,ic2,ic3,...

#You can change seed here, default value set by main.sh is seed/seed_SF10000.txt
seed=seed/seed_SF10000.txt

#install the queries in query_list
install

#run each query in query_list for 3 times
run
```
The log of queries are stored in folder log/ and time information is in err/ 
For example, if you want to run query ic4-10, you can parse all ic queries using 'source main.sh ic*', then 'export query_list=ic4,ic5,ic6,ic7,ic8,ic9,ic10; install; run'. 
Right now, bi19 is too expensive and is out of memory error. 

## Compare results
To compare results with the old one
```bash
# Show the running time of my queries and parse the results.
python3 compare_result.py 

# If you also want to compare the results with the benchmark results of SF10000
python3 compare_result.py -c ./SF100000 
```
Usage for compare_result.py:

python3 compare_result.py [-q/--query query] [-c/--compare result_folder] [--old] [--log log] [--err err]
* -q --query (default all): which query to run, candidates are
  * 'all': all queries
  * 'ic': all ic queries
  * 'ic1,ic2': ic1 and ic2
* -c --compare result to compare (default None): directory containing the target results.
  * to compare against previous SF10000 results for BI queries: python3 compare_result.py -q bi -c SF10000/  
* --old: include this if the target results is in old format. old  result are in ecosys/ldbc_benchmark/tigergraph/queries_pattern_match/result/SF-100/. To compare with old results 'python3 compare_result.py -c ../queries_pattern_match/result/SF-100/ --old'. Some queries may not pass 
* --log log (defaul log) log of queries
* --err err (defaul err) time information of queries

* Output shows the difference of the results, and the smallest time of three runs. The script also dumps current results (parsed from --log) to result/ and the target results (parsed from -c) to result0/. If the text files in these two folders are exactly the same, this means you got the same results from the current run. You can use 'diff' command or text compare tools to see how the results are different between new and old runs.
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