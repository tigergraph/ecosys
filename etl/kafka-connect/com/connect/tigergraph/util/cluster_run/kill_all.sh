#!/bin/bash
if [ ! -f "pid.log" ]
then
  echo "pid.log is not created, exit"
  exit 0
fi
curl -X DELETE 'localhost:8083/connectors/tiger-sink'
cat "pid.log" | xargs kill -9
