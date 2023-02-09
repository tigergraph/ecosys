#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

mkdir -p output/output-sf${SF}
scripts/load-in-one-step.sh |& tee output/output-sf${SF}/load.log
scripts/benchmark.sh |& tee output/output-sf${SF}/benchmark.log
scripts/stop.sh
