# This file will run 1-hop, 2-hop, 3-hop, 6-hop, WCC, PageRank benchmark tests while monitoring CPU and Memory consumption.
# The mornitoring may degrade performance so do not run this script if testing for times!
# Test results are output to their respective files. Peak CPU/MEM usage will be printed as a line at the end of each file.
# File Usage
#   nohup ./filename.sh

# <path/to/seed/file> must be changed to the seed file path. e.g. /home/ubuntu/ecosys/benchmark/neo4j/twitter_rv.net-seed
# <path/to/data/file> must be changed to the raw data file path. e.g. /home/ubuntu/ecosys/benchmark/neo4j/twitter-rv

# k=1 Nearest neighbor
# If logfile already exists, back up the existing logfile
if [ -e twitter-1KNN-prof ]
then
  cp twitter-1KNN-prof twitter-1KNN-prof.bak; rm twitter-1KNN-prof
fi
# Invoke tsar - recording at 1 second intervals - wait 5 seconds befoer beginning next command to get baseline CPU/MEM with nothing running
tsar -i 1 -l -m --cpu --mem >> twitter-1KNN & sleep 5;
nohup python kn.py <path/to/seed/file> 300 tigergraph 1 notes latency;
# Kills the tsar process once the benchmark process has completed
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
# Format file to remove text from impeding our retrieval of data
sed '/Time/d' twitter-1KNN >> twitter-1KNN-prof; rm twitter-1KNN
# Returns peak CPU and peak Memory usage during the benchmark task performed above
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-1KNN-prof >> twitter-1KNN-prof;

# k=2 Nearest Neighbor
if [ -e twitter-2KNN-prof ]
then
  cp twitter-2KNN-prof twitter-2KNN-prof.bak; rm twitter-2KNN-prof
fi
# Tsar recording at default 5 second intervals
tsar -l -m --cpu --mem >> twitter-2KNN & sleep 5;
nohup python kn.py <path/to/seed/file>  300 tigergraph 2 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-2KNN >> twitter-2KNN-prof; rm twitter-2KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-2KNN-prof >> twitter-2KNN-prof;

# k=3 Nearest Neighbor
if [ -e twitter-3KNN-prof ]
then
  cp twitter-3KNN-prof twitter-3KNN-prof.bak; rm twitter-3KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-3KNN & sleep 5;
nohup python kn.py <path/to/seed/file>  10 tigergraph 3 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-3KNN >> twitter-3KNN-prof; rm twitter-3KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-3KNN-prof >> twitter-3KNN-prof;

# k=6 Nearest Neighbor
if [ -e twitter-6KNN-prof ]
then
  cp twitter-6KNN-prof twitter-6KNN-prof.bak; rm twitter-6KNN-prof
fi
tsar -l -m --cpu -mem >> twitter-6KNN & sleep 5;
nohup python kn.py <path/to/seed/file>  10 tigergraph 6 notes latency;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-6KNN >> twitter-6KNN-prof; rm twitter-6KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-6KNN-prof >> twitter-6KNN-prof;

# WCC test
if [ -e twitter-WCC-prof ]
then
  cp twitter-WCC-prof twitter-WCC-prof.bak; rm twitter-WCC-prof
fi
tsar -l -m --cpu --mem >> twitter-WCC & sleep 5;
nohup python wcc.py <path/to/data/file> tigergraph 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-WC >> twitter-6KNN-prof; rm twitter-WC
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-WCC-prof >> twitter-WCC-prof;

# PageRank test
if [ -e twitter-PG-prof ]
then
  cp twitter-PG-prof twitter-PG-prof.bak; rm twitter-PG-prof
fi
tsar -l -m --cpu --mem >> twitter-PG & sleep 5;
nohup python pg.py <path/to/data/file> tigergraph 10 3;
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-PG >> twitter-PG-prof; rm twitter-PG
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-PG-prof >> twitter-PG-prof
