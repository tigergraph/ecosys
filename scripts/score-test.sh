#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ../scoring

if [ "${#}" -ne 2 ]; then
    echo "Usage: score-test.sh <tool> <sf>"
    exit 1
fi

# This script calculates the power and throughput scores for a benchmark execution.
# It does so with a decreased minimum time for the total execution time of the throughput batches.
# This is for testing purposes and should not be used for actual runs.

export THROUGHPUT_MIN_TIME=60

../scripts/configurable-score.sh ${@}
