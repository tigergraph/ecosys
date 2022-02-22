#!/bin/bash

############################################################
# Copyright (c)  2015-now, TigerGraph Inc.
# All rights reserved
# It is provided as it is for benchmark reproducible purpose.
# anyone can use it for benchmark purpose with the
# acknowledgement to TigerGraph.
# Author: Litong Shen litong.shen@tigergraph.com
############################################################

# export LDBC_SNB_DATA_DIR=/home/tigergraph/ldbc_snb_data/social_network/
# export LDBC_SNB_DATA_POSTFIX=_0_0.csv

# for backward compatibility of pre-generated data, check whether a subdir static exists
# and assume the data is generated from the latest version of generator if it exists
if [ -d $LDBC_SNB_DATA_DIR/static ]; then
  SUB_DIR_STATIC=static
  SUB_DIR_DYNAMIC=dynamic
fi

gsql -g ldbc_snb "run loading job load_ldbc_snb using 
v_person_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person${LDBC_SNB_DATA_POSTFIX}\",
v_post_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/post${LDBC_SNB_DATA_POSTFIX}\",
v_tag_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/tag${LDBC_SNB_DATA_POSTFIX}\",
v_place_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/place${LDBC_SNB_DATA_POSTFIX}\",
v_comment_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/comment${LDBC_SNB_DATA_POSTFIX}\",
v_forum_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/forum${LDBC_SNB_DATA_POSTFIX}\",
v_organisation_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/organisation${LDBC_SNB_DATA_POSTFIX}\",
v_tagclass_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/tagclass${LDBC_SNB_DATA_POSTFIX}\",

person_knows_person_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person_knows_person${LDBC_SNB_DATA_POSTFIX}\",
comment_replyOf_post_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/comment_replyOf_post${LDBC_SNB_DATA_POSTFIX}\",
comment_replyOf_comment_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/comment_replyOf_comment${LDBC_SNB_DATA_POSTFIX}\",
post_hasCreator_person_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/post_hasCreator_person${LDBC_SNB_DATA_POSTFIX}\",
post_hasTag_tag_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/post_hasTag_tag${LDBC_SNB_DATA_POSTFIX}\",
comment_hasCreator_person_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/comment_hasCreator_person${LDBC_SNB_DATA_POSTFIX}\",
post_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/post_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\",
comment_hasTag_tag_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/comment_hasTag_tag${LDBC_SNB_DATA_POSTFIX}\",
comment_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/comment_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\",
forum_containerOf_post_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/forum_containerOf_post${LDBC_SNB_DATA_POSTFIX}\",
forum_hasMember_person_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/forum_hasMember_person${LDBC_SNB_DATA_POSTFIX}\",
forum_hasModerator_person_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/forum_hasModerator_person${LDBC_SNB_DATA_POSTFIX}\",
forum_hasTag_tag_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/forum_hasTag_tag${LDBC_SNB_DATA_POSTFIX}\",
organisation_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/organisation_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\",
person_hasInterest_tag_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person_hasInterest_tag${LDBC_SNB_DATA_POSTFIX}\",
person_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\",
person_likes_comment_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person_likes_comment${LDBC_SNB_DATA_POSTFIX}\",
person_likes_post_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person_likes_post${LDBC_SNB_DATA_POSTFIX}\",
person_studyAt_organisation_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person_studyAt_organisation${LDBC_SNB_DATA_POSTFIX}\",
person_workAt_organisation_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_DYNAMIC}/person_workAt_organisation${LDBC_SNB_DATA_POSTFIX}\",
place_isPartOf_place_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/place_isPartOf_place${LDBC_SNB_DATA_POSTFIX}\",
tag_hasType_tagclass_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/tag_hasType_tagclass${LDBC_SNB_DATA_POSTFIX}\",
tagclass_isSubclassOf_tagclass_file=\"${LDBC_SNB_DATA_DIR}/${SUB_DIR_STATIC}/tagclass_isSubclassOf_tagclass${LDBC_SNB_DATA_POSTFIX}\""

