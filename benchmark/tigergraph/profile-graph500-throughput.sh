# 
# Follow 
#  https://github.com/alibaba/tsar to install tsar first, which is used to profiling 
# mem and cpu
#
# This file profile the peak memory and cpu usage for k-hop throughput benchmark.
# usage: nohup  file.sh path_to_seed
#
#check log existence
if [ -e graph500-1KNN-prof ]
then
  cp graph500-1KNN-prof graph500-1KNN-prof.bak; rm graph500-1KNN-prof
fi
#invoke tsar per 1 second
tsar -i 1 -l -m --cpu --mem >> g500-1KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 1 notes throughput
#post processing tsar log, find peak memory and cpu
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-1KNN >> graph500-1KNN-prof; rm g500-1KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-1KNN-prof >> graph500-1KNN-prof;

#repeat for k=2
if [ -e graph500-2KNN-prof ]
then
  cp graph500-2KNN-prof graph500-2KNN-prof.bak; rm graph500-2KNN-prof
fi
tsar -l -m --cpu --mem >> g500-2KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 2 notes throughput
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-2KNN >> graph500-2KNN-prof; rm g500-2KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-2KNN-prof >> graph500-2KNN-prof;
#repeat for k=3
if [ -e graph500-3KNN-prof ]
then
  cp graph500-3KNN-prof graph500-3KNN-prof.bak; rm graph500-3KNN-prof
fi
tsar -l -m --cpu --mem >> g500-3KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 3 notes latency
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-3KNN >> graph500-3KNN-prof; rm g500-3KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-3KNN-prof >> graph500-3KNN-prof;
#repeat for k=6
if [ -e graph500-6KNN-prof ]
then
  cp graph500-6KNN-prof graph500-6KNN-prof.bak; rm graph500-6KNN-prof
fi
tsar -l -m --cpu --mem >> g500-6KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 6 notes latency
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-6KNN >> graph500-6KNN-prof; rm g500-6KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-6KNN-prof >> graph500-6KNN-prof;
#repeat for k=9
if [ -e graph500-9KNN-prof ]
then
  cp graph500-9KNN-prof graph500-9KNN-prof.bak; rm graph500-9KNN-prof
fi
tsar -l -m --cpu --mem >> g500-9KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 9 notes latency
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-9KNN >> graph500-9KNN-prof; rm g500-9KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-9KNN-prof >> graph500-9KNN-prof;
#repeat for k=12
if [ -e graph500-12KNN-prof ]
then
  cp graph500-12KNN-prof graph500-12KNN-prof.bak; rm graph500-12KNN-prof
fi
tsar -l -m --cpu --mem >> g500-12KNN & sleep 5;
nohup python kn.py $1/graph500-22-seed  300 tigergraph 12 notes latency
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' g500-12KNN >> graph500-12KNN-prof; rm g500-12KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' graph500-12KNN-prof >> graph500-12KNN-prof;




#repeat for k=6
#nohup python kn.py $1/graph500-22-seed  300 tigergraph 1 notes throughput
#nohup python kn.py $1/graph500-22-seed  300 tigergraph 2 notes throughput
#nohup python kn.py $1/graph500-22-seed  300 tigergraph 3 notes throughput
#nohup python kn.py $1/graph500-22-seed  300 tigergraph 6 notes throughput
#nohup python kn.py $1/graph500-22-seed  300 tigergraph 9 notes throughput
#nohup python kn.py $1/graph500-22-seed  300 tigergraph 12 notes throughput

