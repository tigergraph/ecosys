#!/bin/bash

set -ex
# get current path
cwd=$(cd $(dirname ${BASH_SOURCE[0]}) && pwd)
cd $cwd

specifyType=$1
graphName=$2
loadingJobName=$3
fileName=$4
separator=$5
endOfLineChar=$6
inputPath=$7
master_node=$8

javac -cp $cwd/third_party/*:. PostDataFromSpark.java 

jar -cf sparkConnectorSampleCode.jar ./ 

# spark cluster run in standalone mode.
spark-submit --class PostDataFromSpark --master $master_node \
    --jars $cwd/../restSampleCode/json.jar --executor-memory 6g \
    sparkConnectorSampleCode.jar $specifyType $graphName $loadingJobName $fileName $separator $endOfLineChar $inputPath
