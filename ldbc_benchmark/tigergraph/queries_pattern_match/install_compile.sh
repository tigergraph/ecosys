#!/bin/bash

mkdir ./GSQL2
python3 query_convert.py

for query in ./GSQL2/*
do
	queryName="${query:8:-5}"
	#queryName="is_1"
	queryName2="./GSQL2/$queryName.gsql"
	#dropStr = "drop query $is_1"
	gsql -g ldbc_snb "drop query $queryName"
	gsql -g ldbc_snb $queryName2
	gsql -g ldbc_snb "install query $queryName"
	
	#echo $queryName
	#queryName=${echo "${a::-5}"}
	#gsql -g ldbc_snb ""
done	

rm -rf GSQL2
