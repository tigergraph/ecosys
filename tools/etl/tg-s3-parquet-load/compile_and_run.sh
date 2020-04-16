#!/bin/bash

set -ex
# get current path
cwd=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)
cd $cwd

properties_file="$cwd/s3.properties"

master_node="local"
target_host="local"
target_path="$cwd/uber_data"
times=1
log_path=""
if [[ $# < 3 ]]; then
  echo "Usage: ./compile_and_run.sh  master_url target_host target_path [times] [log_path]"
  exit 1
fi

master_node=$1
target_host=$2
target_path=$3

if [[ $# > 3 ]]; then
  times=$4
fi

# specify log path, otherwise will use default: /tmp/sparkLoader
if [[ $# > 4 ]]; then
  log_path=$5
fi

cd graphsql
javac -cp $cwd/third_party/*:.  generateDataset.java
cd -

jar -cf graphsql-uber.jar graphsql/ third_party/

# spark cluster run in standalone mode.
spark-submit --class graphsql.generateDataset --master $master_node \
    --jars $cwd/third_party/aws-java-sdk-1.7.4.jar,$cwd/third_party/hadoop-aws-2.7.3.jar,$cwd/third_party/java-json.jar \
    --executor-memory 6g \
    graphsql-uber.jar $properties_file $target_host $target_path $times $log_path
