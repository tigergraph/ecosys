#!/bin/bash

############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################

# TGT_DATA_ROOT_FOLDER is the final destination store your processed raw data
#export TGT_DATA_ROOT_FOLDER=/home/ubuntu/data/raw_data

# RAW_DATA_PATH is the path store your raw data currently
#export RAW_DATA_PATH=/home/ubuntu/data/social_network/

if [ -d "${TGT_DATA_ROOT_FOLDER}" ]; then rm -Rf ${TGT_DATA_ROOT_FOLDER}; fi
mkdir ${TGT_DATA_ROOT_FOLDER}


mkdir ${TGT_DATA_ROOT_FOLDER}/person_knows_person
find ${RAW_DATA_PATH} -name 'person_knows_person*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_knows_person \;
mkdir ${TGT_DATA_ROOT_FOLDER}/comment_replyOf_post
find ${RAW_DATA_PATH} -name 'comment_replyOf_post*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/comment_replyOf_post \;
mkdir ${TGT_DATA_ROOT_FOLDER}/comment_replyOf_comment
find ${RAW_DATA_PATH} -name 'comment_replyOf_comment*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/comment_replyOf_comment \;
mkdir ${TGT_DATA_ROOT_FOLDER}/post_hasCreator_person
find ${RAW_DATA_PATH} -name 'post_hasCreator_person*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/post_hasCreator_person \;
mkdir ${TGT_DATA_ROOT_FOLDER}/post_hasTag_tag
find ${RAW_DATA_PATH} -name 'post_hasTag_tag*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/post_hasTag_tag \;
mkdir ${TGT_DATA_ROOT_FOLDER}/comment_hasCreator_person
find ${RAW_DATA_PATH} -name 'comment_hasCreator_person*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/comment_hasCreator_person \;
mkdir ${TGT_DATA_ROOT_FOLDER}/post_isLocatedIn_place
find ${RAW_DATA_PATH} -name 'post_isLocatedIn_place*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/post_isLocatedIn_place \;
mkdir ${TGT_DATA_ROOT_FOLDER}/comment_hasTag_tag
find ${RAW_DATA_PATH} -name 'comment_hasTag_tag*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/comment_hasTag_tag \;
mkdir ${TGT_DATA_ROOT_FOLDER}/comment_isLocatedIn_place
find ${RAW_DATA_PATH} -name 'comment_isLocatedIn_place*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/comment_isLocatedIn_place \;
mkdir ${TGT_DATA_ROOT_FOLDER}/forum_containerOf_post
find ${RAW_DATA_PATH} -name 'forum_containerOf_post*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/forum_containerOf_post \;
mkdir ${TGT_DATA_ROOT_FOLDER}/forum_hasMember_person
find ${RAW_DATA_PATH} -name 'forum_hasMember_person*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/forum_hasMember_person \;
mkdir ${TGT_DATA_ROOT_FOLDER}/forum_hasModerator_person
find ${RAW_DATA_PATH} -name 'forum_hasModerator_person*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/forum_hasModerator_person \;
mkdir ${TGT_DATA_ROOT_FOLDER}/forum_hasTag_tag
find ${RAW_DATA_PATH} -name 'forum_hasTag_tag*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/forum_hasTag_tag \;
mkdir ${TGT_DATA_ROOT_FOLDER}/university_isLocatedIn_city
find ${RAW_DATA_PATH} -name 'university_isLocatedIn_city*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/university_isLocatedIn_city \;
mkdir ${TGT_DATA_ROOT_FOLDER}/company_isLocatedIn_Country
find ${RAW_DATA_PATH} -name 'company_isLocatedIn_Country*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/company_isLocatedIn_Country \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_hasInterest_tag
find ${RAW_DATA_PATH} -name 'person_hasInterest_tag*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_hasInterest_tag \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_isLocatedIn_place
find ${RAW_DATA_PATH} -name 'person_isLocatedIn_place*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_isLocatedIn_place \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_likes_comment
find ${RAW_DATA_PATH} -name 'person_likes_comment*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_likes_comment \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_likes_post
find ${RAW_DATA_PATH} -name 'person_likes_post*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_likes_post \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_studyAt_organisation
find ${RAW_DATA_PATH} -name 'person_studyAt_organisation*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_studyAt_organisation \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_workAt_organisation
find ${RAW_DATA_PATH} -name 'person_workAt_organisation*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_workAt_organisation \;
mkdir ${TGT_DATA_ROOT_FOLDER}/city_isPartOf_country
find ${RAW_DATA_PATH} -name 'city_isPartOf_country*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/city_isPartOf_country \;
mkdir ${TGT_DATA_ROOT_FOLDER}/country_isPartOf_continent
find ${RAW_DATA_PATH} -name 'country_isPartOf_continent*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/country_isPartOf_continent \;
mkdir ${TGT_DATA_ROOT_FOLDER}/tag_hasType_tagclass
find ${RAW_DATA_PATH} -name 'tag_hasType_tagclass*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/tag_hasType_tagclass \;
mkdir ${TGT_DATA_ROOT_FOLDER}/tagclass_isSubclassOf_tagclass
find ${RAW_DATA_PATH} -name 'tagclass_isSubclassOf_tagclass*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/tagclass_isSubclassOf_tagclass \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_email_emailaddress
find ${RAW_DATA_PATH} -name 'person_email_emailaddress*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_email_emailaddress \;
mkdir ${TGT_DATA_ROOT_FOLDER}/person_speaks_language
find ${RAW_DATA_PATH} -name 'person_speaks_language*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person_speaks_language \;

mkdir ${TGT_DATA_ROOT_FOLDER}/person
find ${RAW_DATA_PATH} -name 'person*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/person \;
mkdir ${TGT_DATA_ROOT_FOLDER}/post
find ${RAW_DATA_PATH} -name 'post*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/post \;
mkdir ${TGT_DATA_ROOT_FOLDER}/city
find ${RAW_DATA_PATH} -name 'city*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/city \;
mkdir ${TGT_DATA_ROOT_FOLDER}/country
find ${RAW_DATA_PATH} -name 'country*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/country \;
mkdir ${TGT_DATA_ROOT_FOLDER}/continent
find ${RAW_DATA_PATH} -name 'continent*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/continent \;
mkdir ${TGT_DATA_ROOT_FOLDER}/comment
find ${RAW_DATA_PATH} -name 'comment*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/comment \;
mkdir ${TGT_DATA_ROOT_FOLDER}/forum
find ${RAW_DATA_PATH} -name 'forum*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/forum \;
mkdir ${TGT_DATA_ROOT_FOLDER}/tagclass
find ${RAW_DATA_PATH} -name 'tagclass*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/tagclass \;
mkdir ${TGT_DATA_ROOT_FOLDER}/tag
find ${RAW_DATA_PATH} -name 'tag*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/tag \;
mkdir ${TGT_DATA_ROOT_FOLDER}/organisation
find ${RAW_DATA_PATH} -name 'organisation*.csv' -print -exec mv '{}' ${TGT_DATA_ROOT_FOLDER}/organisation \;

