# Follow
#  https://github.com/alibaba/tsar to install tsar first, which is used to profiling
# mem and cpu
#
# This file profile the peak memory and cpu usage for k-hop throughput benchmark.
# usage: nohup  file.sh path_to_seed

# Use the following command to change Restpp.timeout_seconds to  3600
#  gadmin --configure timeout_seconds
#
# Use the following two commands to apply the config change.
#  gadmin config-apply
#  gadmin restart
#
#check log existence
if [ -e twitter-1KNN-prof ]
then
  cp twitter-1KNN-prof twitter-1KNN-prof.bak; rm twitter-1KNN-prof
fi
#invoke tsar per 1 second
tsar  -l -m --cpu --mem >> twitter-1KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300  tigergraph 1 notes throughput
#post processing tsar log, find peak memory and cpu
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-1KNN >> twitter-1KNN-prof; rm twitter-1KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-1KNN-prof >> twitter-1KNN-prof;
#repeat for k=2
if [ -e twitter-2KNN-prof ]
then
  cp twitter-2KNN-prof twitter-2KNN-prof.bak; rm twitter-2KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-2KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300  tigergraph 2 notes throughput
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-2KNN >> twitter-2KNN-prof; rm twitter-2KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-2KNN-prof >> twitter-2KNN-prof;
#repeat for k=3
if [ -e twitter-3KNN-prof ]
then
  cp twitter-3KNN-prof twitter-3KNN-prof.bak; rm twitter-3KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-3KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300  tigergraph 3 notes throughput
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-3KNN >> twitter-3KNN-prof; rm twitter-3KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-3KNN-prof >> twitter-3KNN-prof;
#repeat for k=6
if [ -e twitter-6KNN-prof ]
then
  cp twitter-6KNN-prof twitter-6KNN-prof.bak; rm twitter-6KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-6KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300  tigergraph 6 notes throughput
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-6KNN >> twitter-6KNN-prof; rm twitter-6KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-6KNN-prof >> twitter-6KNN-prof;
#repeat for k=9
if [ -e twitter-9KNN-prof ]
then
  cp twitter-9KNN-prof twitter-9KNN-prof.bak; rm twitter-9KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-9KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300  tigergraph 9 notes throughput
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-9KNN >> twitter-9KNN-prof; rm twitter-9KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-9KNN-prof >> twitter-9KNN-prof;
#repeat for k=12
if [ -e twitter-12KNN-prof ]
then
  cp twitter-12KNN-prof twitter-12KNN-prof.bak; rm twitter-12KNN-prof
fi
tsar -l -m --cpu --mem >> twitter-12KNN & sleep 5;
nohup python kn.py $1/twitter_rv.net-seed  300  tigergraph 12 notes throughput
ps -ef | grep tsar | grep -v grep | awk '{print $2}' | xargs kill;
sed '/Time/d' twitter-12KNN >> twitter-12KNN-prof; rm twitter-12KNN
awk 'NR == 1 {max = $7; maxtoo=$13} $7>max {max=$7} $13>maxtoo {maxtoo = $13} END {print "max cpu: " max, " max mem: " maxtoo}' twitter-12KNN-prof >> twitter-12KNN-prof;
