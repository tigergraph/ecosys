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
rm -rf com/tigergraph/*
mkdir -p com/tigergraph/client
cat <<EOT >> com/tigergraph/client/Driver.java
package com.tigergraph.client;
import java.io.OutputStream;

public class Driver {
    public static void main(String[] args) {
        String Gsql_Client_Version= System.getenv("GSQL_CLIENT_VERSION");
        if (Gsql_Client_Version == null) {
            Gsql_Client_Version = System.getProperty("GSQL_CLIENT_VERSION");
        }
        if (Gsql_Client_Version == null) {
            Gsql_Client_Version = "";
        }
        // do two loops: 1st to try the given Gsql_Client_Version;
        // 2nd is to try each one except the given Gsql_Client_Version
        String Supported_Versions="";
        for (int i = 1; i <= 2; i++) {
            if ( i==2) {
                System.out.println( "Supported Versions ( " + Supported_Versions +")");
                System.out.println( "You may use 'GSQL_CLIENT_VERSION=v? java ...' or \n    'java -DGSQL_CLIENT_VERSION=v? ...' to specify the version");
            }
EOT
########################################################################
# 2. add each version. Make sure we can CD to the first GLE directory. #
########################################################################
cd $GLE_DIR; git checkout tg_2.5.0_dev; cd -
#                      client path,             branch,       version_string in combined client
./new_gsql_version.sh $GLE_DIR/src/main/java/   tg_2.5.2_dev    v2_5_2
./new_gsql_version.sh $GLE_DIR/src/main/java/   tg_2.5.0_dev    v2_5_0
./new_gsql_version.sh $GLE_DIR                  tg_2.4.1_dev    v2_4_1
./new_gsql_version.sh $GLE_DIR                  tg_2.4.0_dev    v2_4_0
./new_gsql_version.sh $GLE_DIR                  tg_2.3.2_dev    v2_3_2
######################################
# 3. finish Driver.java endi section #
######################################
cat <<EOT >> com/tigergraph/client/Driver.java
        }
     } // end main
}
EOT

# Continue with " mvn package "
