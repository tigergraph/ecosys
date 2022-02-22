#!/bin/bash
query_list=""
cd queries
for f in ic_[0-9].gsql ic_[0-9][0-9].gsql bi_[0-9].gsql bi_[0-9][0-9].gsql
do
  q=$(echo $f | cut -d. -f1)
  if [ -z "$query_list" ]
    then query_list=$q
    else query_list="$query_list,$q"
  fi
  #gsql $f
done
echo $query_list
drop_command="drop query $query_list"
install_command="install query $query_list"
cd ..

drop(){
  gsql -g ldbc_snb $drop_command
}

install(){
  gsql -g ldbc_snb $install_command
}

run(){
for q in $(echo $query_list | tr ',' ' ')
do
  command=$(grep $q seed.txt | awk -F"$q:" '{print $2}')  
  echo $command
  #time -p (gsql -g ldbc_snb $command) >$q.out 2>$q.err
done
}
