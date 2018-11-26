# This file will run 1-hop, 2-hop, 3-hop, 6-hop, WCC, PageRank benchmark tests while monitoring CPU and Memory consumption.
# The mornitoring may degrade performance so do not run this script if testing for times!
# Test results are output to their respective files. Peak CPU/MEM usage will be printed as a line at the end of each file.

if [ -e twitter-1KNN-prof ]
then
  cp twitter-1KNN-prof twitter-1KNN-prof.bak; rm twitter-1KNN-prof
fi
tsar -i 1 -l -m --cpu --mem >> twitter-1KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300 tigergraph 1 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-1KNN >> twitter-1KNN-prof; rm twitter-1KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-1KNN-prof >> twitter-1KNN-prof;

if [ -e twitter-2KNN-prof ]
then
  cp twitter-2KNN-prof twitter-2KNN-prof.bak; rm twitter-2KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-2KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300 tigergraph 2 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-2KNN >> twitter-2KNN-prof; rm twitter-2KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-2KNN-prof >> twitter-2KNN-prof;

if [ -e twitter-3KNN-prof ]
then
  cp twitter-3KNN-prof twitter-3KNN-prof.bak; rm twitter-3KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-3KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  10 tigergraph 3 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-3KNN >> twitter-3KNN-prof; rm twitter-3KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-3KNN-prof >> twitter-3KNN-prof;

if [ -e twitter-6KNN-prof ]
then
  cp twitter-6KNN-prof twitter-6KNN-prof.bak; rm twitter-6KNN-prof
fi
tsar -l -m --cpu -mem >> twitter-6KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  10 tigergraph 6 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-6KNN >> twitter-6KNN-prof; rm twitter-6KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-6KNN-prof >> twitter-6KNN-prof;

if [ -e twitter-WCC-prof ]
then
  cp twitter-WCC-prof twitter-WCC-prof.bak; rm twitter-WCC-prof
fi
tsar -l -m --cpu --mem >> twitter-WCC & sleep 5;
nohup python wcc.py twitter-rv tigergraph 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-WC >> twitter-6KNN-prof; rm twitter-WC
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-WCC-prof >> twitter-WCC-prof;

if [ -e twitter-PG-prof ]
then
  cp twitter-PG-prof twitter-PG-prof.bak; rm twitter-PG-prof
fi
tsar -l -m --cpu --mem >> twitter-PG & sleep 5;
python pg.py twitter-rv tigergraph 10 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-PG >> twitter-PG-prof; rm twitter-PG
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-PG-prof >> twitter-PG-prof;
