#!/bin/bash

set -ex
# get current path
cwd=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)
cd $cwd

master_node="local"
ip=$1

if [[ $# < 1 ]]; then
  echo "Usage: ./run.sh tg_ip_address"
  exit 1
fi


mvn clean compile assembly:single


jar_file="sparkConnector-1.0-jar-with-dependencies.jar"

# spark cluster run in standalone mode.
#spark-submit --class com.tigergraph.sparkConnector.sparkConnector --master $master_node \
    #--executor-memory 6g target/$jar_file

cd target
java -cp $jar_file com.tigergraph.tgConnector.SparkConnector $ip
cd - > /dev/null
