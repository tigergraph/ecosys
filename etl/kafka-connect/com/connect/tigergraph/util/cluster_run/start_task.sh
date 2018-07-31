#!/bin/bash
if [ ! -f "$1" ]
then
  echo "Cannot find the input config file from the given path \'$1\', exit."
  exit 0
fi

source $1
curl -X POST -H "Content-Type: application/json" "localhost:$restPort/connectors" --data "{\"name\":\"tiger-sink\", \"config\": {\"connector.class\":\"com.connect.tigergraph.TigerGraphSinkConnector\", \"flush_batch_size\":\"$flushBatchSize\",\"tasks.max\":\"$maxTask\", \"topics\":\"$topicName\", \"host_list\":\"$hostList\",\"loading_jobname\":\"$loadingJobName\"}}"
echo ""
