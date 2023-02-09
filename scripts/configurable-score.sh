#!/usr/bin/env bash

set -eu
set -o pipefail

TOOL=${1}
SF=${2}

rm -f bi.duckdb
python3 ../scoring/calculate-scores.py --tool ${TOOL} --timings_dir ../${TOOL}/output/output-sf${SF}/ --throughput_min_time ${THROUGHPUT_MIN_TIME}
