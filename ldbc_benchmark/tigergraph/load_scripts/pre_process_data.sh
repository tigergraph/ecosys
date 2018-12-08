#!/bin/bash

############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################

#export RAW_DATA_PATH=/home/ubuntu/data/social_network/ #change to your raw data file
#export TOTAL_FILE_NUMBER=6 # numThreads specified in ldbc_snb_datagen/params.ini

echo "split vertrex place into city, country, continent"
g++ --std=c++11 SplitPlace.cpp -o SplitPlace
./SplitPlace ${RAW_DATA_PATH} ${TOTAL_FILE_NUMBER} 

echo "#####################################################"
echo "delete place_#_0.csv files"
i="0"
while [ $i -lt ${TOTAL_FILE_NUMBER} ]
do
  rm "${RAW_DATA_PATH}place_${i}_0.csv"
  i=$[$i+1]
done

echo "#####################################################"
echo "split edge organisation_isLocatedIn_place into:
	university_isLocatedIn_city, company_isLocatedIn_country"
echo "split edge place_isPartOf_place into: 
	city_isPartOf_country, country_isPartOf_continent"
g++ --std=c++11 SplitEdges.cpp -o SplitEdges
./SplitEdges ${RAW_DATA_PATH} ${TOTAL_FILE_NUMBER}

echo "#####################################################"
echo "delete organisation_isLocatedIn_place_#_0.csv and place_isPartOf_place_#_0.csv"

i="0"
while [ $i -lt ${TOTAL_FILE_NUMBER} ]
do
  rm "${RAW_DATA_PATH}organisation_isLocatedIn_place_${i}_0.csv"
  rm "${RAW_DATA_PATH}place_isPartOf_place_${i}_0.csv"
  i=$[$i+1]
done
