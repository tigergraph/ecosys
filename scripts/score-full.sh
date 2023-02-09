#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ../scoring

if [ "${#}" -ne 2 ]; then
    echo "Usage: score-full.sh <tool> <sf>"
    exit 1
fi

# This script calculates the power and throughput scores for a full benchmark execution.
# This variant should be used for audit runs: by ensuring a minimum throughput time
# it guarantees that the calculated scores use a sufficiently long sample.

export THROUGHPUT_MIN_TIME=3600

../scripts/configurable-score.sh ${@}
