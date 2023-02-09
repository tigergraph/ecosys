#!/usr/bin/env bash
set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
. vars.sh

cd ..
python3 -u batches.py ${TG_DATA_DIR} --cluster $@