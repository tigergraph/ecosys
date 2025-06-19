#!/bin/bash

if ! python --version 2>&1 | grep "Python 3\.1.\." >/dev/null; then
  echo "Python 3.11+ is needed, please check Python version or use virutal environment"
  exit 1
fi

if ! pip freeze 2>&1 | grep pyTigerGraph >/dev/null; then
  echo "pyTigerGraph is needed, please install it by running: pip install pyTigerGraph"
  exit 2
fi

echo "Initializing GraphRAG. It may take 5 to 10 minutes."
python ./init_graphrag.py

current_stage=
while True; do
  stage=$(docker logs eventual-consistency-service 2>&1 | grep "Processing Start\|DONE. graphrag.run" | tail -1)
  if [[ -n "$stage" && ! "$stage" == "$current_stage" ]]; then
    if [[ "$stage" =~ Processing ]]; then
      echo $stage | cut -d ' ' -f5-7
    elif [[ "$stage" =~ Done ]]; then
      echo "GraphRAG initialization is done."
      break
    fi
  fi
  current_stage=$stage
  sleep 5
done

python ./answer_question.sh
