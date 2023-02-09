#!/usr/bin/env bash
set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
. vars.sh
cd ..
python3 -u benchmark.py --cluster --scale_factor ${SF} --data_dir ${TG_DATA_DIR} --para ${TG_PARAMETER} --endpoint ${TG_ENDPOINT} $@