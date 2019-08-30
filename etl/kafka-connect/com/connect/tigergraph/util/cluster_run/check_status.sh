#!/bin/bash
port=`grep "rest.port" "tigergraphSinkConnector.properties" | awk -F"=" '{print $2}'`
curl -X GET "localhost:$port/connectors/tiger-sink/status"
echo ""
