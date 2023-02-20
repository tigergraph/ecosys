#!/bin/bash

cd $(dirname $0)
BASE_DIR=$(pwd)

source_file() {
  file=$1
  msg="$2"
  src_flag=$3
  if [ -f "$file" ]; then
    if [ "$src_flag" != "false" ]; then
      source $file
    fi
  else
    echo $(tput setaf 1) "[ERROR   ]: $msg" $(tput sgr0)
    exit 1
  fi
}

# source all functions
source_file utils/pretty_print "File utils/pretty_print NOT found, exit" true
source_file utils/env_utils "File utils/env_utils NOT found, exit" true
source_file utils/ssl_utils "File utils/ssl_utils NOT found, exit" true

OSG=$(get_os)
OS=$(echo "$OSG" | cut -d' ' -f1)
version=$(echo "$OSG" | cut -d' ' -f2)
OSV="$OS$(echo "$version" | cut -d'.' -f1)"

generate_root=${BASE_DIR}/SSL_files
CN=kafka-0.tigergraph.com
storetype=jks
pass=tiger123

opt_string="hud:p:c:is:"
opt_long_string="help,usage,directory:,passphrase:,CN:,storetype:,install"
ARGS=`getopt -a -o $opt_string --long $opt_long_string -- "$@"`
if [ $? != 0 ] ; then exit 1 ; fi
eval set -- "${ARGS}"
while :
do
    case $1 in
        -h|--help)
            help
            exit 0
            ;;
        -u|--usage)
            usage
            exit 0
            ;;
        -d|--directory)
            generate_root=$2
            if [ ! -d ${generate_root} ]; then
              error "The path '$generate_root' does not exist"
              exit 1
            else
              generate_root=`path_conver $generate_root`/SSL_files
            fi
            shift
            ;;
        -p|--passphrase)
            pass=$2
            if [ ${#pass} -lt 6 ];then
              error "Password is too short - must be at least 6 characters."
              exit 1
            fi
            shift
            ;;
        -c|--CN)
            CN=$2
            shift
            ;;
        -s|--storetype)
            storetype=$2
            shift
            ;;
        -i|--install)
            SETUP_JDK=true
            SETUP_OPENSSL=true
            ;;
        --)
            shift
            break
            ;;
        *)
            error "${bldred}Invalid option, the correct usage is described below: $txtrst"
            help
            ;;
    esac
shift
done

# this script only support rhel/centos
prog "Checking operation system (OS) version ..."
check_os $OS $version

prog "Checking root/sudo ..."
check_root

# Using option '-i/--install' will install openjdk-1.8.0 and openssl,
# otherwise openjdk-1.8.0 and openssl will not be installed
# install openJDK
install_openJDK
# install openssl
install_openssl

# 1. generate CARoot and CA_key
rm -rf $generate_root
generate_CARoot $generate_root $CN $pass
CA=${generate_root}/ca-root.crt
CAkey=${generate_root}/ca-root.key

# 2. check CARoot and CA_key
check_cert $CA $CAkey $pass

# 3. generate keystore
generate_keystore ${generate_root} ${pass} ${CN} ${storetype} "server.keystore"
keystore=server.keystore

# 4. generate sub-certificate
generate_sub_cert $generate_root $CA $CAkey $pass $CN
subCA=${CN}.crt
subCA_key=${CN}.key

# 5. import CARoot to keystore
import_to_keystore ${keystore} "CARoot" ${CAkey} ${CA} ${pass} ${pass}

# 6. import sub-certificate to keystore
import_to_keystore ${keystore} "CARoot" ${CAkey} ${CA} ${pass} ${pass}
import_to_keystore ${keystore} ${CN} ${subCA_key} ${subCA} ${pass} ${pass}

# 7. generate truststore
generate_truststore ${generate_root} "server.truststore" "${pass}" "${storetype}"
truststore=server.truststore

# 8. import CARoot to truststore
import_to_truststore ${truststore} ${CA} "CARoot" ${pass}