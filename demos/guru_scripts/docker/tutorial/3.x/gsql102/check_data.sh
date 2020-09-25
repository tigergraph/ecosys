#!/bin/bash

# sample script for checking graph size using built-in REST endpoint calls
curl -X POST 'http://localhost:9000/builtins/ldbc_snb' -d '{"function":"stat_vertex_number","type":"*"}' | jq .
curl -X POST 'http://localhost:9000/builtins/ldbc_snb' -d '{"function":"stat_edge_number","type":"*"}' | jq .
