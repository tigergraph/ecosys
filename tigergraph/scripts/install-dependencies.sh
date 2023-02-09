#!/usr/bin/env bash

set -eu
set -o pipefail

cd "$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd ..

pip3 install --user requests

if [[ ! -z $(which yum) ]]; then
    sudo yum install -y parallel
elif [[ ! -z $(which apt-get) ]]; then
    sudo apt-get update
    sudo apt-get install -y parallel
else
    echo "Operating system not supported, please install the dependencies manually"
fi
