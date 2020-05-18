#!/bin/bash

# called by make_aio_gsql_client.sh

GLE=$1   # path to GLE
BRN=$2   # source branch: should be sth like tg_2.4.0_dev
VSTR=$3  # version_string: should be sth like v2_4_0
# is_tag: true if $BRN is a tag
if [ ! -z $4 ]; then
  IS_TAG=$4
else
  IS_TAG=false
fi

#Step 0:  add this version in AIO driver
cat <<EOT >> com/tigergraph/client/Driver.java
            if ( i==1 ) {
                Supported_Versions = Supported_Versions + "$VSTR ";
            }
            if ( ( i==1 && Gsql_Client_Version.equalsIgnoreCase("$VSTR") ) ||
                 ( i==2 && (!Gsql_Client_Version.equalsIgnoreCase("$VSTR")) )){
                try {
                    System.out.println("========================");
                    System.out.println("Trying version: $VSTR");
                    com.tigergraph.$VSTR.client.Driver.main(args);
                } catch (SecurityException e) {
                    ;
                }
            }
EOT

#Step 1: clean up and then create the target dir
cd com/tigergraph
rm -rf $VSTR;  mkdir $VSTR

#Step 2: switch the source to correct branch (release). Get client commit
cd $GLE;
git fetch --all -p
if [ $IS_TAG = true ]; then
  # checkout from tags
  git checkout tags/$BRN
else
  git checkout $BRN
fi
git clean -df
git reset --hard $BRN
if [ $IS_TAG = false ]; then
  # clean and pull only if it's a branch  
  git pull
fi
PKG_CLIENT=com/tigergraph/client
PKG_COMMON=com/tigergraph/common
if [ -d gsql-client ]; then
  # for 3.x
  SRC_CLIENT=gsql-client/src/main/java/$PKG_CLIENT
  SRC_COMMON=gsql-common/src/main/java/$PKG_COMMON
elif [ -d src/main/java ]; then
  # for 2.5.x and newer
  SRC_CLIENT=src/main/java/$PKG_CLIENT
  SRC_COMMON=src/main/java/$PKG_COMMON
else
  # for older versions
  SRC_CLIENT=$PKG_CLIENT
  SRC_COMMON=$PKG_COMMON
fi
client_commit="\"$(git log -1 --pretty="format:%H" -- $SRC_CLIENT $SRC_COMMON)\""
cd -

#Step 3: Copy source code to target
cp -r $GLE/$SRC_CLIENT $VSTR
cp -r $GLE/$SRC_COMMON $VSTR


#Step 4: fix source code
cd $VSTR
if [ "$(uname)" == "Darwin" ]; then
    # On Mac, sed need to have empty string '' after -i 
    #4.1: fix package name with VSTR
    LC_ALL=C find . -type f -name '*.java'      -exec sed -i '' "s/com.tigergraph.c/com.tigergraph.$VSTR.c/" {} + 
    LC_ALL=C find . -type f -name '*.java'      -exec sed -i '' "s/com.tigergraph.c/com.tigergraph.$VSTR.c/" {} +
    #4.2: embed client_commit gotten from Step 2
    LC_ALL=C find . -type f -name 'Util.java'   -exec sed -i '' "s/.*clientCommitHash.*=.*null.*/  if (true) return $client_commit; String clientCommitHash = null;/" {} +
    #4.3: replace System.exit() to SecurityException()  -- so we will try the next client version
    LC_ALL=C find . -type f -name 'Client.java' -exec sed -i '' "s/.*ReturnCode.LOGIN_OR_AUTH_ERROR.*/      throw new SecurityException();/" {} +
    #4.4: remove System.exit(ReturnCode.UNKNOWN_ERROR) 
    LC_ALL=C find . -type f -name 'Driver.java' -exec sed -i '' "s/.*ReturnCode.UNKNOWN_ERROR.*//" {} +
    #4.5: exit if license expires
    LC_ALL=C find . -type f -name 'Client.java' -exec sed -i '' "s/.*System.out.print(json.optString(\"message\"));.*/    if (json != null) {System.out.print(json.optString(\"message\")); if (json.optString(\"message\").contains(\"License expired\")){ System.exit(ReturnCode.LOGIN_OR_AUTH_ERROR);} }/" {} +
else
    #4.1: fix package name with VSTR
    LC_ALL=C find . -type f -name '*.java'      -exec sed -i    "s/com.tigergraph.c/com.tigergraph.$VSTR.c/" {} + 
    LC_ALL=C find . -type f -name '*.java'      -exec sed -i    "s/com.tigergraph.c/com.tigergraph.$VSTR.c/" {} +
    #4.2: embed client_commit gotten from Step 2
    LC_ALL=C find . -type f -name 'Util.java'   -exec sed -i    "s/.*clientCommitHash.*=.*null.*/  if (true) return $client_commit; String clientCommitHash = null;/" {} +
    #4.3: replace System.exit() to SecurityException()  -- so we will try the next client version
    LC_ALL=C find . -type f -name 'Client.java' -exec sed -i    "s/.*ReturnCode.LOGIN_OR_AUTH_ERROR.*/      throw new SecurityException();/" {} +
    #4.4: remove System.exit(ReturnCode.UNKNOWN_ERROR) 
    LC_ALL=C find . -type f -name 'Driver.java' -exec sed -i    "s/.*ReturnCode.UNKNOWN_ERROR.*//" {} +
    #4.5: exit if license expires
    LC_ALL=C find . -type f -name 'Client.java' -exec sed -i    "s/.*System.out.print(json.optString(\"message\"));.*/    if (json != null) {System.out.print(json.optString(\"message\")); if (json.optString(\"message\").contains(\"License expired\")){ System.exit(ReturnCode.LOGIN_OR_AUTH_ERROR);} }/" {} +
fi

