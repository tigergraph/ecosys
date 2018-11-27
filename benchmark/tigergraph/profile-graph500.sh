# This file will run 1-hop, 2-hop, 3-hop, 6-hop, WCC, PageRank benchmark tests while monitoring CPU and Memory consumption.
# The mornitoring may degrade performance so do not run this script if testing for times!
# Test results are output to their respective files. Peak CPU/MEM usage will be printed as a line at the end of each file.
# File Usage
#   nohup ./filname.sh

# K=1 nearest neighbor 
# If logfile already exists, back up the existing logfile
if [ -e graph500-1KNN-prof ]
then
  cp graph500-1KNN-prof graph500-1KNN-prof.bak; rm graph500-1KNN-prof
fi
# Invoke tsar - recording at 1 second intervals - wait 5 seconds before beginning next command to get baseline CPU/MEM with nothing running 
tsar -i 1 -l -m --cpu --mem >> g500-1KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 1 notes latency;
# Kills the tsar process once the benchmark process has completed
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
# Format file to remove text from impeding our retreival of data
sed '/Time/d' g500-1KNN >> graph500-1KNN-prof; rm g500-1KNN
# Returns peak CPU and peak Memory usage during the benchmark task peformed above 
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-1KNN-prof >> graph500-1KNN-prof;

# k=2 Nearest Neighbor
if [ -e graph500-2KNN-prof ]
then
  cp graph500-2KNN-prof graph500-2KNN-prof.bak; rm graph500-2KNN-prof
fi
# Tsar recording at default 5 second intervals
tsar -l -m --cpu --mem >> g500-2KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 2 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-2KNN >> graph500-2KNN-prof; rm g500-2KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-2KNN-prof >> graph500-2KNN-prof;

# k=3 Nearest Neighbor
if [ -e graph500-3KNN-prof ]
then
  cp graph500-3KNN-prof graph500-3KNN-prof.bak; rm graph500-3KNN-prof
fi
tsar -l -m --cpu --mem >> g500-3KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  10 tigergraph 3 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-3KNN >> graph500-3KNN-prof; rm g500-3KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-3KNN-prof >> graph500-3KNN-prof;

# k=6 Nearest Neighbor
if [ -e graph500-6KNN-prof ]
then
  cp graph500-6KNN-prof graph500-6KNN-prof.bak; rm graph500-6KNN-prof
fi
tsar -l -m --cpu --mem >> g500-6KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  10 tigergraph 6 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-6KNN >> graph500-6KNN-prof; rm g500-6KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-6KNN-prof >> graph500-6KNN-prof;

# WCC test
if [ -e graph500-WCC-prof ]
then
  cp graph500-WCC-prof graph500-WCC-prof.bak; rm graph500-WCC-prof
fi
tsar -l -m --cpu --mem >> g500-WCC & sleep 5;
nohup python wcc.py graph500-22 tigergraph 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-WCC >> graph500-WCC-prof; rm g500-WCC
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-WCC-prof >> graph500-WCC-prof;

# PageRank test
if [ -e graph500-PG-prof ]
then
  cp graph500-PG-prof graph500-PG-prof.bak; rm graph500-PG-prof
fi
tsar -l -m --cpu --mem >> g500-PG & sleep 5;
nohup python pg.py graph500-22 tigergraph 10 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-PG >> graph500-PG-prof; rm g500-PG
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-PG-prof >> graph500-PG-prof
