#!/usr/bin/env bash

set -euo pipefail

: ${LDBC_SNB_DATA_DIR:=/home/tigergraph/ldbc_snb_data/social_network}
: ${LDBC_SNB_DATA_POSTFIX:=_0_0.csv}

gsql -g ldbc_snb "
RUN LOADING JOB load_ldbc_snb_composite_merged_fk USING
  file_Comment=\"${LDBC_SNB_DATA_DIR}/Comment${LDBC_SNB_DATA_POSTFIX}\",
  file_Comment_hasTag_Tag=\"${LDBC_SNB_DATA_DIR}/Comment_hasTag_Tag${LDBC_SNB_DATA_POSTFIX}\",
  file_Forum=\"${LDBC_SNB_DATA_DIR}/Forum${LDBC_SNB_DATA_POSTFIX}\",
  file_Forum_hasMember_Person=\"${LDBC_SNB_DATA_DIR}/Forum_hasMember_Person${LDBC_SNB_DATA_POSTFIX}\",
  file_Forum_hasTag_Tag=\"${LDBC_SNB_DATA_DIR}/Forum_hasTag_Tag${LDBC_SNB_DATA_POSTFIX}\",
  file_Organisation=\"${LDBC_SNB_DATA_DIR}/Organisation${LDBC_SNB_DATA_POSTFIX}\",
  file_Person=\"${LDBC_SNB_DATA_DIR}/Person${LDBC_SNB_DATA_POSTFIX}\",
  file_Person_hasInterest_Tag=\"${LDBC_SNB_DATA_DIR}/Person_hasInterest_Tag${LDBC_SNB_DATA_POSTFIX}\",
  file_Person_knows_Person=\"${LDBC_SNB_DATA_DIR}/Person_knows_Person${LDBC_SNB_DATA_POSTFIX}\",
  file_Person_likes_Comment=\"${LDBC_SNB_DATA_DIR}/Person_likes_Comment${LDBC_SNB_DATA_POSTFIX}\",
  file_Person_likes_Post=\"${LDBC_SNB_DATA_DIR}/Person_likes_Post${LDBC_SNB_DATA_POSTFIX}\",
  file_Person_studyAt_University=\"${LDBC_SNB_DATA_DIR}/Person_studyAt_University${LDBC_SNB_DATA_POSTFIX}\",
  file_Person_workAt_Company=\"${LDBC_SNB_DATA_DIR}/Person_workAt_Company${LDBC_SNB_DATA_POSTFIX}\",
  file_Place=\"${LDBC_SNB_DATA_DIR}/Place${LDBC_SNB_DATA_POSTFIX}\",
  file_Post=\"${LDBC_SNB_DATA_DIR}/Post${LDBC_SNB_DATA_POSTFIX}\",
  file_Post_hasTag_Tag=\"${LDBC_SNB_DATA_DIR}/Post_hasTag_Tag${LDBC_SNB_DATA_POSTFIX}\",
  file_TagClass=\"${LDBC_SNB_DATA_DIR}/TagClass${LDBC_SNB_DATA_POSTFIX}\",
  file_Tag=\"${LDBC_SNB_DATA_DIR}/Tag${LDBC_SNB_DATA_POSTFIX}\"
"
