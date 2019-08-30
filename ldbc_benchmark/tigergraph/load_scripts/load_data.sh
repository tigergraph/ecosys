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

gsql -g ldbc "run loading job load_ldbc_edge using 
person_file=\"${TGT_DATA_ROOT_FOLDER}/person\", 
person_mail_file=\"${TGT_DATA_ROOT_FOLDER}/person_email_emailaddress\", 
person_speak_file=\"${TGT_DATA_ROOT_FOLDER}/person_speaks_language\", 
post_file=\"${TGT_DATA_ROOT_FOLDER}/post\", 
tag_file=\"${TGT_DATA_ROOT_FOLDER}/tag\", 
city_file=\"${TGT_DATA_ROOT_FOLDER}/city\", 
country_file=\"${TGT_DATA_ROOT_FOLDER}/country\", 
continent_file=\"${TGT_DATA_ROOT_FOLDER}/continent\", 
comments_file=\"${TGT_DATA_ROOT_FOLDER}/comment\", 
forum_file=\"${TGT_DATA_ROOT_FOLDER}/forum\", 
organisation_file=\"${TGT_DATA_ROOT_FOLDER}/organisation\", 
tagclass_file=\"${TGT_DATA_ROOT_FOLDER}/tagclass\",

person_knows_person_file=\"${TGT_DATA_ROOT_FOLDER}/person_knows_person\", 
comments_replyOf_post_file=\"${TGT_DATA_ROOT_FOLDER}/comment_replyOf_post\", 
comments_replyOf_comments_file=\"${TGT_DATA_ROOT_FOLDER}/comment_replyOf_comment\", 
post_hasCreator_person_file=\"${TGT_DATA_ROOT_FOLDER}/post_hasCreator_person\", 
post_hasTag_tag_file=\"${TGT_DATA_ROOT_FOLDER}/post_hasTag_tag\", 
comments_hasCreator_person_file=\"${TGT_DATA_ROOT_FOLDER}/comment_hasCreator_person\", 
post_isLocatedIn_place_file=\"${TGT_DATA_ROOT_FOLDER}/post_isLocatedIn_place\", 
comments_hasTag_tag_file=\"${TGT_DATA_ROOT_FOLDER}/comment_hasTag_tag\", 
comment_isLocatedIn_place_file=\"${TGT_DATA_ROOT_FOLDER}/comment_isLocatedIn_place\", 
forum_containerOf_post_file=\"${TGT_DATA_ROOT_FOLDER}/forum_containerOf_post\", 
forum_hasMember_person_file=\"${TGT_DATA_ROOT_FOLDER}/forum_hasMember_person\", 
forum_hasModerator_person_file=\"${TGT_DATA_ROOT_FOLDER}/forum_hasModerator_person\", 
forum_hasTag_tag_file=\"${TGT_DATA_ROOT_FOLDER}/forum_hasTag_tag\", 
university_isLocatedIn_city_file=\"${TGT_DATA_ROOT_FOLDER}/university_isLocatedIn_city\", 
company_isLocatedIn_country_file=\"${TGT_DATA_ROOT_FOLDER}/company_isLocatedIn_Country\", 
person_hasInterest_tag_file=\"${TGT_DATA_ROOT_FOLDER}/person_hasInterest_tag\", 
person_isLocatedIn_place_file=\"${TGT_DATA_ROOT_FOLDER}/person_isLocatedIn_place\", 
person_likes_comments_file=\"${TGT_DATA_ROOT_FOLDER}/person_likes_comment\", 
person_likes_post_file=\"${TGT_DATA_ROOT_FOLDER}/person_likes_post\", 
person_studyAt_organisation_file=\"${TGT_DATA_ROOT_FOLDER}/person_studyAt_organisation\", 
person_workAt_organisation_file=\"${TGT_DATA_ROOT_FOLDER}/person_workAt_organisation\", 
city_isPartOf_country_file=\"${TGT_DATA_ROOT_FOLDER}/city_isPartOf_country\", 
country_isPartOf_continent_file=\"${TGT_DATA_ROOT_FOLDER}/country_isPartOf_continent\", 
tag_hasType_tagclass_file=\"${TGT_DATA_ROOT_FOLDER}/tag_hasType_tagclass\", 
tagclass_isSubclassOf_tagclass_file=\"${TGT_DATA_ROOT_FOLDER}/tagclass_isSubclassOf_tagclass\"" 
