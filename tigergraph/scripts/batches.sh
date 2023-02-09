#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

. scripts/vars.sh

if [ ! -d "${TG_DATA_DIR}" ]; then
    echo "Directory ${TG_DATA_DIR} does not exist."
    exit 1
fi

python3 -u batches.py ${TG_DATA_DIR} --endpoint ${TG_ENDPOINT}
