#!/bin/bash
###############################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# Author: yuchen.zhang@tigergraph.com
################################################

# machine number i, i is from [0, 1, ...23]
# nohup ./download_ldbc.sh i > foo.out 2>&1 < /dev/null &

#8 types, 4 types/line
static_types="organisation place tag tagclass 
organisation_isLocatedIn_place tagclass_isSubclassOf_tagclass tag_hasType_tagclass  place_isPartOf_place"
#23 types, 4 types/line
dynamic_types="person post comment forum 
forum_containerOf_post comment_hasCreator_person post_hasCreator_person person_hasInterest_tag
forum_hasMember_person forum_hasModerator_person comment_hasTag_tag post_hasTag_tag
forum_hasTag_tag  comment_isLocatedIn_place post_isLocatedIn_place person_isLocatedIn_place  
person_knows_person person_likes_comment person_likes_post comment_replyOf_comment
comment_replyOf_post person_studyAt_organisation person_workAt_organisation"

root=s3://ldbc-snb-datagen-store/results/params-csv-basic-sf10000/runs/20200606_131341/social_network
n=$1
s=$((n*101))
e=$((s+100))
des=ldbc_snb_6t
echo "copying to $des for node $n"
for t in $static_types; do
  for i in $(seq $s $e); do
    f=$des/$t/${t}_${i}_0.csv
    aws s3 cp $root/static/${t}_${i}_0.csv $f
    #remove files only has header
    if [ $(head $f | wc -l) -le 1 ]; then
      echo "remove $f"
      rm $f
    fi
  done
done

for t in $dynamic_types; do
  for i in $(seq $s $e); do
    f=$des/$t/${t}_${i}_0.csv
    aws s3 cp $root/dynamic/${t}_${i}_0.csv $f
    #remove files only has header
    if [ $(head $f | wc -l) -le 1 ]; then
      echo "remove $f"
      rm $f
    fi
  done
done


