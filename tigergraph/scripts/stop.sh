#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

. scripts/vars.sh

echo -n "Stopping TG container ${TG_CONTAINER_NAME} ..."
(docker ps -a --format {{.Names}} | grep --quiet --word-regexp ${TG_CONTAINER_NAME}) && docker stop ${TG_CONTAINER_NAME} >/dev/null
echo " Stopped."

echo -n "Removing TG container ${TG_CONTAINER_NAME} ..."
(docker ps -a --format {{.Names}} | grep --quiet --word-regexp ${TG_CONTAINER_NAME}) && docker rm ${TG_CONTAINER_NAME} >/dev/null
echo " Removed."
