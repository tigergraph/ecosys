# This file will run 1-hop, 2-hop, 3-hop, 6-hop, WCC, PageRank benchmark tests while monitoring CPU and Memory consumption.
# The mornitoring may degrade performance so do not run this script if testing for times!
# Test results are output to their respective files. Peak CPU/MEM usage will be printed as a line at the end of each file.

if [ -e graph500-1KNN-prof ]
then
  cp graph500-1KNN-prof graph500-1KNN-prof.bak; rm graph500-1KNN-prof
fi
tsar -i 1 -l -m --cpu --mem >> g500-1KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 1 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-1KNN >> graph500-1KNN-prof; rm g500-1KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-1KNN-prof >> graph500-1KNN-prof;

if [ -e graph500-2KNN-prof ]
then
  cp graph500-2KNN-prof graph500-2KNN-prof.bak; rm graph500-2KNN-prof
fi
tsar -l -m --cpu --mem >> g500-2KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 2 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-2KNN >> graph500-2KNN-prof; rm g500-2KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-2KNN-prof >> graph500-2KNN-prof;

if [ -e graph500-3KNN-prof ]
then
  cp graph500-3KNN-prof graph500-3KNN-prof.bak; rm graph500-3KNN-prof
fi
tsar -l -m --cpu --mem >> g500-3KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  10 tigergraph 3 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-3KNN >> graph500-3KNN-prof; rm g500-3KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-3KNN-prof >> graph500-3KNN-prof;

if [ -e graph500-6KNN-prof ]
then
  cp graph500-6KNN-prof graph500-6KNN-prof.bak; rm graph500-6KNN-prof
fi
tsar -l -m --cpu --mem >> g500-6KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  10 tigergraph 6 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-6KNN >> graph500-6KNN-prof; rm g500-6KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-6KNN-prof >> graph500-6KNN-prof;

if [ -e graph500-WCC-prof ]
then
  cp graph500-WCC-prof graph500-WCC-prof.bak; rm graph500-WCC-prof
fi
tsar -l -m --cpu --mem >> g500-WCC & sleep 5;
nohup python wcc.py graph500-22 tigergraph 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-WCC >> graph500-WCC-prof; rm g500-WCC
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-WCC-prof >> graph500-WCC-prof;

if [ -e graph500-PG-prof ]
then
  cp graph500-PG-prof graph500-PG-prof.bak; rm graph500-PG-prof
fi
tsar -l -m --cpu --mem >> g500-PG & sleep 5;
nohup python pg.py graph500-22 tigergraph 10 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-PG >> graph500-PG-prof; rm g500-PG
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-PG-prof >> graph500-PG-prof
