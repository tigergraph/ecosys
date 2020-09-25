import sys
import json
import time
import re
from datetime import datetime

ts = time.time()
utc_offset = (datetime.fromtimestamp(ts) -
              datetime.utcfromtimestamp(ts)).total_seconds()
epoch = datetime(1970, 1, 1)
p = '%Y-%m-%dT%H:%M:%S.%f'

tm_range = re.compile(sys.argv[2])
query_pattern = re.compile("RawRequest.*/query")
querylines=[]
return_pattern = re.compile("ReturnResult")
returnlines=[]
timeout_pattern = re.compile("dispatcher timeout")
timeoutlines=[]
msg_pattern = re.compile("WriteMsg\|get_requestQ_GPE")
msglines=[]

logPath = sys.argv[1]
print "The input restpp log path:\'%s\'" %logPath
print ("The input time range pattern: \'%s\'" %sys.argv[2])
tm_matched_start=False
total_searched_ln=0;
with open(logPath) as fp:
   line = fp.readline()
   while line:
       total_searched_ln += 1
       if tm_range.search(line):
          tm_matched_start=True
          if query_pattern.search(line):
              querylines.append(line)
          elif return_pattern.search(line):
              returnlines.append(line)
          elif timeout_pattern.search(line):
              timeoutlines.append(line)
          elif msg_pattern.search(line):
              msglines.append(line)
       elif tm_matched_start:
           print "\nTime range match found %d/%d matched lines with querylines: %d | returnlines: %d | timeoutlines: %d | msglines: %d, the last searched line:\n\t\'%s\'" %(len(querylines) + len(returnlines) + len(timeoutlines) + len(msglines), total_searched_ln, len(querylines),len(returnlines),len(timeoutlines),len(msglines), line[:-1])
           break
       line = fp.readline()

max_diff=0
min_diff=99999999
#Total
#grep -E "I0228 05:|I0228 06:|I0228 07:" /app/temp/tigergraph/logs/RESTPP_1_1/INFO.20200227-231251.24067|grep -E "RawRequest.*/query" > total.0228.log
#I0228 05:50:42.975419 24206 handler.cpp:271] Engine_req|RawRequest|,203889.RESTPP_1_1.1582890642975.N,NNN,0,0|GET|url = /query/Healthcare/mnr_getMemberSummary?srcIdentifier=67fb310d2fa8fff5a0014757bc6047321ba78135b44537e4da5aa1215873e64c&|payload_data.size() = 0|api = v2
totalRequests = {}
for line in querylines:
  #print line
  tokens = line.split()
  date=tokens[0]
  time=tokens[1]
  mytime = "2020-%s-%sT%s" %(date[1:3], date[3:],time)
  request = tokens[4].split('|')
  #print tokens[4]
  rid = request[2].split(',')[1]
  #print rid
  url = tokens[6].split("|")[0]
  birth = rid.split('.')[2]
  #print birth
  totalRequests[rid] = {"target": "UNKNOWN", "senttime": "UNKNOWN", "rid": rid, "birth": int(birth), "starttime": mytime, "endtime":"UNKNOWN", "diff": 99999, "timeout":True, "url":url}

#print totalRequests

#msgline example
#I0303 22:25:32.062302 19565 zeromq.cpp:271] Comm_ZMQ|MessageQueue|ZMQ|WriteMsg|get_requestQ_GPE_1Q|1|istructure_graph::default,8.RESTPP_1_1.1583274332062.N,YNN,16,0|169
target_cnt = {}
for line in msglines:
  #print line
  tokens = line.split()
  date=tokens[0]
  time=tokens[1]
  mytime = "2020-%s-%sT%s" %(date[1:3], date[3:],time)
  request = tokens[4].split('|')
  #print tokens[4]
  rid = request[6].split(',')[1]
  target = "%s_%s" %(request[4], request[5])
  if rid in totalRequests:
     totalRequests[rid]["target"] = target
     totalRequests[rid]["senttime"] = mytime
     if not target in target_cnt:
         target_cnt[target] = 0
     target_cnt[target] += 1
print "\nQuery distribution: "
print json.dumps(target_cnt,sort_keys=True, indent=4)

#Return
# I0227 14:18:41.377842 94518 requestrecord.cpp:255] Engine_req|ReturnResult|131240.RESTPP_1_1.1582841921377.N|360

for line in returnlines:
  tokens = line.split()
  date=tokens[0]
  time=tokens[1]
  mytime = "2020-%s-%sT%s" %(date[1:3], date[3:],time)
  returnT = (datetime.strptime(mytime, p) - epoch).total_seconds() - utc_offset
  rid = tokens[-1].split('|')[2]
  birth = int(rid.split('.')[2])
  diff = returnT*1000 - birth
  if rid in totalRequests:
     totalRequests[rid]["diff"] = diff
     totalRequests[rid]["endtime"] = mytime
     totalRequests[rid]["timeout"] = False
  if (diff > max_diff):
      max_diff = diff
  if (diff < min_diff):
      min_diff = diff
  #print "%f\t%s\t%s\t%s" %(diff, date, time, tokens[-1])

print "\nThe excuted query has the following response time:\n\t MAX diff: %f ms, MIN diff: %f ms" %(max_diff, min_diff)

#Timeout
#I0228 06:41:28.625191 24205 worker.cpp:1224] Engine_req|OnError|150457.RESTPP_1_1.1582893672505.N|Healthcare::default|dispatcher|dispatcher timeout

for line in timeoutlines:
  tokens = line.split()
  date=tokens[0]
  time=tokens[1]
  mytime = "2020-%s-%sT%s" %(date[1:3], date[3:],time)
  returnT = (datetime.strptime(mytime, p) - epoch).total_seconds() - utc_offset
  rid = tokens[4].split('|')[2]
  birth = int(rid.split('.')[2])
  diff = returnT*1000 - birth
  if rid in totalRequests:
     totalRequests[rid]["diff"] = diff
     totalRequests[rid]["endtime"] = mytime
     totalRequests[rid]["timeout"] = True
#print totalRequests.items()
sorted_d = sorted(totalRequests.items(), key=lambda x: x[1]["birth"])
output="/tmp/query_status.txt%s" %ts
f = open(output, "w")
f.write("target,starttime,senttime,endtime,responsetime(ms),rid,timeout,birthepoc,url\n")
for tp in sorted_d:
  item = tp[1]
  f.write("%s,%s,%s,%s,%s,%s,%s,%s,\"%s\"\n" %( item["target"],item["starttime"],item["senttime"],item["endtime"],str(int(item["diff"])),item["rid"],str(item["timeout"]),str(item["birth"]),str(item["url"])))
f.close()
print "\nThe detailed log are stored at: %s" %output
