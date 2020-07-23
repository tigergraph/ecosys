#!/bin/bash
display_usage() { 
echo "Usage: source main.sh <query> <scale_factor>."
echo "   query: queries to run (support regular expression, default '*.gsql'), Use ic* to run IC queries." 
echo "   scale_factor: 100 or 10000, default 10000." 
echo ""
} 
if [[ ( $1 == "--help") ||  $1 == "-h" ]] 
then 
	display_usage
	exit 0
fi 

query=${1:-*.gsql}    
SF=${2:-10000}  
seed=seed/seed_SF$SF.txt

#Parse the query in queries/*.gsql and queries/SF10000/*.gsql  
#Append query name to query_list
query_list=""
cd queries
for f in $(ls $query SF$SF/$query | sort --version-sort)
do
  q=${f%.gsql}
  q=${q##*/}
  if [ -z "$query_list" ]
    then query_list=$q
    else query_list="$query_list,$q"
  fi
  gsql $f
done
echo $query_list
cd ..

drop(){
  drop_command="drop query $query_list"
  echo $drop_command
  gsql -g ldbc_snb $drop_command
}

install(){
  install_command="install query $query_list"
  echo $install_command
  gsql -g ldbc_snb $install_command
}

run(){
mkdir log err
for q in $(echo $query_list | tr ',' ' ')
do
  command=$(grep $q $seed | awk -F"$q:" '{print $2}')  
  echo $command
  time -p (gsql -g ldbc_snb $command) > log/$q 2> err/$q
  sleep 5
  time -p (gsql -g ldbc_snb $command) > /dev/null 2>> err/$q
  sleep 5
  time -p (gsql -g ldbc_snb $command) > /dev/null 2>> err/$q
  sleep 5
done
}

#install
#run
#echo "run queries in background"
#( trap "true" HUP ; run ) > nohup.out 2>/dev/null </dev/null & disown
