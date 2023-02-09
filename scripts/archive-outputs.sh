#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

scripts/remove-gitignores-from-outputs.sh
for SYSTEM in cypher tigergraph umbra; do
    cd ${SYSTEM}
    zip -r output-${SYSTEM}.zip output/
    cd ..
done
scripts/restore-gitignores-in-outputs.sh
