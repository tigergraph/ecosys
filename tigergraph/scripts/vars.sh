pushd . > /dev/null

cd "$( cd "$( dirname "${BASH_SOURCE[0]:-${(%):-%x}}" )" >/dev/null 2>&1 && pwd )"
cd ..

export TG_VERSION=3.7.0
export TG_CONTAINER_NAME=snb-bi-tg
export TG_DDL_DIR=`pwd`/ddl
export TG_QUERIES_DIR=`pwd`/queries
export TG_DML_DIR=`pwd`/dml
export TG_REST_PORT=9000
export TG_SSH_PORT=14022
export TG_WEB_PORT=14240
export TG_ENDPOINT=http://127.0.0.1:${TG_REST_PORT}
export TG_PARAMETER=../parameters/parameters-sf${SF}

popd > /dev/null
