#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

. scripts/vars.sh

echo "==============================================================================="
echo "Loading the TIGERGRAPH database"
echo "-------------------------------------------------------------------------------"
echo "TG_VERSION: ${TG_VERSION}"
echo "TG_CONTAINER_NAME: ${TG_CONTAINER_NAME}"
echo "==============================================================================="

find ${TG_DATA_DIR} -name _SUCCESS -delete
find ${TG_DATA_DIR} -name "*.crc" -delete
find ${TG_DATA_DIR} -name "*.csv.gz"  -print0 | parallel -q0 gunzip

docker exec --user tigergraph ${TG_CONTAINER_NAME} bash -c "export PATH=/home/tigergraph/tigergraph/app/cmd:\$PATH; cd /ddl; ./setup.sh"
