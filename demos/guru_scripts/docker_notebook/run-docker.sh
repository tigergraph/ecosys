#!/bin/bash

# Copyright (c) 2019, TigerGraph Inc.
# All rights reserved.
#

cwd="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

help(){
  echo "`basename $0` [-h] [-e <TigerGraph edition> ] [-t <TigerGraph version>] [-n <Container name>] [-v <Volume mapping>]"
  echo "  -h  --  show this message"
  echo "  -e  --  TigerGraph edition [default: developer]"
  echo "  -t  --  TigerGraph version [default: latest]"
  echo "  -n  --  Container name [default: tigergraph_developer]"
  echo "  -v  --  Volume mapping, e.g., -v ~/data:/home/tigergraph/tigergraph/loadingData"
  exit 0
}

while getopts ":he:t:n:v:" opt; do
  case $opt in
    h)
      help
      ;;
    e)
      edition=$OPTARG
      ;;
    t)
      tag=$OPTARG
      ;;
    n)
      container_name=$OPTARG
      ;;
    v)
      volume_opt=$OPTARG
      ;;
  esac
done

edition=${edition:-developer}

if [[ "$edition" != "enterprise" ]]; then
  docker_image=tigergraph-dev
  container_name=${container_name:-tigergraph_developer}
else
  echo "Please visit https://www.tigergraph.com/download/ for more information"
fi
[[ -n "$tag" ]] && docker_image="${docker_image}:${tag}"
[[ -n "$volume_opt" ]] && VOLUME_MAPPING="-v $volume_opt"

if ! which docker > /dev/null; then
  echo "docker is not found!"
  exit 1
fi

if docker ps -a --filter name="^${container_name}\$" | grep "${container_name}" > /dev/null; then
  echo "Container name ${container_name} has been used, please set a different name"
  exit 2
fi

ssh_port=14022
gsql_port=9000
gst_port=14240
if docker ps | grep "${gst_port}\|${ssh_port}\|${gst_port}" > /dev/null; then
  read -p "Default port is already in use, assign different ports? (y/N) " use_custom
  if [[ "$use_custom" = "y" ]]; then
    printf "Please set the ports: ssh[14022] gsql[9000] graph studio[14240] separate by whitespace\n"
    printf "Press return directly to get them auto-assigned: "
    read -r ssh_port gsql_port gst_port
    if [[ -z "$ssh_port" || -z "$gsql_port" || -z "$gst_port" ]]; then
      echo "Some ports are not provided, use random ports instead."
    fi
  else
    exit 3
  fi
fi

echo "Pulling latest TigerGraph image"
docker pull docker.tigergraph.com/${docker_image}

echo "Running docker instance in deamon mode..."
docker run -d ${VOLUME_MAPPING} \
  -p ${ssh_port}:22 \
  -p ${gsql_port}:9000 \
  -p ${gst_port}:14240 \
  --name ${container_name} \
  --ulimit nofile=1000000:1000000 \
  -t docker.tigergraph.com/${docker_image}

if [[ $? -eq 0 ]]; then
  container=$(docker ps | grep -w docker.tigergraph.com/${docker_image} | head -1)
  [[ -z "$ssh_port" ]] && ssh_port=${container%%"->22"*} && ssh_port=${ssh_port##*:}
  if [[ $(uname) = "Darwin" ]]; then
    sed -i '' "/^\[localhost\]:$ssh_port/d" ~/.ssh/known_hosts
  else
    sed -i "/^\[localhost\]:$ssh_port/d" ~/.ssh/known_hosts
  fi
  echo "---------------------------------------------------"
  echo "Please run \"ssh tigergraph@localhost -p $ssh_port\" to login."
else
  echo "Failed to start docker container"
  exit 4
fi
