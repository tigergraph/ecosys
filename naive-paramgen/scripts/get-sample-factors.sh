#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

export FACTOR_DIRNAME=social-network-sf0.003-bi-factors

rm -rf scratch/factors/
mkdir -p scratch/factors
rm -f ${FACTOR_DIRNAME}.zip
wget -q https://ldbcouncil.org/ldbc_snb_datagen_spark/${FACTOR_DIRNAME}.zip
rm -rf ${FACTOR_DIRNAME}
unzip -q ${FACTOR_DIRNAME}.zip
cp -r ${FACTOR_DIRNAME}/factors/parquet/raw/composite-merged-fk/* scratch/factors/
