#!/bin/bash
display_usage() { 
echo "Usage: gsql_batch.sh -h   show help message. "
echo "       gsql_batch.sh <queries>"
echo "parse gsql files in batch and store query names in environmental variable $query_list." 
echo "The script also load functions: load, install and run"
echo "   Run SF10000 V2 queries: source gsql_batch.sh queries/*.gsql queries/SF10000/*.gsql"
echo "   Run SF10000 V2 IC queries: source gsql_batch.sh queries/ic*.gsql queries/SF10000/ic*.gsql" 
echo "   Run SF10000 V1 queries: source gsql_batch.sh queries_v1/*.gsql queries_v1/SF10000/*.gsql"
echo ""
echo "Usage for run: run <seed_file>"
echo "default seed_file is seed/seed_SF10000.txt"
echo ""
} 

if [[ ( $1 == "--help") ||  $1 == "-h" ]] 
then 
	display_usage
	exit 0
fi 

#pass all arguments to query variable
query=${@:-"queries/*.gsql queries/SF10000/*.gsql"}    
query_list=""

for f in $(ls $query | sort --version-sort) #$f is the path to file, i.e. queries/ic2.gsql
do
  q=${f%.gsql} #remove the extension .gsql
  q=${q##*/} #remove the directory prefix
  if [ -z "$query_list" ]
    then query_list=$q
    else query_list="$query_list,$q"
  fi
  gsql $f
done
echo $query_list


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

# Usage: run <seed>
run(){
seed=${1:-"seed/seed_SF10000.txt"}
mkdir -p log err
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
