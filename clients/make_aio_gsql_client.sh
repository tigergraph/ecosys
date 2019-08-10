#!/bin/bash
GLE_DIR=$1
if [ "$#" -ne 1 ]; then
    echo "Illegal number of parameters: need GLE src path"
    exit 1
fi

# how to add a new version:
# make sure GLE repo is available.
# 1. Add 2 lines in this file, section 2.
# 2. Change pom.xml with the new version


#######################################
# 1. create Driver.java begin section #
#######################################
mkdir -p com/tigergraph/client
rm -rf com/tigergraph/client/Driver.java
echo "package com.tigergraph.client;
import java.io.OutputStream;

public class Driver {
    public static void main(String[] args) {
" >> com/tigergraph/client/Driver.java

########################################################################
# 2. add each version. Make sure we can CD to the first GLE directory. #
########################################################################
cd $GLE_DIR; git checkout tg_2.5.0_dev; cd -
#                      client path,             branch,       version_string in combined client
./new_gsql_version.sh $GLE_DIR/src/main/java/   tg_2.5.0_dev    v2_5_0
./new_gsql_version.sh $GLE_DIR                  tg_2.4.1_dev    v2_4_1
./new_gsql_version.sh $GLE_DIR                  tg_2.4.0_dev    v2_4_0
./new_gsql_version.sh $GLE_DIR                  tg_2.3.2_dev    v2_3_2

######################################
# 3. finish Driver.java endi section #
######################################
echo "     } // end main
}"    >> com/tigergraph/client/Driver.java

# Contuine with " mvn package "
