#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

if [ "${#}" -ne 2 ]; then
    echo "Usage: cross-validate.sh <tool1> <tool2>"
    exit 1
fi

numdiff \
    --separators='\|\n;: ,{}[]' \
    --absolute-tolerance 0.001 \
    ${1}/output/output-sf${SF}/results.csv \
    ${2}/output/output-sf${SF}/results.csv
