pushd . > /dev/null

cd "$( cd "$( dirname "${BASH_SOURCE[0]:-${(%):-%x}}" )" >/dev/null 2>&1 && pwd )"
cd ..

export TG_REST_PORT=9000
export TG_ENDPOINT=http://127.0.0.1:${TG_REST_PORT}

# SF-100 benchmark
export NUM_NODES=72 # number of pods or nodes
export SF=30000 # data source 100, 1000, 3000, 10000 ...
export DOWNLOAD_THREAD=40 # number of download threads

export TG_DATA_DIR=$HOME/sf${SF}
export TG_PARAMETER=$HOME/parameters-sf${SF}

export DDL_PATH=`pwd`/ddl
export QUERY_PATH=`pwd`/queries
export DML_PATH=`pwd`/dml

popd > /dev/null
