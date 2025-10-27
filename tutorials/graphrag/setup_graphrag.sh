#!/bin/bash

if ! which curl >/dev/null; then
  echo "cURL is not found, please install it and retry."
  exit 1
fi

if ! docker compose >/dev/null; then
  echo "Docker Compose plugin is not working properly, please resolve it and retry."
  echo "Refer to https://docs.docker.com/compose/install/linux/ for more installation instructions."
  exit 2
fi

if ! docker images | grep tigergraph/community >/dev/null; then
  echo "TigerGraph Community version docker image is not found, please download from https://dl.tigergraph.com/ and load it."
  exit 3
fi

tg_version=$(docker images | grep tigergraph/community | awk '{print $2}' | sort -Vr | head -1)
if [[ -z "$tg_version" || ! "$tg_version" =~ ^4\.[23]\. ]]; then
  echo "TigerGraph version is not compatible, please use 4.2.0+"
  exit 4
fi

root_dir=${1:-./graphrag}
tg_username=${2:-tigergraph}
tg_password=${3:-tigergraph}
mkdir -p $root_dir || true
[[ -d $root_dir ]] || (echo "Target dir $root_dir is not found!" && exit 5)

echo "Entering $root_dir..."
cd $root_dir || (echo "Cannot switch to $root_dir!" && exit 6)

echo "Downloading GraphRAG sevice config..."
mkdir -p configs || true
curl -s https://raw.githubusercontent.com/tigergraph/ecosys/refs/heads/master/tutorials/graphrag/docker-compose.yml | sed "s/community:4.2.1/community:${tg_version}/g" > docker-compose.yml
curl -s https://raw.githubusercontent.com/tigergraph/ecosys/refs/heads/master/tutorials/graphrag/configs/nginx.conf -o configs/nginx.conf
curl -s https://raw.githubusercontent.com/tigergraph/ecosys/refs/heads/master/tutorials/graphrag/configs/server_config.json | sed '/"gsPort": "14240"/a\
    "username": "'${tg_username}'",\
    "password": "'${tg_password}'",
' > configs/server_config.json

echo "Starting GraphRAG sevices.."
docker compose up -d
sleep 5

echo "Checking service status..."
if ! curl -s http://localhost:14240/restpp/version >/dev/null; then
  docker exec -it tigergraph /home/tigergraph/tigergraph/app/cmd/gadmin start all >/dev/null
  docker compose up -d >/dev/null
  sleep 5
fi

if ! docker ps | grep "tigergraph/graphrag:latest" >/dev/null; then
  echo "Failed to start GraphRAG service."
  echo 'Please double check tigergraph username and password in configs/server_config.json, and re-run `docker compose up -d`'
  echo 'Or check log via `docker logs graphrag` for detailed failure.'
else
  echo "GraphRAG service started successfully."
  echo "Visit http://localhost to access the chatbot."
fi
cd - >/dev/null

