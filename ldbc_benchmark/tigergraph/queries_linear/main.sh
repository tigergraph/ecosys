#!/bin/bash
query_list=""
cd queries
for q in $(ls | sort --version-sort| cut -d. -f1)
do
  if [ -z "$query_list" ]
    then query_list=$q
    else query_list="$query_list,$q"
  fi
  gsql $q.gsql
done
echo $query_list
drop_command="drop query $query_list"
install_command="install query $query_list"
cd ..

drop(){
  echo $drop_command
  gsql -g ldbc_snb $drop_command
}

install(){
  echo $install_command
  gsql -g ldbc_snb $install_command
}

run(){
mkdir log err
for q in $(echo $query_list | tr ',' ' ')
do
  command=$(grep $q seed.txt | awk -F"$q:" '{print $2}')  
  echo $command
  time -p (gsql -g ldbc_snb $command) > log/$q 2> err/$q
  sleep 5
  time -p (gsql -g ldbc_snb $command) > /dev/null 2>> err/$q
  sleep 5
  time -p (gsql -g ldbc_snb $command) > /dev/null 2>> err/$q
  sleep 5
done
}

install
run
#echo "run queries in background"
#( trap "true" HUP ; run ) > nohup.out 2>/dev/null </dev/null & disown
