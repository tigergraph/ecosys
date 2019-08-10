#!/bin/bash

# called by make_aio_gsql_client.sh

SRC=$1   # path of the gsql client code: com/tigergraph
BRN=$2   # source branch: should be sth like tg_2.4.0_dev
VSTR=$3  # version_string: should be sth like v2_4_0

#Step 0:  add this version in AIO driver
cat <<EOT >> com/tigergraph/client/Driver.java
            if ( i==1 ) {
                Supported_Versions = Supported_Versions + "$VSTR ";
            }
            if ( ( i==1 && Gsql_Client_Version.equalsIgnoreCase("$VSTR") ) ||
                 ( i==2 && (!Gsql_Client_Version.equalsIgnoreCase("$VSTR")) )){
                try {
                    System.out.println("trying $VSTR");
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
cd $SRC; 
git fetch --all -p
git checkout $BRN
git clean -df
git reset --hard origin/$BRN
git pull
client_commit="\"$(git log -1 --pretty="format:%H" -- com/tigergraph/client/ com/tigergraph/common/)\""
cd -

#Step 3: Copy source code to target
cp -r $SRC/com/tigergraph/client $VSTR
cp -r $SRC/com/tigergraph/common $VSTR


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
fi

