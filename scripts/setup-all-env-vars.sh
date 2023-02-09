#!/usr/bin/env bash

#set -o nounset
#set -e

#scalefactor=0.003
#datagendir="/"

while [ $# -gt 0 ]; do
  if [[ $1 == *"--"* ]]; then
    param="${1/--/}"
    declare $param="$2"
    #echo $1 $2
  fi
  shift
done

#General
export SF=$scalefactor
export LDBC_SNB_DATAGEN_DIR=$datagendir

#Tiger
export TG_DATA_DIR=$datagendir/bi-sf$scalefactor-composite-projected-fk/graphs/csv/bi/composite-projected-fk/
#needs to be exported, even if empty
export TG_LICENSE=

#Neo
export NEO4J_CSV_DIR=$datagendir/out-sf$scalefactor/graphs/csv/bi/composite-projected-fk/
export NEO4J_ENV_VARS="${NEO4J_ENV_VARS-} --env NEO4J_dbms_memory_pagecache_size=20G --env NEO4J_dbms_memory_heap_max__size=20G"

#Umbra
export UMBRA_CSV_DIR=$datagendir/bi-sf$scalefactor-composite-merged-fk/graphs/csv/bi/composite-merged-fk/

echo Done.
