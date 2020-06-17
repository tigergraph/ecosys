# Linear query benchmark on ldbc_snb
## Run queries
To parse, install and run the queries. 
```bash
source main.sh
The log of queries are stored in folder log/ and error message containing time information is in err/ 

main.sh is a simply bash script and can be edited. For example, if you want to run query ic4 and ic5, modify line 4 to 
```bash
for q in ic4 ic5 ic6 ic7
```
Right now, bi19 cannot work.

## Compare results
The old results for SF100 is in SF100/. To compare results with the old one
```bash
python3 compare_result.py
```
It also dumps python ast of old results in result0/ and current results in result/. The two text files should be exactly the same. 
Output is as follows
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
is7:Fail: number of rows 3 != 1
time:2.98s
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
bi21:Fail: number of rows 100 != 31
time:8.75s
bi22:PASS
time:111.59s
bi23:PASS
time:5.85s
bi24:PASS
time:107.94s
bi25:No benchmark for bi25
time:1.68s
```
Old results of ic1, is7 and bi21 are wrong.  bi19 exceeds memroy limit and cannot be tested.