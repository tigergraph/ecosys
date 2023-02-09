#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

rm -f scratch/factors.duckdb
rm -rf ../parameters/parameters-sf${SF}
mkdir -p ../parameters/parameters-sf${SF}
python3 paramgen.py
