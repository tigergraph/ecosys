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

gsql -g ldbc_snb "run loading job load_ldbc_snb using 
v_person_file=\"${LDBC_SNB_DATA_DIR}/person${LDBC_SNB_DATA_POSTFIX}\",
v_post_file=\"${LDBC_SNB_DATA_DIR}/post${LDBC_SNB_DATA_POSTFIX}\", 
v_tag_file=\"${LDBC_SNB_DATA_DIR}/tag${LDBC_SNB_DATA_POSTFIX}\", 
v_place_file=\"${LDBC_SNB_DATA_DIR}/place${LDBC_SNB_DATA_POSTFIX}\",
v_comment_file=\"${LDBC_SNB_DATA_DIR}/comment${LDBC_SNB_DATA_POSTFIX}\", 
v_forum_file=\"${LDBC_SNB_DATA_DIR}/forum${LDBC_SNB_DATA_POSTFIX}\", 
v_organisation_file=\"${LDBC_SNB_DATA_DIR}/organisation${LDBC_SNB_DATA_POSTFIX}\", 
v_tagclass_file=\"${LDBC_SNB_DATA_DIR}/tagclass${LDBC_SNB_DATA_POSTFIX}\",

person_knows_person_file=\"${LDBC_SNB_DATA_DIR}/person_knows_person${LDBC_SNB_DATA_POSTFIX}\", 
comment_replyOf_post_file=\"${LDBC_SNB_DATA_DIR}/comment_replyOf_post${LDBC_SNB_DATA_POSTFIX}\", 
comment_replyOf_comment_file=\"${LDBC_SNB_DATA_DIR}/comment_replyOf_comment${LDBC_SNB_DATA_POSTFIX}\", 
post_hasCreator_person_file=\"${LDBC_SNB_DATA_DIR}/post_hasCreator_person${LDBC_SNB_DATA_POSTFIX}\", 
post_hasTag_tag_file=\"${LDBC_SNB_DATA_DIR}/post_hasTag_tag${LDBC_SNB_DATA_POSTFIX}\", 
comment_hasCreator_person_file=\"${LDBC_SNB_DATA_DIR}/comment_hasCreator_person${LDBC_SNB_DATA_POSTFIX}\", 
post_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/post_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\", 
comment_hasTag_tag_file=\"${LDBC_SNB_DATA_DIR}/comment_hasTag_tag${LDBC_SNB_DATA_POSTFIX}\", 
comment_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/comment_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\", 
forum_containerOf_post_file=\"${LDBC_SNB_DATA_DIR}/forum_containerOf_post${LDBC_SNB_DATA_POSTFIX}\", 
forum_hasMember_person_file=\"${LDBC_SNB_DATA_DIR}/forum_hasMember_person${LDBC_SNB_DATA_POSTFIX}\", 
forum_hasModerator_person_file=\"${LDBC_SNB_DATA_DIR}/forum_hasModerator_person${LDBC_SNB_DATA_POSTFIX}\", 
forum_hasTag_tag_file=\"${LDBC_SNB_DATA_DIR}/forum_hasTag_tag${LDBC_SNB_DATA_POSTFIX}\", 
organisation_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/organisation_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\",
person_hasInterest_tag_file=\"${LDBC_SNB_DATA_DIR}/person_hasInterest_tag${LDBC_SNB_DATA_POSTFIX}\", 
person_isLocatedIn_place_file=\"${LDBC_SNB_DATA_DIR}/person_isLocatedIn_place${LDBC_SNB_DATA_POSTFIX}\", 
person_likes_comment_file=\"${LDBC_SNB_DATA_DIR}/person_likes_comment${LDBC_SNB_DATA_POSTFIX}\", 
person_likes_post_file=\"${LDBC_SNB_DATA_DIR}/person_likes_post${LDBC_SNB_DATA_POSTFIX}\", 
person_studyAt_organisation_file=\"${LDBC_SNB_DATA_DIR}/person_studyAt_organisation${LDBC_SNB_DATA_POSTFIX}\", 
person_workAt_organisation_file=\"${LDBC_SNB_DATA_DIR}/person_workAt_organisation${LDBC_SNB_DATA_POSTFIX}\", 
place_isPartOf_place_file=\"${LDBC_SNB_DATA_DIR}/place_isPartOf_place${LDBC_SNB_DATA_POSTFIX}\",
tag_hasType_tagclass_file=\"${LDBC_SNB_DATA_DIR}/tag_hasType_tagclass${LDBC_SNB_DATA_POSTFIX}\", 
tagclass_isSubclassOf_tagclass_file=\"${LDBC_SNB_DATA_DIR}/tagclass_isSubclassOf_tagclass${LDBC_SNB_DATA_POSTFIX}\"" 
